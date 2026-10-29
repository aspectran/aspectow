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
