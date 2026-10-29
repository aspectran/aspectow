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
package com.aspectran.aspectow.appmon.engine.exporter.event;

import com.aspectran.aspectow.appmon.engine.config.EventInfo;
import com.aspectran.aspectow.appmon.engine.exporter.AbstractExporter;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterType;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCount;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCountRollupListener;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCountVO;
import com.aspectran.aspectow.appmon.engine.persist.db.mapper.EventCountMapper;
import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.utils.ToStringBuilder;
import com.aspectran.utils.json.JsonBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * An exporter that generates chart data from event counts.
 * It can query historical data from the database and also listen for real-time
 * rollup events from an {@link EventCount} instance.
 *
 * <p>Created: 2024-12-18</p>
 */
public class ChartDataExporter extends AbstractExporter implements EventCountRollupListener {

    private static final Logger logger = LoggerFactory.getLogger(ChartDataExporter.class);

    private static final ExporterType TYPE = ExporterType.DATA;

    private static final String SUB_TYPE = "chart";

    private final ExporterManager exporterManager;

    private final EventInfo eventInfo;

    private final String prefix;

    /**
     * Instantiates a new ChartDataExporter.
     * @param exporterManager the exporter manager
     * @param eventInfo the event configuration
     */
    public ChartDataExporter(
            @NonNull ExporterManager exporterManager,
            @NonNull EventInfo eventInfo) {
        super(TYPE, SUB_TYPE);
        this.exporterManager = exporterManager;
        this.eventInfo = eventInfo;
        this.prefix = exporterManager.getNodeId() + ":" + eventInfo.getAppId() + ":" +
                TYPE + "/" + SUB_TYPE + ":" + eventInfo.getEventId() + ":";
    }

    @Override
    public String getName() {
        return eventInfo.getEventId();
    }

    @Override
    public void read(@NonNull List<String> messages, CommandOptions commandOptions) {
        if (commandOptions != null && commandOptions.isNoneScope()) {
            return;
        }
        messages.add(prefix + readChartData(commandOptions));
    }

    @Override
    public void readIfChanged(@NonNull List<String> messages, CommandOptions commandOptions) {
        if (commandOptions != null) {
            if (commandOptions.hasCommand(CommandOptions.COMMAND_LOAD_PREVIOUS) ||
                    commandOptions.isNoneScope()) {
                return;
            }
        }
        messages.add(prefix + readChartData(commandOptions));
    }

    @Override
    public void broadcast(String message) {
        exporterManager.broadcast(prefix + message);
    }

    /**
     * Called when an event count is rolled up. Broadcasts the new data point.
     * @param eventCount the event count that was rolled up
     */
    @Override
    public void onRolledUp(@NonNull EventCount eventCount) {
        LocalDateTime dt = eventCount.getTallied().getDatetime();
        if (dt == null) {
            return;
        }
        String datetime = dt.truncatedTo(ChronoUnit.MINUTES)
                .atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String[] labels = new String[] { datetime };
        long[] data1 = new long[] { eventCount.getTallied().getDelta() };
        long[] data2 = new long[] { eventCount.getTallied().getAux() };
        String message = toJson(null, null, null, labels, data1, data2, true);
        broadcast(message);
    }

    private String readChartData(@Nullable CommandOptions commandOptions) {
        String period = (commandOptions != null ? commandOptions.getPeriod() : null);
        if (period != null && !period.isEmpty() && !"live".equalsIgnoreCase(period)) {
            return readPeriodicChartData(commandOptions, period);
        }

        String timeZone = (commandOptions != null ? commandOptions.getTimeZone() : null);
        String dateUnit = (commandOptions != null ? commandOptions.getDateUnit() : null);
        String dateOffsetStr = (commandOptions != null ? commandOptions.getDateOffset() : null);
        boolean isGroupScope = (commandOptions != null && commandOptions.isGroupScope());

        int zoneOffsetInSeconds = 0;
        if (timeZone != null) {
            try {
                zoneOffsetInSeconds = ZonedDateTime.now(ZoneId.of(timeZone)).getOffset().getTotalSeconds();
            } catch (Exception e) {
                // Ignore invalid timeZone
            }
        }

        LocalDateTime dateOffset = null;
        if (dateOffsetStr != null) {
            try {
                ZonedDateTime utcOffset = ZonedDateTime.parse(dateOffsetStr);
                ZonedDateTime truncated = truncateToUnit(utcOffset.toLocalDateTime(), dateUnit, zoneOffsetInSeconds);
                dateOffset = truncated.toLocalDateTime();
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to parse dateOffset: {}", dateOffsetStr);
                }
            }
        }

