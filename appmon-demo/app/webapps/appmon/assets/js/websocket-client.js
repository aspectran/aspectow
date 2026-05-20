/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-05-10
 */

/**
 * WebSocket implementation of the AppMon client.
 * In Gateway Mode, it manages a single physical connection for the entire cluster.
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onSubscribed, onPrimary, onClosed, onFailed, isGatewayMode) {
        super(node, viewer, onSubscribed, onPrimary, onClosed, onFailed, isGatewayMode);
        this.heartbeatInterval = 5000;
        this.socket = null;
        this.heartbeatTimer = null;
        this.pendingMessages = [];
    }

    start(appsToSubscribe) {
        this.openSocket(appsToSubscribe);
    }

    stop() {
        this.closeSocket();
    }

    openSocket(appsToSubscribe) {
        this.closeSocket(false);
        const url = new URL(this.node.endpoint.path + "/appmon/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log("connect:", url.href);
        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            console.log(this.node.id, "socket connected");
            this.pendingMessages.push("Socket connection successful");
            
            // Connect to the first node
            this.connect(this.node.id, appsToSubscribe);
            this.heartbeatPing();
            this.retryCount = 0;
        };

        this.socket.onmessage = (event) => {
            if (typeof event.data !== "string") return;
            const msg = event.data;
            //console.log(msg);

            const idx = msg.indexOf(':');
            if (idx === -1) return;

            const nodeId = msg.substring(0, idx);
            const message = msg.substring(idx + 1);

            if (this.primary) {
                // Standard control messages
                if (message.startsWith(":pong:")) {
                    this.node.endpoint.token = message.substring(6);
                    this.heartbeatPing();
                    return;
                }

                // Control messages in Gateway Mode
                if (this.isGatewayMode && message.startsWith(":subscribed:")) {
                    const alive = (message === ":subscribed:alive");
                    this.primaryConnection(nodeId, false, alive);
                    return;
                }

                // Data messages
                if (this.isGatewayMode) {
                    const viewer = this.clusterViewers[nodeId];
                    if (viewer) {
                        viewer.processMessage(message);
                    } else {
                        console.warn("No viewer registered for nodeId:", nodeId, "Message:", message);
                    }
                } else {
                    this.viewer.processMessage(message);
                }
            } else if (message.startsWith(":subscribed:")) {
                const primary = (message === ":subscribed:established");
                this.primaryConnection(nodeId, primary, true);
            } else {
                console.error("Unexpected message received before primary connection established:", message);
            }
        };

        this.socket.onclose = (event) => {
            this.closeSocket(true);
            if (this.onClosed) {
                this.onClosed(this.node);
            }
            if (!event || event.code !== 1000) {
                this.reconnect(appsToSubscribe);
            }
        };

        this.socket.onerror = (event) => {
            console.log(this.node.id, "websocket error:", event);
            this.viewer.printErrorMessage("Could not connect to the WebSocket server.");
            if (this.onFailed) {
                this.onFailed(this.node);
            }
        };
    }

    closeSocket(afterClosing) {
        if (this.socket) {
            this.primary = false;
            if (!afterClosing) {
                this.socket.close();
            }
            this.socket = null;
        }
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    connect(nodeId, appsToSubscribe) {
        const options = ["command:subscribe"];
        options.push("timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone);
        if (appsToSubscribe) {
            options.push("appsToSubscribe:" + appsToSubscribe);
        }
        this.sendCommand(options, nodeId);
    }

    primaryConnection(nodeId, primary, alive) {
        if (primary) {
            this.primary = true;
            this.primaryNodeId = nodeId;
        }

        const config = this.isGatewayMode ? this.clusterNodes[nodeId] : this;
        if (config) {
            config.node.alive = !!alive;
            if (config.onSubscribed && !config.node.subscribed) {
                config.onSubscribed(config.node);
            }
        }

        const viewer = this.isGatewayMode ? this.clusterViewers[nodeId] : this.viewer;
        if (!alive) {
            viewer.printMessage("Node " + nodeId + " not alive");
        }
        if (primary) {
            if (config && config.onPrimary) config.onPrimary(config.node);
            while (this.pendingMessages.length) {
                viewer.printMessage(this.pendingMessages.shift());
            }
            this.sendCommand(["command:established"], nodeId);
        }
    }

    sendCommand(options, nodeId) {
        if (options && this.socket && this.socket.readyState === WebSocket.OPEN) {
            const arr = options.slice();
            arr.push("nodeId:" + (nodeId || this.primaryNodeId));
            const cmd = arr.join(";");
            console.log("command:", cmd);
            this.socket.send(cmd);
        }
    }

    heartbeatPing() {
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
        }
        this.heartbeatTimer = setTimeout(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send("command:ping");
            }
        }, this.heartbeatInterval);
    }
}
