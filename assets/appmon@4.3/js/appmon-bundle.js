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
 * The base class for AppMon communication clients.
 * Provides common functionality for connection management and retries.
 *
 * @version 4.3
 * @last-modified 2026-10-28
 */
class BaseClient {
    constructor(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode) {
        this.node = node;
        this.viewer = viewer;
        this.clusterViewers = {};
        this.clusterNodes = {};
        this.onSubscribed = onSubscribed;
        this.onClosed = onClosed;
        this.onFailed = onFailed;
        this.onNodeJoined = null;
        this.onNodeStatusChanged = null;
        this.onNodeLeft = null;
        this.onRequireRebuild = null;
        this.isGatewayMode = isGatewayMode;
        this.viewerResolver = null;
        this.metricsViewer = null;
        this.connectedNodeId = null;
        this.nodeToSubscribe = null;
        this.appsToSubscribe = null;
        this.retryCount = 0;
        this.reconnecting = false;
        this.maxRetries = 10;
        this.retryInterval = 5000;
        this.everConnected = false;
        this.retryTimer = null;
        this.lastResumeReconnect = 0;
        this.stopped = false;
        this.destroyed = false;

        this.handleResume = () => {
            if (this.destroyed || this.stopped) {
                return;
            }
            if (typeof document !== "undefined" && document.visibilityState === "visible") {
                this.onResume();
            }
        };
        this.handleOnline = () => {
            if (this.destroyed || this.stopped) {
                return;
            }
            this.onResume();
        };

        if (typeof document !== "undefined") {
            document.addEventListener("visibilitychange", this.handleResume);
        }
        if (typeof window !== "undefined") {
            window.addEventListener("pageshow", this.handleResume);
            window.addEventListener("focus", this.handleResume);
            window.addEventListener("online", this.handleOnline);
        }
    }

    setViewerResolver(resolver) {
        this.viewerResolver = resolver;
    }

    setMetricsViewer(metricsViewer) {
        this.metricsViewer = metricsViewer;
    }

    addClusterViewer(nodeId, viewer) {
        this.clusterViewers[nodeId] = viewer;
    }

    addClusterNode(node, onSubscribed) {
        this.clusterNodes[node.id] = {node, onSubscribed};
    }

    getViewer(nodeId) {
        if (this.viewerResolver) {
            const defaultViewer = (this.isGatewayMode && this.node.id !== nodeId) ? this.clusterViewers[nodeId] : this.viewer;
            const resolved = this.viewerResolver(nodeId, defaultViewer);
            if (resolved !== undefined) return resolved;
        }
        if (this.isGatewayMode && this.node.id !== nodeId) {
            return this.clusterViewers[nodeId];
        }
        return this.viewer;
    }

    getNodeConfig (nodeId) {
        return this.isGatewayMode ? this.clusterNodes[nodeId] : this;
    }

    notifyClosed() {
        if (this.isGatewayMode) {
            for (let id in this.clusterNodes) {
                const config = this.clusterNodes[id];
                if (this.onClosed) this.onClosed(config.node);
            }
        } else {
            if (this.onClosed) this.onClosed(this.node);
        }
    }

    notifyFailed() {
        if (this.isGatewayMode) {
            for (let id in this.clusterNodes) {
                const config = this.clusterNodes[id];
                if (this.onFailed) this.onFailed(config.node);
            }
        } else {
            if (this.onFailed) this.onFailed(this.node);
        }
    }

    printMessage(message) {
        if (this.isGatewayMode) {
            for (let id in this.clusterViewers) {
                const viewer = this.clusterViewers[id];
                if (viewer) {
                    viewer.printMessage(message);
                }
            }
        } else {
            this.viewer.printMessage(message);
        }
    }

    printErrorMessage(message) {
        if (this.isGatewayMode) {
            for (let id in this.clusterViewers) {
                const viewer = this.clusterViewers[id];
                if (viewer) {
                    viewer.printErrorMessage(message);
                }
            }
        } else {
            this.viewer.printErrorMessage(message);
        }
    }

    /**
     * Starts the client connection.
     * @param {string} [appsToSubscribe] - Names of apps to subscribe.
     * @param {string} [nodeToSubscribe] - Node ID to subscribe.
     */
    start(appsToSubscribe, nodeToSubscribe) {
        throw new Error("Method 'start()' must be implemented.");
    }

    /**
     * Checks if the client is currently connected.
     * @returns {boolean}
     */
    isConnected() {
        return false;
    }

    getConnectedNodeId() {
        return this.connectedNodeId || (this.node ? this.node.id : null);
    }

    setConnectedNodeId(nodeId) {
        this.connectedNodeId = nodeId;
    }

    /**
     * Called when the page becomes visible or active again (e.g. after returning from mobile background).
     */
    onResume() {
        if (this.destroyed || this.stopped) {
            return;
        }
        if (!this.isConnected()) {
            if (this.lastResumeReconnect && (Date.now() - this.lastResumeReconnect < 500)) {
                return;
            }
            this.lastResumeReconnect = Date.now();
            console.log(this.getConnectedNodeId(), "app/page resumed, attempting immediate reconnect");
            this.reconnect(true);
        }
    }

    /**
     * Stops the client connection.
     */
    stop() {
        this.stopped = true;
        if (this.retryTimer) {
            clearTimeout(this.retryTimer);
            this.retryTimer = null;
        }
    }

    /**
     * Destroys the client and removes event listeners.
     */
    destroy() {
        this.destroyed = true;
        this.stop();
        if (typeof document !== "undefined") {
            document.removeEventListener("visibilitychange", this.handleResume);
        }
        if (typeof window !== "undefined") {
            window.removeEventListener("pageshow", this.handleResume);
            window.removeEventListener("focus", this.handleResume);
            window.removeEventListener("online", this.handleOnline);
        }
    }

    /**
     * Refreshes the monitoring data with the specified options.
     * @param {string[]} [options] - Refresh options.
     * @param {string} [nodeId] - Target node ID.
     * @param {string} [scope] - Scope of refresh (e.g. "group" or "node").
     */
    refresh(options, nodeId, scope) {
        let cmdOptions = ["command:refresh"];
        if (scope) cmdOptions.push("scope:" + scope);
        if (options) cmdOptions.push(...options);
        this.sendCommand(cmdOptions, nodeId);
    }

    focus(appId, nodeId) {
        this.sendCommand([
            "command:focus",
            "appId:" + appId
        ], nodeId);
    }

    loadPrevious(appId, logId, loadedLines, nodeId) {
        this.sendCommand([
            "command:loadPrevious",
            "appId:" + appId,
            "logId:" + logId,
            "loadedLines:" + loadedLines
        ], nodeId);
    }

    /**
     * Sends a command with the specified options.
     * @param {string[]} [options] - Command options.
     * @param {string} [nodeId] - Target node ID.
     */
    sendCommand(options, nodeId) {
        throw new Error("Method 'sendCommand()' must be implemented.");
    }

    /**
     * Handles reconnection logic when a connection is lost or fails.
     * @param {boolean} [immediate=false] - If true, reconnects immediately without backoff delay.
     */
    reconnect(immediate = false) {
        if (this.destroyed || this.stopped) {
            return;
        }
        if (this.retryTimer) {
            clearTimeout(this.retryTimer);
            this.retryTimer = null;
        }
        if (immediate) {
            this.retryCount = 0;
            this.reconnecting = true;
            console.log(this.getConnectedNodeId(), "trying to reconnect immediately");
            this.printMessage("Trying to reconnect immediately...");
            this.start(this.appsToSubscribe, this.nodeToSubscribe);
            return;
        }
        if (this.retryCount++ < this.maxRetries) {
            this.reconnecting = true;
            const nodeIndex = (this.node && typeof this.node.index === 'number') ? this.node.index : 0;
            const jitter = Math.floor(Math.random() * 1000);
            const retryInterval = (this.retryInterval * this.retryCount) + (nodeIndex * 200) + jitter;
            const status = "(" + this.retryCount + "/" + this.maxRetries + ", interval=" + retryInterval + "ms)";
            console.log(this.getConnectedNodeId(), "trying to reconnect", status);
            this.printMessage("Trying to reconnect... " + status);
            this.retryTimer = setTimeout(() => {
                this.retryTimer = null;
                this.start(this.appsToSubscribe, this.nodeToSubscribe);
            }, retryInterval);
        } else {
            console.log(this.getConnectedNodeId(), "max connection attempts exceeded");
            this.printMessage("Max connection attempts exceeded.");
            this.notifyFailed();
            if (this.everConnected) {
                console.log(this.getConnectedNodeId(), "will keep retrying connection in background...");
                this.retryTimer = setTimeout(() => {
                    this.retryTimer = null;
                    this.retryCount = Math.max(0, this.maxRetries - 2);
                    this.start(this.appsToSubscribe, this.nodeToSubscribe);
                }, this.retryInterval * 2);
            }
        }
    }
}

/**
 * WebSocket implementation of the AppMon client.
 * In Gateway Mode, it manages a single physical connection for the entire cluster.
 *
 * @version 4.3
 * @last-modified 2026-10-28
 */
class WebsocketClient extends BaseClient {
    constructor(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode) {
        super(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode);
        this.heartbeatInterval = 50000;
        this.heartbeatTimer = null;
        this.socket = null;
        this.handshakeSuccessful = false;
        this.everConnected = false;
        this.established = false;
    }

    start(appsToSubscribe, nodeToSubscribe) {
        if (this.destroyed) return;
        this.stopped = false;
        this.nodeToSubscribe = nodeToSubscribe;
        this.appsToSubscribe = appsToSubscribe;
        this.openSocket();
    }

    stop() {
        super.stop();
        this.closeSocket();
        this.handshakeSuccessful = false;
    }

    destroy() {
        super.destroy();
        this.closeSocket(false);
        this.handshakeSuccessful = false;
    }

    isConnected() {
        return !this.stopped && !this.destroyed && !!(this.socket && this.socket.readyState === WebSocket.OPEN && this.handshakeSuccessful);
    }

    onResume() {
        if (this.destroyed || this.stopped || !this.everConnected) {
            return;
        }
        if (this.socket && this.socket.readyState === WebSocket.CONNECTING) {
            return;
        }
        if (!this.socket || this.socket.readyState === WebSocket.CLOSING || this.socket.readyState === WebSocket.CLOSED || !this.handshakeSuccessful) {
            if (this.lastResumeReconnect && (Date.now() - this.lastResumeReconnect < 500)) {
                return;
            }
            this.lastResumeReconnect = Date.now();
            console.log(this.getConnectedNodeId(), "WebSocket disconnected while in background, reconnecting immediately");
            this.closeSocket(true);
            this.reconnect(true);
        } else if (this.socket.readyState === WebSocket.OPEN) {
            this.sendPing();
        }
    }

