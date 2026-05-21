/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * The base class for AppMon communication clients.
 * Provides common functionality for connection management and retries.
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
     * @param {string} [appsToSubscribe] - Names of apps to join.
     */
    start(appsToSubscribe) {
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
     * @param {string} [appsToSubscribe] - Names of apps to subscribe.
     */
    reconnect(appsToSubscribe) {
        if (this.retryCount++ < this.maxRetries) {
            const retryInterval = (this.retryInterval * this.retryCount) + (this.node.index * 200) + this.node.random1000;
            const status = "(" + this.retryCount + "/" + this.maxRetries + ", interval=" + retryInterval + ")";
            console.log(this.node.id, "trying to reconnect", status);
            this.viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(() => this.start(appsToSubscribe), retryInterval);
        } else {
            console.log(this.node.id, "abort reconnect attempt");
            this.viewer.printMessage("Max connection attempts exceeded.");
            if (this.onFailed) this.onFailed(this.node);
        }
    }
}
