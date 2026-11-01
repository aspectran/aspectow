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
 * @last-modified 2026-11-01
 */
class DashboardChart {
    constructor($container, eventId) {
        this.$container = $container;
        this.eventId = eventId;
        this.chart = null;
        this.dateUnit = null;
        this.dateOffset = null;
        this.updateScheduled = false;
        this.isTableView = false;
        this.isDropdownOpen = false;
        this.themeObserver = null;
    }

    isDarkMode() {
        return document.documentElement.getAttribute("data-bs-theme") === "dark" ||
               document.body.classList.contains("dark-mode");
    }

    getThemeColors() {
        const isDark = this.isDarkMode();
        if (isDark) {
            return {
                tooltipBg: "rgba(23, 32, 42, 0.95)",
                tooltipTitle: "#ffffff",
                tooltipBody: "#e0e6ed",
                tooltipBorder: "rgba(255, 255, 255, 0.15)",
                crosshair: "rgba(200, 210, 225, 0.45)",
                gridLine: "#252b30"
            };
        } else {
            return {
                tooltipBg: "rgba(255, 255, 255, 0.96)",
                tooltipTitle: "#0f172a",
                tooltipBody: "#334155",
                tooltipBorder: "rgba(0, 0, 0, 0.12)",
                crosshair: "rgba(100, 116, 139, 0.45)",
                gridLine: "#e4e4e4"
            };
        }
    }

    getResponsiveFontSize(chartWidth, type = "tick") {
        const width = chartWidth || (this.$container ? this.$container.width() : 0);
        if (type === "title") {
            if (width > 0 && width < 450) return 10;
            if (width > 0 && width < 768) return 11;
            return 12;
        }
        if (type === "tooltip") {
            if (width > 0 && width < 450) return 11;
            return 12;
        }
        // tick label
        if (width > 0 && width < 380) return 9;
        if (width > 0 && width < 520) return 10;
        if (width > 0 && width < 768) return 11;
        return 12;
    }

    observeThemeChange() {
        if (this.themeObserver) return;
        if (typeof window !== "undefined" && window.MutationObserver) {
            this.themeObserver = new MutationObserver((mutations) => {
                for (const m of mutations) {
                    if (m.type === "attributes") {
                        this.applyTheme();
                        break;
                    }
                }
            });
            this.themeObserver.observe(document.documentElement, {
                attributes: true,
                attributeFilter: ["data-bs-theme"]
            });
            if (document.body) {
                this.themeObserver.observe(document.body, {
                    attributes: true,
                    attributeFilter: ["class"]
                });
            }
        }
    }