    openSocket() {
        if (this.destroyed || this.stopped) return;
        this.closeSocket(false);
        const url = new URL(this.node.endpoint.path + "/appmon/websocket/" + this.node.endpoint.token, location.href);
        if (!this.isGatewayMode && this.node.port && (location.hostname === "localhost" || location.hostname === "127.0.0.1")) {
            url.port = this.node.port;
        }
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log("connecting to websocket:", url.href);
        this.socket = new WebSocket(url.href);

        this.socket.onopen = () => {
            if (this.destroyed || this.stopped) {
                this.closeSocket(false);
                return;
            }
            this.handshakeSuccessful = true;
            this.everConnected = true;
            this.reconnecting = false;
            if (this.retryTimer) {
                clearTimeout(this.retryTimer);
                this.retryTimer = null;
            }
            console.log(this.getConnectedNodeId(), "websocket connected");

            // Connect to the current node
            this.subscribe();
            this.sendPing();
            this.retryCount = 0;
        };

        this.socket.onmessage = (event) => {
            if (this.destroyed || this.stopped) return;
            if (typeof event.data !== "string") return;
            const msg = event.data;
            const idx = msg.indexOf(':');
            if (idx === -1) {
                console.warn("Invalid message format received:", msg);
                return;
            }

            const nodeId = msg.substring(0, idx);
            const message = msg.substring(idx + 1);

            // Standard control messages
            if (message.startsWith(":pong:")) {
                this.node.endpoint.token = message.substring(6);
                this.sendPing();
                return;
            }

            if (message.startsWith(":subscribed:")) {
                if (this.isGatewayMode && !this.established) {
                    this.setConnectedNodeId(nodeId);
                }
                const alive = message.endsWith(":alive");
                this.establish(nodeId, alive);
                return;
            }

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
                } else if (!this.isGatewayMode || this.clusterNodes[nodeId]) {
                    console.warn("No viewer registered for nodeId:", nodeId, "Message:", message);
                }
            }
        };

        this.socket.onclose = (event) => {
            const wasHandshakeSuccessful = this.handshakeSuccessful;
            this.closeSocket(true);

            if (this.destroyed || this.stopped) {
                return;
            }

            if (!wasHandshakeSuccessful && !this.everConnected) {
                console.warn("WebSocket handshake failed. Code:", event.code);
                return;
            }

            this.notifyClosed();
            if (event.code === 1003) {
                console.warn("Websocket connection refused: ", event.code);
                this.printErrorMessage("Socket connection refused by server.");
                if (this.onRequireRebuild) {
                    setTimeout(() => this.onRequireRebuild(), 1000);
                }
                return;
            }
            if (event.code === 1011) {
                console.log("Websocket connection closed: ", event.code);
                this.printErrorMessage("Websocket connection closed due to server error.");
                return;
            }
            if (event.code === 1000 || this.retryCount === 0) {
                console.log("Websocket connection closed: ", event.code);
                this.printMessage("Websocket connection closed.");
            }
            if (event.code !== 1000) {
                this.reconnect();
            }
        };

        this.socket.onerror = (event) => {
            if (this.destroyed || this.stopped) {
                return;
            }
            console.error(this.getConnectedNodeId(), "websocket error:", event);
            if (!this.everConnected && this.node.endpoint.mode !== "polling") {
                this.node.endpoint.mode = "polling";
                if (this.isGatewayMode) {
                    for (let id in this.clusterNodes) {
                        const config = this.clusterNodes[id];
                        if (config && config.node && config.node.endpoint) {
                            config.node.endpoint.mode = "polling";
                        }
                    }
                }
                console.warn(this.getConnectedNodeId(), "webSocket is not supported. Switching to polling mode.");
                this.notifyFailed();
            } else {
                this.printErrorMessage("Could not connect to the WebSocket server.");
            }
        };
    }

    closeSocket(afterClosing) {
        this.established = false;
        if (this.socket) {
            const socket = this.socket;
            this.socket = null;
            socket.onopen = null;
            socket.onmessage = null;
            socket.onerror = null;
            socket.onclose = null;
            if (!afterClosing) {
                try {
                    socket.close(1000, "Normal closure");
                } catch (e) {
                    // ignore
                }
            }
        }
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    subscribe() {
        const options = ["command:subscribe"];
        options.push("timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone);
        if (this.nodeToSubscribe) {
            options.push("nodeToSubscribe:" + this.nodeToSubscribe);
        }
        if (this.appsToSubscribe) {
            options.push("appsToSubscribe:" + this.appsToSubscribe);
        }
        this.sendCommand(options);
    }

    establish(nodeId, alive) {
        const primary = (nodeId === this.getConnectedNodeId());

        const config = this.getNodeConfig(nodeId);
        if (config) {
            config.node.alive = !!alive;
            if (config.onSubscribed && !config.node.subscribed) {
                config.onSubscribed(config.node, primary);
            }
        }

        const shouldSendEstablished = this.isGatewayMode ? !this.established : primary;
        if (shouldSendEstablished) {
            this.established = true;
            const options = ["command:established"];
            if (this.nodeToSubscribe) options.push("nodeToSubscribe:" + this.nodeToSubscribe);
            if (this.appsToSubscribe) options.push("appsToSubscribe:" + this.appsToSubscribe);
            this.sendCommand(options);
        }
        if (!alive) {
            const viewer = this.getViewer(nodeId);
            if (viewer) viewer.printErrorMessage("Node " + nodeId + " not alive");
        }
    }

    sendCommand(options, nodeId) {
        if (options && this.socket && this.socket.readyState === WebSocket.OPEN) {
            const arr = options.slice();
            arr.push("nodeId:" + (nodeId || this.getConnectedNodeId()));
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

/**
 * HTTP Polling implementation of the AppMon client.
 *
 * @version 4.3
 * @last-modified 2026-10-28
 */
class PollingClient extends BaseClient {
    constructor(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode = false) {
        super(node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode);
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
        if (this.destroyed) return;
        this.stopped = false;
        this.nodeToSubscribe = nodeToSubscribe;
        this.appsToSubscribe = appsToSubscribe;
        this.subscribe();
    }

    stop() {
        super.stop();
        this.stopped = true;
        this.established = false;
        this.setConnectedNodeId(null);
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    destroy() {
        this.destroyed = true;
        this.stop();
        super.destroy();
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
            console.log(this.getConnectedNodeId(), "PollingClient resumed while disconnected, reconnecting immediately");
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

                    if (data.nodeId) {
                        this.setConnectedNodeId(data.nodeId);
                    }

                    if (this.isGatewayMode) {
                        for (let id in this.clusterNodes) {
                            this.establish(id, data.nodeAliveMap && data.nodeAliveMap[id]);
                        }
                    } else {
                        this.establish(this.getConnectedNodeId(), true);
                    }

                    if (!this.stopped) {
                        this.appsToSubscribe = data.appsToSubscribe;
                        this.immediatePoll();
                    }
                } else {
                    console.log(this.getConnectedNodeId(), "connection failed");
                    this.printErrorMessage("Connection failed.");
                    this.reconnect();
                }
            },
            error: (xhr, status, error) => {
                console.log(this.getConnectedNodeId(), "connection failed", error);
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
                    console.log(this.getConnectedNodeId(), "connection lost");
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
                console.log(this.getConnectedNodeId(), "connection lost", error);
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
                    if (this.pollingTimer) {
                        clearTimeout(this.pollingTimer);
                        this.pollingTimer = setTimeout(() => this.poll(), data.pollingInterval);
                    }
                } else {
                    console.log(this.node.id, "failed to change polling interval");
                }
            },
            error: (xhr, status, error) => {
                console.log(this.node.id, "failed to change polling interval", error);
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
                    } else if (!this.isGatewayMode || this.clusterNodes[nodeId]) {
                        console.warn("No viewer registered for nodeId:", nodeId, "Message:", message);
                    }
                }
            });
        }
    }

    establish(nodeId, alive) {
        const primary = (nodeId === this.getConnectedNodeId());

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
            arr.push("nodeId:" + (nodeId || this.getConnectedNodeId()));
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

/**
 * Advanced Canvas-based particle engine for AppMon traffic visualization.
 * Handles tab visibility to prevent "bullet bursts" when returning to the tab.
 *
 * @version 4.3
 * @last-modified 2026-10-20
 */
class TrafficPainter {
    constructor(canvas, options = {}) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.bullets = [];
        this.animationId = null;
        this.isRunning = false;
        this.finishLineOffset = 0;

        this.enableBlackHole = options.enableBlackHole !== false;
        this.ejectRatio = 0;
        this.prismProgress = 0.0;
        this.prismAlpha = 1.0;
        this.currentLeftPrismX = 0;
        this.currentRightPrismX = 0;
        this.blackHole = {
            radius: 14,
            gravityRadius: 140,
            angle: 0,
            pulse: 0,
            trappedCount: 0
        };

        this.resize();
        this.resizeObserver = new ResizeObserver(() => this.resize());
        this.resizeObserver.observe(this.canvas);
    }

    resize() {
        const rect = this.canvas.getBoundingClientRect();
        this.canvas.width = rect.width;
        this.canvas.height = rect.height;
        this.updatePrismPositions();
        this.draw();
    }

    updatePrismPositions() {
        const finishLine = this.canvas.width - this.finishLineOffset;
        const bhX = finishLine * 0.5;
        const spreadDistance = 60;
        const prismOffset = this.blackHole.radius + 2 + spreadDistance;
        const leftActiveX = bhX - prismOffset;
        const rightActiveX = bhX + prismOffset;
        const leftParkedX = 10;
        const rightParkedX = Math.max(leftParkedX + 40, finishLine - 10);

        const p = this.prismProgress || 0.0;
        const ease = p * p * (3 - 2 * p);
        this.currentLeftPrismX = leftParkedX + (leftActiveX - leftParkedX) * ease;
        this.currentRightPrismX = rightParkedX + (rightActiveX - rightParkedX) * ease;
    }

    /**
     * Adds a new bullet to the painter.
     * @param {Object} data - Bullet data (error, elapsedTime, activityCount).
     * @param {Function} onArriving - Callback when bullet reaches the finish line.
     */
    addBullet(data, onArriving) {
        const elapsedTime = data.elapsedTime || 0;
        const activityCount = data.activityCount || 0;
        const hasError = !!(data.error);
        const timeIntensity = Math.min(elapsedTime / 5000, 1);
        const targetMax = 1000;
        const activityIntensity = activityCount > 0 
            ? Math.min(Math.log10(activityCount + 1) / Math.log10(targetMax + 1), 1)
            : 0;
        
        const baseSize = 3.0 + (timeIntensity * 4);
        const fullSize = baseSize + (activityIntensity * 4);
        const initialSize = this.enableBlackHole ? baseSize : fullSize;
        const baseSpeed = (this.canvas.width - this.finishLineOffset) / (900 / 16.6);
        const speed = baseSpeed * (1 - (timeIntensity * 0.6));
        const initY = Math.random() * (this.canvas.height - 20) + 10;

        const bullet = {
            x: -(Math.random() * 150),
            y: initY,
            initY: initY,
            targetY: initY,
            speed: speed,
            baseSize: baseSize,
            fullSize: fullSize,
            size: initialSize,
            renderSize: initialSize,
            timeIntensity: timeIntensity,
            activityIntensity: activityIntensity,
            color: hasError ? '#ff0000' : (timeIntensity > 0.5 ? '#f1c40f' : '#11d539'),
            elapsedTime: Math.max(elapsedTime, 500),
            arrived: false,
            arrivedTime: 0,
            impactPulse: 0,
            alpha: 1.0,
            bhStage: 0,
            transitFrames: 0,
            orbitAngle: 0,
            orbitRadius: 0,
            orbitSpeed: 0,
            onArriving: onArriving
        };
        this.bullets.push(bullet);

        if (!this.isRunning) {
            this.start();
        }
    }

    start() {
        this.isRunning = true;
        this.lastTime = performance.now();
        const loop = (currentTime) => {
            if (document.hidden) {
                this.isRunning = false;
                return;
            }
            
            const deltaTime = Math.min((currentTime - this.lastTime) / 16.66, 3.0);
            this.lastTime = currentTime;

            this.update(deltaTime);
            this.draw();

            const isPrismAnimating = (this.bullets.length === 0 && (this.prismProgress > 0 || this.prismAlpha < 1.0));
            if (this.bullets.length > 0 || isPrismAnimating) {
                this.animationId = requestAnimationFrame(loop);
            } else {
                this.isRunning = false;
                this.draw();
            }
        };
        this.animationId = requestAnimationFrame(loop);
    }

    update(deltaTime) {
        const finishLine = this.canvas.width - this.finishLineOffset;
        const now = Date.now();
        const bhX = finishLine * 0.5;
        const bhY = this.canvas.height * 0.5;

        // 두 개의 유리막 사이 넓이 (전개 시 좌우 유리막 스팬)
        const spreadDistance = 60;
        const prismOffset = this.blackHole.radius + 2 + spreadDistance;
        const prismSpan = prismOffset * 2 + 30;
        const isNarrowTrack = finishLine < prismSpan;

        // 트랙의 넓이가 두 개의 유리막 넓이보다 좁은 경우:
        // 처음(대기 시)에는 양 끝에 보이고, 총알이 나타나면 유리막을 감춤 (페이드아웃)
        // 충분히 넓은 경우:
        // 총알이 나타나면 양 끝에서 중앙으로 슬라이딩 전개
        const hasBullets = this.bullets.length > 0;
        const targetProgress = (!isNarrowTrack && hasBullets) ? 1.0 : 0.0;
        const targetAlpha = (isNarrowTrack && hasBullets) ? 0.0 : 1.0;

        // 슬라이딩 진행도 업데이트
        const deploySpeed = 0.14;  // 전개 속도: 총알 발생 시 신속히 블랙홀 옆으로 이동
        const retractSpeed = 0.05; // 복귀 속도: 총알 소멸 시 트랙 양 끝으로 우아하게 복귀
        const moveSpeed = targetProgress > this.prismProgress ? deploySpeed : retractSpeed;
        if (targetProgress > this.prismProgress) {
            this.prismProgress = Math.min(1.0, this.prismProgress + moveSpeed * deltaTime);
        } else if (targetProgress < this.prismProgress) {
            this.prismProgress = Math.max(0.0, this.prismProgress - moveSpeed * deltaTime);
        }

        // 투명도(Alpha) 업데이트 (좁은 트랙 감춤/표시 처리)
        const fadeOutSpeed = 0.15; // 총알 등장 시 신속히 페이드아웃
        const fadeInSpeed = 0.06;  // 총알 소멸 시 부드럽게 페이드인
        const alphaSpeed = targetAlpha < this.prismAlpha ? fadeOutSpeed : fadeInSpeed;
        if (targetAlpha > this.prismAlpha) {
            this.prismAlpha = Math.min(1.0, this.prismAlpha + alphaSpeed * deltaTime);
        } else if (targetAlpha < this.prismAlpha) {
            this.prismAlpha = Math.max(0.0, this.prismAlpha - alphaSpeed * deltaTime);
        }

        this.updatePrismPositions();

        let trappedCount = 0;

        for (let i = this.bullets.length - 1; i >= 0; i--) {
            const b = this.bullets[i];

            if (!b.arrived) {
                if (this.enableBlackHole && finishLine > 120) {
                    if (b.bhStage === 0) {
                        const distToBh = bhX - b.x;
                        const leftGateX = this.currentLeftPrismX;

                        if (distToBh <= Math.max(b.speed * deltaTime, 6)) {
                            b.x = bhX;
                            b.y = bhY;
                            b.bhStage = 1;
                            const minFrames = 4;
                            const maxFrames = 50;
                            b.transitFrames = Math.round(minFrames + b.timeIntensity * (maxFrames - minFrames));
                            b.orbitAngle = Math.random() * Math.PI * 2;
                            b.orbitSpeed = (0.12 + Math.random() * 0.08) * (Math.random() < 0.5 ? 1 : -1);
                            b.orbitRadius = (this.blackHole.radius * 0.75) * (0.25 + Math.random() * 0.75);
                            b.renderSize = Math.max(1.2, Math.min(2.5, b.baseSize * 0.4));
                            this.blackHole.pulse = Math.max(this.blackHole.pulse, 0.6);
                        } else if (b.x >= leftGateX) {
                            // 좌측 유리막을 통과하면서 블랙홀 인력 영역으로 유선형 진입
                            const pullDist = Math.max(10, bhX - leftGateX);
                            const pullProgress = Math.min(1.0, Math.max(0.0, (b.x - leftGateX) / pullDist));
                            const easePull = Math.pow(pullProgress, 1.8);
                            b.y = (b.initY || bhY) + (bhY - (b.initY || bhY)) * easePull;
                            if (distToBh < 35) {
                                b.renderSize = Math.max(0.6, b.baseSize * (distToBh / 35));
                            }
                            b.x += b.speed * deltaTime;
                        } else {
                            // 좌측 유리막에 도달하기 전까지는 완전한 수평 평행 비행
                            b.y = b.initY || bhY;
                            b.x += b.speed * deltaTime;
                        }
                    } else if (b.bhStage === 1) {
                        trappedCount++;
                        b.transitFrames -= deltaTime;
                        b.orbitAngle += b.orbitSpeed * deltaTime;
                        b.x = bhX + Math.cos(b.orbitAngle) * b.orbitRadius;
                        b.y = bhY + Math.sin(b.orbitAngle) * b.orbitRadius;

                        if (b.transitFrames <= 0) {
                            b.bhStage = 2;
                            b.size = b.fullSize;

                            // 프리즘 레인 확산 (Prism Lane Dispersion):
                            // 황금비(Golden Ratio: 0.61803398875) 기반 저변위 수열(Low-Discrepancy Sequence)
                            this.ejectRatio = ((this.ejectRatio || 0) + 0.61803398875) % 1.0;
                            const minY = 14;
                            const maxY = this.canvas.height - 14;
                            b.targetY = minY + this.ejectRatio * (maxY - minY);

                            // 1. 사출 시작 위치: 블랙홀 출구 테두리
                            const halfH = (this.canvas.height - 24) * 0.5;
                            const ejectAngle = (b.targetY - bhY) / (halfH || 1);
                            b.ejectStartX = bhX + this.blackHole.radius + 2;
                            b.ejectStartY = bhY + Math.sin(ejectAngle * 0.45) * (this.blackHole.radius * 0.6);

                            b.x = b.ejectStartX;
                            b.y = b.ejectStartY;
                            b.renderSize = 0.8;
                            b.speed *= 1.25;
                            this.blackHole.pulse = Math.max(this.blackHole.pulse, 0.5);
                        }
                    } else if (b.bhStage === 2) {
                        // 2. 분산 구간: 블랙홀 출구에서 우측 유리막(currentRightPrismX)까지 부채꼴로 퍼져나감
                        const rightGateX = isNarrowTrack 
                            ? Math.min(this.currentRightPrismX, b.ejectStartX + Math.max(20, (finishLine - b.ejectStartX) * 0.6))
                            : this.currentRightPrismX;
                        const totalSpread = Math.max(15, rightGateX - b.ejectStartX);
                        const progress = Math.min(1.0, Math.max(0.0, (b.x - b.ejectStartX) / totalSpread));
                        // Smoothstep S-Curve: 우측 유리막 지점에 도달할 때 완벽한 수평 레인으로 정렬
                        const ease = progress * progress * (3 - 2 * progress);
                        b.y = b.ejectStartY + (b.targetY - b.ejectStartY) * ease;
                        b.renderSize = Math.min(b.size, 0.8 + (b.size - 0.8) * Math.min(1.0, progress * 2.0));
                        b.x += b.speed * deltaTime;

                        if (progress >= 1.0 || b.x >= rightGateX) {
                            // 우측 유리막을 통과하는 순간 분산 완료 및 수평 레인 정렬 완료!
                            b.y = b.targetY;
                            b.renderSize = b.size;
                            b.bhStage = 3; // 이후 도착선까지 완전한 평행 수평 비행!
                        }
                    } else {
                        // 3. 우측 유리막 통과 후: 독립 레인에서 완전한 수평 평행 비행 (개수 즉시 가늠!)
                        b.y = b.targetY;
                        b.x += b.speed * deltaTime;
                    }
                } else {
                    b.x += b.speed * deltaTime;
                }

                if (b.x >= finishLine) {
                    b.x = finishLine;
                    b.renderSize = b.size;
                    b.arrived = true;
                    b.arrivedTime = now;
                    b.impactPulse = 1.0;
                }
            } else {
                if (b.impactPulse > 0) {
                    b.impactPulse -= 0.05 * deltaTime;
                }

                const stayElapsed = now - b.arrivedTime;
                if (stayElapsed > b.elapsedTime + 200) {
                    b.alpha -= 0.04 * deltaTime;
                    if (b.alpha <= 0) {
                        if (b.onArriving) b.onArriving();
                        this.bullets.splice(i, 1);
                    }
                }
            }
        }

        if (this.enableBlackHole && finishLine > 120) {
            this.blackHole.trappedCount = trappedCount;
            const speedBoost = Math.min(trappedCount * 0.006, 0.10);
            this.blackHole.angle += (0.04 + speedBoost) * deltaTime;
            if (this.blackHole.pulse > 0) {
                this.blackHole.pulse -= 0.08 * deltaTime;
                if (this.blackHole.pulse < 0) this.blackHole.pulse = 0;
            }
        }
    }

    draw() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        const finishLine = this.canvas.width - this.finishLineOffset;
        const hasBlackHole = this.enableBlackHole && finishLine > 120;
        if (hasBlackHole) {
            const bhX = finishLine * 0.5;
            const bhY = this.canvas.height * 0.5;
            this.drawBlackHole(bhX, bhY);
            this.drawPrism(this.currentLeftPrismX);
            this.drawPrism(this.currentRightPrismX);
        }

        for (let i = 0; i < this.bullets.length; i++) {
            const b = this.bullets[i];

            // 0. Bullets trapped inside the black hole (swirling mini photons)
            if (b.bhStage === 1) {
                this.ctx.globalAlpha = b.alpha * 0.85;
                this.ctx.fillStyle = b.color;
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, b.renderSize, 0, Math.PI * 2);
                this.ctx.fill();

                this.ctx.fillStyle = '#fff';
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, Math.max(0.6, b.renderSize * 0.45), 0, Math.PI * 2);
                this.ctx.fill();
                continue;
            }

            this.ctx.globalAlpha = b.alpha;
            
            const drawSize = b.arrived ? b.size * (1 + b.impactPulse * 0.3) : b.renderSize;

            // 1. Aura (Glow) - high performance alternative to shadowBlur
            this.ctx.fillStyle = b.color;
            this.ctx.beginPath();
            let auraSize = drawSize * (1.2 + b.timeIntensity + (b.arrived ? b.impactPulse : 0));
            this.ctx.globalAlpha = b.alpha * 0.3;
            this.ctx.arc(b.x, b.y, auraSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 2. Main Bullet Body
            this.ctx.globalAlpha = b.alpha;
            this.ctx.beginPath();
            this.ctx.arc(b.x, b.y, drawSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 3. Activity Glow (Hot Core) - apply only after exiting the black hole
            const canShowActivity = !hasBlackHole || b.bhStage >= 2;
            if (canShowActivity && b.activityIntensity > 0.3) {
                const coreSize = drawSize * (0.3 + b.activityIntensity * 0.4);
                this.ctx.fillStyle = '#fff';
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, coreSize, 0, Math.PI * 2);
                this.ctx.fill();
            }
        }
        this.ctx.globalAlpha = 1.0;
    }

    drawBlackHole(cx, cy) {
        const pulse = this.blackHole.pulse;
        const trappedCount = this.blackHole.trappedCount || 0;
        const trappedBoost = Math.min(trappedCount * 0.04, 0.45);
        const r = this.blackHole.radius * (1 + pulse * 0.25 + trappedBoost * 0.15);

        this.ctx.save();

        // 1. External Accretion Aura (Soft violet / energetic magenta glow when holding bullets)
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, r * (2.4 + trappedBoost * 0.6), 0, Math.PI * 2);
        const auraAlpha = Math.min(0.12 + trappedCount * 0.015, 0.30);
        this.ctx.fillStyle = trappedCount > 5 
            ? `rgba(220, 80, 255, ${auraAlpha})` 
            : `rgba(120, 70, 255, ${auraAlpha})`;
        this.ctx.fill();

        // 2. Pulse Shockwave Ring when bullet is absorbed/ejected or highly populated
        if (pulse > 0.05) {
            this.ctx.beginPath();
            this.ctx.arc(cx, cy, r * (1.8 + (1 - pulse) * 1.2), 0, Math.PI * 2);
            this.ctx.strokeStyle = `rgba(0, 240, 255, ${pulse * 0.6})`;
            this.ctx.lineWidth = 1.5;
            this.ctx.stroke();
        }

        // 3. Swirling Accretion Arcs (Rotating space distortion)
        this.ctx.translate(cx, cy);
        this.ctx.rotate(this.blackHole.angle);

        // Cyan / Gold Arc (Heats up as trapped bullets increase)
        this.ctx.beginPath();
        this.ctx.arc(0, 0, r * 1.5, 0, Math.PI * 1.15);
        this.ctx.strokeStyle = trappedCount > 8 ? 'rgba(255, 190, 40, 0.9)' : 'rgba(0, 229, 255, 0.75)';
        this.ctx.lineWidth = 2 + Math.min(trappedCount * 0.08, 1.2);
        this.ctx.stroke();

        // Magenta / Violet Arc
        this.ctx.beginPath();
        this.ctx.arc(0, 0, r * 1.75, Math.PI, Math.PI * 2.25);
        this.ctx.strokeStyle = 'rgba(217, 70, 239, 0.65)';
        this.ctx.lineWidth = 1.5;
        this.ctx.stroke();

        this.ctx.restore();

        // 4. Event Horizon (Black Void Core)
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, r, 0, Math.PI * 2);
        this.ctx.fillStyle = '#060b17';
        this.ctx.fill();

        // 5. Photon Ring (Crisp boundary highlight)
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, r, 0, Math.PI * 2);
        this.ctx.strokeStyle = trappedCount > 5 ? 'rgba(255, 210, 120, 0.95)' : 'rgba(100, 210, 255, 0.9)';
        this.ctx.lineWidth = 1.5;
        this.ctx.stroke();
    }

    drawPrism(px) {
        if (this.prismAlpha <= 0.001) return;
        const h = this.canvas.height;
        this.ctx.save();
        if (this.prismAlpha < 1.0) {
            this.ctx.globalAlpha = this.prismAlpha;
        }

        // 1. 반투명 프리즘 유리막 본체 (Ambient Glass Sheet)
        const glassGrad = this.ctx.createLinearGradient(px - 3, 0, px + 3, 0);
        glassGrad.addColorStop(0, 'rgba(0, 229, 255, 0.04)');
        glassGrad.addColorStop(0.5, 'rgba(217, 70, 239, 0.14)');
        glassGrad.addColorStop(1, 'rgba(0, 229, 255, 0.04)');

        this.ctx.fillStyle = glassGrad;
        this.ctx.fillRect(px - 3, 6, 6, h - 12);

        // 2. 프리즘 크리스탈 엣지 (세로 광선 굴절선)
        const lineGrad = this.ctx.createLinearGradient(0, 6, 0, h - 6);
        lineGrad.addColorStop(0, 'rgba(0, 240, 255, 0.0)');
        lineGrad.addColorStop(0.15, 'rgba(0, 240, 255, 0.45)');
        lineGrad.addColorStop(0.5, 'rgba(235, 130, 255, 0.85)');
        lineGrad.addColorStop(0.85, 'rgba(0, 240, 255, 0.45)');
        lineGrad.addColorStop(1, 'rgba(0, 240, 255, 0.0)');

        this.ctx.strokeStyle = lineGrad;
        this.ctx.lineWidth = 1.5;
        this.ctx.beginPath();
        this.ctx.moveTo(px, 6);
        this.ctx.lineTo(px, h - 6);
        this.ctx.stroke();

        // 3. 내부 광학 굴절 하이라이트 (미세 투과 반사선)
        this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
        this.ctx.lineWidth = 0.8;
        this.ctx.beginPath();
        this.ctx.moveTo(px + 1, 14);
        this.ctx.lineTo(px + 1, h - 14);
        this.ctx.stroke();

        // 4. 상·하단 사이버네틱 마운트 핀
        this.ctx.fillStyle = 'rgba(0, 240, 255, 0.7)';
        this.ctx.fillRect(px - 4, 6, 8, 2);
        this.ctx.fillRect(px - 4, h - 8, 8, 2);

        this.ctx.restore();
    }

    clear() {
        this.bullets = [];
        if (!this.isRunning && (this.prismProgress > 0 || this.prismAlpha < 1.0)) {
            this.start();
        } else {
            this.draw();
        }
    }

    destroy() {
        if (this.animationId) cancelAnimationFrame(this.animationId);
        this.resizeObserver.disconnect();
    }
}

/**
 * The chart component for the AppMon dashboard.
 * Responsible for rendering and updating individual charts using Chart.js.
 *
 * @version 4.3
 * @last-modified 2026-10-29
 */
class DashboardChart {
    constructor($container, eventId) {
        this.$container = $container;
        this.eventId = eventId;
        this.chart = null;
        this.dateUnit = null;
        this.dateOffset = null;
        this.updateScheduled = false;
    }

    isDrawn() {
        return (this.chart !== null);
    }

    getLabels() {
        return (this.chart ? this.chart.data.labels : []);
    }

    getDataset(index) {
        return (this.chart ? this.chart.data.datasets[index].data : []);
    }

    ensureCanvas() {
        let $canvas = this.$container.find("canvas");
        if (!$canvas.length) {
            $canvas = $("<canvas/>").appendTo(this.$container);
        }
        return $canvas;
    }

