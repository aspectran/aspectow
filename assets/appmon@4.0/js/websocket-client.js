/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * WebSocket implementation of the AppMon client.
 * In Gateway Mode, it manages a single physical connection for the entire cluster.
 *
 * @version 4.0
 * @last-modified 2026-05-22
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onSubscribed, onPrimary, onClosed, onFailed, isGatewayMode) {
        super(node, viewer, onSubscribed, onPrimary, onClosed, onFailed, isGatewayMode);
        this.heartbeatInterval = 50000;
        this.heartbeatTimer = null;
        this.socket = null;
        this.pendingMessages = [];
    }

    start(appsToSubscribe, nodeToSubscribe) {
        this.nodeToSubscribe = nodeToSubscribe;
        this.appsToSubscribe = appsToSubscribe;
        this.openSocket();
    }

    stop() {
        this.closeSocket();
    }

    openSocket() {
        this.closeSocket(false);
        const url = new URL(this.node.endpoint.path + "/appmon/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log("connecting to websocket:", url.href);
        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            console.log(this.node.id, "socket connected");
            this.pendingMessages.push("Socket connection successful");
            
            // Connect to the first node
            this.connect(this.node.id);
            this.sendPing();
            this.retryCount = 0;
        };

        this.socket.onmessage = (event) => {
            if (typeof event.data !== "string") return;
            const msg = event.data;
            const idx = msg.indexOf(':');
            if (idx === -1) {
                console.warn("Invalid message format received:", msg);
                return;
            }

            const nodeId = msg.substring(0, idx);
            const message = msg.substring(idx + 1);

            if (this.primary) {
                // Standard control messages
                if (message.startsWith(":pong:")) {
                    this.node.endpoint.token = message.substring(6);
                    this.sendPing();
                    return;
                }

                // Control messages in Gateway Mode
                if (this.isGatewayMode && message.startsWith(":subscribed:")) {
                    const alive = (message === ":subscribed:alive");
                    this.establish(nodeId, false, alive);
                    return;
                }

                // Data messages
                const viewer = this.getViewer(nodeId);
                if (viewer) {
                    viewer.processMessage(message);
                } else {
                    console.warn("No viewer registered for nodeId:", nodeId, "Message:", message);
                }
            } else if (message.startsWith(":subscribed:")) {
                const primary = message.startsWith(":subscribed:primary:");
                const alive = message.endsWith(":alive");
                this.establish(nodeId, primary, alive);
            } else {
                console.error("Unexpected message received before primary connection established:", message);
            }
        };

        this.socket.onclose = (event) => {
            this.closeSocket(true);
            if (this.onClosed) this.onClosed(this.node);
            if (event.code === 1003) {
                console.warn("Websocket connection refused: ", event.code);
                this.viewer.printErrorMessage("Socket connection refused by server.");
                return;
            }
            if (event.code === 1011) {
                console.log("Websocket connection closed: ", event.code);
                this.viewer.printErrorMessage("Websocket connection closed due to server error.");
                return;
            }
            if (event.code === 1000 || this.retryCount === 0) {
                console.log("Websocket connection closed: ", event.code);
                this.viewer.printMessage("Websocket connection closed.");
            }
            if (event.code !== 1000) {
                setTimeout(() => this.reconnect(), 1000);
            }
        };

        this.socket.onerror = (event) => {
            console.error(this.node.id, "websocket error:", event);
            this.viewer.printErrorMessage("Could not connect to the WebSocket server.");
            if (this.onFailed) this.onFailed(this.node);
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

    connect(nodeId) {
        const options = ["command:subscribe"];
        options.push("timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone);
        if (this.nodeToSubscribe) {
            options.push("nodeToSubscribe:" + this.nodeToSubscribe);
        }
        if (this.appsToSubscribe) {
            options.push("appsToSubscribe:" + this.appsToSubscribe);
        }
        this.sendCommand(options, nodeId);
    }

    establish(nodeId, primary, alive) {
        if (primary) {
            this.primary = true;
            this.primaryNodeId = nodeId;
        }

        const config = this.getNodeConfig(nodeId);
        if (config) {
            config.node.alive = !!alive;
            if (config.onSubscribed && !config.node.subscribed) {
                config.onSubscribed(config.node);
            }
        }

        const viewer = this.getViewer(nodeId);
        if (primary) {
            if (config && config.onPrimary) config.onPrimary(config.node);
            while (this.pendingMessages.length) {
                viewer.printMessage(this.pendingMessages.shift());
            }
            this.sendCommand([
                "command:established",
                "nodeToSubscribe:" + (this.nodeToSubscribe || "")
            ], nodeId);
        }
        if (!alive) {
            viewer.printErrorMessage("Node " + nodeId + " not alive");
        }
    }

    sendCommand(options, nodeId) {
        if (options && this.socket && this.socket.readyState === WebSocket.OPEN) {
            const arr = options.slice();
            arr.push("nodeId:" + (nodeId || this.primaryNodeId));
            const cmd = arr.join(";");
            console.log("send", cmd);
            this.socket.send(cmd);
        }
    }

    sendPing() {
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