    applyTheme() {
        if (!this.chart) return;
        const theme = this.getThemeColors();
        const tt = this.chart.options.plugins && this.chart.options.plugins.tooltip;
        if (tt) {
            tt.backgroundColor = theme.tooltipBg;
            tt.titleColor = theme.tooltipTitle;
            tt.bodyColor = theme.tooltipBody;
            tt.borderColor = theme.tooltipBorder;
        }
        if (this.chart.options.scales.y && this.chart.options.scales.y.grid) {
            this.chart.options.scales.y.grid.color = theme.gridLine;
        }
        this.chart.update("none");
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

    ensureControls() {
        let $controls = this.$container.find(".chart-zoom-controls");
        $controls.find(".btn-zoom-in").off("click").on("click", (e) => {
            e.stopPropagation();
            this.zoomIn();
        });
        $controls.find(".btn-zoom-out").off("click").on("click", (e) => {
            e.stopPropagation();
            this.zoomOut();
        });
        $controls.find(".btn-zoom-reset").off("click").on("click", (e) => {
            e.stopPropagation();
            this.resetZoom();
        });

        const $dropdown = $controls.find(".chart-options-dropdown");
        $dropdown.off(".chartOptions")
            .on("show.bs.dropdown.chartOptions", () => {
                this.isDropdownOpen = true;
                this.$container.addClass("show-controls");
            })
            .on("hidden.bs.dropdown.chartOptions", () => {
                this.isDropdownOpen = false;
                if (!this.isZoomed() && !this.isTableView) {
                    this.$container.removeClass("show-controls");
                    this.updateControls();
                }
            });

        // Fallback only if Bootstrap JS is not available
        if (typeof bootstrap === "undefined" || !bootstrap.Dropdown) {
            $controls.find(".btn-chart-options").off("click").on("click", (e) => {
                e.stopPropagation();
                $controls.find(".dropdown-menu").toggleClass("show");
            });
        }

        $controls.find(".option-view-table").off("click").on("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.closeDropdown();
            this.toggleTableView();
        });
        $controls.find(".option-download-csv").off("click").on("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.closeDropdown();
            this.downloadCsv();
        });
        $controls.find(".option-download-png").off("click").on("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.closeDropdown();
            this.downloadPng();
        });

        return $controls;
    }

    closeDropdown() {
        const $controls = this.$container.find(".chart-zoom-controls");
        const $btn = $controls.find(".btn-chart-options");
        if ($btn.length && typeof bootstrap !== "undefined" && bootstrap.Dropdown) {
            const instance = bootstrap.Dropdown.getInstance($btn[0]);
            if (instance) instance.hide();
        }
        $controls.find(".dropdown-menu").removeClass("show");
    }

    bindInteractionEvents($canvas) {
        $canvas.off(".chartZoom");
        if (this.docEventNamespace) {
            $(document).off(this.docEventNamespace);
        }
        this.docEventNamespace = "pointerdown.chartZoom_" + (this.eventId || "chart") + "_" + Math.random().toString(36).substring(2, 7);

        let lastTapTime = 0;
        let lastTapX = 0;
        let lastTapY = 0;

        $canvas.on("touchend.chartZoom", (e) => {
            const now = Date.now();
            const touch = e.originalEvent && e.originalEvent.changedTouches ? e.originalEvent.changedTouches[0] : null;
            if (!touch) return;

            const timeDiff = now - lastTapTime;
            const dist = Math.hypot(touch.clientX - lastTapX, touch.clientY - lastTapY);

            // Show zoom controls on first tap
            this.showControls();

            if (timeDiff < 350 && dist < 20) {
                const rect = $canvas[0].getBoundingClientRect();
                const xPixel = touch.clientX - rect.left;
                if (this.chart && this.chart.scales && this.chart.scales.x) {
                    const index = Math.round(this.chart.scales.x.getValueForPixel(xPixel));
                    if (!isNaN(index) && index >= 0) {
                        this.zoomIn(index);
                    }
                }
                lastTapTime = 0;
                return;
            }

            lastTapTime = now;
            lastTapX = touch.clientX;
            lastTapY = touch.clientY;
        });

        $canvas.on("click.chartZoom", () => {
            this.showControls();
        });

        $canvas.on("dblclick.chartZoom", (e) => {
            const rect = $canvas[0].getBoundingClientRect();
            const xPixel = e.clientX - rect.left;
            if (this.chart && this.chart.scales && this.chart.scales.x) {
                const index = Math.round(this.chart.scales.x.getValueForPixel(xPixel));
                if (!isNaN(index) && index >= 0) {
                    this.zoomIn(index);
                }
            }
        });

        // Hide controls on click outside when not zoomed
        $(document).on(this.docEventNamespace, (e) => {
            if (!this.$container.is(e.target) && this.$container.has(e.target).length === 0) {
                this.closeDropdown();
                if (!this.isZoomed() && !this.isTableView && !this.isDropdownOpen) {
                    this.$container.removeClass("show-controls");
                    this.updateControls();
                }
            }
        });
    }

    showControls() {
        this.$container.addClass("show-controls");
        this.updateControls();
    }

    isZoomed() {
        if (!this.chart || !this.chart.data || !this.chart.data.labels) return false;
        const total = this.chart.data.labels.length;
        if (total <= 1) return false;
        const xOpts = this.chart.options.scales.x;
        const min = (xOpts && xOpts.min != null ? xOpts.min : 0);
        const max = (xOpts && xOpts.max != null ? xOpts.max : total - 1);
        return (min > 0 || max < total - 1);
    }

    isZoomedOrPanned() {
        return this.isZoomed();
    }

    updateControls() {
        const $controls = this.$container.find(".chart-zoom-controls");
        if (!$controls.length) return;

        // Hide chart zoom/options controls completely when in table view
        if (this.isTableView) {
            $controls.hide();
            return;
        }

        const zoomed = this.isZoomed();
        const shouldShow = zoomed || this.isDropdownOpen || this.$container.hasClass("show-controls");
        if (zoomed) {
            this.$container.addClass("is-zoomed");
        } else {
            this.$container.removeClass("is-zoomed");
        }

        if (shouldShow) {
            $controls.show();
        } else {
            $controls.hide();
        }

        if (this.chart && this.chart.data && this.chart.data.labels) {
            const total = this.chart.data.labels.length;
            const min = this.chart.options.scales.x.min != null ? this.chart.options.scales.x.min : 0;
            const max = this.chart.options.scales.x.max != null ? this.chart.options.scales.x.max : total - 1;
            const range = max - min;
            const minRange = Math.min(8, total - 1);

            $controls.find(".btn-zoom-in").prop("disabled", range <= minRange);
            $controls.find(".btn-zoom-out").prop("disabled", !zoomed);
            $controls.find(".btn-zoom-reset").prop("disabled", !zoomed);
        }
    }

    toggleTableView() {
        this.isTableView = !this.isTableView;
        if (this.isTableView) {
            this.$container.addClass("is-table-view");
            this.renderTable();
        } else {
            this.$container.removeClass("is-table-view");
        }
        this.updateControls();
    }

    renderTable() {
        let $tableWrap = this.$container.find(".chart-table-wrap");
        if (!$tableWrap.length) {
            $tableWrap = $('<div class="chart-table-wrap"></div>').appendTo(this.$container);
        }

        if (!this.chart || !this.chart.data || !this.chart.data.labels) {
            $tableWrap.html('<button type="button" class="btn btn-close-table bi bi-x-lg" title="Close table view"></button><div class="text-muted p-2 text-center">No data available</div>');
            $tableWrap.find(".btn-close-table").off("click").on("click", (e) => {
                e.stopPropagation();
                this.toggleTableView();
            });
            return;
        }

        const labels = this.chart.data.labels;
        const ds1 = this.chart.data.datasets[0] || { label: "Data 1", data: [] };
        const ds2 = this.chart.data.datasets[1] || null;

        let html = '<button type="button" class="btn btn-close-table bi bi-x-lg" title="Close table view"></button>';
        html += '<table class="table table-sm table-striped table-hover">';
        html += '<thead><tr>';
        html += '<th class="col-date">Date / Time</th>';
        html += `<th class="text-end col-val1">${this.escapeHtml(ds1.label || "Value 1")}</th>`;
        if (ds2) {
            html += `<th class="text-end col-val2">${this.escapeHtml(ds2.label || "Value 2")}</th>`;
        }
        html += '</tr></thead><tbody>';

        for (let i = 0; i < labels.length; i++) {
            const formattedDate = this.formatDateTime(labels[i]);
            const val1 = ds1.data[i] != null ? Number(ds1.data[i]).toLocaleString() : "-";
            html += `<tr><td>${formattedDate}</td><td class="text-end">${val1}</td>`;
            if (ds2) {
                const val2 = ds2.data[i] != null ? Number(ds2.data[i]).toLocaleString() : "-";
                html += `<td class="text-end">${val2}</td>`;
            }
            html += '</tr>';
        }
        html += '</tbody></table>';
        $tableWrap.html(html);

        $tableWrap.find(".btn-close-table").off("click").on("click", (e) => {
            e.stopPropagation();
            this.toggleTableView();
        });
    }

    formatDateTime(labelStr) {
        const datetime = dayjs(labelStr);
        if (!datetime.isValid()) return labelStr;
        switch (this.dateUnit) {
            case "day":
                return datetime.format("YYYY-MM-DD HH:00");
            case "week":
                return datetime.format("YYYY-MM-DD (ddd)");
            case "month":
                return datetime.format("YYYY-MM-DD");
            case "hour":
                return datetime.format("YYYY-MM-DD HH:00");
            case "year":
                return datetime.format("YYYY-MM");
            default:
                return datetime.format("YYYY-MM-DD HH:mm:ss");
        }
    }

    escapeHtml(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    downloadCsv() {
        if (!this.chart || !this.chart.data || !this.chart.data.labels || this.chart.data.labels.length === 0) {
            return;
        }

        const labels = this.chart.data.labels;
        const ds1 = this.chart.data.datasets[0] || { label: "Data 1", data: [] };
        const ds2 = this.chart.data.datasets[1] || null;

        let csv = `"Date/Time","${(ds1.label || "Data 1").replace(/"/g, '""')}"`;
        if (ds2) {
            csv += `,"${(ds2.label || "Data 2").replace(/"/g, '""')}"`;
        }
        csv += "\r\n";

        for (let i = 0; i < labels.length; i++) {
            const formattedDate = this.formatDateTime(labels[i]);
            const val1 = ds1.data[i] != null ? ds1.data[i] : 0;
            csv += `"${formattedDate}",${val1}`;
            if (ds2) {
                const val2 = ds2.data[i] != null ? ds2.data[i] : 0;
                csv += `,${val2}`;
            }
            csv += "\r\n";
        }

        const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `${this.eventId}-chart-${dayjs().format("YYYYMMDD-HHmmss")}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }

    downloadPng() {
        if (!this.chart) return;
        const canvas = this.$container.find("canvas")[0];
        if (!canvas) return;

        const isDark = this.isDarkMode();
        const bgColor = isDark ? "#000000" : "#ffffff";

        const offCanvas = document.createElement("canvas");
        offCanvas.width = canvas.width;
        offCanvas.height = canvas.height;
        const ctx = offCanvas.getContext("2d");
        ctx.fillStyle = bgColor;
        ctx.fillRect(0, 0, offCanvas.width, offCanvas.height);
        ctx.drawImage(canvas, 0, 0);

        const dataUrl = offCanvas.toDataURL("image/png");
        const link = document.createElement("a");
        link.href = dataUrl;
        link.download = `${this.eventId}-chart-${dayjs().format("YYYYMMDD-HHmmss")}.png`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    zoomIn(centerIndex = null) {
        if (!this.chart || !this.chart.data || !this.chart.data.labels) return;
        const total = this.chart.data.labels.length;
        if (total <= 1) return;

        let currMin = this.chart.options.scales.x.min != null ? this.chart.options.scales.x.min : 0;
        let currMax = this.chart.options.scales.x.max != null ? this.chart.options.scales.x.max : total - 1;
        let currRange = currMax - currMin;

        const minRange = Math.min(8, total - 1);
        if (currRange <= minRange) {
            // Reset to full view if already at maximum zoom level
            this.resetZoom();
            return;
        }

        const newRange = Math.max(minRange, Math.round(currRange * 0.5));
        const center = (centerIndex != null && centerIndex >= 0 && centerIndex < total)
            ? centerIndex
            : Math.round((currMin + currMax) / 2);

        let newMin = Math.round(center - newRange / 2);
        let newMax = newMin + newRange;

        if (newMin < 0) {
            newMin = 0;
            newMax = Math.min(total - 1, newRange);
        } else if (newMax >= total) {
            newMax = total - 1;
            newMin = Math.max(0, total - 1 - newRange);
        }

        this.chart.options.scales.x.min = newMin;
        this.chart.options.scales.x.max = newMax;
        this.chart.update("none");
        this.updateControls();
    }

    zoomOut() {
        if (!this.chart || !this.chart.data || !this.chart.data.labels) return;
        if (!this.isZoomed()) return;

        const total = this.chart.data.labels.length;
        let currMin = this.chart.options.scales.x.min != null ? this.chart.options.scales.x.min : 0;
        let currMax = this.chart.options.scales.x.max != null ? this.chart.options.scales.x.max : total - 1;
        let currRange = currMax - currMin;

        const newRange = Math.round(currRange / 0.5);
        if (newRange >= total - 1) {
            this.resetZoom();
            return;
        }

        const center = Math.round((currMin + currMax) / 2);
        let newMin = Math.round(center - newRange / 2);
        let newMax = newMin + newRange;

        if (newMin < 0) {
            newMin = 0;
            newMax = Math.min(total - 1, newRange);
        } else if (newMax >= total) {
            newMax = total - 1;
            newMin = Math.max(0, total - 1 - newRange);
        }

        this.chart.options.scales.x.min = newMin;
        this.chart.options.scales.x.max = newMax;
        this.chart.update("none");
        this.updateControls();
    }

    resetZoom() {
        if (this.chart) {
            delete this.chart.options.scales.x.min;
            delete this.chart.options.scales.x.max;
            this.chart.update("none");
            this.$container.removeClass("is-zoomed show-controls");
            this.updateControls();
        }
    }

    draw(dateUnit, labels, data1, data2) {
        this.destroy();
        const $canvas = this.ensureCanvas();
        this.ensureControls();
        this.bindInteractionEvents($canvas);

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
        const theme = this.getThemeColors();
        this.observeThemeChange();

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
                                ctx.strokeStyle = this.getThemeColors().crosshair;
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
                        backgroundColor: theme.tooltipBg,
                        titleColor: theme.tooltipTitle,
                        titleFont: (context) => ({
                            size: this.getResponsiveFontSize(context.chart?.width, "tooltip"),
                            weight: 'bold'
                        }),
                        bodyColor: theme.tooltipBody,
                        bodyFont: (context) => ({
                            size: this.getResponsiveFontSize(context.chart?.width, "tick")
                        }),
                        borderColor: theme.tooltipBorder,
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
                    }
                },
                scales: {
                    x: {
                        display: true,
                        ticks: {
                            font: (context) => ({
                                size: this.getResponsiveFontSize(context.chart?.width, "tick")
                            }),
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
                            color: (ctx) => (this.eventId === "activity" && data2[ctx.tick.value] > 0 ? "#ff6384" : this.getThemeColors().gridLine)
                        } : {}
                    },
                    y: {
                        display: true,
                        title: {
                            display: true,
                            text: (this.eventId === "session" ? `${dataLabel1} / ${dataLabel2}` : dataLabel1),
                            font: (context) => ({
                                size: this.getResponsiveFontSize(context.chart?.width, "title")
                            })
                        },
                        ticks: {
                            font: (context) => ({
                                size: this.getResponsiveFontSize(context.chart?.width, "tick")
                            })
                        },
                        suggestedMin: 0,
                        suggestedMax: 5,
                        stacked: (this.eventId === "activity"),
                        grid: {
                            color: () => this.getThemeColors().gridLine
                        }
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
        this.updateControls();
    }

    setData(labels, data1, data2) {
        if (this.chart) {
            this.chart.data.labels = labels;
            this.chart.data.datasets[0].data = data1;
            this.chart.data.datasets[1].data = data2;
            if (this.isZoomed()) {
                const total = labels.length;
                if (this.chart.options.scales.x.max >= total) {
                    this.resetZoom();
                } else {
                    this.updateControls();
                }
            }
            this.update();
            if (this.isTableView) {
                this.renderTable();
            }
        }
    }

    /**
     * Applies real-time rollup data to the chart.
     * Recognizes date-formatted labels and accumulates values in real time.
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
            if (this.isTableView) {
                this.renderTable();
            }
        }
    }

    requestUpdate() {
        if (this.updateScheduled) return;
        this.updateScheduled = true;
        requestAnimationFrame(() => {
            this.updateScheduled = false;
            if (this.chart) {
                this.chart.update("none");
                if (this.isTableView) {
                    this.renderTable();
                }
            }
        });
    }

    destroy() {
        this.updateScheduled = false;
        if (this.themeObserver) {
            this.themeObserver.disconnect();
            this.themeObserver = null;
        }
        if (this.docEventNamespace) {
            $(document).off(this.docEventNamespace);
            this.docEventNamespace = null;
        }
        if (this.chart) {
            const $canvas = this.$container.find("canvas");
            $canvas.off(".chartZoom");
            this.chart.destroy();
            this.chart = null;
        }
        this.isTableView = false;
        this.$container.removeClass("is-zoomed show-controls is-table-view");
        this.$container.find(".chart-table-wrap").remove();
        this.updateControls();
    }
}