    draw(dateUnit, labels, data1, data2) {
        this.destroy();
        const $canvas = this.ensureCanvas();

        let dataLabel1;
        let borderColor1;
        let backgroundColor1;
        let gradientStart1;
        let dataLabel2;
        let borderColor2;
        let backgroundColor2;
        switch (this.eventId) {
            case "activity":
                dataLabel1 = "Activities";
                borderColor1 = "#4493c8";
                backgroundColor1 = "#cce0fa";
                gradientStart1 = "rgba(68, 147, 200, 0.35)";
                dataLabel2 = "Errors";
                borderColor2 = "#ff6384";
                backgroundColor2 = "#ff6384";
                break;
            case "session":
                dataLabel1 = "Sessions";
                borderColor1 = "#44c577";
                backgroundColor1 = "#bcefd0";
                gradientStart1 = "rgba(68, 197, 119, 0.35)";
                dataLabel2 = (dateUnit ? "Peak Users" : "Active Users");
                borderColor2 = "#9b59b6";
                backgroundColor2 = "#ebdcf2";
                break;
            default:
                dataLabel1 = "";
                dataLabel2 = "";
                borderColor1 = "#4493c8";
                backgroundColor1 = "#cce0fa";
                gradientStart1 = "rgba(68, 147, 200, 0.35)";
                borderColor2 = "#ff6384";
                backgroundColor2 = "#ff6384";
        }

        const chartType = (!dateUnit ? "line" : "bar");
        this.chart = new Chart($canvas[0], {
            type: chartType,
            plugins: [
                {
                    id: 'crosshairGuideline',
                    afterDraw: (chart) => {
                        if (chart.tooltip && typeof chart.tooltip.getActiveElements === "function") {
                            const activeElements = chart.tooltip.getActiveElements();
                            if (activeElements && activeElements.length > 0) {
                                const ctx = chart.ctx;
                                const x = activeElements[0].element.x;
                                const topY = chart.scales.y.top;
                                const bottomY = chart.scales.y.bottom;
                                ctx.save();
                                ctx.beginPath();
                                ctx.setLineDash([3, 3]);
                                ctx.moveTo(x, topY);
                                ctx.lineTo(x, bottomY);
                                ctx.lineWidth = 1;
                                ctx.strokeStyle = 'rgba(120, 130, 145, 0.45)';
                                ctx.stroke();
                                ctx.restore();
                            }
                        }
                    }
                }
            ],
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        enabled: true,
                        reverse: (this.eventId === "activity"),
                        mode: 'index',
                        intersect: false,
                        filter: (tooltipItem) => (tooltipItem.raw !== null && tooltipItem.raw !== undefined),
                        padding: 10,
                        boxPadding: 4,
                        cornerRadius: 6,
                        backgroundColor: 'rgba(23, 32, 42, 0.90)',
                        titleColor: '#ffffff',
                        titleFont: { size: 12, weight: 'bold' },
                        bodyColor: '#e0e6ed',
                        bodyFont: { size: 12 },
                        borderColor: 'rgba(255, 255, 255, 0.12)',
                        borderWidth: 1,
                        callbacks: {
                            title: (tooltip) => {
                                const datetime = dayjs(labels[tooltip[0].dataIndex]);
                                switch (dateUnit) {
                                    case "day":
                                        return datetime.format("YYYY-MM-DD HH:00");
                                    case "week":
                                        return datetime.format("YYYY-MM-DD (ddd)");
                                    case "month":
                                        return datetime.format("YYYY-MM-DD");
                                    case "hour":
                                        return datetime.format("LL HH:00");
                                    case "year":
                                        return datetime.format("YYYY-MM");
                                    default:
                                        return datetime.format("LLL");
                                }
                            },
                            label: (tooltipItem) => {
                                const val = tooltipItem.parsed.y;
                                const label = tooltipItem.dataset.label || '';
                                return label + ": " + (val != null ? Number(val).toLocaleString() : 0);
                            }
                        }
                    },
                    zoom: {
                        zoom: {
                            wheel: { enabled: false },
                            pinch: { enabled: true },
                            drag: {
                                enabled: true,
                                threshold: 21,
                                backgroundColor: "rgba(225,225,225,0.35)",
                                borderColor: "rgba(225,225,225)",
                                borderWidth: 1
                            },
                            mode: "x",
                            onZoomComplete: () => {
                                const $resetZoom = this.$container.find(".reset-zoom");
                                if (this.isZoomedOrPanned()) {
                                    $resetZoom.off("click").on("click", () => this.resetZoom()).show();
                                } else {
                                    $resetZoom.hide();
                                }
                            }
                        },
                        pan: { enabled: true, mode: "x", modifierKey: "ctrl" }
                    }
                },
                scales: {
                    x: {
                        display: true,
                        ticks: {
                            autoSkip: (dateUnit === "month"),
                            includeBounds: false,
                            callback: (value, index) => {
                                const datetime = dayjs(labels[value]);
                                const datetime2 = (value > 0 ? dayjs(labels[value - 1]) : null);
                                switch (dateUnit) {
                                    case "day":
                                        return datetime.format("HH:00");
                                    case "week":
                                        return datetime.format("ddd (M/D)");
                                    case "month":
                                        return datetime.format("D");
                                    case "hour":
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:00")
                                            : datetime.format("HH:00");
                                    case "year":
                                        return datetime.format("MMM");
                                    default: // 5m.
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:mm")
                                            : datetime.format("HH:mm");
                                }
                            }
                        },
                        stacked: (this.eventId === "activity"),
                        grid: chartType === "line" ? {
                            color: (ctx) => (this.eventId === "activity" && data2[ctx.tick.value] > 0 ? "#ff6384" : "#e4e4e4")
                        } : {}
                    },
                    y: {
                        display: true,
                        title: {
                            display: true,
                            text: (this.eventId === "session" ? `${dataLabel1} / ${dataLabel2}` : dataLabel1)
                        },
                        suggestedMin: 0,
                        suggestedMax: 5,
                        stacked: (this.eventId === "activity"),
                        grid: { color: "#e4e4e4" }
                    }
                }
            },
            data: {
                labels: labels,
                datasets: [
                    chartType === "line" ? {
                        label: dataLabel1,
                        data: data1,
                        fill: true,
                        borderColor: borderColor1,
                        backgroundColor: (context) => {
                            const chart = context.chart;
                            const { ctx, chartArea } = chart;
                            if (!chartArea) return backgroundColor1;
                            const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
                            gradient.addColorStop(0, gradientStart1);
                            gradient.addColorStop(1, "rgba(255, 255, 255, 0.0)");
                            return gradient;
                        },
                        borderWidth: 1.8,
                        tension: 0.35,
                        pointStyle: false,
                        pointHoverRadius: 4,
                        order: 2
                    } : {
                        label: dataLabel1,
                        data: data1,
                        minBarLength: 2,
                        fill: true,
                        borderWidth: 1,
                        borderColor: borderColor1,
                        backgroundColor: borderColor1,
                        borderSkipped: false,
                        order: 2
                    },
                    this.eventId === "session" ? {
                        label: dataLabel2,
                        data: data2,
                        type: "line",
                        fill: false,
                        borderWidth: 2,
                        tension: 0.35,
                        borderColor: borderColor2,
                        backgroundColor: borderColor2,
                        pointStyle: (chartType === "bar" ? "circle" : false),
                        pointRadius: (chartType === "bar" ? 2.5 : 0),
                        pointHoverRadius: 5,
                        pointBackgroundColor: borderColor2,
                        pointBorderColor: "#ffffff",
                        pointBorderWidth: 1.5,
                        order: 1
                    } : {
                        label: dataLabel2,
                        data: data2,
                        type: chartType,
                        fill: true,
                        borderWidth: 1,
                        borderColor: borderColor2,
                        backgroundColor: backgroundColor2,
                        borderSkipped: false,
                        showLine: false,
                        pointStyle: false,
                        order: 1
                    }
                ]
            }
        });

        this.dateUnit = dateUnit;
        const $resetZoom = this.$container.find(".reset-zoom");
        if (this.isZoomedOrPanned()) {
            $resetZoom.show();
        } else {
            $resetZoom.hide();
        }
    }

    isZoomedOrPanned() {
        return (this.chart && typeof this.chart.isZoomedOrPanned === "function" && this.chart.isZoomedOrPanned());
    }

    resetZoom() {
        if (this.chart && typeof this.chart.resetZoom === "function") {
            this.chart.resetZoom();
        }
    }

    setData(labels, data1, data2) {
        if (this.chart) {
            this.chart.data.labels = labels;
            this.chart.data.datasets[0].data = data1;
            this.chart.data.datasets[1].data = data2;
            this.update();
        }
    }

    /**
     * Applies real-time rollup data to the chart.
     * Recognizes date-formatted labels and accumulates values in real time.
     *
     * @param {string} label - ISO 8601 date string
     * @param {number} delta - Primary data delta (e.g. activities or sessions)
     * @param {number|null} aux - Secondary/auxiliary data delta (e.g. errors or active users)
     * @param {boolean} isGroupView - Whether this chart is in group view (accumulate)
     * @param {string|null} dateUnit - Date unit ("hour", "day", "month", "year", or null for realtime)
     */
    applyRollup(label, delta, aux, isGroupView = false, dateUnit = null) {
        if (!this.chart || !label) return;

        const chartLabels = this.chart.data.labels;
        const chartData1 = this.chart.data.datasets[0].data;
        const chartData2 = this.chart.data.datasets[1].data;
        if (!chartLabels) return;

        const incomingDate = dayjs(label);
        if (!incomingDate.isValid()) return;

        const activeDateUnit = dateUnit || this.dateUnit;
        const normalizedIncoming = activeDateUnit
            ? incomingDate.startOf(activeDateUnit)
            : incomingDate.startOf("minute");

        // Search backward from the latest labels
        let targetIndex = -1;
        for (let i = chartLabels.length - 1; i >= 0; i--) {
            const existingLabel = chartLabels[i];
            if (existingLabel === label) {
                targetIndex = i;
                break;
            }
            const existingDate = dayjs(existingLabel);
            const normalizedExisting = activeDateUnit
                ? existingDate.startOf(activeDateUnit)
                : existingDate.startOf("minute");
            if (normalizedExisting.isSame(normalizedIncoming)) {
                targetIndex = i;
                break;
            }
            if (normalizedExisting.isBefore(normalizedIncoming)) {
                break;
            }
        }

        if (targetIndex !== -1) {
            chartData1[targetIndex] = (chartData1[targetIndex] || 0) + (delta || 0);
            if (chartData2[targetIndex] !== null && aux !== null && aux !== undefined) {
                chartData2[targetIndex] = (chartData2[targetIndex] || 0) + aux;
            }
        } else {
            const newLabelStr = activeDateUnit ? normalizedIncoming.toISOString() : label;
            const lastIndex = chartLabels.length - 1;
            const lastDate = (lastIndex >= 0 ? dayjs(chartLabels[lastIndex]) : null);
            const normalizedLast = (lastDate ? (activeDateUnit ? lastDate.startOf(activeDateUnit) : lastDate.startOf("minute")) : null);

            if (!normalizedLast || normalizedIncoming.isAfter(normalizedLast)) {
                chartLabels.push(newLabelStr);
                chartData1.push(delta || 0);
                chartData2.push(aux !== null && aux !== undefined ? aux : null);
            } else {
                let insertIndex = chartLabels.length;
                for (let i = chartLabels.length - 1; i >= 0; i--) {
                    const d = dayjs(chartLabels[i]);
                    const normD = activeDateUnit ? d.startOf(activeDateUnit) : d.startOf("minute");
                    if (normD.isBefore(normalizedIncoming)) {
                        insertIndex = i + 1;
                        break;
                    }
                    if (i === 0) {
                        insertIndex = 0;
                    }
                }
                chartLabels.splice(insertIndex, 0, newLabelStr);
                chartData1.splice(insertIndex, 0, delta || 0);
                chartData2.splice(insertIndex, 0, aux !== null && aux !== undefined ? aux : null);
            }
        }
    }

    update() {
        if (this.chart) {
            this.chart.update();
        }
    }

    requestUpdate() {
        if (this.updateScheduled) return;
        this.updateScheduled = true;
        requestAnimationFrame(() => {
            this.updateScheduled = false;
            if (this.chart) {
                this.chart.update("none");
            }
        });
    }

    destroy() {
        this.updateScheduled = false;
        if (this.chart) {
            this.chart.destroy();
            this.chart = null;
        }
    }
}

/**
 * The viewer component for the AppMon dashboard.
 * Responsible for rendering monitoring data, including logs, metrics, and charts.
 *
 * @version 4.3
 * @last-modified 2026-10-29
 */
class DashboardViewer {
    constructor(sampleInterval, options = {}) {
        this.flagsUrl = options.flagsUrl || "https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/countries/flags/";

        this.isGroupView = false;
        this.groupId = null;

        this.client = null;
        this.enable = false;
        this.visible = false;

        this.displays = {};
        this.painters = {};
        this.charts = {};
        this.consoles = {};
        this.indicators = {};

        this.sampleInterval = sampleInterval;
        this.maxBullets = 500;
        this.activeBulletCount = 0;

        this.activitiesByNode = {};
        this.currentActivityCounts = {};

        this.tempResidentInactiveSecs = 30;
        this.sessionStatsByNode = {};
        this.sessionFilterByDisplay = {};

        this.cachedCanvasWidth = 0;
        this.rollupsByNode = {};
    }

    onNodeLeft(nodeId) {
        if (this.isGroupView && nodeId) {
            for (let exporterKey in this.activitiesByNode) {
                delete this.activitiesByNode[exporterKey][nodeId];
                this.printActivityStatus(exporterKey, this.aggregateActivities(exporterKey));
            }
            for (let exporterKey in this.sessionStatsByNode) {
                delete this.sessionStatsByNode[exporterKey][nodeId];
                this.printSessionEventData(null, exporterKey, this.aggregateSessionStats(exporterKey));
            }
            for (let exporterKey in this.rollupsByNode) {
                const labelMap = this.rollupsByNode[exporterKey];
                if (labelMap) {
                    for (let label in labelMap) {
                        delete labelMap[label][nodeId];
                    }
                }
            }
            for (let key in this.displays) {
                if (key.includes(":event:session")) {
                    const $display = this.displays[key];
                    $display.find(`ul.sessions li[data-node-id='${nodeId}']`).each(function () {
                        const timer = $(this).data("timer");
                        if (timer) clearTimeout(timer);
                        $(this).remove();
                    });
                    this.updateActiveUserCount($display);
                    this.updateSessionNodeFilterButtons($display);
                }
            }
        }
    }

    setIsGroupView(flag, groupId) {
        this.isGroupView = !!flag;
        this.groupId = groupId || null;
    }

    setClient(client) {
        this.client = client;
    }

    setEnable(flag) {
        if (this.enable === !!flag) return;
        this.enable = !!flag;
        if (this.enable) {
            this.resetAllInterimTimers();
        }
    }

    setVisible(flag) {
        this.visible = !!flag;
        if (!this.visible) {
            this.clearBullets();
        }
    }

    putDisplay$(appId, eventId, $display) {
        const key = appId + ":event:" + eventId;
        this.displays[key] = $display;
        if (eventId === "activity" && $display.hasClass("track-box")) {
            const canvas = $display.find(".traffic-canvas")[0];
            if (canvas) {
                this.painters[key] = new TrafficPainter(canvas);
            }
        }
    }

    putChart$(appId, eventId, $chart) {
        const key = appId + ":data:" + eventId;
        this.charts[key] = new DashboardChart($chart, eventId);
    }

    putConsole$(appId, logId, $console) {
        const key = appId + ":log:" + logId;
        this.consoles[key] = $console;
        this.printInitialGroupLogMessage(key);
    }

    putIndicator$(appId, exporterType, exporterName, $indicator) {
        this.indicators[appId + ":" + exporterType + ":" + exporterName] = $indicator;
    }

    getDisplay$(key) {
        return this.displays[key] || null;
    }

    getChart$(key) {
        return this.charts[key] || null;
    }

    getConsole$(key) {
        return this.consoles[key] || null;
    }

    getIndicator$(key) {
        return this.indicators[key] || null;
    }

    updateCanvasWidth() {
        this.cachedCanvasWidth = 0;
    }

    resetCurrentActivityCounts() {
        this.currentActivityCounts = {};
        for (let key in this.indicators) {
            if (key.includes(":event:activity")) {
                this.printCurrentActivityCount(key, 0);
            }
        }
        this.clearBullets();
    }

    clearAllSessions() {
        for (let key in this.displays) {
            if (key.includes(":event:session")) {
                const $display = this.displays[key];
                const $sessions = $display.find("ul.sessions");
                $sessions.find("li").each(function () {
                    const timer = $(this).data("timer");
                    if (timer) clearTimeout(timer);
                });
                $sessions.empty();
                this.updateActiveUserCount($display);
                if (this.isGroupView) {
                    this.updateSessionNodeFilterButtons($display);
                }
            }
        }
    }

    refreshConsole($console) {
        if ($console) {
            this.appendToConsole($console);
        } else {
            for (let key in this.consoles) {
                if (!this.consoles[key].data("pause")) {
                    this.appendToConsole(this.consoles[key]);
                }
            }
        }
    }

    clearConsole($console) {
        if ($console) {
            let timer = $console.data("timer");
            if (timer) {
                clearTimeout(timer);
                $console.removeData("timer");
            }
            $console.removeData("log-buffer");
            let prevTimer = $console.data("prev-timer");
            if (prevTimer) {
                clearTimeout(prevTimer);
                $console.removeData("prev-timer");
            }
            $console.removeData("log-prev-buffer");
            $console.removeData("prev-anchor");
            $console.empty();
            if (this.isGroupView) {
                for (let key in this.consoles) {
                    if (this.consoles[key][0] === $console[0]) {
                        this.printInitialGroupLogMessage(key);
                        break;
                    }
                }
            }
        }
    }

    clearAllConsoles() {
        for (let key in this.consoles) {
            this.clearConsole(this.consoles[key]);
        }
    }

    setLoading(appId, isLoading) {
        for (let key in this.charts) {
            if (key.startsWith(appId + ":")) {
                const dashboardChart = this.charts[key];
                const $chartBox = dashboardChart.$container.closest(".chart-box");
                const $overlay = $chartBox.find(".loading-overlay");
                if (isLoading) {
                    $overlay.css("display", "flex");
                } else {
                    $overlay.hide();
                }
            }
        }
    }

    printInitialGroupLogMessage(key) {
        if (this.isGroupView) {
            this.printMessage("Listening for live log events from all cluster nodes...", key);
        }
    }

