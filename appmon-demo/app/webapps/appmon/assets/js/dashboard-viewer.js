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
 * The viewer component for the AppMon dashboard.
 * Responsible for rendering monitoring data, including logs, metrics, and charts.
 *
 * @version 4.3
 * @last-modified 2026-10-09
 */
class DashboardViewer {
    constructor(sampleInterval, options = {}) {
        this.flagsUrl = options.flagsUrl || "https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/countries/flags/";
        this.tempResidentInactiveSecs = 30;
        this.sampleInterval = sampleInterval;

        this.client = null;
        this.enable = false;
        this.visible = false;
        this.isGroupView = false;
        this.groupId = null;
        this.nodeIndicators = {};
        this.expectedNodesInGroup = [];
        this.activitiesByNode = {};
        this.sessionStatsByNode = {};
        this.rollupBuffer = {};
        this.displays = {};
        this.charts = {};
        this.consoles = {};
        this.indicators = {};
        this.currentActivityCounts = {};
        this.cachedCanvasWidth = 0;
        this.activeBulletCount = 0;
        this.maxBullets = 500;
        this.painters = {};
        this.sessionFilterByDisplay = {};
    }

    setIsGroupView(flag, groupId) {
        this.isGroupView = !!flag;
        this.groupId = groupId || null;
    }

    putNodeIndicator$(nodeId, $indicator) {
        this.nodeIndicators[nodeId] = $indicator;
    }

    setExpectedNodesInGroup(nodes) {
        this.expectedNodesInGroup = nodes || [];
    }

    onNodeLeft(nodeId) {
        if (this.isGroupView && nodeId) {
            for (let exporterKey in this.activitiesByNode) {
                delete this.activitiesByNode[exporterKey][nodeId];
            }
            for (let exporterKey in this.sessionStatsByNode) {
                delete this.sessionStatsByNode[exporterKey][nodeId];
            }
            for (let key in this.displays) {
                if (key.includes(":event:session")) {
                    this.displays[key].find(`ul.sessions li[data-node-id='${nodeId}']`).each(function () {
                        const timer = $(this).data("timer");
                        if (timer) clearTimeout(timer);
                        $(this).remove();
                    });
                }
            }
        }
    }

    setClient(client) {
        this.client = client;
    }

    setEnable(flag) {
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
        if ($display.hasClass("track-box")) {
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
        this.consoles[appId + ":log:" + logId] = $console;
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
                if (this.isGroupView) {
                    this.updateSessionNodeFilterButtons($display);
                }
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
        }
    }

    clearAllConsoles() {
        for (let key in this.consoles) {
            this.clearConsole(this.consoles[key]);
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
            case "data":
                if (messageContent.length) {
                    if (subType === "chart") {
                        const chartData = JSON.parse(messageContent);
                        this.processChartData(nodeId, appId, exporterType, exporterName, exporterKey, chartData);
                    }
                }
                break;
            case "log":
                this.printLogMessage(nodeId, appId, exporterType, exporterName, exporterKey, messageContent, subType);
                break;
        }
    }

