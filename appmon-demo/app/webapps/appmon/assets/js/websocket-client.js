/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-05-10
 */

/**
 * WebSocket implementation of the AppMon client.
 * In Gateway Mode, it manages a single physical connection for the entire cluster.
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onJoined, onEstablished, onClosed, onFailed, isGatewayMode = false) {
        super(node, viewer, onJoined, onEstablished, onClosed, onFailed);
        this.endpointMode = "websocket";
        this.heartbeatInterval = 5000;
        this.socket = null;
        this.heartbeatTimer = null;
        this.pendingMessages = [];
        this.established = false;
        this.isGatewayMode = isGatewayMode;
        this.clusterViewers = {};
        this.clusterNodes = {};
    }

    addClusterViewer(nodeId, viewer) {
        this.clusterViewers[nodeId] = viewer;
    }

    addClusterNode(nodeId, node, onJoined, onEstablished) {
        this.clusterNodes[nodeId] = { node, onJoined, onEstablished };
    }

    start(appsToJoin) {
        this.openSocket(appsToJoin);
    }

    stop() {
        this.closeSocket();
    }

    sendCommand(options, targetNodeId) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            let cmd = options ? options.slice() : [];
            if (this.isGatewayMode && targetNodeId) {
                cmd.push("nodeId:" + targetNodeId);
            }
            console.log("cmd", cmd);
            this.socket.send(cmd.join(";"));
        }
    }

    openSocket(appsToJoin) {
        this.closeSocket(false);
        const url = new URL(this.node.endpoint.path + "/appmon/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log("Connecting to:", url.href);
        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            console.log(this.node.id, "socket connected");
            this.pendingMessages.push("Socket connection successful");
            
            // Join the first node
            const options = [
                "command:join",
                "timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone
            ];
            if (appsToJoin) {
                options.push("appsToJoin:" + appsToJoin);
            }
            this.socket.send(options.join(";"));
            this.heartbeatPing();
            this.retryCount = 0;
        };

        this.socket.onmessage = (event) => {
            if (typeof event.data !== "string") return;
            const msg = event.data;
            //console.log(msg);

            const idx = msg.indexOf(':');
            if (idx === -1) return;

            const firstPart = msg.substring(0, idx);
            const remaining = msg.substring(idx + 1);

            if (this.isEstablished()) {
                if (remaining.startsWith("pong:")) {
                    this.node.endpoint.token = remaining.substring(5);
                    this.heartbeatPing();
                } else {
                    const viewer = this.clusterViewers[firstPart];
                    if (viewer) {
                        viewer.processMessage(remaining);
                    } else {
                        console.warn("No viewer registered for nodeId:", firstPart, "Message:", remaining);
                    }
                }
            } else if (remaining.startsWith("joined:")) {
                // Control messages in Gateway Mode: [nodeId]:joined:[sessionId]
                console.log(this.node.id, remaining, this.node.endpoint.token);
                const payload = remaining.substring(7);
                this.establish(payload, firstPart);
            } else {
                console.error("Unexpected message received before establishment:", remaining);
            }
        };

        this.socket.onclose = (event) => {
            this.closeSocket(true);
            if (this.onClosed) {
                this.onClosed(this.node);
            }
            if (!event || event.code !== 1000) {
                this.rejoin(appsToJoin);
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
        this.clearSessionId();
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

    establish(payload, targetNodeId) {
        if (!this.established) {
            this.established = true;
            this.setSessionId(payload);
            while (this.pendingMessages.length) {
                this.viewer.printMessage(this.pendingMessages.shift());
            }
        }

        const nodeId = targetNodeId || this.node.id;
        const config = (this.isGatewayMode && this.clusterNodes[nodeId]) ? this.clusterNodes[nodeId] : this;

        if (config.onJoined) config.onJoined(config.node, payload);
        if (config.onEstablished) config.onEstablished(config.node);
        
        this.sendCommand(["command:established"], nodeId);
    }

    isEstablished() {
        return this.established;
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