    printMessage(message, consoleName) {
        if (consoleName) {
            const $console = this.getConsole$(consoleName);
            if ($console) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push({ html: message, className: "event ellipses" });
                this.appendToConsole($console);
            }
        } else {
            for (let key in this.consoles) {
                this.printMessage(message, key);
            }
        }
    }

    printErrorMessage(message, consoleName) {
        if (consoleName || !Object.keys(this.consoles).length) {
            const $console = this.getConsole$(consoleName);
            if ($console) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                buffer.push({ html: message, className: "event error" });
                this.appendToConsole($console);
            }
        } else {
            for (let key in this.consoles) {
                this.printErrorMessage(message, key);
            }
        }
    }

    prepareToLoadPrevious($console) {
        if (!$console) return 0;
        const loadedLines = $console.find("div").not(".event").length;

        if ($console.data("tailing")) {
            $console.data("tailing", false);
            const $consoleBox = $console.closest(".console-box");
            const $tailingSwitch = $consoleBox.find(".tailing-switch");
            $consoleBox.find(".tailing-status").removeClass("on");
            $tailingSwitch.attr("title", $tailingSwitch.data("title-off"));
        }

        const el = $console[0];
        if (el && el.firstChild) {
            $console.data("prev-anchor", el.firstChild);
        } else {
            $console.removeData("prev-anchor");
        }

        return loadedLines;
    }

    appendToConsole($console) {
        if (!$console) return;
        let timer = $console.data("timer");
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(() => {
            const el = $console[0];
            if (!el) return;

            // Process Buffered Messages
            const buffer = $console.data("log-buffer");
            if (buffer && buffer.length > 0) {
                const fragment = document.createDocumentFragment();
                while (buffer.length > 0) {
                    const item = buffer.shift();
                    const div = document.createElement("div");
                    if (typeof item === "string") {
                        div.textContent = item;
                    } else {
                        if (item.html) div.innerHTML = item.html;
                        else div.textContent = item.text;
                        if (item.className) div.className = item.className;
                    }
                    fragment.appendChild(div);
                }
                el.appendChild(fragment);
            }

            // Scroll to bottom if tailing
            if ($console.data("tailing")) {
                el.scrollTop = el.scrollHeight;
            }

            // Truncate old messages
            const divList = el.getElementsByTagName("div");
            if (divList.length > 11000) {
                const removeCount = divList.length - 10000;
                for (let i = 0; i < removeCount; i++) {
                    el.removeChild(divList[0]);
                }
            }
        }, 300);
        $console.data("timer", timer);
    }

    prependToConsole($console, noAnchoring) {
        if (!$console) return;
        let timer = $console.data("prev-timer");
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(() => {
            requestAnimationFrame(() => {
                const el = $console[0];
                if (!el) return;

                const buffer = $console.data("log-prev-buffer");
                if (buffer && buffer.length > 0) {
                    const oldScrollHeight = el.scrollHeight;
                    const oldScrollTop = el.scrollTop;

                    const fragment = document.createDocumentFragment();
                    while (buffer.length > 0) {
                        const item = buffer.shift();
                        const div = document.createElement("div");
                        if (typeof item === "string") {
                            div.textContent = item;
                        } else {
                            if (item.html) div.innerHTML = item.html;
                            else div.textContent = item.text;
                            if (item.className) div.className = item.className;
                        }
                        fragment.appendChild(div);
                    }

                    if (noAnchoring) {
                        el.prepend(fragment);
                        el.scrollTop = 0;
                        $console.removeData("prev-anchor");
                    } else {
                        const anchor = $console.data("prev-anchor");
                        if (anchor && anchor.parentNode === el) {
                            el.insertBefore(fragment, anchor);
                        } else {
                            el.appendChild(fragment);
                        }
                        el.scrollTop = oldScrollTop + (el.scrollHeight - oldScrollHeight);
                    }
                }
            });
        }, 50);
        $console.data("prev-timer", timer);
    }

    processMessage(nodeId, message) {
        const idx1 = message.indexOf(":");
        const idx2 = (idx1 !== -1 ? message.indexOf(":", idx1 + 1) : -1);
        const idx3 = (idx2 !== -1 ? message.indexOf(":", idx2 + 1) : -1);
        if (idx3 === -1) {
            return;
        }

        const appId = message.substring(0, idx1);
        let exporterType = message.substring(idx1 + 1, idx2);
        const exporterName = message.substring(idx2 + 1, idx3);

        let subType = "";
        if (exporterType.includes("/")) {
            const parts = exporterType.split("/");
            exporterType = parts[0];
            subType = parts[1];
        }

        const exporterKey = appId + ":" + exporterType + ":" + exporterName;
        const messageContent = message.substring(idx3 + 1);

        switch (exporterType) {
            case "event":
                if (exporterName === "_indicate") {
                    this.indicate(nodeId, appId, exporterType, exporterName);
                    return;
                }
                if (messageContent.length) {
                    const eventData = JSON.parse(messageContent);
                    this.processEventData(nodeId, appId, exporterType, exporterName, exporterKey, eventData);
                }
                break;
            case "log":
                this.processLogMessage(nodeId, appId, exporterType, exporterName, exporterKey, messageContent, subType);
                break;
            case "data":
                if (messageContent.length) {
                    if (subType === "chart") {
                        const chartData = JSON.parse(messageContent);
                        this.processChartData(nodeId, appId, exporterType, exporterName, exporterKey, chartData);
                    }
                }
                break;
        }
    }

    processLogMessage(nodeId, appId, exporterType, logId, exporterKey, messageContent, subType) {
        this.indicate(nodeId, appId, exporterType, logId);
        const $console = this.getConsole$(exporterKey);
        if ($console) {
            const formatLine = (line) => {
                if (this.isGroupView && nodeId) {
                    const safeNodeId = String(nodeId).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
                    const safeLine = String(line).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
                    return { html: `<span class="node-badge" title="${safeNodeId}">${safeNodeId}</span>` + safeLine };
                }
                return line;
            };

            if (subType === "p") {
                if (messageContent) {
                    const lines = messageContent.split("\n");
                    let prevBuffer = $console.data("log-prev-buffer");
                    if (!prevBuffer) {
                        prevBuffer = [];
                        $console.data("log-prev-buffer", prevBuffer);
                    }
                    for (let i = 0; i < lines.length; i++) {
                        prevBuffer.push(formatLine(lines[i]));
                    }
                    this.prependToConsole($console);
                } else {
                    let prevBuffer = $console.data("log-prev-buffer");
                    if (!prevBuffer) {
                        prevBuffer = [];
                        $console.data("log-prev-buffer", prevBuffer);
                    }
                    const msg = (this.isGroupView && nodeId ? `[${nodeId}] ` : "") + "No more logs to load.";
                    prevBuffer.push({ html: msg, className: "event ellipses" });
                    this.prependToConsole($console, true);
                    $console.closest(".console-box").find(".load-previous").hide();
                }
            } else if (!$console.data("pause")) {
                let buffer = $console.data("log-buffer");
                if (!buffer) {
                    buffer = [];
                    $console.data("log-buffer", buffer);
                }
                if (messageContent.includes("\n")) {
                    const lines = messageContent.split("\n");
                    for (let i = 0; i < lines.length; i++) {
                        buffer.push(formatLine(lines[i]));
                    }
                } else {
                    buffer.push(formatLine(messageContent));
                }
                this.appendToConsole($console);
            }
        }
    }

    processEventData(nodeId, appId, exporterType, eventId, exporterKey, eventData) {
        switch (eventId) {
            case "activity":
                this.indicate(nodeId, appId, exporterType, eventId);
                if (eventData.activities) {
                    if (this.isGroupView && nodeId) {
                        if (!this.activitiesByNode[exporterKey]) {
                            this.activitiesByNode[exporterKey] = {};
                        }
                        this.activitiesByNode[exporterKey][nodeId] = eventData.activities;
                        this.printActivityStatus(exporterKey, this.aggregateActivities(exporterKey));
                    } else {
                        this.printActivityStatus(exporterKey, eventData.activities);
                    }
                }
                if (this.visible) {
                    const $track = this.getDisplay$(exporterKey);
                    if ($track) {
                        const varName = exporterKey.replace(/:/g, '_');
                        if (!this.currentActivityCounts[varName]) {
                            this.currentActivityCounts[varName] = 0;
                            this.printCurrentActivityCount(exporterKey, 0);
                        }
                        this.launchBullet($track, eventData, () => {
                            this.currentActivityCounts[varName]++;
                            this.printCurrentActivityCount(exporterKey, this.currentActivityCounts[varName]);
                        }, () => {
                            if (this.currentActivityCounts[varName] > 0) {
                                this.currentActivityCounts[varName]--;
                            }
                            this.printCurrentActivityCount(exporterKey, this.currentActivityCounts[varName]);
                        });
                    }
                } else {
                    this.printCurrentActivityCount(exporterKey, 0);
                }
                this.updateActivityCount(
                    nodeId,
                    appId + ":" + exporterType + ":session",
                    eventData.sessionId,
                    eventData.activityCount || 0);
                break;
            case "session":
                if (this.isGroupView && nodeId) {
                    if (!this.sessionStatsByNode[exporterKey]) {
                        this.sessionStatsByNode[exporterKey] = {};
                    }
                    this.sessionStatsByNode[exporterKey][nodeId] = eventData;
                    const aggregatedEventData = this.aggregateSessionStats(exporterKey, eventData);
                    this.printSessionEventData(nodeId, exporterKey, aggregatedEventData);
                } else {
                    this.printSessionEventData(nodeId, exporterKey, eventData);
                }
                break;
        }
    }

    launchBullet($track, eventData, onLeaving, onArriving) {
        if (eventData.elapsedTime === undefined || eventData.elapsedTime === null) return;

        // Skip visualization and counting if tab is hidden
        if (document.hidden) return;

        if (onLeaving) onLeaving();

        // Find the painter associated with this track-box
        let painter = null;
        for (let key in this.displays) {
            if (this.displays[key][0] === $track[0]) {
                painter = this.painters[key];
                break;
            }
        }

        if (painter) {
            if (this.activeBulletCount < this.maxBullets) {
                this.activeBulletCount++;
                painter.addBullet(eventData, () => {
                    this.activeBulletCount--;
                    if (onArriving) onArriving();
                });
            } else {
                // Still update counts via timer even if capped
                setTimeout(() => {
                    if (onArriving) onArriving();
                }, eventData.elapsedTime + 900);
            }
        }
    }

    clearBullets() {
        for (let key in this.painters) {
            this.painters[key].clear();
        }
        this.activeBulletCount = 0;
    }

    indicate(nodeId, appId, exporterType, exporterName) {
        this.blink(this.getIndicator$("group:event:"));
        this.blink(this.getIndicator$("node:event:" + nodeId));
        if (this.visible) {
            this.blink(this.getIndicator$("app:event:" + appId));
            if (exporterType === "log") {
                this.blink(this.getIndicator$(appId + ":log:" + exporterName));
            }
        }
    }

    blink($indicator) {
        if ($indicator && !$indicator.hasClass("on")) {
            $indicator.addClass("blink on");
            setTimeout(() => {
                $indicator.removeClass("blink on");
            }, 500);
        }
    }

    aggregateActivities(exporterKey) {
        let interim = 0, errors = 0, total = 0;
        const actMap = this.activitiesByNode[exporterKey];
        if (actMap) {
            for (let nid in actMap) {
                const act = actMap[nid];
                if (act) {
                    interim += (act.interim || 0);
                    errors += (act.errors || 0);
                    total += (act.total || 0);
                }
            }
        }
        return { interim, errors, total };
    }

    printActivityStatus(exporterKey, activities) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            const separator = (activities.errors > 0 ? " / " : (activities.interim > 0 ? "+" : "-"));
            $activityStatus.find(".interim .separator").text(separator);
            $activityStatus.find(".interim .total").text(activities.interim > 0 ? activities.interim : "");
            $activityStatus.find(".interim .errors").text(activities.errors > 0 ? activities.errors : "");
            $activityStatus.find(".cumulative .total").text(activities.total);
        }
    }

    resetInterimActivityStatus(exporterKey) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            $activityStatus.find(".interim .separator").text("");
            $activityStatus.find(".interim .total").text(0);
            $activityStatus.find(".interim .errors").text("");
        }
    }

    resetInterimTimer(exporterKey) {
        if (this.sampleInterval) {
            const $activityStatus = this.getIndicator$(exporterKey);
            if ($activityStatus) {
                const $samplingTimerBar = $activityStatus.find(".sampling-timer-bar");
                const $samplingTimerStatus = $activityStatus.find(".sampling-timer-status");
                if ($samplingTimerBar.length) {
                    const now = Date.now();
                    const lastReset = $samplingTimerBar.data("last-reset") || 0;
                    if (now - lastReset < 1000) {
                        return;
                    }
                    $samplingTimerBar.data("last-reset", now);

                    let timer = $samplingTimerBar.data("timer");
                    if (timer) {
                        clearInterval(timer);
                        $samplingTimerBar.removeData("timer");
                    }
                    $samplingTimerBar.stop(true, false);
                    let second = (dayjs().minute() * 60 + dayjs().second()) % this.sampleInterval;
                    $samplingTimerBar.animate({ height: 0 }, 600);
                    $samplingTimerBar.animate({ height: (second++ / this.sampleInterval * 100).toFixed(2) + "%" }, 400);
                    $samplingTimerStatus.text(second + "/" + this.sampleInterval);
                    timer = setInterval(() => {
                        if (!this.enable) {
                            clearInterval(timer);
                            $samplingTimerBar.removeData("timer");
                            return;
                        }
                        const percent = second++ / this.sampleInterval * 100;
                        $samplingTimerBar.css("height", percent.toFixed(2) + "%");
                        $samplingTimerStatus.text(second + "/" + this.sampleInterval);
                        if (second > this.sampleInterval) second = 0;
                        else if (second % 10 === 0) {
                            second = (dayjs().minute() * 60 + dayjs().second()) % this.sampleInterval;
                        }
                    }, 1000);
                    $samplingTimerBar.data("timer", timer);
                }
            }
        }
    }

    resetAllInterimTimers() {
        for (let key in this.indicators) {
            const $activityStatus = this.getIndicator$(key);
            if ($activityStatus.hasClass("activity-status")) {
                this.resetInterimTimer(key);
            }
        }
    }

    printCurrentActivityCount(exporterKey, count) {
        const $activityStatus = this.getIndicator$(exporterKey);
        if ($activityStatus) {
            $activityStatus.find(".current .total").text(count);
        }
    }
    
    aggregateSessionStats(exporterKey, baseEventData = {}) {
        let numberOfCreated = 0;
        let numberOfExpired = 0;
        let numberOfActives = 0;
        let highestNumberOfActives = 0;
        let numberOfRejected = 0;
        let minStartTime = null;

        const statsMap = this.sessionStatsByNode[exporterKey];
        if (statsMap) {
            for (let nid in statsMap) {
                const s = statsMap[nid];
                if (s) {
                    numberOfCreated += (s.numberOfCreated || 0);
                    numberOfExpired += (s.numberOfExpired || 0);
                    numberOfActives += (s.numberOfActives || 0);
                    highestNumberOfActives += (s.highestNumberOfActives || 0);
                    numberOfRejected += (s.numberOfRejected || 0);
                    if (s.startTime) {
                        if (!minStartTime || s.startTime < minStartTime) {
                            minStartTime = s.startTime;
                        }
                    }
                }
            }
        }

        return {
            ...baseEventData,
            numberOfCreated,
            numberOfExpired,
            numberOfActives,
            highestNumberOfActives,
            numberOfRejected,
            startTime: minStartTime
        };
    }

    printSessionEventData(nodeId, exporterKey, eventData) {
        const $display = this.getDisplay$(exporterKey);
        if ($display) {
            $display.find(".numberOfUsers").text(eventData.numberOfUsers || 0);
            $display.find(".numberOfCreated").text(eventData.numberOfCreated);
            $display.find(".numberOfExpired").text(eventData.numberOfExpired);
            $display.find(".numberOfActives").text(eventData.numberOfActives);
            $display.find(".highestNumberOfActives").text(eventData.highestNumberOfActives);
            $display.find(".numberOfRejected").text(eventData.numberOfRejected);
            if (eventData.startTime) {
                $display.find(".startTime").text(dayjs.utc(eventData.startTime).local().format("LLL"));
            }
            const $sessions = $display.find("ul.sessions");

            if (eventData.fullSync) {
                const newSids = (eventData.createdSessions || []).map(s => {
                    const session = (typeof s === "string" ? JSON.parse(s) : s);
                    return session.sessionId;
                });
                $sessions.find("li").each(function () {
                    const $li = $(this);
                    const sid = $li.attr("data-sid") || $li.data("sid");
                    const nid = $li.attr("data-node-id");
                    if ((!nodeId || !nid || String(nid) === String(nodeId)) && !newSids.includes(sid)) {
                        const timer = $li.data("timer");
                        if (timer) clearTimeout(timer);
                        $li.remove();
                    }
                });
            }

            if (eventData.createdSessions) {
                eventData.createdSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session, nodeId));
            }
            if (eventData.destroyedSessions) {
                eventData.destroyedSessions.forEach(sessionId => {
                    const nodeSelector = (this.isGroupView && nodeId) ? `[data-node-id='${nodeId}']` : "";
                    $sessions.find(`li[data-sid='${sessionId}']${nodeSelector}`).remove();
                });
            }
            if (eventData.evictedSessions) {
                const nodeSelector = (this.isGroupView && nodeId) ? `[data-node-id='${nodeId}']` : "";
                eventData.evictedSessions.forEach(sessionId => {
                    const $li = $sessions.find(`li[data-sid='${sessionId}']${nodeSelector}`);
                    let timer = $li.data("timer");
                    if (timer) clearTimeout(timer);
                    if ($li.data("temp-resident")) {
                        $li.remove(); // Temp resident session removed immediately upon eviction
                        return;
                    }
                    $li.addClass("inactive");
                    let inactiveInterval = $li.data("inactive-interval") || 0;
                    inactiveInterval = (inactiveInterval <= 0 ? this.tempResidentInactiveSecs : Math.min(inactiveInterval, this.tempResidentInactiveSecs)) * 1000;
                    timer = setTimeout(() => {
                        $li.remove();
                        this.updateActiveUserCount($display);
                        if (this.isGroupView) {
                            this.updateSessionNodeFilterButtons($display);
                        }
                    }, inactiveInterval);
                    $li.data("timer", timer);
                });
            }
            if (eventData.residedSessions) {
                eventData.residedSessions.forEach(session => this.addSession($sessions, typeof session === "string" ? JSON.parse(session) : session, nodeId));
            }
            if (eventData.changedSessions) {
                for (let oldSessionId in eventData.changedSessions) {
                    const session = eventData.changedSessions[oldSessionId];
                    this.changeSessionId($sessions, oldSessionId, typeof session === "string" ? JSON.parse(session) : session, nodeId);
                }
            }
            this.updateActiveUserCount($display);
            if (this.isGroupView) {
                this.updateSessionNodeFilterButtons($display);
            }
        }
    }

    changeSessionId($sessions, oldSessionId, session, nodeId) {
        const nodeSelector = (this.isGroupView && nodeId) ? `[data-node-id='${nodeId}']` : "";
        $sessions.find(`li[data-sid='${oldSessionId}']${nodeSelector}`).each(function () {
            const timer = $(this).data("timer");
            if (timer) clearTimeout(timer);
        }).remove();
        this.addSession($sessions, session, nodeId);
    }

    addSession($sessions, session, nodeId) {
        const nodeSelector = (this.isGroupView && nodeId) ? `[data-node-id='${nodeId}']` : "";
        $sessions.find(`li[data-sid='${session.sessionId}']${nodeSelector}`).each(function () {
            const timer = $(this).data("timer");
            if (timer) clearTimeout(timer);
        }).remove();

        const $count = $("<div class='count'></div>").text(session.activityCount || 0);
        if (session.activityCount > 0) $count.text(session.activityCount);
        if (session.activityCount > 0 && !session.tempResident || !session.countryCode) $count.addClass("counting");
        if (session.username) $count.addClass("active");

        const $li = $("<li/>")
            .attr("data-sid", session.sessionId)
            .attr("data-inactive-interval", session.inactiveInterval)
            .append($count);

        const userKey = session.username ? ("u:" + session.username) :
            (session.ipAddress ? ("i:" + session.ipAddress) : ("s:" + session.sessionId));
        $li.attr("data-user-key", userKey);

        if (this.isGroupView && nodeId) {
            $li.attr("data-node-id", nodeId);
            const $sessionBox = $sessions.closest(".session-box");
            const appId = $sessionBox.data("app-id");
            const eventId = $sessionBox.data("event-id");
            const key = appId + ":event:" + eventId;
            const currentFilter = this.sessionFilterByDisplay[key];
            if (currentFilter && String(currentFilter) !== String(nodeId)) {
                $li.addClass("filtered-out");
            }
        }

        const inactiveInterval = session.inactiveInterval;
        if (inactiveInterval && inactiveInterval > 0) {
            $li.attr("data-inactive-interval", inactiveInterval);
            const timer = setTimeout(() => {
                $li.remove();
                this.updateActiveUserCount($sessions.closest(".session-box"));
                if (this.isGroupView) {
                    this.updateSessionNodeFilterButtons($sessions.closest(".session-box"));
                }
            }, inactiveInterval * 1000);
            $li.data("timer", timer);
        }

        if (session.tempResident) {
            $li.attr("data-temp-resident", true).addClass("inactive");
        }

        if (session.countryCode) {
            const code = session.countryCode.toLowerCase();
            const countryInfo = (typeof countries !== "undefined" && countries[session.countryCode])
                ? countries[session.countryCode]
                : null;
            $("<img class='flag' alt=''/>")
                .attr("src", this.flagsUrl + code + ".png")
                .attr("alt", session.countryCode)
                .attr("title", countryInfo ? countryInfo.name : session.countryCode)
                .appendTo($li);
        }
        if (session.username) {
            $("<div class='username'/>").text(session.username).appendTo($li);
        }

        const $detail = $("<div class='detail'/>")
            .append($("<p/>").text(session.sessionId))
            .append($("<p/>").text(dayjs.utc(session.createAt).local().format("LLL")));
        if (session.ipAddress) $detail.append($("<p/>").text(session.ipAddress));
        $detail.appendTo($li);

        if (session.tempResident) $li.appendTo($sessions);
        else $li.prependTo($sessions);
    }

    updateActivityCount(nodeId, exporterKey, sessionId, activityCount) {
        const $display = this.getDisplay$(exporterKey);
        if ($display) {
            const nodeSelector = (this.isGroupView && nodeId) ? `[data-node-id='${nodeId}']` : "";
            const $li = $display.find(`ul.sessions li[data-sid='${sessionId}']${nodeSelector}`);
            const $count = $li.find(".count").text(activityCount);
            if (activityCount > 1) $count.addClass("counting");
            const inactiveInterval = $li.data("inactive-interval");
            if (inactiveInterval) {
                let timer = $li.data("timer");
                if (timer) clearTimeout(timer);
                timer = setTimeout(() => {
                    $li.remove();
                    this.updateActiveUserCount($display);
                    if (this.isGroupView) {
                        this.updateSessionNodeFilterButtons($display);
                    }
                }, inactiveInterval * 1000);
                $li.data("timer", timer);
            }
        }
    }

    setSessionFilter(appId, eventId, nodeId) {
        const key = appId + ":event:" + eventId;
        this.sessionFilterByDisplay[key] = nodeId || null;
        const $display = this.getDisplay$(key);
        if ($display) {
            this.applySessionFilter($display, nodeId || null);
        }
    }

    applySessionFilter($display, targetNodeId) {
        const $sessions = $display.find("ul.sessions");
        if (!targetNodeId) {
            $sessions.find("li").removeClass("filtered-out");
        } else {
            $sessions.find("li").each(function () {
                const nid = $(this).attr("data-node-id");
                $(this).toggleClass("filtered-out", String(nid) !== String(targetNodeId));
            });
        }
    }

    updateSessionNodeFilterButtons($display) {
        if (!this.isGroupView || !$display || !$display.length) return;
        const $sessionBox = $display.hasClass("session-box") ? $display : $display.closest(".session-box");
        if (!$sessionBox.length) return;

        const $filter = $sessionBox.find(".session-node-filter");
        if (!$filter.length) return;

        const $sessions = $sessionBox.find("ul.sessions");
        const nodeCounts = {};
        $sessions.find("li").each(function () {
            const nid = $(this).attr("data-node-id");
            if (nid) {
                nodeCounts[nid] = (nodeCounts[nid] || 0) + 1;
            }
        });

        const appId = $sessionBox.data("app-id") || $sessionBox.attr("data-app-id");
        const eventId = $sessionBox.data("event-id") || $sessionBox.attr("data-event-id");
        const key = appId + ":event:" + eventId;
        const currentFilterNodeId = this.sessionFilterByDisplay[key];

        $filter.find(".btn-node").each(function () {
            const nid = $(this).attr("data-node-id");
            if (!nid) {
                return; // "All" button remains visible
            }
            const count = nodeCounts[nid] || 0;
            if (count > 0) {
                $(this).show();
            } else {
                $(this).hide();
            }
        });

        if (currentFilterNodeId && (!nodeCounts[currentFilterNodeId] || nodeCounts[currentFilterNodeId] <= 0)) {
            $filter.find(".btn-node").removeClass("on");
            $filter.find(".btn-node").filter(function () {
                return !$(this).attr("data-node-id");
            }).addClass("on");
            this.setSessionFilter(appId, eventId, null);
        }
    }

    updateActiveUserCount($display) {
        if (!$display || !$display.length) return;
        const $sessionBox = $display.hasClass("session-box") ? $display : $display.closest(".session-box");
        const $sessions = $sessionBox.find("ul.sessions");
        const userKeys = new Set();
        $sessions.find("li:not(.inactive)").each(function () {
            const ukey = $(this).attr("data-user-key");
            if (ukey) {
                userKeys.add(ukey);
            }
        });
        $sessionBox.find(".numberOfUsers").text(userKeys.size);
    }

    processChartData(nodeId, appId, exporterType, eventId, exporterKey, chartData) {
        const dashboardChart = this.getChart$(exporterKey);
        if (!dashboardChart) return;
        this.setLoading(appId, false);

        if (eventId === "activity") {
            const key = appId + ":event:" + eventId;
            if (!dashboardChart.isDrawn()) {
                if (!this.isGroupView || chartData.scope === "group") {
                    this.resetInterimTimer(key);
                }
            } else if (chartData.rolledUp) {
                if (this.isGroupView && nodeId) {
                    const actMap = this.activitiesByNode[key];
                    if (actMap && actMap[nodeId]) {
                        actMap[nodeId].interim = 0;
                        actMap[nodeId].errors = 0;
                        this.printActivityStatus(key, this.aggregateActivities(key));
                    }
                } else {
                    this.resetInterimActivityStatus(key);
                }
            }
        }

        const dateUnit = (chartData.rolledUp ? dashboardChart.dateUnit : (chartData.period || chartData.dateUnit));
        const dateOffset = (chartData.rolledUp ? dashboardChart.dateOffset : chartData.dateOffset);
        const labels = chartData.labels;
        const data1 = chartData.data1;
        const data2 = chartData.data2;

        if (!chartData.rolledUp) {
            if (this.isGroupView && chartData.scope !== "group" ||
                !this.isGroupView && chartData.scope === "group") {
                return;
            }
            dashboardChart.ensureCanvas();
            if (!chartData.period) {
                this.pruneDataPoints(labels, data1, data2, dashboardChart.$container);
            }
            dashboardChart.draw(dateUnit, labels, data1, data2);
            dashboardChart.dateOffset = dateOffset;
            dashboardChart.period = chartData.period || null;
        } else if (dashboardChart.isDrawn() && !dateOffset && !dashboardChart.period) {
            const label = labels && labels.length ? labels[0] : null;
            const delta = data1 && data1.length ? data1[0] : 0;
            const aux = data2 && data2.length ? data2[0] : null;
            if (label) {
                const diff = this.trackRollupDiff(nodeId, exporterKey, label, delta, aux);
                if (diff.deltaDiff !== 0 || (diff.auxDiff !== null && diff.auxDiff !== 0)) {
                    dashboardChart.applyRollup(label, diff.deltaDiff, diff.auxDiff, this.isGroupView, dateUnit);
                    this.pruneDataPoints(
                        dashboardChart.getLabels(),
                        dashboardChart.getDataset(0),
                        dashboardChart.getDataset(1),
                        dashboardChart.$container
                    );
                    dashboardChart.requestUpdate();
                }
                if (eventId === "activity" && diff.isFirstForLabel) {
                    const key = appId + ":event:" + eventId;
                    this.resetInterimTimer(key);
                }
            }
        }
    }

    trackRollupDiff(nodeId, exporterKey, label, delta, aux) {
        if (!this.rollupsByNode[exporterKey]) {
            this.rollupsByNode[exporterKey] = {};
        }
        const labelMap = this.rollupsByNode[exporterKey];
        let isFirstForLabel = false;
        if (!labelMap[label]) {
            labelMap[label] = {};
            isFirstForLabel = true;
            const keys = Object.keys(labelMap);
            if (keys.length > 50) {
                delete labelMap[keys[0]];
            }
        }
        const nodeEntries = labelMap[label];
        const nid = nodeId || "unknown";
        const prev = nodeEntries[nid] || { delta: 0, aux: 0 };
        const currentDelta = delta || 0;
        const currentAux = (aux !== null && aux !== undefined) ? aux : null;

        const deltaDiff = currentDelta - prev.delta;
        let auxDiff = null;
        if (currentAux !== null) {
            auxDiff = currentAux - (prev.aux !== null ? prev.aux : 0);
        }

        nodeEntries[nid] = { delta: currentDelta, aux: currentAux };
        return { deltaDiff, auxDiff, isFirstForLabel };
    }

    pruneDataPoints(labels, data1, data2, $container) {
        if (this.cachedCanvasWidth === 0) {
            let w = 0;
            if ($container) {
                w = $container.find("canvas").width();
                if (w === 0) w = $container.width();
            }
            if (w === 0) {
                for (let key in this.charts) {
                    const dashboardChart = this.charts[key];
                    if (dashboardChart) {
                        w = dashboardChart.$container.find("canvas").width();
                        if (w === 0) w = dashboardChart.$container.width();
                        if (w > 0) break;
                    }
                }
            }
            if (w > 0) {
                this.cachedCanvasWidth = w - 90;
            }
        }
        const maxLabels = (this.cachedCanvasWidth > 0 ? Math.floor(this.cachedCanvasWidth / 21) : 0);
        if (maxLabels > 0) {
            const cnt = labels.length - maxLabels;
            if (cnt > 0) {
                labels.splice(0, cnt);
                data1.splice(0, cnt);
                data2.splice(0, cnt);
            }
        }
    }

    getMaxStartDatetime(appId) {
        let result = "";
        for (let key in this.charts) {
            if (key.startsWith(appId + ":")) {
                const dashboardChart = this.charts[key];
                if (dashboardChart && dashboardChart.isDrawn()) {
                    const labels = dashboardChart.getLabels();
                    if (labels.length && labels[0] > result) {
                        result = labels[0];
                    }
                }
            }
        }
        return result;
    }
}

