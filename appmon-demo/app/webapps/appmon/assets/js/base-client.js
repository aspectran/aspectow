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
 * @last-modified 2026-10-09
 */
class BaseClient {
    constructor(primaryNodeId, node, viewer, onSubscribed, onClosed, onFailed, isGatewayMode) {
        this.primaryNodeId = primaryNodeId;
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
        this.nodeToSubscribe = null;
        this.appsToSubscribe = null;
        this.retryCount = 0;
        this.reconnecting = false;
        this.maxRetries = 10;
        this.retryInterval = 5000;
        this.everConnected = false;
        this.metricsViewer = null;
        this.retryTimer = null;
        this.lastResumeReconnect = 0;

        this.handleResume = () => {
            if (typeof document !== "undefined" && document.visibilityState === "visible") {
                this.onResume();
            }
        };
        this.handleOnline = () => {
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

    /**
     * Called when the page becomes visible or active again (e.g. after returning from mobile background).
     */
    onResume() {
        if (!this.isConnected()) {
            if (this.lastResumeReconnect && (Date.now() - this.lastResumeReconnect < 500)) {
                return;
            }
            this.lastResumeReconnect = Date.now();
            console.log(this.primaryNodeId, "app/page resumed, attempting immediate reconnect");
            this.reconnect(true);
        }
    }

    /**
     * Stops the client connection.
     */
    stop() {
        if (this.retryTimer) {
            clearTimeout(this.retryTimer);
            this.retryTimer = null;
        }
    }

    /**
     * Destroys the client and removes event listeners.
     */
    destroy() {
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

    select(nodeToSelect, nodeId, groupId) {
        const options = [
            "command:select",
            "nodeToSelect:" + (nodeToSelect || "")
        ];
        if (groupId) {
            options.push("groupId:" + groupId);
        }
        this.sendCommand(options, nodeId);
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
        if (this.retryTimer) {
            clearTimeout(this.retryTimer);
            this.retryTimer = null;
        }
        if (immediate) {
            this.retryCount = 0;
            this.reconnecting = true;
            console.log(this.primaryNodeId, "trying to reconnect immediately");
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
            console.log(this.primaryNodeId, "trying to reconnect", status);
            this.printMessage("Trying to reconnect... " + status);
            this.retryTimer = setTimeout(() => {
                this.retryTimer = null;
                this.start(this.appsToSubscribe, this.nodeToSubscribe);
            }, retryInterval);
        } else {
            console.log(this.primaryNodeId, "max connection attempts exceeded");
            this.printMessage("Max connection attempts exceeded.");
            this.notifyFailed();
            if (this.everConnected) {
                console.log(this.primaryNodeId, "will keep retrying connection in background...");
                this.retryTimer = setTimeout(() => {
                    this.retryTimer = null;
                    this.retryCount = Math.max(0, this.maxRetries - 2);
                    this.start(this.appsToSubscribe, this.nodeToSubscribe);
                }, this.retryInterval * 2);
            }
        }
    }
}
