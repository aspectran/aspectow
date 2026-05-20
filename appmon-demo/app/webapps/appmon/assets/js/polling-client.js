/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * HTTP Polling implementation of the AppMon client.
 */
class PollingClient extends BaseClient {
    constructor(node, viewer, onSubscribed, onPrimary, onClosed, onFailed, isGatewayMode = false) {
        super(node, viewer, onSubscribed, onPrimary, onClosed, onFailed, isGatewayMode);
        this.pendingCommands = [];
        this.pollingTimer = null;
        this.stopped = false;
    }

    addClusterViewer(nodeId, viewer) {
        this.clusterViewers[nodeId] = viewer;
    }

    addClusterNode(node, onSubscribed, onPrimary) {
        this.clusterNodes[node.id] = {node, onSubscribed, onPrimary};
    }

    start(appsToSubscribe) {
        this.stopped = false;
        this.connect(this.node.id, appsToSubscribe);
    }

    stop() {
        this.stopped = true;
        this.primary = false;
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    connect(nodeId, appsToSubscribe) {
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/subscribe",
            type: "post",
            dataType: "json",
            data: {
                nodeId: nodeId,
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                appsToSubscribe: appsToSubscribe
            },
            success: (data) => {
                if (data) {
                    if (data.established && !data.appsToSubscribe) {
                        console.warn("No verified apps found. Please check the configuration of the backend.");
                        return;
                    }

                    if (data.established) {
                        this.retryCount = 0;
                        this.node.endpoint['mode'] = "polling";
                        this.node.endpoint['pollingInterval'] = data.pollingInterval;
                    }

                     this.primaryConnection(data.nodeId, data.established, data.alive);

                    // if (data.messages) {
                    //     this.processMessages(data.messages);
                    // }

                    if (this.primary && !this.stopped) {
                        this.polling(data.appsToSubscribe);
                    }
                } else {
                    console.log(this.node.id, "connection failed");
                    this.viewer.printErrorMessage("Connection failed.");
                    this.reconnect(appsToSubscribe);
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "connection failed", error);
                this.viewer.printErrorMessage("Connection failed.");
                this.reconnect(appsToSubscribe);
            }
        });
    }

    polling(appsToSubscribe) {
        if (this.stopped) return;
        let commands = null;
        if (this.pendingCommands.length) {
            commands = this.pendingCommands.slice();
            this.pendingCommands.length = 0;
        }
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/pull",
            type: "post",
            cache: false,
            data: commands ? {
                "commands[]": commands
            } : null,
            success: (data) => {
                if (this.stopped) return;
                if (data && data.messages) {
                    this.processMessages(data.messages);
                    this.pollingTimer = setTimeout(() => {
                        this.polling(appsToSubscribe);
                    }, this.node.endpoint.pollingInterval);
                } else {
                    console.log(this.node.id, "connection lost");
                    this.viewer.printErrorMessage("Connection lost.");
                    if (this.onClosed) this.onClosed(this.node);
                    this.reconnect(appsToSubscribe);
                }
            },
            error: (xhr, status, error) => {
                if (this.stopped) return;
                console.log(this.node.id, "connection lost", error);
                this.viewer.printErrorMessage("Connection lost.");
                if (this.onClosed) this.onClosed(this.node);
                this.reconnect(appsToSubscribe);
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
                const idx = msg.indexOf(':');
                if (idx === -1) return;

                const nodeId = msg.substring(0, idx);
                const message = msg.substring(idx + 1);

                if (this.primary) {
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
                } else {
                    console.error("Unexpected message received before primary connection established:", message);
                }
            });
        }
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
            viewer.printMessage("Polling every " + this.node.endpoint.pollingInterval + " milliseconds.");
            this.sendCommand(["command:established"], nodeId);
        }
    }

    sendCommand(options, nodeId) {
        if (options) {
            let arr = options.slice();
            arr.push("nodeId:" + (nodeId || this.primaryNodeId));
            const cmd = arr.join(";");
            console.log("command:", cmd);
            if (!this.pendingCommands.includes(cmd)) {
                this.pendingCommands.push(cmd);
            }
        }
    }
}