/**
 * The MetricsViewer component for the AppMon dashboard.
 * Dedicated to managing real-time node and application metrics,
 * sparkline history charts, and metric detail popovers.
 *
 * @version 4.3
 * @last-modified 2026-10-09
 */
class MetricsViewer {
    constructor() {
        this.metrics = {};
        this.metricHistories = {};
        this.maxMetricHistory = 30;
        this.activePopoverKey = null;
    }

    putMetric$(nodeId, appId, metricId, $metric) {
        const key = nodeId + ":" + appId + ":metric:" + metricId;
        this.metrics[key] = $metric;
    }

    getMetric$(nodeId, exporterKey) {
        return this.metrics[nodeId + ":" + exporterKey];
    }

    processMessage(nodeId, message) {
        const idx1 = message.indexOf(":");
        const idx2 = (idx1 !== -1 ? message.indexOf(":", idx1 + 1) : -1);
        const idx3 = (idx2 !== -1 ? message.indexOf(":", idx2 + 1) : -1);
        if (idx3 === -1) {
            return;
        }

        const appId = message.substring(0, idx1);
        let exporterType = message.substring(idx1 + 1, idx2);
        const metricId = message.substring(idx2 + 1, idx3);

        if (exporterType.includes("/")) {
            exporterType = exporterType.split("/")[0];
        }

        if (exporterType === "metric") {
            const exporterKey = appId + ":metric:" + metricId;
            const messageContent = message.substring(idx3 + 1);
            if (messageContent.length) {
                try {
                    const metricData = JSON.parse(messageContent);
                    this.processMetricData(nodeId, appId, exporterType, metricId, exporterKey, metricData);
                } catch (e) {
                    console.error("Failed to parse metric data:", e, messageContent);
                }
            }
        }
    }

    processMetricData(nodeId, appId, exporterType, metricId, exporterKey, metricData) {
        const $metric = this.getMetric$(nodeId, exporterKey);
        const historyKey = nodeId + ":" + exporterKey;
        if ($metric) {
            const $dd = $metric.find("dd").not(".sparkline-wrap");
            let $val = $dd.find(".value");
            if (!$val.length) {
                $dd.empty();
                $val = $("<span class=\"value\"></span>").appendTo($dd);
                if (metricData.unit) {
                    $("<small class=\"unit\"></small>").text(metricData.unit).appendTo($dd);
                }
            } else {
                const $unit = $dd.find(".unit");
                if ($unit.length && !$unit.text() && metricData.unit) {
                    $unit.text(metricData.unit);
                }
            }
            let formatted = metricData.format;
            for (let key in metricData.data) {
                formatted = formatted.replace("{" + key + "}", metricData.data[key]);
            }
            $val.text(formatted);
            $metric.attr("title", JSON.stringify(metricData.data, null, 2));

            // Record history and render sparkline
            this.recordMetricHistory(historyKey, metricId, metricData, formatted);
            const $sparkline = $metric.find("canvas.sparkline");
            if ($sparkline.length) {
                this.renderSparkline($sparkline[0], historyKey, metricId);
            }

            // Update popover chart if this metric is currently displayed
            if (this.activePopoverKey === historyKey) {
                this.renderPopoverChart();
            }
        }
    }

    recordMetricHistory(historyKey, metricId, metricData, formatted) {
        if (!this.metricHistories[historyKey]) {
            this.metricHistories[historyKey] = [];
        }
        const history = this.metricHistories[historyKey];
        const data = metricData.data || {};

        let val1 = 0;
        let val2 = null;
        let label1 = "Value";
        let label2 = null;

        if (metricId === "cpu") {
            val1 = (typeof data.processCpu === "number" ? data.processCpu : 0);
            val2 = (typeof data.systemCpu === "number" && data.systemCpu >= 0 ? data.systemCpu : null);
            label1 = "Process CPU";
            label2 = "System CPU";
        } else if (metricId === "heap") {
            val1 = (typeof data.used === "number" ? data.used : 0);
            val2 = (typeof data.max === "number" && data.max > 0 ? data.max : (typeof data.committed === "number" ? data.committed : null));
            label1 = "Used Memory";
            label2 = (typeof data.max === "number" && data.max > 0 ? "Max Memory" : "Committed");
        } else if (metricId.endsWith("-tp") || metricId === "tp") {
            val1 = (typeof data.active === "number" ? data.active : 0);
            val2 = (typeof data.total === "number" ? data.total : null);
            label1 = "Active Threads";
            label2 = "Total Threads";
        } else if (metricId.startsWith("cp") || metricId.endsWith("-cp") || metricId === "cp" || data.poolName) {
            val1 = (typeof data.used === "number" ? data.used : (typeof data.active === "number" ? data.active : 0));
            val2 = (typeof data.total === "number" ? data.total : (typeof data.max === "number" ? data.max : null));
            label1 = "Used Connections";
            label2 = "Total Connections";
        } else {
            // Determine primary and secondary values from format string if available
            const formatKeys = [];
            if (metricData.format) {
                const matches = metricData.format.match(/\{([a-zA-Z0-9_-]+)\}/g);
                if (matches) {
                    matches.forEach(m => {
                        const k = m.slice(1, -1);
                        if (typeof data[k] === "number" && !formatKeys.includes(k)) {
                            formatKeys.push(k);
                        }
                    });
                }
            }
            if (formatKeys.length > 0) {
                val1 = data[formatKeys[0]];
                label1 = formatKeys[0];
                if (formatKeys.length > 1) {
                    val2 = data[formatKeys[1]];
                    label2 = formatKeys[1];
                }
            } else {
                const priorityKeys = ["used", "active", "current", "count", "value", "total"];
                const numKeys = Object.keys(data).filter(k => typeof data[k] === "number");
                const pKey = priorityKeys.find(k => typeof data[k] === "number");
                if (pKey) {
                    val1 = data[pKey];
                    label1 = pKey;
                    const remKeys = numKeys.filter(k => k !== pKey);
                    if (remKeys.length > 0) {
                        val2 = data[remKeys[0]];
                        label2 = remKeys[0];
                    }
                } else if (numKeys.length > 0) {
                    val1 = data[numKeys[0]];
                    label1 = numKeys[0];
                    if (numKeys.length > 1) {
                        val2 = data[numKeys[1]];
                        label2 = numKeys[1];
                    }
                }
            }
        }

        history.push({
            time: Date.now(),
            val1,
            val2,
            label1,
            label2,
            formatted,
            unit: metricData.unit || "",
            title: metricData.title || metricId,
            metricId,
            data
        });

        if (history.length > this.maxMetricHistory) {
            history.shift();
        }
    }

    renderSparkline(canvas, historyKey, metricId) {
        const history = this.metricHistories[historyKey];
        if (!history || history.length < 2) return;

        const dpr = window.devicePixelRatio || 1;
        const width = 48;
        const height = 16;
        if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
            canvas.width = width * dpr;
            canvas.height = height * dpr;
        }

        const ctx = canvas.getContext("2d");
        ctx.save();
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, width, height);

        let min = Infinity;
        let max = -Infinity;
        for (let i = 0; i < history.length; i++) {
            const v = history[i].val1;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (metricId === "cpu") {
            min = 0;
            max = Math.max(max, 100);
        } else if (max === min) {
            max += 1;
            min = Math.max(0, min - 1);
        }
        const range = max - min || 1;
        const paddingY = 2;
        const paddingX = 2;
        const usableH = height - (paddingY * 2);
        const usableW = width - (paddingX * 2);
        const stepX = usableW / (history.length - 1);

        const points = [];
        for (let i = 0; i < history.length; i++) {
            const x = paddingX + (i * stepX);
            const normY = (history[i].val1 - min) / range;
            const y = height - paddingY - (normY * usableH);
            points.push({ x, y });
        }

        const isHigh = (metricId === "cpu" && history[history.length - 1].val1 >= 85);
        const strokeColor = isHigh ? "#ef4444" : "#0284c7";
        const fillColor = isHigh ? "rgba(239, 68, 68, 0.3)" : "rgba(2, 132, 199, 0.25)";

        // Gradient Fill
        ctx.beginPath();
        ctx.moveTo(points[0].x, height);
        points.forEach(p => ctx.lineTo(p.x, p.y));
        ctx.lineTo(points[points.length - 1].x, height);
        ctx.closePath();
        ctx.fillStyle = fillColor;
        ctx.fill();

        // Stroke Line
        ctx.beginPath();
        points.forEach((p, idx) => {
            if (idx === 0) ctx.moveTo(p.x, p.y);
            else ctx.lineTo(p.x, p.y);
        });
        ctx.strokeStyle = strokeColor;
        ctx.lineWidth = 1.2;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        ctx.stroke();

        // Last Point Dot
        const last = points[points.length - 1];
        ctx.beginPath();
        ctx.arc(last.x, last.y, 1.8, 0, Math.PI * 2);
        ctx.fillStyle = strokeColor;
        ctx.fill();

        ctx.restore();
    }

    toggleMetricPopover(exporterKey, $metric, nodeId) {
        const historyKey = nodeId ? (nodeId + ":" + exporterKey) : exporterKey;
        if (this.activePopoverKey === historyKey) {
            this.hideMetricPopover();
        } else {
            this.showMetricPopover(exporterKey, $metric, nodeId);
        }
    }

    showMetricPopover(exporterKey, $metric, nodeId) {
        const historyKey = nodeId ? (nodeId + ":" + exporterKey) : exporterKey;
        const history = this.metricHistories[historyKey];
        if (!history || !history.length) return;

        this.activePopoverKey = historyKey;

        const $popover = $("#metric-popover");
        if (!$popover.length) return;

        const offset = $metric.offset();
        const mWidth = $metric.outerWidth();
        const mHeight = $metric.outerHeight();
        const pWidth = 310;

        let top = offset.top + mHeight + 7;
        let left = offset.left + (mWidth / 2) - (pWidth / 2);
        left = Math.max(10, Math.min(left, $(window).width() - pWidth - 10));

        const arrowLeft = offset.left + (mWidth / 2) - left;
        $popover.find(".popover-arrow").css("left", Math.max(14, Math.min(arrowLeft, pWidth - 18)) + "px");

        $popover.css({ top: top + "px", left: left + "px" }).fadeIn(120);
        this.renderPopoverChart();
    }

    hideMetricPopover() {
        this.activePopoverKey = null;
        $("#metric-popover").hide();
    }

    renderPopoverChart() {
        if (!this.activePopoverKey) return;
        const history = this.metricHistories[this.activePopoverKey];
        if (!history || !history.length) return;

        const $popover = $("#metric-popover");
        const last = history[history.length - 1];

        $popover.find(".popover-title").text(last.title || last.metricId);
        $popover.find(".popover-current").text(last.formatted + (last.unit ? " " + last.unit : ""));

        // Calculate Min, Max, Avg for val1
        let min1 = Infinity;
        let max1 = -Infinity;
        let sum1 = 0;
        let count1 = 0;
        history.forEach(h => {
            if (typeof h.val1 === "number") {
                if (h.val1 < min1) min1 = h.val1;
                if (h.val1 > max1) max1 = h.val1;
                sum1 += h.val1;
                count1++;
            }
        });
        const avg1 = count1 > 0 ? (sum1 / count1) : 0;

        const formatVal = (v) => {
            if (last.metricId === "cpu") return v.toFixed(1) + "%";
            if (last.metricId === "heap") {
                if (v >= 1048576) return (v / 1048576).toFixed(1) + " GB";
                return (v / 1024).toFixed(0) + " MB";
            }
            return Number.isInteger(v) ? v : v.toFixed(1);
        };

        $popover.find(".stat-min").text(count1 > 0 ? formatVal(min1) : "-");
        $popover.find(".stat-max").text(count1 > 0 ? formatVal(max1) : "-");
        $popover.find(".stat-avg").text(count1 > 0 ? formatVal(avg1) : "-");

        // Details breakdown
        const $details = $popover.find(".popover-details").empty();
        if (last.metricId === "cpu") {
            if (typeof last.data.systemCpu === "number" && last.data.systemCpu >= 0) {
                $details.append(`<span><strong>System:</strong>${last.data.systemCpu}%</span>`);
            }
            if (last.data.systemLoad !== undefined && last.data.systemLoad >= 0) {
                $details.append(`<span><strong>Load:</strong>${last.data.systemLoad}</span>`);
            }
            if (last.data.processors) {
                $details.append(`<span><strong>Cores:</strong>${last.data.processors}</span>`);
            }
        } else if (last.metricId === "heap") {
            if (last.data.maxKB) {
                $details.append(`<span><strong>Max:</strong>${last.data.maxKB}</span>`);
            }
            if (last.data.usedKB) {
                $details.append(`<span><strong>Used:</strong>${last.data.usedKB}</span>`);
            }
        } else if (last.metricId.endsWith("-tp") || last.metricId === "tp") {
            if (last.data.max !== undefined) {
                $details.append(`<span><strong>Max Pool:</strong>${last.data.max < 0 ? 'Unbounded' : last.data.max}</span>`);
            }
            if (last.data.queued !== undefined) {
                $details.append(`<span><strong>Queued:</strong>${last.data.queued}</span>`);
            }
            if (last.data.workerName) {
                $details.append(`<span><strong>Worker:</strong>${last.data.workerName}</span>`);
            }
        } else if (last.metricId.startsWith("cp") || last.metricId.endsWith("-cp") || last.metricId === "cp" || last.data.poolName) {
            if (last.data.idle !== undefined) {
                $details.append(`<span><strong>Idle:</strong>${last.data.idle}</span>`);
            }
            if (last.data.awaiting !== undefined) {
                $details.append(`<span><strong>Awaiting:</strong>${last.data.awaiting}</span>`);
            }
            if (last.data.active !== undefined) {
                $details.append(`<span><strong>Active:</strong>${last.data.active}</span>`);
            }
            if (last.data.poolName) {
                $details.append(`<span><strong>Pool:</strong>${last.data.poolName}</span>`);
            }
        }

        // Render Canvas
        const canvas = $popover.find("canvas.popover-chart")[0];
        if (!canvas) return;

        const dpr = window.devicePixelRatio || 1;
        const width = 286;
        const height = 110;
        if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
            canvas.width = width * dpr;
            canvas.height = height * dpr;
        }

        const ctx = canvas.getContext("2d");
        ctx.save();
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, width, height);

        // Overall scale calculation
        let chartMin = min1;
        let chartMax = max1;
        if (last.val2 !== null) {
            history.forEach(h => {
                if (typeof h.val2 === "number") {
                    if (h.val2 < chartMin) chartMin = h.val2;
                    if (h.val2 > chartMax) chartMax = h.val2;
                }
            });
        }
        if (last.metricId === "cpu") {
            chartMin = 0;
            chartMax = Math.max(chartMax, 100);
        } else if (chartMax === chartMin) {
            chartMax += 1;
            chartMin = Math.max(0, chartMin - 1);
        }
        const range = chartMax - chartMin || 1;
        const padTop = 14;
        const padBottom = 16;
        const padLeft = 36;
        const padRight = 8;
        const plotW = width - padLeft - padRight;
        const plotH = height - padTop - padBottom;

        // Grid lines (3 horizontal lines)
        const gridColor = "rgba(0, 0, 0, 0.08)";
        const textColor = "#64748b";

        ctx.strokeStyle = gridColor;
        ctx.lineWidth = 1;
        ctx.font = "9px tabular-nums, sans-serif";
        ctx.fillStyle = textColor;
        ctx.textAlign = "right";

        for (let i = 0; i <= 2; i++) {
            const y = padTop + (plotH * (i / 2));
            ctx.beginPath();
            ctx.moveTo(padLeft, y);
            ctx.lineTo(width - padRight, y);
            ctx.stroke();

            const gridVal = chartMax - (range * (i / 2));
            ctx.fillText(formatVal(gridVal), padLeft - 4, y + 3);
        }

        if (history.length >= 2) {
            const stepX = plotW / (history.length - 1);

            // Render Secondary line (val2) if present
            if (last.val2 !== null) {
                const p2 = [];
                history.forEach((h, i) => {
                    if (typeof h.val2 === "number") {
                        const x = padLeft + (i * stepX);
                        const normY = (h.val2 - chartMin) / range;
                        const y = padTop + plotH - (normY * plotH);
                        p2.push({ x, y });
                    }
                });
                if (p2.length >= 2) {
                    ctx.save();
                    ctx.setLineDash([3, 3]);
                    ctx.beginPath();
                    p2.forEach((p, idx) => {
                        if (idx === 0) ctx.moveTo(p.x, p.y);
                        else ctx.lineTo(p.x, p.y);
                    });
                    ctx.strokeStyle = "#94a3b8";
                    ctx.lineWidth = 1.2;
                    ctx.stroke();
                    ctx.restore();
                }
            }

            // Render Primary line (val1)
            const p1 = [];
            history.forEach((h, i) => {
                const x = padLeft + (i * stepX);
                const normY = (h.val1 - chartMin) / range;
                const y = padTop + plotH - (normY * plotH);
                p1.push({ x, y });
            });

            // Primary Gradient Fill
            ctx.beginPath();
            ctx.moveTo(p1[0].x, padTop + plotH);
            p1.forEach(p => ctx.lineTo(p.x, p.y));
            ctx.lineTo(p1[p1.length - 1].x, padTop + plotH);
            ctx.closePath();
            const grad = ctx.createLinearGradient(0, padTop, 0, padTop + plotH);
            grad.addColorStop(0, "rgba(2, 132, 199, 0.35)");
            grad.addColorStop(1, "rgba(2, 132, 199, 0.0)");
            ctx.fillStyle = grad;
            ctx.fill();

            // Primary Stroke
            ctx.beginPath();
            p1.forEach((p, idx) => {
                if (idx === 0) ctx.moveTo(p.x, p.y);
                else ctx.lineTo(p.x, p.y);
            });
            ctx.strokeStyle = "#0284c7";
            ctx.lineWidth = 1.8;
            ctx.lineCap = "round";
            ctx.lineJoin = "round";
            ctx.stroke();

            // Last Dot
            const lastP = p1[p1.length - 1];
            ctx.beginPath();
            ctx.arc(lastP.x, lastP.y, 2.5, 0, Math.PI * 2);
            ctx.fillStyle = "#0284c7";
            ctx.fill();
        }

        ctx.restore();
    }

    clear() {
        this.metrics = {};
        this.metricHistories = {};
        this.activePopoverKey = null;
        $("#metric-popover").hide();
    }
}

