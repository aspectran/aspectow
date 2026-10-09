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
 * The builder component for the AppMon dashboard.
 * Responsible for assembling the dashboard UI based on configuration data.
 *
 * @version 4.3
 * @last-modified 2026-10-09
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
    }

    build(baseUrl, appsToSubscribe, nodeToSubscribe) {
        this.baseUrl = baseUrl;
        this.appsToSubscribe = appsToSubscribe;
        this.nodeToSubscribe = nodeToSubscribe;
        this.currentGroupId = null;
        this.selectedNodeIdByGroup = {};
        this.metricsExpandedByGroup = {};

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
                        const node = {
                            ...nodeInfo,
                            index: index++,
                            active: false,
                            alive: false,
                            primary: false,
                            subscribed: false,
                            subscribeAttempts: 0
                        };
                        node.endpoint.mode = node.endpoint.mode || "auto";
                        node.endpoint.path = baseUrl + node.endpoint.path + "/" + node.id;
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
                        const nodeIdsInGroup = this.nodes.filter(n => n.group === group.id).map(n => n.id);
                        groupViewer.setExpectedNodesInGroup(nodeIdsInGroup);
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
                    if (this.nodes.length) {
                        this.connect(0);
                    }

                    // Select the initial group
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
            }
            if (node.subscribeAttempts === 1 && node.index + 1 < this.nodes.length) {
                console.log("connecting next node:", node.index + 1);
                this.connect(node.index + 1);
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
                        this.sharedClient = client;
                        client.addClusterViewer(node.id, viewer);
                        client.addClusterNode(node, onSubscribed);
                        client.onNodeJoined = onNodeJoined;
                        client.onNodeStatusChanged = onNodeStatusChanged;
                        client.onNodeLeft = onNodeLeft;
                        client.onRequireRebuild = onRequireRebuild;
                    }
                    this.viewers[node.index].setClient(client);
                    this.clients[node.index] = client;
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

        const node = this.nodes[nodeIndex];
        if (nodeIndex === 0) {
            console.log("cluster mode:", this.clusterMode);
            console.log("endpoint mode:", node.endpoint.mode);
        }
        console.log("connecting node:", nodeIndex);

        if (node.subscribed) return;
        const viewer = this.viewers[nodeIndex];

        if (this.isGatewayMode && this.sharedClient) {
            this.sharedClient.addClusterViewer(node.id, viewer);
            this.sharedClient.addClusterNode(node, onSubscribed);
            viewer.setClient(this.sharedClient);
            this.clients[node.index] = this.sharedClient;
            this.sharedClient.connect(node.id);
            return;
        }

        let client;
        if (node.endpoint.mode === "polling") {
            client = new PollingClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        } else {
            client = new WebsocketClient(node, viewer, onSubscribed, onClosed, onFailed, this.isGatewayMode);
        }
        client.setMetricsViewer(this.metricsViewer);
        this.configureViewerResolver(client);
        if (this.isGatewayMode) {
            this.sharedClient = client;
            client.addClusterViewer(node.id, viewer);
            client.addClusterNode(node, onSubscribed);
            client.onNodeJoined = onNodeJoined;
            client.onNodeStatusChanged = onNodeStatusChanged;
            client.onNodeLeft = onNodeLeft;
            client.onRequireRebuild = onRequireRebuild;
        }
        viewer.setClient(client);
        this.clients[node.index] = client;
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

    sendSelectCommand(selectedNodeId) {
        const targetNodeToSelect = selectedNodeId || "";
        if (this.isGatewayMode && this.sharedClient && this.sharedClient.select) {
            this.sharedClient.select(targetNodeToSelect);
        } else {
            this.nodes.forEach(n => {
                if (n.group === this.currentGroupId) {
                    const client = this.clients[n.index];
                    if (client && client.select) {
                        client.select(targetNodeToSelect, n.id);
                    }
                }
            });
        }
    }

    changeNode(nodeIndex) {
        const node = this.nodes[nodeIndex];
        if (!node) return;

        const nodesInGroup = this.nodes.filter(n => n.group === node.group);
        if (nodesInGroup.length <= 1) {
            // If only one node in group, keep node active and stay in Node View
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

        const selectedNodeId = this.selectedNodeIdByGroup[this.currentGroupId] || "";
        this.sendSelectCommand(selectedNodeId);

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

        this.updateMetricsVisibility();
    }

    isMetricsExpanded(groupId) {
        if (!groupId) groupId = this.currentGroupId;
        if (this.metricsExpandedByGroup[groupId] !== undefined) {
            return this.metricsExpandedByGroup[groupId];
        }
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        return (nodesInGroup.length <= 4);
    }

    updateMetricsVisibility() {
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
                $(`.node.metrics-bar[data-node-index=${selectedNode.index}][data-has-metrics=true]`).show();
            }
        }
    }

    changeNodeState(node, errorOccurred) {
        const $indicator = $(`.node.tabs .tabs-title[data-node-index=${node.index}] .indicator`);
        $indicator.removeClass($indicator.data("icon-connected") + " connected " +
                           $indicator.data("icon-disconnected") + " disconnected " +
                           $indicator.data("icon-error") + " error");
        if (errorOccurred) {
            $indicator.addClass($indicator.data("icon-error") + " error");
        } else if (node.subscribed && node.alive) {
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

        this.sendSelectCommand(selectedNodeId || "");

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
                setTimeout(() => this.showNodeApp(appId), 0);
                $tabTitle.addClass("active");
                if ($tabTitle.length && $tabTitle[0].scrollIntoView) {
                    $tabTitle[0].scrollIntoView({ behavior: 'smooth', inline: 'nearest', block: 'nearest' });
                }
                exists = true;
                this.nodes.forEach(node => {
                    if (node.primary) {
                        const client = this.clients[node.index];
                        if (client && client.focus) {
                            setTimeout(() => {
                                client.focus(appId, node.id);
                                this.refreshData(appId, true);
                            }, 10);
                        }
                    }
                });
            } else {
                app.active = false;
                $tabTitle.removeClass("active");
            }
        });
        if (!exists && appId) return this.changeApp();
        return appId;
    }

    showNodeApp(appId) {
        $(".control-bar[data-app-id!=" + appId + "]").hide();
        $(".control-bar[data-app-id=" + appId + "]").show();
        this.updateVisibility(appId);
        this.updateNodeTabs();
    }

    initView() {
        if (this.groups.length) $(".group-bar").show();
        $(".speed-options").addClass("hide");
        if (this.nodes.some(d => d.endpoint.mode === "polling")) {
            $(".speed-options").removeClass("hide");
        }
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
            const $controlBar = $btn.closest(".control-bar");
            const appId = $controlBar.data("app-id");
            const unit = $btn.data("unit") || "";
            $btn.parent().data("unit", unit).find(".btn").removeClass("on");
            $btn.addClass("on");
            $controlBar.find(".date-offset-options").data("offset", "").find(".btn.current").removeClass("on");
            this.viewers.forEach(v => v.updateCanvasWidth());
            Object.values(this.groupViewers).forEach(v => v.updateCanvasWidth());
            this.refreshData(appId, false);
        });
        $(".date-offset-options .btn").off().on("click", (e) => {
            const $btn = $(e.currentTarget);
            const $controlBar = $btn.closest(".control-bar");
            const appId = $controlBar.data("app-id");
            const offset = $btn.data("offset") || "";
            const $parent = $btn.parent();
            if (offset !== "current") {
                $parent.find(".btn.current").addClass("on");
            } else {
                $parent.find(".btn").addClass("on");
                $parent.find(".btn.current").removeClass("on");
            }
            $parent.data("offset", offset);
            this.refreshData(appId, false, offset);
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
            $btn.toggleClass("on", faster);
            this.nodes.forEach(node => {
                if (node.endpoint.mode === "polling") {
                    this.clients[node.index].changePollingInterval(faster ? 1 : 0);
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

    refreshData(appId, withLogs, dateOffset) {
        const groupId = this.currentGroupId;
        const nodesInGroup = this.nodes.filter(n => n.group === groupId);
        const isSingleNodeGroup = (nodesInGroup.length <= 1);
        const selectedNodeId = isSingleNodeGroup ? (nodesInGroup[0] ? nodesInGroup[0].id : null) : this.selectedNodeIdByGroup[groupId];
        const isGroupView = !isSingleNodeGroup && !selectedNodeId;

        const options = ["appId:" + appId];
        const dateUnit = $(".control-bar[data-app-id=" + appId + "] .date-unit-options").data("unit");
        if (dateUnit) options.push("dateUnit:" + dateUnit);

        if (dateOffset === "previous") {
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
                $(".control-bar[data-app-id=" + appId + "] .date-offset-options .btn.previous").removeClass("on");
                return;
            }
        }

        setTimeout(() => {
            if (isGroupView) {
                const groupViewer = this.groupViewers[groupId];
                if (groupViewer) groupViewer.setLoading(appId, true);
                if (withLogs && groupViewer) {
                    if (groupViewer.clearAllConsoles) {
                        groupViewer.clearAllConsoles();
                    } else {
                        $(`.console-box.group-view[data-group-id=${groupId}] .console`).each((_, el) => {
                            groupViewer.clearConsole($(el));
                        });
                    }
                }

                const aliveNodesInGroup = this.nodes.filter(n => n.group === groupId && n.alive);
                if (aliveNodesInGroup.length > 0) {
                    const repNode = aliveNodesInGroup.find(n => n.primary) || aliveNodesInGroup[0];
                    const chartOptions = [...options, "scope:group"];
                    this.clients[repNode.index].refresh(chartOptions, repNode.id, "group");

                    if (withLogs) {
                        aliveNodesInGroup.forEach(node => {
                            this.clients[node.index].refresh(["appId:" + appId, "withLogs:true"], node.id);
                        });
                    }
                }
            } else {
                const selectedNode = this.nodes.find(n => n.id === selectedNodeId);
                if (selectedNode && selectedNode.alive) {
                    this.viewers[selectedNode.index].setLoading(appId, true);
                    if (withLogs) this.clearConsole(selectedNode.index);
                    const nodeOptions = [...options];
                    if (withLogs) nodeOptions.push("withLogs:true");
                    this.clients[selectedNode.index].refresh(nodeOptions, selectedNode.id);
                }
            }
        }, 50);
    }

    suspendMonitoring() {
        if (this.nodeJoinedTimer) {
            clearTimeout(this.nodeJoinedTimer);
            this.nodeJoinedTimer = null;
        }
        this.clients.forEach(client => {
            if (client) client.stop();
        });
        this.viewers.forEach(viewer => {
            if (viewer) viewer.setEnable(false);
        });
        Object.values(this.groupViewers).forEach(viewer => {
            if (viewer) viewer.setEnable(false);
        });
        this.sharedClient = null;
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
        $(".group-bar, .node-bar, .node.metrics-bar, .app-bar, .app.tabs, .control-bar, .dashboard-grid").hide();
        $(".group.tabs .tabs-title.available, .node.tabs .tabs-title.available, .app.tabs .tabs-title.available, " +
          ".node.metrics-bar.available, .node.metrics-bar .metric.available, .control-bar.available, " +
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
            this.viewers[node.index].putIndicator$("node", "event", "", $nodeIndicator);
            if (node.group && this.groupViewers[node.group]) {
                this.groupViewers[node.group].putNodeIndicator$(node.id, $nodeIndicator);
            }
            this.addNodeMetricsBar(node);
        });

        this.apps.forEach(app => {
            const $appTab = this.addAppTab(app);
            const $appIndicator = $appTab.find(".indicator");
            this.addControlBar(app);

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

    addControlBar(appInfo) {
        const $bar = $(".control-bar");
        const $newBar = $bar.first().hide().clone().addClass("available").attr("data-app-id", appInfo.id);
        $newBar.find(".btn.default").text(this.counterPersistInterval + "min.");
        return $newBar.insertAfter($bar.last());
    }

    addGroupEventBox(groupInfo, appInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available group-view")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id });
        const $titleBar = $box.find(".title-bar");
        $titleBar.find("i").removeClass("bi-server").addClass("bi-collection");
        $titleBar.find(".number").empty();
        $titleBar.find("h4").text(groupInfo.title || groupInfo.id);
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
            .insertBefore($(".console-box").first()).show();
    }

    addGroupChartBox($chartsBox, groupInfo, appInfo, eventInfo) {
        const $chart = $chartsBox.find(".chart-box");
        return $chart.first().hide().clone().addClass("available")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .appendTo($chartsBox).show();
    }

    addGroupConsoleBox(groupInfo, appInfo, logInfo) {
        const $console = $(".console-box");
        const $newBox = $console.first().hide().clone().addClass("available group-view col-lg-6")
            .attr({ "data-group-id": groupInfo.id, "data-app-id": appInfo.id, "data-log-id": logInfo.id });
        //$newBox.find(".status-bar i").removeClass("bi-server").addClass("bi-collection");
        $newBox.find(".status-bar h4").text(logInfo.file);
        return $newBox.insertAfter($console.last());
    }

    addEventBox(nodeInfo, appInfo) {
        const $box = $(".event-box").first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id });
        const $titleBar = $box.find(".title-bar");
        $titleBar.find("h4").text(nodeInfo.title || nodeInfo.id);

        const nodesInGroup = this.nodes.filter(n => n.group === nodeInfo.group);
        if (nodesInGroup.length > 1) {
            $titleBar.find(".number").text(" " + nodeInfo.nodeNoInGroup);
        } else {
            $titleBar.find(".number").empty();
        }
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
            .insertBefore($(".console-box").first()).show();
    }

    addChartBox($chartsBox, nodeInfo, appInfo, eventInfo) {
        const $chart = $chartsBox.find(".chart-box");
        return $chart.first().hide().clone().addClass("available")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-event-id": eventInfo.id })
            .appendTo($chartsBox).show();
    }

    addConsoleBox(nodeInfo, appInfo, logInfo) {
        const $console = $(".console-box");
        const $newBox = $console.first().hide().clone().addClass("available col-lg-6")
            .attr({ "data-node-index": nodeInfo.index, "data-app-id": appInfo.id, "data-log-id": logInfo.id });
        $newBox.find(".status-bar h4").text(logInfo.file);
        return $newBox.insertAfter($console.last());
    }
}
