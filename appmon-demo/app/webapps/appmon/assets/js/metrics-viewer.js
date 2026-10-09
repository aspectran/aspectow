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