/**
 * The builder component for the AppMon dashboard.
 * Responsible for assembling the dashboard UI based on configuration data.
 *
 * @version 4.3
 * @last-modified 2026-10-30
 */
class DashboardBuilder {
    constructor(options = {}) {
        this.options = options;
        this.settings = {};
        this.clusterMode = "direct";
        this.isGatewayMode = false;
        this.counterPersistInterval = 5;
        this.groups = [];
        this.nodes = [];
        this.apps = [];
        this.metrics = [];
        this.viewers = [];
        this.groupViewers = {};
        this.metricsViewer = new MetricsViewer();
        this.clients = [];
        this.currentGroupId = null;
        this.selectedNodeIdByGroup = {};
        this.metricsExpandedByGroup = {};
        this.currentAjax = null;
        this.nodeJoinedTimer = null;
        this.refreshTimer = null;
        this.refreshChartTimer = null;
        this.dateUnit = null;
        this.currentPeriod = "live";
        this.currentBaseDate = dayjs();
        this.consoleWideMode = false;
    }

    build(baseUrl, appsToSubscribe, nodeToSubscribe) {
        this.baseUrl = baseUrl;
        this.appsToSubscribe = appsToSubscribe;
        this.nodeToSubscribe = nodeToSubscribe;
        this.currentGroupId = null;
        this.selectedNodeIdByGroup = {};
        this.metricsExpandedByGroup = {};
        this.dateUnit = null;
        this.currentPeriod = "live";
        this.currentBaseDate = dayjs();
        this.consoleWideMode = false;

        if (this.currentAjax) {
            this.currentAjax.abort();
            this.currentAjax = null;
        }

        this.suspendMonitoring();
        this.showLoadingMessage();
        this.currentAjax = $.ajax({
            url: baseUrl + "/appmon/config/data",
            type: "get",
            dataType: "json",
            data: {
                nodeToSubscribe: nodeToSubscribe || null,
                appsToSubscribe: appsToSubscribe || null
            },
            success: (data) => {
                this.currentAjax = null;
                if (data) {
                    if (!data.nodes || !data.nodes.length || !data.appsToSubscribe || !data.apps || !data.apps.length) {
                        this.showEmptyAppMessage();
                        return;
                    }
                    if (appsToSubscribe) {
                        this.appsToSubscribe = data.appsToSubscribe;
                    }

                    this.settings = { ...data.settings };
                    this.clusterMode = this.settings.clusterMode || "direct";
                    this.isGatewayMode = (this.settings.clusterMode === "gateway");
                    this.counterPersistInterval = this.settings.counterPersistInterval || 5;
                    this.groups = [];
                    this.nodes = [];
                    this.apps = [];
                    this.viewers = [];
                    this.groupViewers = {};
                    this.metricsViewer.clear();
                    this.clients = [];

                    let index = 0;
                    data.nodes.forEach(nodeInfo => {
                        if (this.nodeToSubscribe && this.nodeToSubscribe !== nodeInfo.id) {
                            return;
                        }
                        const isOffline = (nodeInfo.status && nodeInfo.status.toLowerCase() === "offline");
                        const node = {
                            ...nodeInfo,
                            index: index++,
                            active: false,
                            alive: !isOffline,
                            primary: false,
                            subscribed: false,
                            subscribeAttempts: 0
                        };
                        node.endpoint.mode = node.endpoint.mode || "auto";
                        node.endpoint.path = baseUrl + node.endpoint.path;
                        node.endpoint.token = data.token;
                        this.nodes.push(node);
                        this.viewers[node.index] = new DashboardViewer(this.counterPersistInterval * 60, this.options);
                        console.log(index, "node", node);
                    });

                    // Assign group-specific logical numbers to each node
                    const groupNodeCounts = {};
                    this.nodes.forEach(node => {
                        const groupId = node.group;
                        if (!groupNodeCounts[groupId]) {
                            groupNodeCounts[groupId] = 0;
                        }
                        groupNodeCounts[groupId]++;
                        node.nodeNoInGroup = groupNodeCounts[groupId];
                    });

                    if (data.groups) {
                        data.groups.forEach(groupInfo => {
                            if (this.nodes.some(node => node.group === groupInfo.id)) {
                                const group = { ...groupInfo, active: false };
                                if (data.myGroupId && groupInfo.id === data.myGroupId) {
                                    this.groups.unshift(group);
                                } else {
                                    this.groups.push(group);
                                }
                            }
                        });
                    }

                    // Create a Group DashboardViewer for each group
                    this.groups.forEach(group => {
                        const groupViewer = new DashboardViewer(this.counterPersistInterval * 60, this.options);
                        groupViewer.setIsGroupView(true, group.id);
                        this.groupViewers[group.id] = groupViewer;
                    });

                    data.apps.forEach(appInfo => {
                        const app = { ...appInfo, active: false };
                        this.apps.push(app);
                        console.log("app", app);
                    });

                    this.clearView();
                    this.buildView();
                    this.bindEvents();

                    // Select the initial group and app synchronously before connecting
                    if (this.groups.length > 0) {
                        let initialGroupId = null;
                        if (this.nodeToSubscribe) {
                            const targetNode = data.nodes.find(n => n.id === this.nodeToSubscribe);
                            if (targetNode && targetNode.group) {
                                initialGroupId = targetNode.group;
                            }
                        }
                        if (!initialGroupId) {
                            initialGroupId = this.groups[0].id;
                        }
                        this.changeGroup(initialGroupId);
                    } else {
                        this.changeApp();
                    }

                    if (location.hash) {
                        const appId = location.hash.substring(1);
                        const targetApp = this.apps.find(app => app.id === appId);
                        if (targetApp) {
                            if (targetApp.group && targetApp.group !== this.currentGroupId) {
                                this.changeGroup(targetApp.group);
                            }
                            this.changeApp(appId);
                        }
                    }

                    if (this.nodes.length) {
                        let initialNodeIndex = 0;
                        if (this.isGatewayMode && data.myNodeId) {
                            const myIndex = this.nodes.findIndex(n => n.id === data.myNodeId);
                            if (myIndex !== -1) {
                                initialNodeIndex = myIndex;
                            }
                        }
                        console.log("cluster mode:", this.clusterMode);
                        console.log("endpoint mode:", this.nodes[initialNodeIndex].endpoint.mode);
                        this.connect(initialNodeIndex);
                    }
                }
            },
            error: (xhr, status) => {
                this.currentAjax = null;
                if (status === "abort") {
                    return;
                }
                if (xhr.status === 403) {
                    alert("Authentication has expired. You will be redirected to the main page.");
                    location.href = baseUrl;
                }
            }
        });
    }

    rebuild() {
        if (this.nodeJoinedTimer) {
            clearTimeout(this.nodeJoinedTimer);
            this.nodeJoinedTimer = null;
        }
        this.build(this.baseUrl, this.appsToSubscribe, this.nodeToSubscribe);
    }

    configureViewerResolver(client) {
        if (!client) return;
        client.setViewerResolver((nodeId, defaultViewer) => {
            const node = this.nodes.find(n => n.id === nodeId);
            const groupId = node ? node.group : this.currentGroupId;
            const nodesInGroup = this.nodes.filter(n => n.group === groupId);

            if (nodesInGroup.length <= 1) {
                return (node ? this.viewers[node.index] : defaultViewer) || defaultViewer;
            }

            const selectedNodeId = groupId ? this.selectedNodeIdByGroup[groupId] : null;

            if (!selectedNodeId) {
                // Group View mode: route to Group DashboardViewer
                if (groupId && this.groupViewers[groupId]) {
                    return this.groupViewers[groupId];
                }
                return defaultViewer;
            }

            // Node View mode: route to the corresponding node DashboardViewer
            // (the selected node's viewer is visible, while background node viewers update their tab indicators and internal state)
            if (node) {
                return this.viewers[node.index] || defaultViewer;
            }

            return defaultViewer;
        });
    }

    connect(nodeIndex) {
        const onSubscribed = (node, primary) => {
            if (node.subscribed && node.subscribeAttempts > 0) return;
            if (primary) {
                console.log("primary connection node:", node.id);
                node.primary = true;
            }
            node.subscribed = true;
            node.subscribeAttempts++;
            console.log(node.id, "subscribe attempts:", node.subscribeAttempts);
            this.changeNodeState(node);
            if (node.subscribeAttempts === 1) {
                this.initView();
            } else {
                this.clearSessions(node.index);
                this.clearConsole(node.index);
            }
            if (node.alive) {
                this.viewers[node.index].setEnable(true);
                if (node.group && this.groupViewers[node.group]) {
                    this.groupViewers[node.group].setEnable(true);
                }
            }
            const activeApp = this.apps.find(a => a.active);
            if (activeApp) {
                this.updateVisibility(activeApp.id);
                const client = this.clients[node.index];
                if (client && client.focus) {
                    client.focus(activeApp.id, node.id);
                }
                const groupId = this.currentGroupId;
                const nodesInGroup = this.nodes.filter(n => n.group === groupId);
                const isSingleNodeGroup = (nodesInGroup.length <= 1);
                const selectedNodeId = isSingleNodeGroup ? (nodesInGroup[0] ? nodesInGroup[0].id : null) : this.selectedNodeIdByGroup[groupId];
                const isGroupView = !isSingleNodeGroup && !selectedNodeId;

                const shouldRefresh = (isGroupView && (!node.group || node.group === groupId)) ||
                    (!isGroupView && node.id === selectedNodeId) ||
                    (this.nodes.length === 1);
                if (shouldRefresh) {
                    this.refreshData(activeApp.id, true);
                }
            }
            if (!this.isGatewayMode && node.subscribeAttempts === 1 && node.index + 1 < this.nodes.length) {
                const nextNodeIndex = node.index + 1;
                console.log("connecting next node:", nextNodeIndex);
                this.connect(nextNodeIndex);
            }
        };

        const onClosed = (node) => {
            node.subscribed = false;
            node.alive = false;
            node.primary = false;
            this.changeNodeState(node);
            this.viewers[node.index].setEnable(false);
            if (node.group && this.groupViewers[node.group]) {
                this.groupViewers[node.group].onNodeLeft(node.id);
            }
        };

        const onFailed = (node) => {
            this.changeNodeState(node, true);
            if (node.endpoint.mode === "polling" && node.subscribeAttempts < 1) {
                const currentClient = this.clients[node.index];
                if (currentClient && currentClient.constructor.name === "PollingClient") {
                    return;
                }
                const client = this.clients[node.index];
                if (client) client.destroy();
                setTimeout(() => {
                    const currentClientAsync = this.clients[node.index];
                    if (currentClientAsync && currentClientAsync.constructor.name === "PollingClient") {
                        return;
                    }
                    const viewer = this.viewers[node.index];
                    const client = new PollingClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
                    client.setMetricsViewer(this.metricsViewer);
                    this.configureViewerResolver(client);
                    if (this.isGatewayMode) {
                        this.nodes.forEach(n => {
                            n.endpoint.mode = "polling";
                            const v = this.viewers[n.index];
                            client.addClusterViewer(n.id, v);
                            client.addClusterNode(n, (nodeObj, primaryFlag) => {
                                onSubscribed(nodeObj, primaryFlag);
                            });
                            v.setClient(client);
                            this.clients[n.index] = client;
                        });
                        client.onNodeJoined = onNodeJoined;
                        client.onNodeStatusChanged = onNodeStatusChanged;
                        client.onNodeLeft = onNodeLeft;
                        client.onRequireRebuild = onRequireRebuild;
                    } else {
                        viewer.setClient(client);
                        this.clients[node.index] = client;
                    }
                    this.updateSpeedOptionsVisibility();
                    client.start(this.appsToSubscribe, this.nodeToSubscribe);
                }, (node.index - 1) * 1000);
            }
        };

        const onNodeJoined = (node) => {
            this.groups.forEach(group => {
                if (group.id === node.group) {
                    if (this.nodeJoinedTimer) {
                        clearTimeout(this.nodeJoinedTimer);
                    }
                    this.nodeJoinedTimer = setTimeout(() => {
                        this.nodeJoinedTimer = null;
                        this.showNewNodeNotification(node.id);
                    }, 3000);
                }
            });
        };

        const onNodeStatusChanged = (node) => {
            const existing = this.nodes.find(n => n.id === node.id);
            if (existing) {
                existing.status = node.status;
                this.changeNodeState(existing);
            }
        };

        const onNodeLeft = (nodeId) => {
            const node = this.nodes.find(n => n.id === nodeId);
            if (node) {
                node.subscribed = false;
                node.alive = false;
                this.changeNodeState(node);
                this.viewers[node.index].setEnable(false);
                if (node.group && this.groupViewers[node.group]) {
                    this.groupViewers[node.group].onNodeLeft(nodeId);
                }
                if (!node.primary) {
                    this.viewers[node.index].printErrorMessage("Node " + nodeId + " is left");
                }
            }
        };

        const onRequireRebuild = () => {
            this.rebuild();
        };

        console.log("connecting node:", nodeIndex);

        const node = this.nodes[nodeIndex];
        if (node.subscribed) return;

        const viewer = this.viewers[nodeIndex];

        let client;
        if (node.endpoint.mode === "polling") {
            client = new PollingClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        } else {
            client = new WebsocketClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        }
        client.setMetricsViewer(this.metricsViewer);
        this.configureViewerResolver(client);
        if (this.isGatewayMode) {
            this.nodes.forEach(n => {
                const v = this.viewers[n.index];
                client.addClusterViewer(n.id, v);
                client.addClusterNode(n, (nodeObj, primaryFlag) => {
                    onSubscribed(nodeObj, primaryFlag);
                });
                v.setClient(client);
                this.clients[n.index] = client;
            });
            client.onNodeJoined = onNodeJoined;
            client.onNodeStatusChanged = onNodeStatusChanged;
            client.onNodeLeft = onNodeLeft;
            client.onRequireRebuild = onRequireRebuild;
        } else {
            viewer.setClient(client);
            this.clients[node.index] = client;
        }
        client.start(this.appsToSubscribe, this.nodeToSubscribe);
    }

    showNewNodeNotification(nodeId) {
        const $notification = $("#new-node-notification");
        if ($notification.length > 0) {
            $notification.find(".node-id").text(nodeId);
            $notification.find(".refresh-btn").off("click").on("click", () => {
                $notification.hide();
                this.rebuild();
            });
            $notification.fadeIn();
        } else {
            const result = confirm("A new node '" + nodeId + "' has joined the cluster. Would you like to refresh the dashboard?");
            if (result) {
                this.rebuild();
            }
        }
    }

    changeNode(nodeIndex) {
        const node = this.nodes[nodeIndex];
        if (!node) return;

        const nodesInGroup = this.nodes.filter(n => n.group === node.group);
        if (nodesInGroup.length <= 1) {
            // Single node in group: keep node active and trigger refresh
            node.active = true;
            this.selectedNodeIdByGroup[this.currentGroupId] = node.id;
            this.updateNodeTabs();
            const activeApp = this.apps.find(a => a.active);
            if (activeApp) {
                this.updateVisibility(activeApp.id);
                this.refreshData(activeApp.id, true);
            }
            return;
        }

        const wasActive = node.active;

        // Reset all nodes in the current group
        this.nodes.forEach(n => {
            if (n.group === this.currentGroupId) {
                n.active = false;
            }
        });

        // Toggle or exclusively activate
        if (!wasActive) {
            node.active = true;
            this.selectedNodeIdByGroup[this.currentGroupId] = node.id;
        } else {
            delete this.selectedNodeIdByGroup[this.currentGroupId];
        }

        this.updateNodeTabs();

        const activeApp = this.apps.find(a => a.active);
        if (activeApp) {
            this.updateVisibility(activeApp.id);
            this.refreshData(activeApp.id, true);
        }
    }

    updateNodeTabs() {
        const availableTabs = $(`.node.tabs .tabs-title[data-group-id=${this.currentGroupId}]`);
        availableTabs.removeClass("active");
        this.nodes.filter(d => d.active && d.group === this.currentGroupId).forEach(d => {
            const $tab = $(".node.tabs .tabs-title[data-node-index=" + d.index + "]");
            $tab.addClass("active");
            if ($tab.length && $tab[0].scrollIntoView) {
                $tab[0].scrollIntoView({ behavior: 'smooth', inline: 'nearest', block: 'nearest' });
            }
        });
    }

    updateVisibility(appId) {
        const groupId = this.currentGroupId;
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        const isSingleNodeGroup = (nodesInGroup.length <= 1);
        const selectedNodeId = isSingleNodeGroup ? (nodesInGroup[0] ? nodesInGroup[0].id : null) : this.selectedNodeIdByGroup[groupId];
        const isGroupView = !isSingleNodeGroup && !selectedNodeId;

        // Control Group-view DOM
        const groupSelector = `[data-group-id=${groupId}][data-app-id=${appId}]`;
        if (isGroupView) {
            $(`.event-box.group-view${groupSelector}`).show();
            $(`.charts-box.group-view${groupSelector}`).show();
            $(`.console-box.group-view${groupSelector}`).show();

            $(`.event-box.group-view:not(${groupSelector})`).hide();
            $(`.charts-box.group-view:not(${groupSelector})`).hide();
            $(`.console-box.group-view:not(${groupSelector})`).hide();

            if (this.groupViewers[groupId]) {
                this.groupViewers[groupId].setVisible(true);
                $(`.console-box.group-view${groupSelector}`).each((_, el) => {
                    const $console = $(el).find(".console");
                    if (!$console.data("pause")) {
                        this.groupViewers[groupId].refreshConsole($console);
                    }
                });
                this.groupViewers[groupId].updateCanvasWidth();
            }
        } else {
            $(`.event-box.group-view, .charts-box.group-view, .console-box.group-view`).hide();
            if (this.groupViewers[groupId]) {
                this.groupViewers[groupId].setVisible(false);
            }
        }

        // Control Node-view DOM
        this.nodes.forEach(node => {
            const isNodeVisible = (!isGroupView && node.group === groupId && (isSingleNodeGroup || node.id === selectedNodeId));
            const action = isNodeVisible ? "show" : "hide";

            const nodeSelector = `[data-node-index=${node.index}][data-app-id=${appId}]`;
            const otherAppSelector = `[data-node-index=${node.index}][data-app-id!=${appId}]`;

            $(`.event-box:not(.group-view)${otherAppSelector}, .charts-box:not(.group-view)${otherAppSelector}, .console-box:not(.group-view)${otherAppSelector}`).hide();
            $(`.event-box:not(.group-view)${nodeSelector}, .charts-box:not(.group-view)${nodeSelector}, .console-box:not(.group-view)${nodeSelector}`)[action]();

            this.viewers[node.index].setVisible(isNodeVisible);
            if (isNodeVisible) {
                $(`.track-box[data-node-index=${node.index}] .bullet`).remove();
                $(`.console-box:not(.group-view)${nodeSelector}`).each((_, el) => {
                    const $console = $(el).find(".console");
                    if (!$console.data("pause")) {
                        this.viewers[node.index].refreshConsole($console);
                    }
                });
                this.viewers[node.index].updateCanvasWidth();
            }
        });

        this.updateMetricsVisibility(appId);
    }

    isMetricsExpanded(groupId) {
        if (!groupId) groupId = this.currentGroupId;
        if (this.metricsExpandedByGroup[groupId] !== undefined) {
            return this.metricsExpandedByGroup[groupId];
        }
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        return (nodesInGroup.length <= 4);
    }

