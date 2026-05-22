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
 * @version 4.0
 * @last-modified 2026-05-22
 */
class BaseClient {
    constructor(node, viewer, onSubscribed, onPrimary, onClosed, onFailed, isGatewayMode) {
        this.node = node;
        this.viewer = viewer;
        this.clusterViewers = {};
        this.clusterNodes = {};
        this.onSubscribed = onSubscribed;
        this.onPrimary = onPrimary;
        this.onClosed = onClosed;
        this.onFailed = onFailed;
        this.isGatewayMode = isGatewayMode;
        this.nodeToSubscribe = null;
        this.appsToSubscribe = null;
        this.primary = false;
        this.primaryNodeId = node.id
        this.retryCount = 0;
        this.maxRetries = 10;
        this.retryInterval = 5000;
    }

    addClusterViewer(nodeId, viewer) {
        this.clusterViewers[nodeId] = viewer;
    }

    addClusterNode(node, onSubscribed, onPrimary) {
        this.clusterNodes[node.id] = {node, onSubscribed, onPrimary};
    }

    getViewer(nodeId) {
        return this.isGatewayMode ? this.clusterViewers[nodeId] : this.viewer;
    }

    getNodeConfig (nodeId) {
        return this.isGatewayMode ? this.clusterNodes[nodeId] : this;
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
     * Stops the client connection.
     */
    stop() {
        // Default implementation does nothing
    }

    /**
     * Refreshes the monitoring data with the specified options.
     * @param {string[]} [options] - Refresh options.
     * @param {string} [nodeId] - Target node ID.
     */
    refresh(options, nodeId) {
        let cmdOptions = ["command:refresh"];
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
     */
    reconnect() {
        if (this.retryCount++ < this.maxRetries) {
            const retryInterval = (this.retryInterval * this.retryCount) + (this.node.index * 200) + this.node.random1000;
            const status = "(" + this.retryCount + "/" + this.maxRetries + ", interval=" + retryInterval + ")";
            console.log(this.node.id, "trying to reconnect", status);
            this.viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(() => this.start(this.appsToSubscribe, this.nodeToSubscribe), retryInterval);
        } else {
            console.log(this.node.id, "abort reconnect attempt");
            this.viewer.printMessage("Max connection attempts exceeded.");
            if (this.onFailed) this.onFailed(this.node);
        }
    }
}
