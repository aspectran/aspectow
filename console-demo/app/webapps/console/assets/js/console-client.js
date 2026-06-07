/*
 * Copyright (c) 2026-present The Aspectran Project
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
 * ConsoleClient provides a unified interface for real-time communication with Console activities,
 * automatically falling back to HTTP long-polling if WebSockets are unavailable.
 *
 * @version 4.0
 * @last-modified 2026-06-07
 */
class ConsoleClient {

    constructor(node, options = {}) {
        this.node = node;
        this.options = Object.assign({
            heartbeatInterval: 50000,
            pollingInterval: 3000,
            maxRetries: 10,
            retryInterval: 5000,
            token: null,
            onOpen: null,
            onMessage: null,
            onClose: null,
            onRetry: null,
            onBeforeConnect: null,
            onSubscribed: null,
            onEstablished: null,
            onFailed: null,
            onError: null
        }, options);

        if (this.options.token && this.node.endpoint) {
            this.node.endpoint.token = this.options.token;
        }

        this.socket = null;
        this.heartbeatTimer = null;
        this.pollingTimer = null;
        this.retryCount = 0;
        this.established = false;
        this.manualClose = false;
        this.activityPath = null;
        this.mode = 'websocket'; // 'websocket' or 'polling'
    }

    /**
     * Connects to the server using the provided node endpoint.
     * @param {string} activityPath - The activity-specific path (e.g., 'nodes', 'commands', 'scheduler')
     */
    start(activityPath) {
        this.activityPath = activityPath;
        this.manualClose = false;
        this.openSocket();
    }

    /**
     * Closes the connection manually.
     */
    stop() {
        this.manualClose = true;
        this.closeSocket(false);
        this.stopPolling();
    }

    /**
     * Opens a new connection.
     */
    openSocket() {
        if (this.options.onBeforeConnect) {
            Promise.resolve(this.options.onBeforeConnect(this.node)).then((token) => {
                if (token) {
                    this.node.endpoint.token = token;
                }
                this.connect();
            }).catch((err) => {
                console.error(this.node.id, "failed to prepare connection:", err);
                this.switchToPolling();
            });
        } else {
            this.connect();
        }
    }

    /**
     * Closes the socket and clears timers.
     * @param {boolean} afterClosing - whether this is called after the socket is already closed
     * @private
     */
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