    updateMetricsVisibility(appId) {
        if (!appId) {
            const activeApp = this.apps.find(a => a.active);
            appId = (activeApp ? activeApp.id : null);
        }
        const groupId = this.currentGroupId;
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        const isSingleNodeGroup = (nodesInGroup.length <= 1);
        const selectedNodeId = isSingleNodeGroup ? (nodesInGroup[0] ? nodesInGroup[0].id : null) : this.selectedNodeIdByGroup[groupId];
        const isGroupView = !isSingleNodeGroup && !selectedNodeId;
        const $metricsOptions = $(".metrics-options");
        const $metricsToggle = $(".metrics-options .metrics-toggle");

        if (isGroupView) {
            $metricsOptions.show();
            const isExpanded = this.isMetricsExpanded(groupId);
            if (isExpanded) {
                $metricsToggle.addClass("on");
                $(`.node.metrics-bar`).hide();
                $(`.node.metrics-bar[data-has-metrics=true]`).each((_, el) => {
                    const nodeIdx = $(el).data("node-index");
                    const node = this.nodes[nodeIdx];
                    if (node && node.group === groupId) {
                        $(el).show();
                        $(el).find(".app-metrics").hide();
                    }
                });
            } else {
                $metricsToggle.removeClass("on");
                $(`.node.metrics-bar`).hide();
            }
        } else {
            $metricsOptions.hide();
            $(`.node.metrics-bar`).hide();
            const targetNodeId = isSingleNodeGroup ? (nodesInGroup[0] ? nodesInGroup[0].id : null) : selectedNodeId;
            const selectedNode = this.nodes.find(n => n.id === targetNodeId && n.group === groupId);
            if (selectedNode) {
                const $bar = $(`.node.metrics-bar[data-node-index=${selectedNode.index}][data-has-metrics=true]`).show();
                if (appId) {
                    const $currentAppMetrics = $bar.find(`.app-metrics .metric[data-app-id=${appId}]`);
                    $bar.find(`.app-metrics .metric[data-app-id!=${appId}]`).hide();
                    if ($currentAppMetrics.length > 0) {
                        $currentAppMetrics.show();
                        $bar.find(".app-metrics").show();
                    } else {
                        $bar.find(".app-metrics").hide();
                    }
                } else {
                    $bar.find(".app-metrics").hide();
                }
            }
        }
    }

    changeNodeState(node, errorOccurred) {
        const $indicator = $(`.node.tabs .tabs-title[data-node-index=${node.index}] .indicator`);
        $indicator.removeClass($indicator.data("icon-connected") + " connected " +
                           $indicator.data("icon-disconnected") + " disconnected " +
                           $indicator.data("icon-error") + " error");
        const isOffline = (node.status && node.status.toLowerCase() === "offline");
        if (errorOccurred) {
            $indicator.addClass($indicator.data("icon-error") + " error");
        } else if (!isOffline && node.subscribed && node.alive) {
            $indicator.addClass($indicator.data("icon-connected") + " connected");
        } else {
            $indicator.addClass($indicator.data("icon-disconnected") + " disconnected");
        }
    }

    changeGroup(groupId) {
        if (this.currentGroupId === groupId) return;
        this.currentGroupId = groupId;

        this.groups.forEach(group => {
            const $tabTitle = $(".group.tabs .tabs-title[data-group-id=" + group.id + "]");
            if (group.id === groupId) {
                group.active = true;
                $tabTitle.addClass("active");
                if ($tabTitle.length && $tabTitle[0].scrollIntoView) {
                    $tabTitle[0].scrollIntoView({ behavior: 'smooth', inline: 'nearest', block: 'nearest' });
                }
            } else {
                group.active = false;
                $tabTitle.removeClass("active");
            }
        });

        // Filter Node Tabs
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        let selectedNodeId = this.selectedNodeIdByGroup[groupId];
        if (nodesInGroup.length === 1) {
            selectedNodeId = nodesInGroup[0].id;
            this.selectedNodeIdByGroup[groupId] = selectedNodeId;
        }

        this.nodes.forEach(node => {
            const $tab = $(".node.tabs .tabs-title[data-node-index=" + node.index + "]");
            if (!groupId || node.group === groupId) $tab.show(); else $tab.hide();
            if (selectedNodeId) {
                node.active = (node.id === selectedNodeId);
            } else {
                node.active = false; // Start in Group View mode when multiple nodes
            }
        });

        // Filter App Tabs
        this.apps.forEach(app => {
            const $tab = $(".app.tabs .tabs-title[data-app-id=" + app.id + "]");
            if (!groupId || !app.group || app.group === groupId) $tab.show(); else $tab.hide();
        });

        // Select first available app in the new group context
        const firstAvailableApp = this.apps.find(app => {
            return !groupId || !app.group || app.group === groupId;
        });

        this.changeApp(firstAvailableApp ? firstAvailableApp.id : null);
        this.updateNodeTabs();
    }

    changeApp(appId) {
        let exists = false;
        this.apps.forEach(app => {
            if (!appId) appId = app.id;
            const $tabTitle = $(".app.tabs .tabs-title[data-app-id=" + app.id + "]");
            if (app.id === appId) {
                app.active = true;
                this.showNodeApp(appId);
                this.resetDateOffsetOptions(appId);
                $tabTitle.addClass("active");
                if ($tabTitle.length && $tabTitle[0].scrollIntoView) {
                    $tabTitle[0].scrollIntoView({ behavior: 'smooth', inline: 'nearest', block: 'nearest' });
                }
                exists = true;
                const activeNode = this.nodes.find(n => n.primary) || this.nodes.find(n => n.subscribed);
                if (activeNode) {
                    const client = this.clients[activeNode.index];
                    if (client && client.focus) {
                        setTimeout(() => {
                            client.focus(appId, activeNode.id);
                            this.refreshData(appId, true);
                        }, 10);
                    }
                }
            } else {
                app.active = false;
                $tabTitle.removeClass("active");
            }
        });
        if (!exists && appId) return this.changeApp();
        return appId;
    }

    showNodeApp(appId) {
        this.updateVisibility(appId);
        this.updateNodeTabs();
    }

    updateSpeedOptionsVisibility() {
        const isPolling = this.nodes.some(d => d.endpoint && d.endpoint.mode === "polling") ||
            this.clients.some(c => c && c.constructor.name === "PollingClient");
        if (isPolling) {
            $(".speed-options").removeClass("d-none hide").show();
        } else {
            $(".speed-options").addClass("d-none").hide();
        }
    }

    initView() {
        if (this.groups.length) $(".group-bar").show();
        this.updateSpeedOptionsVisibility();
        this.updateDateNavigator();
        this.apps.forEach(app => {
            const $eventBox = $(`.event-box[data-app-id=${app.id}]`);
            const $chartsBox = $(`.charts-box[data-app-id=${app.id}]`);
            if ($eventBox.length && $chartsBox.length && $eventBox.find(".session-box.available").length === 0) {
                $eventBox.removeClass("col-lg-6").addClass("fixed-layout");
                $chartsBox.removeClass("col-lg-6").addClass("fixed-layout");
            }
        });
    }