    printLogMessage(nodeId, appId, exporterType, logId, exporterKey, messageContent, subType) {
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
                        let interim = 0, errors = 0, total = 0;
                        const actMap = this.activitiesByNode[exporterKey];
                        for (let nid in actMap) {
                            const act = actMap[nid];
                            if (act) {
                                interim += (act.interim || 0);
                                errors += (act.errors || 0);
                                total += (act.total || 0);
                            }
                        }
                        this.printActivityStatus(exporterKey, { interim, errors, total });
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
                    let numberOfCreated = 0;
                    let numberOfExpired = 0;
                    let numberOfActives = 0;
                    let highestNumberOfActives = 0;
                    let numberOfUnmanaged = 0;
                    let numberOfRejected = 0;
                    let minStartTime = null;

                    const statsMap = this.sessionStatsByNode[exporterKey];
                    for (let nid in statsMap) {
                        const s = statsMap[nid];
                        if (s) {
                            numberOfCreated += (s.numberOfCreated || 0);
                            numberOfExpired += (s.numberOfExpired || 0);
                            numberOfActives += (s.numberOfActives || 0);
                            highestNumberOfActives += (s.highestNumberOfActives || 0);
                            numberOfUnmanaged += (s.numberOfUnmanaged || 0);
                            numberOfRejected += (s.numberOfRejected || 0);
                            if (s.startTime) {
                                if (!minStartTime || s.startTime < minStartTime) {
                                    minStartTime = s.startTime;
                                }
                            }
                        }
                    }

                    const aggregatedEventData = {
                        ...eventData,
                        numberOfCreated,
                        numberOfExpired,
                        numberOfActives,
                        highestNumberOfActives,
                        numberOfUnmanaged,
                        numberOfRejected,
                        startTime: minStartTime
                    };
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
        if (nodeId && this.nodeIndicators[nodeId]) {
            this.blink(this.nodeIndicators[nodeId]);
        } else {
            this.blink(this.getIndicator$("node:event:"));
        }
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
                    let timer = $samplingTimerBar.data("timer");
                    if (timer) {
                        clearInterval(timer);
                        $samplingTimerBar.removeData("timer");
                    }
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
                        if (second > 300) second = 0;
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

    printSessionEventData(nodeId, exporterKey, eventData) {
        const $display = this.getDisplay$(exporterKey);
        if ($display) {
            $display.find(".numberOfCreated").text(eventData.numberOfCreated);
            $display.find(".numberOfExpired").text(eventData.numberOfExpired);
            $display.find(".numberOfActives").text(eventData.numberOfActives);
            $display.find(".highestNumberOfActives").text(eventData.highestNumberOfActives);
            $display.find(".numberOfUnmanaged").text(eventData.numberOfUnmanaged);
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
                    if (this.isGroupView) {
                        this.updateSessionNodeFilterButtons($display);
                    }
                }, inactiveInterval * 1000);
                $li.data("timer", timer);
            }
        }
    }

    processChartData(nodeId, appId, exporterType, eventId, exporterKey, chartData) {
        const dashboardChart = this.getChart$(exporterKey);
        if (!dashboardChart) return;
        this.setLoading(appId, false);

        if (eventId === "activity") {
            const prefix = appId + ":event:" + eventId;
            if (!dashboardChart.isDrawn()) this.resetInterimTimer(prefix);
            else if (chartData.rolledUp) {
                if (this.isGroupView && nodeId) {
                    if (this.activitiesByNode[nodeId]) {
                        this.activitiesByNode[nodeId].interim = 0;
                        this.activitiesByNode[nodeId].errors = 0;
                        let interim = 0, errors = 0, total = 0;
                        for (let nid in this.activitiesByNode) {
                            const act = this.activitiesByNode[nid];
                            if (act) {
                                interim += (act.interim || 0);
                                errors += (act.errors || 0);
                                total += (act.total || 0);
                            }
                        }
                        this.printActivityStatus(prefix, { interim, errors, total });
                    }
                } else {
                    this.resetInterimTimer(prefix);
                    this.resetInterimActivityStatus(prefix);
                }
            }
        }

        const dateUnit = (chartData.rolledUp ? dashboardChart.dateUnit : chartData.dateUnit);
        const dateOffset = (chartData.rolledUp ? dashboardChart.dateOffset : chartData.dateOffset);
        const labels = chartData.labels;
        const data1 = chartData.data1;
        const data2 = chartData.data2.map(n => (eventId === "activity" ? n : null));

        if (!dashboardChart.isDrawn() || !chartData.rolledUp) {
            if (this.isGroupView && this.expectedNodesInGroup && this.expectedNodesInGroup.length > 1 && chartData.scope !== "group") {
                return;
            }
            dashboardChart.ensureCanvas();
            this.pruneDataPoints(labels, data1, data2, dashboardChart.$container);
            dashboardChart.draw(dateUnit, labels, data1, data2);
            dashboardChart.dateOffset = dateOffset;
        } else if (!dateOffset) {
            if (this.isGroupView) {
                this.bufferGroupRollup(nodeId, appId, eventId, exporterKey, dateUnit, labels[0], data1[0], data2[0]);
            } else {
                if (!dateUnit) {
                    dashboardChart.rollup(labels, data1, data2, true);
                    this.pruneDataPoints(dashboardChart.getLabels(), dashboardChart.getDataset(0), dashboardChart.getDataset(1), dashboardChart.$container);
                    dashboardChart.update();
                } else if (this.client) {
                    setTimeout(() => {
                        const options = [
                            "appId:" + appId,
                            "dateUnit:" + dateUnit,
                            "timeZone:" + Intl.DateTimeFormat().resolvedOptions().timeZone
                        ];
                        this.client.refresh(options);
                    }, 900);
                }
            }
        }
    }

    bufferGroupRollup(nodeId, appId, eventId, exporterKey, dateUnit, label, delta, error) {
        const dashboardChart = this.getChart$(exporterKey);
        if (!dashboardChart || !label) return;

        const bufferKey = exporterKey + ":" + label;
        if (!this.rollupBuffer[bufferKey]) {
            this.rollupBuffer[bufferKey] = {
                appId,
                eventId,
                exporterKey,
                dateUnit,
                label,
                nodes: {},
                timer: null
            };
        }

        const entry = this.rollupBuffer[bufferKey];
        entry.nodes[nodeId || "unknown"] = { delta: delta || 0, error: (error !== null ? (error || 0) : null) };

        const expectedCount = (this.expectedNodesInGroup && this.expectedNodesInGroup.length > 0)
            ? this.expectedNodesInGroup.filter(n => n.alive).length
            : 1;

        if (Object.keys(entry.nodes).length >= expectedCount) {
            if (entry.timer) {
                clearTimeout(entry.timer);
                entry.timer = null;
            }
            this.flushGroupRollup(bufferKey);
        } else if (!entry.timer) {
            entry.timer = setTimeout(() => {
                this.flushGroupRollup(bufferKey);
            }, 3000);
        }
    }

    flushGroupRollup(bufferKey) {
        const entry = this.rollupBuffer[bufferKey];
        if (!entry) return;
        delete this.rollupBuffer[bufferKey];

        const dashboardChart = this.getChart$(entry.exporterKey);
        if (!dashboardChart) return;

        let totalDelta = 0;
        let totalError = (entry.eventId === "activity" ? 0 : null);
        for (let nid in entry.nodes) {
            const item = entry.nodes[nid];
            totalDelta += (item.delta || 0);
            if (totalError !== null && item.error !== null) {
                totalError += (item.error || 0);
            }
        }

        if (!entry.dateUnit) {
            dashboardChart.rollup([entry.label], [totalDelta], [totalError], true);
            this.pruneDataPoints(dashboardChart.getLabels(), dashboardChart.getDataset(0), dashboardChart.getDataset(1), dashboardChart.$container);
            dashboardChart.update();
        } else {
            dashboardChart.rollupDateUnit(entry.dateUnit, entry.label, totalDelta, totalError);
        }

        if (entry.eventId === "activity") {
            const prefix = entry.appId + ":event:" + entry.eventId;
            this.resetInterimTimer(prefix);
        }
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
        return maxLabels;
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
