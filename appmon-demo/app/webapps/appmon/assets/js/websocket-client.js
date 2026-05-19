/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-05-10
 */

/**
 * WebSocket implementation of the AppMon client.
 * In Gateway Mode, it manages a single physical connection for the entire cluster.
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onConnected, onEstablished, onClosed, onFailed, isGatewayMode) {
        super(node, viewer, onConnected, onEstablished, onClosed, onFailed, isGatewayMode);
        this.endpointMode = "websocket";
        this.heartbeatInterval = 5000;
        this.socket = null;
        this.heartbeatTimer = null;
        this.pendingMessages = [];
        this.clusterViewers = {};
        this.clusterNodes = {};
        this.joinedNodes = new Set();
    }

    addClusterViewer(nodeId, viewer) {
        this.clusterViewers[nodeId] = viewer;
    }

    addClusterNode(nodeId, node, onConnected, onEstablished) {
        const config = { node, onConnected, onEstablished };
        this.clusterNodes[nodeId] = config;
        if (this.joinedNodes.has(nodeId)) {
            this.establish(nodeId);
        }
    }

    start(appsToJoin) {
        this.openSocket(appsToJoin);
    }

    stop() {
        this.closeSocket();
    }

    openSocket(appsToJoin) {
        this.closeSocket(false);
        const url = new URL(this.node.endpoint.path + "/appmon/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log("connect:", url.href);
        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            console.log(this.node.id, "socket connected");
            this.pendingMessages.push("Socket connection successful");
            
            // Connect to the first node
            this.connect(this.node.id, appsToJoin);
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

            if (this.established) {
                // Standard control messages
                if (message.startsWith(":pong:")) {
                    this.node.endpoint.token = message.substring(6);
                    this.heartbeatPing();
                    return;
                }

                // Control messages in Gateway Mode
                if (this.isGatewayMode && message.startsWith(":joined:")) {
                    this.establish(nodeId);
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
            } else if (message.startsWith(":joined:")) {
                this.establish(nodeId);
            } else {
                console.error("Unexpected message received before establishment:", message);
            }
        };

        this.socket.onclose = (event) => {
            this.closeSocket(true);
            if (this.onClosed) {
                this.onClosed(this.node);
            }
            if (!event || event.code !== 1000) {
                this.reconnect(appsToJoin);
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
            this.established = false;
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

    connect(nodeId, appsToJoin) {
        const options = ["command:join"];
        if (!this.established) {
            // Connect to the first node
            options.push("timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone);
            if (appsToJoin) {
                options.push("appsToJoin:" + appsToJoin);
            }
            this.sendCommand(options, nodeId);
        } else if (nodeId !== this.establishedNodeId) {
            // Connect to another node in Gateway mode
            this.sendCommand(options, nodeId);
        }
    }

    establish(nodeId) {
        if (!this.established) {
            this.established = true;
            this.establishedNodeId = nodeId;
        }

        this.joinedNodes.add(nodeId);

        const config = this.isGatewayMode ? this.clusterNodes[nodeId] : this;
        if (config && config.onConnected) {
            if (!config.node.connected) {
                config.onConnected(config.node);
            }
        }

        const thisNodeId = this.node.id;
        if (this.established && nodeId === thisNodeId) {
            if (config && config.onEstablished) config.onEstablished(config.node);
            while (this.pendingMessages.length) {
                this.viewer.printMessage(this.pendingMessages.shift());
            }
            this.sendCommand(["command:established"], nodeId);
        }
    }

    sendCommand(options, nodeId) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            let cmd = options ? options.slice() : [];
            const targetNodeId = nodeId || this.establishedNodeId;
            cmd.push("nodeId:" + targetNodeId);
            console.log("cmd", cmd);
            this.socket.send(cmd.join(";"));
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