    bindEvents() {
        $(".group.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const groupId = $(e.currentTarget).closest(".tabs-title").data("group-id");
            this.changeGroup(groupId);
        });
        $(".node.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const nodeIndex = $(e.currentTarget).closest(".tabs-title").data("node-index");
            this.changeNode(nodeIndex);
        });
        $(".app.tabs .tabs-title.available a").off("click").on("click", (e) => {
            const appId = $(e.currentTarget).closest(".tabs-title").data("app-id");
            this.changeApp(appId);
        });
        $(".date-unit-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const appId = $btn.closest(".charts-box").data("app-id");
            const period = $btn.data("period") || "live";
            this.currentPeriod = period;
            this.dateUnit = (period === "live" ? null : period);

            const $chartsBoxes = $(".charts-box.available");
            $chartsBoxes.find(".date-unit-options .btn").removeClass("on");
            $chartsBoxes.find(`.date-unit-options .btn[data-period="${period}"]`).addClass("on");

            if (period === "live") {
                $chartsBoxes.find(".date-offset-options").show();
                $chartsBoxes.find(".date-navigator").hide();
                this.resetDateOffsetOptions(appId);
            } else {
                $chartsBoxes.find(".date-offset-options").hide();
                $chartsBoxes.find(".date-navigator").show();
                this.updateDateNavigator();
            }
            this.viewers.forEach(v => v.updateCanvasWidth());
            Object.values(this.groupViewers).forEach(v => v.updateCanvasWidth());
            this.refreshChartData(appId);
        });
        $(".date-offset-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            if ($btn.hasClass("disabled")) return;
            const appId = $btn.closest(".charts-box").data("app-id");
            const offset = $btn.data("offset") || "";
            const $dateOffsetOptions = $(`.charts-box[data-app-id=${appId}] .date-offset-options`);
            if (offset === "previous") {
                $dateOffsetOptions.find(".btn.current").removeClass("disabled");
            } else if (offset === "current") {
                $dateOffsetOptions.find(".btn.current").addClass("disabled");
                $dateOffsetOptions.find(".btn.previous").removeClass("disabled");
            }
            this.refreshChartData(appId, offset);
        });
        $(".date-navigator .btn-prev").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const appId = $btn.closest(".charts-box").data("app-id");
            if (this.currentPeriod === "day") {
                this.currentBaseDate = this.currentBaseDate.subtract(1, "day");
            } else if (this.currentPeriod === "week") {
                this.currentBaseDate = this.currentBaseDate.subtract(1, "week");
            } else if (this.currentPeriod === "month") {
                this.currentBaseDate = this.currentBaseDate.subtract(1, "month");
            } else if (this.currentPeriod === "year") {
                this.currentBaseDate = this.currentBaseDate.subtract(1, "year");
            }
            this.updateDateNavigator();
            this.refreshChartData(appId);
        });
        $(".date-navigator .btn-next").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            if ($btn.hasClass("disabled")) return;
            const appId = $btn.closest(".charts-box").data("app-id");
            if (this.currentPeriod === "day") {
                this.currentBaseDate = this.currentBaseDate.add(1, "day");
            } else if (this.currentPeriod === "week") {
                this.currentBaseDate = this.currentBaseDate.add(1, "week");
            } else if (this.currentPeriod === "month") {
                this.currentBaseDate = this.currentBaseDate.add(1, "month");
            } else if (this.currentPeriod === "year") {
                this.currentBaseDate = this.currentBaseDate.add(1, "year");
            }
            this.updateDateNavigator();
            this.refreshChartData(appId);
        });
        $(".date-navigator .btn-today").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const appId = $btn.closest(".charts-box").data("app-id");
            this.currentBaseDate = dayjs();
            this.updateDateNavigator();
            this.refreshChartData(appId);
        });
        $(".date-navigator .btn-date").off().on("click", (e) => {
            const $picker = $(e.currentTarget).find(".date-picker-input");
            if ($picker.length) {
                if (typeof $picker[0].showPicker === "function") {
                    try {
                        $picker[0].showPicker();
                    } catch (err) {
                        // ignore if already open or unsupported (e.g. WebKit bug #261703)
                    }
                } else {
                    $picker[0].focus();
                }
            }
        });
        $(".date-navigator .date-picker-input").off().on("change", (e) => {
            const val = $(e.currentTarget).val();
            const appId = $(e.currentTarget).closest(".charts-box").data("app-id");
            if (val) {
                this.currentBaseDate = dayjs(val);
                this.updateDateNavigator();
                this.refreshChartData(appId);
            }
        });
        $(".metrics-options .metrics-toggle").off("click").on("click", (e) => {
            const groupId = this.currentGroupId;
            const currentExpanded = this.isMetricsExpanded(groupId);
            this.metricsExpandedByGroup[groupId] = !currentExpanded;
            this.updateMetricsVisibility();
        });
        $(".speed-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const faster = !$btn.hasClass("on");
            $(".speed-options .btn").toggleClass("on", faster);
            [...new Set(this.clients)].forEach(client => {
                if (client && typeof client.changePollingInterval === "function") {
                    client.changePollingInterval(faster ? 1 : 0);
                }
            });
        });
        $(".open-popup").off("click").on("click", (e) => {
            let url = this.baseUrl + "/appmon/dashboard/popup/" + (this.appsToSubscribe || "");
            if (this.nodeToSubscribe) {
                url += "?nodeId=" + encodeURIComponent(this.nodeToSubscribe);
            }
            const name = "appmon_dashboard_popup";
            const features = "width=1500,height=1045,menubar=no,toolbar=no,location=no,status=no,resizable=yes,scrollbars=yes";
            const popup = window.open(url, name, features);
            if (popup) {
                this.suspendMonitoring();
                this.showPopupModeMessage();
                popup.focus();
            }
        });
        $(document).off("click", ".session-box .panel.status .knob-bar")
            .on("click", ".session-box .panel.status .knob-bar", function() {
                $(this).parent().toggleClass("expanded");
            });
        $(document).off("click", ".session-box .session-node-filter .btn")
            .on("click", ".session-box .session-node-filter .btn", (e) => {
                const $btn = $(e.currentTarget);
                const $filter = $btn.closest(".session-node-filter");
                const $sessionBox = $btn.closest(".session-box");
                const groupId = $sessionBox.data("group-id");
                const appId = $sessionBox.data("app-id");
                const eventId = $sessionBox.data("event-id");
                const nodeId = $btn.attr("data-node-id") || "";

                $filter.find(".btn").removeClass("on");
                $btn.addClass("on");

                if (groupId && this.groupViewers[groupId]) {
                    this.groupViewers[groupId].setSessionFilter(appId, eventId, nodeId);
                }
            });
        $(document).off("click", ".session-box ul.sessions li")
            .on("click", ".session-box ul.sessions li", function() {
                $(this).toggleClass("designated");
            });
        $(".console-box .tailing-switch").off("click").on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $consoleBox = $btn.closest(".console-box");
            const $console = $consoleBox.find(".console");
            const nodeIndex = $consoleBox.data("node-index");
            const groupId = $consoleBox.data("group-id");
            const isTailing = !!$console.data("tailing");
            const newTailingState = !isTailing;

            $console.data("tailing", newTailingState);
            $consoleBox.find(".tailing-status").toggleClass("on", newTailingState);
            $btn.attr("title", newTailingState ? $btn.data("title-on") : $btn.data("title-off"));

            if (newTailingState) {
                if (groupId && this.groupViewers[groupId]) {
                    this.groupViewers[groupId].refreshConsole($console);
                } else if (nodeIndex !== undefined && this.viewers[nodeIndex]) {
                    this.viewers[nodeIndex].refreshConsole($console);
                }
            }
        });
        $(".console-box .pause-switch").off("click").on("click", function() {
            const $btn = $(this);
            const $icon = $btn.find(".icon");
            const $console = $btn.closest(".console-box").find(".console");
            const isPause = !!$console.data("pause");
            const newPauseState = !isPause;

            $console.data("pause", newPauseState);
            $btn.toggleClass("on", newPauseState);

            if (newPauseState) {
                $btn.attr("title", $btn.data("title-resume"));
                $icon.removeClass($icon.data("icon-pause")).addClass($icon.data("icon-resume"));
            } else {
                $btn.attr("title", $btn.data("title-pause"));
                $icon.removeClass($icon.data("icon-resume")).addClass($icon.data("icon-pause"));
            }
        });
        $(".console-box .wide-switch").off("click").on("click", () => {
            this.setConsoleWideMode(!this.consoleWideMode);
        });
        $(".console-box .expand-switch").off("click").on("click", function() {
            const $btn = $(this);
            const $icon = $btn.find(".icon");
            const $consoleBox = $btn.closest(".console-box");
            const isMaximized = $consoleBox.hasClass("maximized");
            const newMaximizedState = !isMaximized;

            $consoleBox.toggleClass("maximized", newMaximizedState);
            $btn.toggleClass("on", newMaximizedState);

            if (newMaximizedState) {
                $btn.attr("title", $btn.data("title-compress"));
                $icon.removeClass($icon.data("icon-expand")).addClass($icon.data("icon-compress"));
                $("body").css("overflow", "hidden");
            } else {
                $btn.attr("title", $btn.data("title-expand"));
                $icon.removeClass($icon.data("icon-compress")).addClass($icon.data("icon-expand"));
                $("body").css("overflow", "");
            }
        });
        $(".console-box .clear-screen").off("click").on("click", (e) => {
            const $consoleBox = $(e.currentTarget).closest(".console-box");
            const groupId = $consoleBox.data("group-id");
            const nodeIndex = $consoleBox.data("node-index");
            if (groupId && this.groupViewers[groupId]) {
                this.groupViewers[groupId].clearConsole($consoleBox.find(".console"));
            } else if (nodeIndex !== undefined && this.viewers[nodeIndex]) {
                this.viewers[nodeIndex].clearConsole($consoleBox.find(".console"));
            }
        });
        $(".console-box .console").off("scroll").on("scroll", (e) => {
            const $console = $(e.currentTarget);
            const $consoleBox = $console.closest(".console-box");
            if ($console.scrollTop() === 0) {
                $consoleBox.find(".load-previous").fadeIn();
            } else {
                $consoleBox.find(".load-previous").fadeOut();
            }
        });
        $(".console-box .load-previous").off("click").on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $consoleBox = $btn.closest(".console-box");
            const $console = $consoleBox.find(".console");
            const nodeIndex = $consoleBox.data("node-index");
            const groupId = $consoleBox.data("group-id");
            const appId = $consoleBox.data("app-id");
            const logId = $consoleBox.data("log-id");

            if (groupId && this.groupViewers[groupId]) {
                const loadedLines = this.groupViewers[groupId].prepareToLoadPrevious($console);
                const aliveNodes = this.nodes.filter(n => n.group === groupId && n.alive);
                aliveNodes.forEach(node => {
                    this.clients[node.index].loadPrevious(appId, logId, loadedLines, node.id);
                });
            } else if (nodeIndex !== undefined && this.viewers[nodeIndex]) {
                const loadedLines = this.viewers[nodeIndex].prepareToLoadPrevious($console);
                this.clients[nodeIndex].loadPrevious(appId, logId, loadedLines, this.nodes[nodeIndex].id);
            }
        });
        $(window).off("resize").on("resize", () => {
            this.viewers.forEach(v => v.updateCanvasWidth());
            Object.values(this.groupViewers).forEach(v => v.updateCanvasWidth());
        });
        $(document).off("visibilitychange").on("visibilitychange", () => {
            if (!document.hidden) {
                this.viewers.forEach(v => {
                    v.resetCurrentActivityCounts();
                });
                Object.values(this.groupViewers).forEach(v => {
                    v.resetCurrentActivityCounts();
                });
                this.apps.forEach(app => {
                    if (app.active) {
                        this.refreshData(app.id, app.active);
                    }
                });
            }
        });
        $(document).off("click.metricPopover", ".metrics-bar .metric.available")
            .on("click.metricPopover", ".metrics-bar .metric.available", (e) => {
                e.stopPropagation();
                const $metric = $(e.currentTarget);
                const nodeIndex = $metric.data("node-index");
                const exporterKey = $metric.data("exporter-key");
                const node = (nodeIndex !== undefined) ? this.nodes[nodeIndex] : null;
                const nodeId = node ? node.id : null;
                this.metricsViewer.toggleMetricPopover(exporterKey, $metric, nodeId);
            });
        $(document).off("click.metricPopoverClose", "#metric-popover .btn-close-popover")
            .on("click.metricPopoverClose", "#metric-popover .btn-close-popover", (e) => {
                e.stopPropagation();
                this.metricsViewer.hideMetricPopover();
            });
        $(document).off("click.metricPopoverOutside")
            .on("click.metricPopoverOutside", (e) => {
                if (!$(e.target).closest("#metric-popover, .metrics-bar .metric.available").length) {
                    this.metricsViewer.hideMetricPopover();
                }
            });
        $(document).off("keydown.metricPopover")
            .on("keydown.metricPopover", (e) => {
                if (e.key === "Escape") {
                    this.metricsViewer.hideMetricPopover();
                }
            });
    }

    updateDateNavigator() {
        let label = "";
        let isTodayOrFuture = false;
        const now = dayjs();

        if (this.currentPeriod === "day") {
            label = this.currentBaseDate.format("YYYY-MM-DD");
            isTodayOrFuture = !this.currentBaseDate.isBefore(now, "day");
        } else if (this.currentPeriod === "week") {
            const dayOfWeek = this.currentBaseDate.day();
            const diff = (dayOfWeek === 0 ? -6 : 1 - dayOfWeek);
            const monday = this.currentBaseDate.add(diff, "day");
            const sunday = monday.add(6, "day");
            label = monday.format("M/D") + " ~ " + sunday.format("M/D");
            isTodayOrFuture = !sunday.isBefore(now, "day");
        } else if (this.currentPeriod === "month") {
            label = this.currentBaseDate.format("YYYY-MM");
            isTodayOrFuture = !this.currentBaseDate.isBefore(now, "month");
        } else if (this.currentPeriod === "year") {
            label = this.currentBaseDate.format("YYYY");
            isTodayOrFuture = !this.currentBaseDate.isBefore(now, "year");
        }

        const $boxes = $(".charts-box.available");
        $boxes.find(".date-navigator .date-label").text(label);
        $boxes.find(".date-navigator .date-picker-input").val(this.currentBaseDate.format("YYYY-MM-DD"));
        $boxes.find(".date-navigator .btn-next").toggleClass("disabled", isTodayOrFuture);
    }

    getDateUnit() {
        if (this.currentPeriod && this.currentPeriod !== "live") {
            return this.currentPeriod;
        }
        return this.dateUnit || null;
    }

    resetDateOffsetOptions(appId) {
        const $options = appId
            ? $(`.charts-box[data-app-id=${appId}] .date-offset-options`)
            : $(".charts-box.available .date-offset-options");
        $options.find(".btn.current").addClass("disabled");
        $options.find(".btn.previous").removeClass("disabled");
        this.updateDateNavigator();
    }

    refreshChartData(appId, dateOffset) {
        if (!appId) {
            const activeApp = this.apps.find(a => a.active);
            if (!activeApp) return;
            appId = activeApp.id;
        }

        if (this.refreshChartTimer) {
            clearTimeout(this.refreshChartTimer);
            this.refreshChartTimer = null;
        }

        const groupId = this.currentGroupId;
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        const isSingleNodeGroup = (nodesInGroup.length <= 1);
        const selectedNodeId = isSingleNodeGroup ? (nodesInGroup[0] ? nodesInGroup[0].id : null) : this.selectedNodeIdByGroup[groupId];
        const isGroupView = !isSingleNodeGroup && !selectedNodeId;

        const options = ["appId:" + appId, "type:data/chart"];
        if (this.currentPeriod && this.currentPeriod !== "live") {
            options.push("period:" + this.currentPeriod);
            options.push("baseDate:" + this.currentBaseDate.format("YYYY-MM-DD"));
        } else if (dateOffset === "previous") {
            let maxStartDate = "";
            if (isGroupView && this.groupViewers[groupId]) {
                maxStartDate = this.groupViewers[groupId].getMaxStartDatetime(appId);
            } else if (!isGroupView) {
                const node = this.nodes.find(n => n.id === selectedNodeId);
                if (node && this.viewers[node.index]) {
                    maxStartDate = this.viewers[node.index].getMaxStartDatetime(appId);
                }
            }
            if (maxStartDate) {
                options.push("dateOffset:" + maxStartDate);
            } else {
                $(`.charts-box[data-app-id=${appId}] .date-offset-options .btn.previous`).addClass("disabled");
                return;
            }
        }

        this.refreshChartTimer = setTimeout(() => {
            this.refreshChartTimer = null;

            if (isGroupView) {
                const groupViewer = this.groupViewers[groupId];
                if (groupViewer) groupViewer.setLoading(appId, true);

                const aliveNodesInGroup = this.nodes.filter(n => n.group === groupId && (n.alive || n.subscribed));
                if (aliveNodesInGroup.length > 0) {
                    const repNode = aliveNodesInGroup.find(n => n.primary) || aliveNodesInGroup[0];
                    const chartOptions = [...options];
                    const repClient = this.clients[repNode.index];
                    if (repClient) {
                        repClient.refresh(chartOptions, repNode.id, "group");
                    }
                }
            } else {
                const selectedNode = this.nodes.find(n => n.id === selectedNodeId);
                if (selectedNode) {
                    this.viewers[selectedNode.index].setLoading(appId, true);
                    const nodeOptions = [...options];
                    const client = this.clients[selectedNode.index];
                    if (client) {
                        client.refresh(nodeOptions, selectedNode.id);
                    }
                }
            }
        }, 100);
    }

    refreshData(appId, withLogs, dateOffset) {
        if (!appId) {
            const activeApp = this.apps.find(a => a.active);
            if (!activeApp) return;
            appId = activeApp.id;
        }

        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }

        if (withLogs) {
            this.pendingRefreshWithLogs = true;
        }

        const groupId = this.currentGroupId;
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        const isSingleNodeGroup = (nodesInGroup.length <= 1);
        const selectedNodeId = isSingleNodeGroup ? (nodesInGroup[0] ? nodesInGroup[0].id : null) : this.selectedNodeIdByGroup[groupId];
        const isGroupView = !isSingleNodeGroup && !selectedNodeId;

        const options = ["appId:" + appId];
        if (this.currentPeriod && this.currentPeriod !== "live") {
            options.push("period:" + this.currentPeriod);
            options.push("baseDate:" + this.currentBaseDate.format("YYYY-MM-DD"));
        } else if (dateOffset === "previous") {
            let maxStartDate = "";
            if (isGroupView && this.groupViewers[groupId]) {
                maxStartDate = this.groupViewers[groupId].getMaxStartDatetime(appId);
            } else if (!isGroupView) {
                const node = this.nodes.find(n => n.id === selectedNodeId);
                if (node && this.viewers[node.index]) {
                    maxStartDate = this.viewers[node.index].getMaxStartDatetime(appId);
                }
            }
            if (maxStartDate) {
                options.push("dateOffset:" + maxStartDate);
            } else {
                $(`.charts-box[data-app-id=${appId}] .date-offset-options .btn.previous`).addClass("disabled");
                return;
            }
        }

        this.refreshTimer = setTimeout(() => {
            this.refreshTimer = null;
            const refreshWithLogs = this.pendingRefreshWithLogs;
            this.pendingRefreshWithLogs = false;

            if (isGroupView) {
                const groupViewer = this.groupViewers[groupId];
                if (groupViewer) groupViewer.setLoading(appId, true);
                if (refreshWithLogs && groupViewer) {
                    groupViewer.clearAllConsoles();
                }

                const aliveNodesInGroup = this.nodes.filter(n => n.group === groupId && (n.alive || n.subscribed));
                if (aliveNodesInGroup.length > 0) {
                    const repNode = aliveNodesInGroup.find(n => n.primary) || aliveNodesInGroup[0];
                    const chartOptions = [...options];
                    const repClient = this.clients[repNode.index];
                    if (repClient) {
                        repClient.refresh(chartOptions, repNode.id, "group");
                    }
                    aliveNodesInGroup.forEach(node => {
                        if (node.id !== repNode.id) {
                            const nodeOptions = [...options];
                            const client = this.clients[node.index];
                            if (client) {
                                client.refresh(nodeOptions, node.id, "none");
                            }
                        }
                    });
                }
            } else {
                const selectedNode = this.nodes.find(n => n.id === selectedNodeId);
                if (selectedNode) {
                    this.viewers[selectedNode.index].setLoading(appId, true);
                    if (refreshWithLogs) this.clearConsole(selectedNode.index);
                    const nodeOptions = [...options];
                    if (refreshWithLogs) nodeOptions.push("withLogs:true");
                    const client = this.clients[selectedNode.index];
                    if (client) {
                        client.refresh(nodeOptions, selectedNode.id);
                    }
                }
            }
        }, 100);
    }

    suspendMonitoring() {
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }
        if (this.refreshChartTimer) {
            clearTimeout(this.refreshChartTimer);
            this.refreshChartTimer = null;
        }
        if (this.nodeJoinedTimer) {
            clearTimeout(this.nodeJoinedTimer);
            this.nodeJoinedTimer = null;
        }
        [...new Set(this.clients)].forEach(client => {
            if (client) client.destroy();
        });
        this.clients = [];
        this.viewers.forEach(viewer => {
            if (viewer) viewer.setEnable(false);
        });
        Object.values(this.groupViewers).forEach(viewer => {
            if (viewer) viewer.setEnable(false);
        });
    }

    showLoadingMessage() {
        this.clearView();
        $("#appmon-loading-message").show();
    }

    showEmptyAppMessage() {
        this.clearView();
        const $emptyBox = $("#appmon-empty-message");
        if ($emptyBox.length > 0) {
            $emptyBox.find(".retry-btn").off("click").on("click", () => {
                $emptyBox.hide();
                this.rebuild();
            });
            $emptyBox.show();
        }
    }

    showPopupModeMessage() {
        this.clearView();
        const $messageBox = $("#appmon-popup-message");
        if ($messageBox.length > 0) {
            $messageBox.find(".resume-here").off("click").on("click", () => {
                location.reload();
            });
            $messageBox.show();
        }
    }

    clearView() {
        $("#appmon-loading-message").hide();
        $("#appmon-empty-message").hide();
        $("#appmon-popup-message").hide();
        $(".group-bar, .node-bar, .node.metrics-bar, .app-bar, .app.tabs, .dashboard-grid").hide();
        $(".group.tabs .tabs-title.available, .node.tabs .tabs-title.available, .app.tabs .tabs-title.available, " +
          ".node.metrics-bar.available, .node.metrics-bar .metric.available, " +
          ".event-box.available, .charts-box.available, .chart-box.available, .console-box.available").remove();
        $(".group.tabs .tabs-title:not(.available), .node.tabs .tabs-title:not(.available), .app.tabs .tabs-title:not(.available), " +
          ".node.metrics-bar:not(.available), .console-box:not(.available)").hide();
    }

    clearConsole(nodeIndex) {
        $(`.console-box[data-node-index=${nodeIndex}] .console`).each((_, el) => {
            if (this.viewers[nodeIndex]) {
                this.viewers[nodeIndex].clearConsole($(el));
            } else {
                $(el).empty();
            }
        });
    }

    clearSessions(nodeIndex) {
        $(`.session-box[data-node-index=${nodeIndex}] .sessions`).empty();
    }

    setConsoleWideMode(isWide) {
        this.consoleWideMode = isWide;
        this.apps.forEach(app => {
            if (app.logs && app.logs.length > 1) {
                const $boxes = $(`.console-box.available[data-app-id=${app.id}]`);
                $boxes.toggleClass("col-lg-6", !isWide).toggleClass("col-lg-12", isWide);
                const $switches = $boxes.find(".wide-switch");
                $switches.toggleClass("on", isWide);
                if (isWide) {
                    $switches.attr("title", function() { return $(this).data("title-normal") || "Half width"; })
                        .find(".icon").each(function() {
                        const $ic = $(this);
                        $ic.removeClass($ic.data("icon-wide")).addClass($ic.data("icon-normal"));
                    });
                } else {
                    $switches.attr("title", function() { return $(this).data("title-wide") || "Full width"; })
                        .find(".icon").each(function() {
                        const $ic = $(this);
                        $ic.removeClass($ic.data("icon-normal")).addClass($ic.data("icon-wide"));
                    });
                }
            }
        });
    }

    buildView() {
        $(".node-bar, .app-bar, .app.tabs, .dashboard-grid").show();
        if (this.groups.length > 0) {
            $(".group-bar").show();
            this.groups.forEach(group => {
                const $groupTab = this.addGroupTab(group);
                const $groupIndicator = $groupTab.find(".indicator");
                const groupViewer = this.groupViewers[group.id];
                if (groupViewer) {
                    groupViewer.putIndicator$("group", "event", "", $groupIndicator);
                }
                this.nodes.forEach(node => {
                    if (node.group === group.id) {
                        this.viewers[node.index].putIndicator$("group", "event", "", $groupIndicator);
                    }
                });
            });
        } else {
            $(".group-bar").hide();
        }

        this.nodes.forEach(node => {
            const $nodeTab = this.addNodeTab(node);
            const $nodeIndicator = $nodeTab.find(".indicator");
            this.changeNodeState(node);
            this.viewers[node.index].putIndicator$("node", "event", node.id, $nodeIndicator);
            if (node.group && this.groupViewers[node.group]) {
                this.groupViewers[node.group].putIndicator$("node", "event", node.id, $nodeIndicator);
            }
            this.addNodeMetricsBar(node);
        });

        this.apps.forEach(app => {
            const $appTab = this.addAppTab(app);
            const $appIndicator = $appTab.find(".indicator");

            // Setup Group-level DOM and Viewers for multi-node groups
            this.groups.forEach(group => {
                const nodesInGroup = this.nodes.filter(n => n.group === group.id);
                if (nodesInGroup.length > 1 && (!app.group || app.group === group.id)) {
                    const groupViewer = this.groupViewers[group.id];
                    if (groupViewer) {
                        groupViewer.putIndicator$("app", "event", app.id, $appIndicator);
                        if (app.events && app.events.length) {
                            const $groupEventBox = this.addGroupEventBox(group, app);
                            app.events.forEach(event => {
                                if (event.id === "activity") {
                                    const $trackBox = this.addGroupTrackBox($groupEventBox, group, app, event);
                                    groupViewer.putDisplay$(app.id, event.id, $trackBox);
                                    groupViewer.putIndicator$(app.id, "event", event.id, $trackBox.find(".activity-status"));
                                } else if (event.id === "session") {
                                    groupViewer.putDisplay$(app.id, event.id, this.addGroupSessionBox($groupEventBox, group, app, event));
                                }
                            });
                            const $groupChartsBox = this.addGroupChartsBox(group, app);
                            $groupChartsBox.find(".control-bar").find(".date-unit-options .btn.default").text(this.counterPersistInterval + "m");
                            app.events.forEach(event => {
                                if (event.id === "activity" || event.id === "session") {
                                    groupViewer.putChart$(app.id, event.id, this.addGroupChartBox($groupChartsBox, group, app, event).find(".chart"));
                                }
                            });
                        }
                        if (app.logs) {
                            app.logs.forEach(logInfo => {
                                const $groupConsoleBox = this.addGroupConsoleBox(group, app, logInfo);
                                const $console = $groupConsoleBox.find(".console").data("tailing", true);
                                $groupConsoleBox.find(".tailing-status").addClass("on");
                                groupViewer.putConsole$(app.id, logInfo.id, $console);
                                groupViewer.putIndicator$(app.id, "log", logInfo.id, $groupConsoleBox.find(".status-bar"));
                            });
                        }
                    }
                }
            });

            // Setup Node-level DOM and Viewers
            this.nodes.forEach(node => {
                if (!app.group || app.group === node.group) {
                    const viewer = this.viewers[node.index];
                    viewer.putIndicator$("app", "event", app.id, $appIndicator);
                    if (app.metrics && app.metrics.length) {
                        app.metrics.forEach(metric => {
                            const $metric = metric.heading ?
                                this.addNodeMetric(node, metric) :
                                this.addAppMetric(node, app, metric);
                            $metric.data("exporter-key", app.id + ":metric:" + metric.id);
                            this.metricsViewer.putMetric$(node.id, app.id, metric.id, $metric);
                        });
                    }
                    if (app.events && app.events.length) {
                        const $eventBox = this.addEventBox(node, app);
                        app.events.forEach(event => {
                            if (event.id === "activity") {
                                const $trackBox = this.addTrackBox($eventBox, node, app, event);
                                viewer.putDisplay$(app.id, event.id, $trackBox);
                                viewer.putIndicator$(app.id, "event", event.id, $trackBox.find(".activity-status"));
                            } else if (event.id === "session") {
                                viewer.putDisplay$(app.id, event.id, this.addSessionBox($eventBox, node, app, event));
                            }
                        });
                        const $chartsBox = this.addChartsBox(node, app);
                        $chartsBox.find(".control-bar").find(".date-unit-options .btn.default").text(this.counterPersistInterval + "m");
                        app.events.forEach(event => {
                            if (event.id === "activity" || event.id === "session") {
                                viewer.putChart$(app.id, event.id, this.addChartBox($chartsBox, node, app, event).find(".chart"));
                            }
                        });
                    }
                    if (app.logs) {
                        app.logs.forEach(logInfo => {
                            const $consoleBox = this.addConsoleBox(node, app, logInfo);
                            const $console = $consoleBox.find(".console").data("tailing", true);
                            $consoleBox.find(".tailing-status").addClass("on");
                            viewer.putConsole$(app.id, logInfo.id, $console);
                            viewer.putIndicator$(app.id, "log", logInfo.id, $consoleBox.find(".status-bar"));
                        });
                    }
                }
            });
        });
        this.changeApp();
    }

    addGroupTab(groupInfo) {
        const $tabs = $(".group.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-group-id": groupInfo.id, "title": groupInfo.description });
        $tab.find("a .title").text(" " + (groupInfo.title || groupInfo.id) + " ");
        return $tab.show().appendTo($tabs);
    }

    addNodeTab(nodeInfo) {
        const $tabs = $(".node.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-node-id": nodeInfo.id , "data-group-id": nodeInfo.group });
        $tab.find("a .title").text(" " + (nodeInfo.title || nodeInfo.id) + " ");
        const nodesInGroup = this.nodes.filter(n => n.group === nodeInfo.group);
        if (nodesInGroup.length > 1) {
            $tab.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        } else {
            $tab.find(".number").empty();
        }
        return $tab.show().appendTo($tabs);
    }

    addAppTab(appInfo) {
        const $tabs = $(".app.tabs");
        const $tab = $tabs.find(".tabs-title").first().hide().clone().addClass("available")
            .attr({ "data-app-id": appInfo.id, "data-group-id": appInfo.group, "title": appInfo.title });
        $tab.find("a .title").text(" " + appInfo.title + " ");
        return $tab.show().appendTo($tabs);
    }

    addNodeMetricsBar(nodeInfo) {
        const $bar = $(".node.metrics-bar");
        const $newBar = $bar.first().hide().clone().addClass("available").attr("data-node-index", nodeInfo.index);
        $newBar.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        return $newBar.insertAfter($bar.last());
    }

    addNodeMetric(nodeInfo, metricInfo) {
        const $bar = $(`.node.metrics-bar[data-node-index=${nodeInfo.index}]`).show();
        $bar.attr("data-has-metrics", true);
        const $container = $bar.find(".node-metrics").show();
        const $metric = $container.find(".metric").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-metric-id": metricInfo.id });
        $metric.find("dt .name").text(metricInfo.title).attr("title", metricInfo.description);
        if (metricInfo.unit) {
            $metric.find(".unit").text(metricInfo.unit);
        }
        return $metric.appendTo($container).show();
    }

    addAppMetric(nodeInfo, appInfo, metricInfo) {
        const $bar = $(`.node.metrics-bar[data-node-index=${nodeInfo.index}]`).show();
        $bar.attr("data-has-metrics", true);
        const $container = $bar.find(".app-metrics").show();
        const $metric = $container.find(".metric").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-metric-id": metricInfo.id });
        $metric.find("dt .name").text(metricInfo.title).attr("title", metricInfo.description);
        if (metricInfo.unit) {
            $metric.find(".unit").text(metricInfo.unit);
        }
        return $metric.appendTo($container).show();
    }

    addGroupEventBox(groupInfo, appInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available group-view")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id });
        return $box.insertBefore($(".console-box").first());
    }

    addGroupTrackBox($eventBox, groupInfo, appInfo, eventInfo) {
        const $track = $eventBox.find(".track-box");
        return $track.first().hide().clone().addClass("available")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($track.last()).show();
    }

    addGroupSessionBox($eventBox, groupInfo, appInfo, eventInfo) {
        const $session = $eventBox.find(".session-box");
        const $newSession = $session.first().hide().clone().addClass("available")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id, "data-event-id": eventInfo.id });

        const nodesInGroup = this.nodes.filter(n => n.group === groupInfo.id);
        if (nodesInGroup.length > 1) {
            const $filter = $newSession.find(".session-node-filter").show();
            $filter.empty();
            $(`<button type="button" class="btn btn-node on" data-node-id="">All</button>`).appendTo($filter);
            nodesInGroup.forEach(node => {
                const label = node.nodeNoInGroup || node.id;
                $(`<button type="button" class="btn btn-node" data-node-id="${node.id}" title="${node.id}" style="display: none;">${label}</button>`).appendTo($filter);
            });

            const sessionsEl = $newSession.find("ul.sessions")[0];
            if (sessionsEl && window.MutationObserver) {
                const observer = new MutationObserver(() => {
                    const groupViewer = this.groupViewers[groupInfo.id];
                    if (groupViewer) {
                        groupViewer.updateSessionNodeFilterButtons($newSession);
                    }
                });
                observer.observe(sessionsEl, { childList: true });
            }
        }

        return $newSession.insertAfter($session.last()).show();
    }

    addGroupChartsBox(groupInfo, appInfo) {
        return $(".charts-box").first().hide().clone().addClass("available group-view")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id })
            .insertBefore($(".console-box").first());
    }

    addGroupChartBox($chartsBox, groupInfo, appInfo, eventInfo) {
        const $chart = $chartsBox.find(".chart-box");
        return $chart.first().hide().clone().addClass("available")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .appendTo($chartsBox).show();
    }

    addGroupConsoleBox(groupInfo, appInfo, logInfo) {
        const isSingle = (appInfo.logs && appInfo.logs.length === 1);
        const isWide = isSingle || this.consoleWideMode;
        const colClass = isWide ? "col-lg-12" : "col-lg-6";
        const $console = $(".console-box");
        const $newBox = $console.first().hide().clone().addClass("available group-view " + colClass)
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id, "data-log-id": logInfo.id });
        $newBox.find(".status-bar h4").text(logInfo.file);
        if (isSingle) {
            $newBox.find(".wide-switch").hide();
        } else if (this.consoleWideMode) {
            $newBox.find(".wide-switch").addClass("on")
                .attr("title", function() { return $(this).data("title-normal") || "Half width"; })
                .find(".icon").each(function() {
                    const $ic = $(this);
                    $ic.removeClass($ic.data("icon-wide")).addClass($ic.data("icon-normal"));
                });
        }
        return $newBox.insertAfter($console.last());
    }

    addEventBox(nodeInfo, appInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id });
        return $box.insertBefore($(".console-box").first());
    }

    addTrackBox($eventBox, nodeInfo, appInfo, eventInfo) {
        const $track = $eventBox.find(".track-box");
        return $track.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($track.last()).show();
    }

    addSessionBox($eventBox, nodeInfo, appInfo, eventInfo) {
        const $session = $eventBox.find(".session-box");
        return $session.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .insertAfter($session.last()).show();
    }

    addChartsBox(nodeInfo, appInfo) {
        return $(".charts-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id })
            .insertBefore($(".console-box").first());
    }

    addChartBox($chartsBox, nodeInfo, appInfo, eventInfo) {
        const $chart = $chartsBox.find(".chart-box");
        return $chart.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .appendTo($chartsBox).show();
    }

    addConsoleBox(nodeInfo, appInfo, logInfo) {
        const isSingle = (appInfo.logs && appInfo.logs.length === 1);
        const isWide = isSingle || this.consoleWideMode;
        const colClass = isWide ? "col-lg-12" : "col-lg-6";
        const $console = $(".console-box");
        const $newBox = $console.first().hide().clone().addClass("available " + colClass)
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-log-id": logInfo.id });
        $newBox.find(".status-bar h4").text(logInfo.file);
        if (isSingle) {
            $newBox.find(".wide-switch").hide();
        } else if (this.consoleWideMode) {
            $newBox.find(".wide-switch").addClass("on")
                .attr("title", function() { return $(this).data("title-normal") || "Half width"; })
                .find(".icon").each(function() {
                    const $ic = $(this);
                    $ic.removeClass($ic.data("icon-wide")).addClass($ic.data("icon-normal"));
                });
        }
        return $newBox.insertAfter($console.last());
    }
}
