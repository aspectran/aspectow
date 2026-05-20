/*
 * Aspectow AppMon 4.0
 * Last modified: 2026-04-29
 */

/**
 * The base class for AppMon communication clients.
 * Provides common functionality for connection management and retries.
 */
class BaseClient {
    constructor(node, viewer, onConnected, onEstablished, onClosed, onFailed, isGatewayMode) {
        this.node = node;
        this.viewer = viewer;
        this.clusterViewers = {};
        this.clusterNodes = {};
        this.onConnected = onConnected;
        this.onEstablished = onEstablished;
        this.onClosed = onClosed;
        this.onFailed = onFailed;
        this.isGatewayMode = isGatewayMode;
        this.established = false;
        this.establishedNodeId = node.id
        this.retryCount = 0;
        this.maxRetries = 10;
        this.retryInterval = 5000;
    }

    addClusterViewer(nodeId, viewer) {
        this.clusterViewers[nodeId] = viewer;
    }

    addClusterNode(node, onConnected, onEstablished) {
        this.clusterNodes[node.id] = {node, onConnected, onEstablished};
    }

    /**
     * Starts the client connection.
     * @param {string} [appsToJoin] - Names of apps to join.
     */
    start(appsToJoin) {
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
        if (options) {
            cmdOptions.push(...options);
        }
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
     * @param {string} [appsToJoin] - Names of apps to join.
     */
    reconnect(appsToJoin) {
        if (this.retryCount++ < this.maxRetries) {
            const retryInterval = (this.retryInterval * this.retryCount) + (this.node.index * 200) + this.node.random1000;
            const status = "(" + this.retryCount + "/" + this.maxRetries + ", interval=" + retryInterval + ")";
            console.log(this.node.id, "trying to reconnect", status);
            this.viewer.printMessage("Trying to reconnect... " + status);
            setTimeout(() => {
                this.start(appsToJoin);
            }, retryInterval);
        } else {
            console.log(this.node.id, "abort reconnect attempt");
            this.viewer.printMessage("Max connection attempts exceeded.");
            if (this.onFailed) {
                this.onFailed(this.node);
            }
        }
    }
}
