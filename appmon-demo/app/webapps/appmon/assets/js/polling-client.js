/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * HTTP Polling implementation of the AppMon client.
 */
class PollingClient extends BaseClient {
    constructor(node, viewer, onConnected, onEstablished, onClosed, onFailed, isGatewayMode = false) {
        super(node, viewer, onConnected, onEstablished, onClosed, onFailed, isGatewayMode);
        this.endpointMode = "polling";
        this.commands = [];
        this.pollingTimer = null;
        this.stopped = false;
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
        this.stopped = false;
        this.join(this.node.id, appsToJoin);
    }

    stop() {
        this.stopped = true;
        this.established = false;
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    connect(nodeId, appsToJoin) {
        // if (this.isGatewayMode && this.established && nodeId !== this.establishedNodeId) {
        //     this.sendCommand(["command:join"], nodeId);
        //     return;
        // }
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/join",
            type: "post",
            dataType: "json",
            data: {
                nodeId: nodeId,
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                appsToJoin: appsToJoin
            },
            success: (data) => {
                if (data) {
                    if (!this.established && !data.appsToJoin) {
                        alert("No verified apps found. Please check the configuration of the backend.");
                        return;
                    }

                    this.retryCount = 0;
                    this.node.endpoint['mode'] = this.endpointMode;
                    this.node.endpoint['pollingInterval'] = data.pollingInterval;

                    // Establish explicitly on successful join response for the first node
                    // (Subsequent nodes in gateway mode will receive ::joined: messages via polling)
                    //if (!this.established || nodeId === this.establishedNodeId) {
                         this.establish(data.nodeId);
                    //}

                    // if (data.messages) {
                    //     this.processMessages(data.messages);
                    // }

                    this.joinedNodes.forEach(node => {
                    });

                    if (!this.stopped) {
                        this.polling(appsToJoin);
                    }
                } else {
                    console.log(this.node.id, "connection failed");
                    this.viewer.printErrorMessage("Connection failed.");
                    this.reconnect(appsToJoin);
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "connection failed", error);
                this.viewer.printErrorMessage("Connection failed.");
                this.reconnect(appsToJoin);
            }
        });
    }

    polling(appsToJoin) {
        if (this.stopped) return;
        let withCommands = null;
        if (this.commands.length) {
            withCommands = this.commands.slice();
            this.commands.length = 0;
        }
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/pull",
            type: "get",
            cache: false,
            data: withCommands ? {
                "commands[]": withCommands
            } : null,
            success: (data) => {
                if (this.stopped) return;
                if (data && data.messages) {
                    this.processMessages(data.messages);
                    this.pollingTimer = setTimeout(() => {
                        this.polling(appsToJoin);
                    }, this.node.endpoint.pollingInterval);
                } else {
                    console.log(this.node.id, "connection lost");
                    this.viewer.printErrorMessage("Connection lost.");
                    if (this.onClosed) {
                        this.onClosed(this.node);
                    }
                    this.reconnect(appsToJoin);
                }
            },
            error: (xhr, status, error) => {
                if (this.stopped) return;
                console.log(this.node.id, "connection lost", error);
                this.viewer.printErrorMessage("Connection lost.");
                if (this.onClosed) {
                    this.onClosed(this.node);
                }
                this.reconnect(appsToJoin);
            }
        });
    }

    changePollingInterval(speed) {
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/interval",
            type: "post",
            dataType: "json",
            data: { speed: speed },
            success: (data) => {
                if (data && data.pollingInterval) {
                    this.node.endpoint.pollingInterval = data.pollingInterval;
                    console.log(this.node.id, "pollingInterval", data.pollingInterval);
                    this.viewer.printMessage("Polling every " + data.pollingInterval + " milliseconds.");
                } else {
                    console.log(this.node.id, "failed to change polling interval");
                    this.viewer.printMessage("Failed to change polling interval.");
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "failed to change polling interval", error);
                this.viewer.printMessage("Failed to change polling interval.");
            }
        });
    }

    processMessages(messages) {
        if (messages) {
            messages.forEach(msg => {
                console.log(msg);
                const idx = msg.indexOf(':');
                if (idx === -1) return;

                const nodeId = msg.substring(0, idx);
                const message = msg.substring(idx + 1);

                if (this.established) {
                    // Control messages in Gateway Mode
                    // if (this.isGatewayMode && message.startsWith(":joined:")) {
                    //     this.establish(nodeId);
                    //     return;
                    // }

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
                // } else if (message.startsWith(":joined:")) {
                //     const payload = message.substring(8);
                //     this.establish(nodeId, payload);
                } else {
                    console.error("Unexpected message received before establishment:", message);
                }
            });
        }
    }

    establish(nodeId) {
        if (!this.established) {
            this.established = true;
            this.establishedNodeId = nodeId;
        }

        this.joinedNodes.add(nodeId);

        const config = (this.isGatewayMode && this.clusterNodes[nodeId]) ? this.clusterNodes[nodeId] : this;
        if (config.onConnected) config.onConnected(config.node);

        if (this.established && this.establishedNodeId === nodeId) {
            if (config.onEstablished) config.onEstablished(config.node);
            this.viewer.printMessage("Polling every " + this.node.endpoint.pollingInterval + " milliseconds.");
        }
    }

    sendCommand(options, nodeId) {
        if (options) {
            let cmd = options ? options.slice() : [];
            const targetNodeId = nodeId || this.establishedNodeId;
            cmd.push("nodeId:" + targetNodeId);
            cmd.forEach(option => this.withCommand(option));
        }
    }

    withCommand(command) {
        if (!this.commands.includes(command)) {
            this.commands.push(command);
        }
    }
}