    /**
     * Actually opens a new WebSocket connection.
     * @private
     */
    connect() {
        this.mode = 'websocket';
        this.closeSocket(false);

        let path = this.node.endpoint.path;
        if (this.activityPath) {
            const p = (path.endsWith('/') ? path : path + '/');
            const a = (this.activityPath.startsWith('/') ? this.activityPath.substring(1) : this.activityPath);
            path = p + a;
        }
        const url = new URL(path + "/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log(this.node.id, "connecting to websocket:", url.href);
        try {
            this.socket = new WebSocket(url.href);

            this.socket.onopen = (event) => {
                console.log(this.node.id, "websocket connected");
                this.retryCount = 0;

                const subscribeMessage = { header: "subscribe", targetNodeId: this.node.id };
                this.socket.send(JSON.stringify(subscribeMessage));
                this.sendPing();

                if (this.options.onOpen) {
                    this.options.onOpen(event);
                }
            };

            this.socket.onmessage = (event) => {
                if (typeof event.data === "string") {
                    try {
                        const response = JSON.parse(event.data);
                        const header = response.header;

                        if (header === 'subscribed') {
                            console.log(this.node.id, "subscribed", this.activityPath);
                            this.establish(response);
                            this.sendMessage(JSON.stringify({ header: "established" }));
                        } else if (header === 'pong') {
                            this.sendPing();
                        } else {
                            this.handleMessage(response);
                        }
                    } catch (e) {
                        console.error("Failed to parse incoming WebSocket message:", event.data, e);
                    }
                }
            };

            this.socket.onclose = (event) => {
                this.closeSocket(true);
                if (!this.manualClose) {
                    if (this.options.onClose) {
                        setTimeout(() => this.options.onClose(event), 100);
                    }
                    if (event.code === 1003) {
                        console.warn("Websocket connection refused: ", event.code, (event.reason || "Unauthorized"));
                        return;
                    }
                    if (event.code === 1011) {
                        console.log("Websocket connection closed: ", event.code);
                        return;
                    }
                    if (event.code === 1000 || this.retryCount === 0) {
                        console.log("Websocket connection closed: ", event.code);
                    }
                    if (event.code !== 1000) {
                        setTimeout(() => this.reconnect(), 1000);
                    }
                }
            };

            this.socket.onerror = (event) => {
                console.error(this.node.id, "websocket error:", event);
                this.switchToPolling();
            };
        } catch (e) {
            console.error(this.node.id, "failed to create websocket:", e);
            this.switchToPolling();
        }
    }

    switchToPolling() {
        if (this.mode === 'polling' || this.manualClose) return;
        console.warn(this.node.id, "switching to HTTP polling mode");
        this.mode = 'polling';
        this.closeSocket(false);
        this.startPolling();
    }

    startPolling() {
        this.stopPolling();
        let path = this.node.endpoint.path;
        if (this.activityPath) {
            const p = (path.endsWith('/') ? path : path + '/');
            const a = (this.activityPath.startsWith('/') ? this.activityPath.substring(1) : this.activityPath);
            path = p + a;
        }

        const subscribeUrl = path + "/subscribe?nodeId=" + this.node.id;
        fetch(subscribeUrl, {
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(res => res.json())
            .then(res => {
                if (res.success) {
                    console.log(this.node.id, "subscribed", this.activityPath);
                    this.establish(res.data);
                    this.poll();
                } else {
                    throw new Error(res.error ? res.error.message : "Failed to join polling session");
                }
            })
            .catch(err => {
                console.error(this.node.id, "failed to join polling session:", err);
                if (this.options.onFailed) {
                    this.options.onFailed(this.node);
                }
            });
    }

    poll() {
        if (this.mode !== 'polling' || this.manualClose) return;

        let path = this.node.endpoint.path;
        if (this.activityPath) {
            const p = (path.endsWith('/') ? path : path + '/');
            const a = (this.activityPath.startsWith('/') ? this.activityPath.substring(1) : this.activityPath);
            path = p + a;
        }

        const pullUrl = path + "/pull";
        fetch(pullUrl, {
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(res => res.json())
            .then(res => {
                if (res.success) {
                    const response = JSON.parse(res.data);
                    this.handleMessage(response);
                    this.pollingTimer = setTimeout(() => this.poll(), this.options.pollingInterval);
                } else {
                    throw new Error(res.error ? res.error.message : "Polling failed");
                }
            })
            .catch(err => {
                console.error(this.node.id, "polling error:", err);
                this.pollingTimer = setTimeout(() => this.poll(), this.options.retryInterval);
            });
    }

    stopPolling() {
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    /**
     * Completes the connection process after receiving the 'subscribed' message.
     * @param {Object} payload - payload from the server
     * @private
     */
    establish(payload) {
        if (this.options.onSubscribed) {
            this.options.onSubscribed(this.node, payload);
        }
        if (this.options.onEstablished) {
            this.options.onEstablished(this.node);
        }
        this.established = true;
    }

    handleMessage(message) {
        if (this.options.onMessage) {
            this.options.onMessage(message);
        }
    }

    /**
     * Sends a raw message to the server.
     * @param {string} data - the message data to send
     */
    sendMessage(data) {
        if (this.mode === 'websocket' && this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(data);
        } else if (this.mode === 'polling') {
            try {
                const message = JSON.parse(data);
                this.executePollingCommand(message.command, message.targetNodeId);
            } catch (e) {
                console.error("Failed to parse message for polling execution:", data);
            }
        }
    }

    executePollingCommand(command, nodeId) {
        let path = this.node.endpoint.path;
        if (this.activityPath) {
            const p = (path.endsWith('/') ? path : path + '/');
            const a = (this.activityPath.startsWith('/') ? this.activityPath.substring(1) : this.activityPath);
            path = p + a;
        }

        const executeUrl = path + "/execute";
        const formData = new URLSearchParams();
        formData.append("nodeId", nodeId || this.node.id);
        formData.append("command", command);

        fetch(executeUrl, {
            method: 'POST',
            body: formData,
            headers: {
                'Accept': 'application/json'
            }
        }).then(res => res.json())
        .then(res => {
            if (res.success) {
                console.log(this.node.id, "polling command executed:", res.data);
            } else {
                console.error(this.node.id, "polling command failed:", res.error.message);
            }
        })
        .catch(err => console.error(this.node.id, "polling command failed:", err));
    }

    /**
     * Sends a heartbeat ping to the server.
     * @private
     */
    sendPing() {
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
        }
        this.heartbeatTimer = setTimeout(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send(JSON.stringify({ header: "ping" }));
            }
        }, this.options.heartbeatInterval);
    }

    /**
     * Retries the connection with exponential backoff.
     * @private
     */
    reconnect() {
        if (this.retryCount < this.options.maxRetries) {
            this.retryCount++;
            const jitter = Math.floor(Math.random() * 1000);
            const interval = (this.options.retryInterval * this.retryCount) + jitter;
            const status = "(" + this.retryCount + "/" + this.options.maxRetries + ", interval=" + interval + "ms)";
            console.log(this.node.id, "reconnect attempt", status);
            if (this.options.onRetry) {
                this.options.onRetry(this.retryCount, this.options.maxRetries, interval);
            }
            setTimeout(() => {
                this.openSocket();
            }, interval);
        } else {
            console.log(this.node.id, "abort reconnect attempt, switching to polling");
            this.switchToPolling();
        }
    }

}