        final int finalZoneOffsetInSeconds = zoneOffsetInSeconds;
        final LocalDateTime finalDateOffset = dateOffset;
        EventCountMapper dao = exporterManager.getBean(EventCountMapper.class);
        List<EventCountVO> list = exporterManager.instantActivity(() -> {
            String nodeId = eventInfo.getNodeId();
            String groupId = exporterManager.getAppMonManager().getGroupId();
            String appId = eventInfo.getAppId();
            String eventId = eventInfo.getEventId();
            if (isGroupScope && groupId != null) {
                return switch (dateUnit) {
                    case "hour" -> dao.getGroupChartDataByHour(groupId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case "day" -> dao.getGroupChartDataByDay(groupId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case "month" -> dao.getGroupChartDataByMonth(groupId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case "year" -> dao.getGroupChartDataByYear(groupId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case null, default -> dao.getGroupChartData(groupId, appId, eventId, finalDateOffset);
                };
            } else {
                return switch (dateUnit) {
                    case "hour" -> dao.getChartDataByHour(nodeId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case "day" -> dao.getChartDataByDay(nodeId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case "month" -> dao.getChartDataByMonth(nodeId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case "year" -> dao.getChartDataByYear(nodeId, appId, eventId, finalZoneOffsetInSeconds, finalDateOffset);
                    case null, default -> dao.getChartData(nodeId, appId, eventId, finalDateOffset);
                };
            }
        });

        int size = list.size();
        String[] labels = new String[size];
        long[] data1 = new long[size];
        long[] data2 = new long[size];
        for (int i = 0; i < size; i++) {
            EventCountVO vo = list.get(i);
            labels[i] = normalizeDatetime(vo.getDatetime(), dateUnit, zoneOffsetInSeconds);
            data1[i] = vo.getDelta();
            data2[i] = vo.getAux();
        }

        String effectiveDateOffset = (dateOffset != null ?
                dateOffset.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null);
        return toJson(isGroupScope ? "group" : null, dateUnit, effectiveDateOffset, labels, data1, data2, false);
    }

    @Nullable
    private String normalizeDatetime(LocalDateTime datetime, String dateUnit, int zoneOffsetInSeconds) {
        ZonedDateTime truncated = truncateToUnit(datetime, dateUnit, zoneOffsetInSeconds);
        return (truncated != null ? truncated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null);
    }

    private ZonedDateTime truncateToUnit(LocalDateTime datetime, String dateUnit, int zoneOffsetInSeconds) {
        if (datetime == null) {
            return null;
        }
        ZonedDateTime utcTime = datetime.atZone(ZoneOffset.UTC);
        if (dateUnit == null) {
            return utcTime.truncatedTo(ChronoUnit.MINUTES);
        }
        try {
            ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetInSeconds);
            ZonedDateTime localTime = utcTime.withZoneSameInstant(zoneOffset);

            ZonedDateTime normalizedLocal = switch (dateUnit) {
                case "hour" -> localTime.truncatedTo(ChronoUnit.HOURS);
                case "day" -> localTime.truncatedTo(ChronoUnit.DAYS);
                case "month" -> localTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                case "year" -> localTime.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
                default -> localTime.truncatedTo(ChronoUnit.MINUTES);
            };

            return normalizedLocal.withZoneSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return utcTime.truncatedTo(ChronoUnit.MINUTES);
        }
    }

    private String toJson(
            String scope, String dateUnit, String dateOffset, String[] labels,
            long[] data1, long[] data2, boolean rolledUp) {
        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .put("scope", scope)
                    .put("dateUnit", dateUnit)
                    .put("dateOffset", dateOffset)
                    .put("labels", labels)
                    .put("data1", data1)
                    .put("data2", data2)
                    .put("rolledUp", rolledUp)
                .endObject()
                .toString();
    }

    private String readPeriodicChartData(@NonNull CommandOptions commandOptions, @NonNull String rawPeriod) {
        String period = switch (rawPeriod.toLowerCase()) {
            case "1d", "day" -> "day";
            case "1w", "week" -> "week";
            case "1m", "month" -> "month";
            case "1y", "year" -> "year";
            default -> "day";
        };

        String timeZone = commandOptions.getTimeZone();
        ZoneId zoneId;
        if (timeZone != null) {
            try {
                zoneId = ZoneId.of(timeZone);
            } catch (Exception e) {
                zoneId = ZoneId.systemDefault();
            }
        } else {
            zoneId = ZoneId.systemDefault();
        }

        String baseDateStr = commandOptions.getBaseDate();
        LocalDate baseDate;
        if (baseDateStr != null && !baseDateStr.isEmpty()) {
            try {
                baseDate = LocalDate.parse(baseDateStr);
            } catch (Exception e) {
                baseDate = LocalDate.now(zoneId);
            }
        } else {
            baseDate = LocalDate.now(zoneId);
        }

        boolean isGroupScope = commandOptions.isGroupScope();
        boolean isSession = "session".equals(eventInfo.getEventId());

        ZonedDateTime startZdt;
        ZonedDateTime endZdt;
        int slotCount;
        String[] labels;

        switch (period) {
            case "week" -> {
                LocalDate monday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                startZdt = monday.atStartOfDay(zoneId);
                endZdt = startZdt.plusWeeks(1);
                slotCount = 7;
                labels = new String[slotCount];
                for (int i = 0; i < slotCount; i++) {
                    labels[i] = startZdt.plusDays(i).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }
            case "month" -> {
                LocalDate firstDay = baseDate.withDayOfMonth(1);
                startZdt = firstDay.atStartOfDay(zoneId);
                endZdt = startZdt.plusMonths(1);
                slotCount = baseDate.lengthOfMonth();
                labels = new String[slotCount];
                for (int i = 0; i < slotCount; i++) {
                    labels[i] = startZdt.plusDays(i).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }
            case "year" -> {
                LocalDate firstDayOfYear = baseDate.withDayOfYear(1);
                startZdt = firstDayOfYear.atStartOfDay(zoneId);
                endZdt = startZdt.plusYears(1);
                slotCount = 12;
                labels = new String[slotCount];
                for (int i = 0; i < slotCount; i++) {
                    labels[i] = startZdt.plusMonths(i).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }
            default -> { // "day"
                startZdt = baseDate.atStartOfDay(zoneId);
                endZdt = startZdt.plusDays(1);
                slotCount = 24;
                labels = new String[slotCount];
                for (int i = 0; i < slotCount; i++) {
                    labels[i] = startZdt.plusHours(i).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            }
        }

        long[] data1 = new long[slotCount];
        long[] data2 = new long[slotCount];

        LocalDateTime startUtc = startZdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endUtc = endZdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        EventCountMapper dao = exporterManager.getBean(EventCountMapper.class);
        String nodeId = eventInfo.getNodeId();
        String groupId = exporterManager.getAppMonManager().getGroupId();
        String appId = eventInfo.getAppId();
        String eventId = eventInfo.getEventId();

        List<EventCountVO> currentList = exporterManager.instantActivity(() -> {
            if (isGroupScope && groupId != null) {
                return dao.getGroupChartDataByRange(groupId, appId, eventId, startUtc, endUtc);
            } else {
                return dao.getChartDataByRange(nodeId, appId, eventId, startUtc, endUtc);
            }
        });

        populateSlots(currentList, period, startZdt, zoneId, isSession, slotCount, data1, data2);

        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .put("scope", isGroupScope ? "group" : null)
                    .put("period", period)
                    .put("baseDate", baseDate.toString())
                    .put("labels", labels)
                    .put("data1", data1)
                    .put("data2", data2)
                .endObject()
                .toString();
    }

    private void populateSlots(
            List<EventCountVO> voList, String period, ZonedDateTime startZdt,
            ZoneId zoneId, boolean isSession, int slotCount, long[] outData1, long[] outData2) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        for (EventCountVO vo : voList) {
            if (vo.getDatetime() == null) continue;
            ZonedDateTime localDt = vo.getDatetime().atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId);
            int slotIdx = -1;
            switch (period) {
                case "week" -> {
                    long days = ChronoUnit.DAYS.between(startZdt.toLocalDate(), localDt.toLocalDate());
                    if (days >= 0 && days < slotCount) {
                        slotIdx = (int) days;
                    }
                }
                case "month" -> {
                    int day = localDt.getDayOfMonth() - 1;
                    if (day >= 0 && day < slotCount) {
                        slotIdx = day;
                    }
                }
                case "year" -> {
                    int month = localDt.getMonthValue() - 1;
                    if (month >= 0 && month < slotCount) {
                        slotIdx = month;
                    }
                }
                default -> { // "day"
                    long hours = ChronoUnit.HOURS.between(startZdt, localDt);
                    if (hours >= 0 && hours < slotCount) {
                        slotIdx = (int) hours;
                    }
                }
            }
            if (slotIdx >= 0) {
                outData1[slotIdx] += vo.getDelta();
                if (isSession) {
                    outData2[slotIdx] = Math.max(outData2[slotIdx], vo.getAux());
                } else {
                    outData2[slotIdx] += vo.getAux();
                }
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        // Not used
    }

    @Override
    protected void doStop() throws Exception {
        // Not used
    }

    @Override
    public String toString() {
        if (isStopped()) {
            return ToStringBuilder.toString(super.toString(), eventInfo);
        } else {
            return super.toString();
        }
    }

}
