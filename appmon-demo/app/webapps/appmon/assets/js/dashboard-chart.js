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
 * The chart component for the AppMon dashboard.
 * Responsible for rendering and updating individual charts using Chart.js.
 *
 * @version 4.3
 * @last-modified 2026-10-16
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
        switch (this.eventId) {
            case "activity":
                dataLabel1 = "Activities";
                borderColor1 = "#4493c8";
                backgroundColor1 = "#cce0fa";
                break;
            case "session":
                dataLabel1 = "Sessions";
                borderColor1 = "#44c577";
                backgroundColor1 = "#bcefd0";
                break;
            default:
                dataLabel1 = "";
        }

        const chartType = (!dateUnit ? "line" : "bar");
        this.chart = new Chart($canvas[0], {
            type: chartType,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        enabled: true,
                        reverse: true,
                        mode: 'x',
                        intersect: false,
                        callbacks: {
                            title: (tooltip) => {
                                const datetime = dayjs(labels[tooltip[0].dataIndex]);
                                switch (dateUnit) {
                                    case "hour": return datetime.format("LL HH:00");
                                    case "day": return datetime.format("LL");
                                    case "month": return datetime.date(1).format("LL");
                                    case "year": return datetime.format("YYYY");
                                    default: return datetime.format("LLL");
                                }
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
                            autoSkip: false,
                            includeBounds: false,
                            callback: (value, index) => {
                                const datetime = dayjs(labels[value]);
                                const datetime2 = (value > 0 ? dayjs(labels[value - 1]) : null);
                                switch (dateUnit) {
                                    case "hour":
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:00")
                                            : datetime.format("HH:00");
                                    case "day":
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "year")))
                                            ? datetime.format("YYYY M/D")
                                            : datetime.format("M/D");
                                    case "month":
                                        return datetime.format("YYYY/M");
                                    case "year":
                                        return datetime.format("YYYY");
                                    default: // 5m.
                                        return (index === 0 || (datetime2 && !datetime.isSame(datetime2, "day")))
                                            ? datetime.format("M/D HH:mm")
                                            : datetime.format("HH:mm");
                                }
                            }
                        },
                        stacked: true,
                        grid: chartType === "line" ? {
                            color: (ctx) => (data2[ctx.tick.value] > 0 ? "#ff6384" : "#e4e4e4")
                        } : {}
                    },
                    y: {
                        display: true,
                        title: { display: true, text: dataLabel1 },
                        suggestedMin: 0,
                        suggestedMax: 5,
                        stacked: true,
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
                        backgroundColor: backgroundColor1,
                        borderWidth: 1.4,
                        tension: 0.1,
                        pointStyle: false,
                        order: 2
                    } : {
                        label: dataLabel1,
                        data: data1,
                        minBarLength: 2,
                        fill: true,
                        borderWidth: 1,
                        borderColor: borderColor1,
                        backgroundColor: borderColor1,
                        order: 2
                    },
                    {
                        label: "Errors",
                        data: data2,
                        type: chartType,
                        fill: true,
                        borderWidth: 1,
                        borderColor: "#ff6384",
                        backgroundColor: "#ff6384",
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
     * @param {number|null} error - Secondary data delta (e.g. errors)
     * @param {boolean} isGroupView - Whether this chart is in group view (accumulate)
     * @param {string|null} dateUnit - Date unit ("hour", "day", "month", "year", or null for realtime)
     */
    applyRollup(label, delta, error, isGroupView = false, dateUnit = null) {
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
            if (isGroupView) {
                chartData1[targetIndex] = (chartData1[targetIndex] || 0) + (delta || 0);
                if (chartData2[targetIndex] !== null && error !== null && error !== undefined) {
                    chartData2[targetIndex] = (chartData2[targetIndex] || 0) + error;
                }
            } else {
                chartData1[targetIndex] = (chartData1[targetIndex] || 0) + (delta || 0);
                if (chartData2[targetIndex] !== null && error !== null && error !== undefined) {
                    chartData2[targetIndex] = (chartData2[targetIndex] || 0) + error;
                }
            }
        } else {
            const newLabelStr = activeDateUnit ? normalizedIncoming.toISOString() : label;
            const lastIndex = chartLabels.length - 1;
            const lastDate = (lastIndex >= 0 ? dayjs(chartLabels[lastIndex]) : null);
            const normalizedLast = (lastDate ? (activeDateUnit ? lastDate.startOf(activeDateUnit) : lastDate.startOf("minute")) : null);

            if (!normalizedLast || normalizedIncoming.isAfter(normalizedLast)) {
                chartLabels.push(newLabelStr);
                chartData1.push(delta || 0);
                chartData2.push(error !== null && error !== undefined ? error : null);
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
                chartData2.splice(insertIndex, 0, error !== null && error !== undefined ? error : null);
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
