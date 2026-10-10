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
 * HTTP Polling implementation of the AppMon client.
 *
 * @version 4.3
 * @last-modified 2026-10-09
 */
class PollingClient extends BaseClient {
    constructor(primaryNodeId, node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode = false) {
        super(primaryNodeId, node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode);
        this.pendingCommands = [];
        this.pollingTimer = null;
        this.stopped = false;
        this.established = false;

        if (!this.isGatewayMode && this.node.port && (location.hostname === "localhost" || location.hostname === "127.0.0.1")) {
            const url = new URL(this.node.endpoint.path, location.href);
            url.port = this.node.port;
            this.node.endpoint.path = url.origin + url.pathname;
        }
    }

    start(appsToSubscribe, nodeToSubscribe) {
        this.stopped = false;
        this.nodeToSubscribe = nodeToSubscribe;
        this.appsToSubscribe = appsToSubscribe;
        this.subscribe();
    }

    stop() {
        super.stop();
        this.stopped = true;
        this.primary = false;
        this.primaryNodeId = null;
        this.established = false;
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    isConnected() {
        return !this.stopped && this.everConnected && !this.reconnecting;
    }

    onResume() {
        if (!this.stopped && this.reconnecting) {
            if (this.lastResumeReconnect && (Date.now() - this.lastResumeReconnect < 500)) {
                return;
            }
            this.lastResumeReconnect = Date.now();
            console.log(this.primaryNodeId, "PollingClient resumed while disconnected, reconnecting immediately");
            this.reconnect(true);
        }
    }

    subscribe(nodeId) {
        $.ajax({
            url: this.node.endpoint.path + "/appmon/polling/subscribe",
            type: "post",
            dataType: "json",
            data: {
                nodeId: nodeId,
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                nodeToSubscribe: this.nodeToSubscribe,
                appsToSubscribe: this.appsToSubscribe
            },
            success: (data) => {
                if (data) {
                    if (!data.appsToSubscribe) {
                        console.warn("No verified apps found. Please check the configuration of the backend.");
                        return;
                    }

                    this.everConnected = true;
                    this.reconnecting = false;
                    if (this.retryTimer) {
                        clearTimeout(this.retryTimer);
                        this.retryTimer = null;
                    }
                    this.retryCount = 0;
                    this.node.endpoint['mode'] = "polling";
                    this.node.endpoint['pollingInterval'] = data.pollingInterval;

                    if (this.isGatewayMode) {
                        for (let id in this.clusterNodes) {
                            this.establish(id, data.nodeAliveMap && data.nodeAliveMap[id]);
                        }
                    } else {
                        this.establish(this.primaryNodeId, true);
                    }

                    if (!this.stopped) {
                        this.appsToSubscribe = data.appsToSubscribe;
                        this.immediatePoll();
                    }
                } else {
                    console.log(this.node.id, "connection failed");
                    this.printErrorMessage("Connection failed.");
                    this.reconnect();
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "connection failed", error);
                this.printErrorMessage("Connection failed.");
                this.reconnect();
            }
        });
    }

    poll() {
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
                    const interval = this.node.endpoint.pollingInterval || 3000;
                    this.pollingTimer = setTimeout(() => {
                        this.poll();
                    }, interval);
                } else {
                    console.log(this.node.id, "connection lost");
                    this.printErrorMessage("Connection lost.");
                    this.notifyClosed();
                    this.reconnect();
                }
            },
            error: (xhr, status, error) => {
                if (this.stopped) return;
                if (commands && commands.length) {
                    this.pendingCommands.unshift(...commands);
                }
                console.log(this.node.id, "connection lost", error);
                this.printErrorMessage("Connection lost.");
                this.notifyClosed();
                this.reconnect();
            }
        });
    }

    immediatePoll() {
        if (this.pollingTimer) clearTimeout(this.pollingTimer);
        this.pollingTimer = setTimeout(() => this.poll(), 500);
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
                    if (this.pollingTimer) {
                        clearTimeout(this.pollingTimer);
                        this.pollingTimer = setTimeout(() => this.poll(), data.pollingInterval);
                    }
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

                if (this.isGatewayMode) {
                    if (message.startsWith(":node:joined:")) {
                        try {
                            const nodeInfo = JSON.parse(message.substring(13));
                            if (this.onNodeJoined) this.onNodeJoined(nodeInfo);
                        } catch (e) {
                            console.error("Failed to parse node:joined message:", message, e);
                        }
                        return;
                    }
                    if (message.startsWith(":node:statusChanged:")) {
                        try {
                            const nodeInfo = JSON.parse(message.substring(20));
                            if (this.onNodeStatusChanged) this.onNodeStatusChanged(nodeInfo);
                        } catch (e) {
                            console.error("Failed to parse node:statusChanged message:", message, e);
                        }
                        return;
                    }
                    if (message === ":node:left") {
                        if (this.onNodeLeft) this.onNodeLeft(nodeId);
                        return;
                    }
                }

                // Data messages
                const idx1 = message.indexOf(":");
                const idx2 = (idx1 !== -1 ? message.indexOf(":", idx1 + 1) : -1);
                const type = (idx1 !== -1 && idx2 !== -1) ? message.substring(idx1 + 1, idx2) : "";

                if (type === "metric" || type.startsWith("metric/")) {
                    if (this.metricsViewer) {
                        this.metricsViewer.processMessage(nodeId, message);
                    }
                } else {
                    const viewer = this.getViewer(nodeId);
                    if (viewer) {
                        viewer.processMessage(nodeId, message);
                    } else {
                        console.warn("No viewer registered for nodeId:", nodeId, "Message:", message);
                    }
                }
            });
        }
    }

    establish(nodeId, alive) {
        const primary = (nodeId === this.primaryNodeId);

        const config = this.getNodeConfig(nodeId);
        if (config) {
            config.node.alive = !!alive;
            if (config.onSubscribed && !config.node.subscribed) {
                config.onSubscribed(config.node, primary);
            }
        }

        const viewer = this.getViewer(nodeId);
        if (viewer) {
            if (!alive) {
                viewer.printErrorMessage("Node " + nodeId + " not alive");
            } else {
                viewer.printMessage(nodeId + "Polling every " + this.node.endpoint.pollingInterval + " milliseconds.");
            }
        }
    }

    sendCommand(options, nodeId) {
        if (options) {
            let arr = options.slice();
            arr.push("nodeId:" + (nodeId || this.primaryNodeId));
            const cmd = arr.join(";");
            console.log("send", cmd);
            if (!this.pendingCommands.includes(cmd)) {
                this.pendingCommands.push(cmd);
            }
            if (cmd.startsWith("command:refresh;")) {
                this.immediatePoll();
            }
        }
    }
}
