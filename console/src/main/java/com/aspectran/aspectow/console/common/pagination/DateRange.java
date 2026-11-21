/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.console.common.pagination;

import com.aspectran.core.activity.Translet;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates client-side date range filtering parameters (startDate, endDate, timeZone)
 * and automatically converts them to normalized UTC timestamps for database queries.
 *
 * <p>Created: 2026-11-21</p>
 */
public class DateRange implements Serializable {

    @Serial
    private static final long serialVersionUID = 6387426682494515111L;

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final String startDate;

    private final String endDate;

    private final String timeZone;

    private final ZoneId zoneId;

    private final Instant startInstant;

    private final Instant endInstant;

    private final String startUtc;

    private final String endUtc;

    /**
     * Constructs a new DateRange with normalized UTC boundaries.
     * @param startDate the raw start date from client (e.g. yyyy-MM-dd)
     * @param endDate the raw end date from client (e.g. yyyy-MM-dd)
     * @param timeZone the client's IANA time zone identifier (e.g. Asia/Seoul)
     */
    public DateRange(String startDate, String endDate, String timeZone) {
        this.startDate = StringUtils.hasText(startDate) ? startDate.trim() : null;
        this.endDate = StringUtils.hasText(endDate) ? endDate.trim() : null;
        this.timeZone = StringUtils.hasText(timeZone) ? timeZone.trim() : null;

        ZoneId parsedZone = ZoneOffset.UTC;
        if (this.timeZone != null) {
            try {
                parsedZone = ZoneId.of(this.timeZone);
            } catch (Exception e) {
                parsedZone = ZoneId.systemDefault();
            }
        }
        this.zoneId = parsedZone;

        Instant sInstant = null;
        String sUtc = null;
        if (this.startDate != null) {
            try {
                if (this.startDate.length() == 10) {
                    LocalDate ld = LocalDate.parse(this.startDate);
                    sInstant = ld.atStartOfDay(this.zoneId).toInstant();
                    sUtc = UTC_FORMATTER.format(sInstant);
                } else {
                    sUtc = this.startDate;
                }
            } catch (Exception e) {
                sUtc = this.startDate;
            }
        }
        this.startInstant = sInstant;
        this.startUtc = sUtc;

        Instant eInstant = null;
        String eUtc = null;
        if (this.endDate != null) {
            try {
                if (this.endDate.length() == 10) {
                    LocalDate ld = LocalDate.parse(this.endDate);
                    eInstant = ld.atTime(LocalTime.MAX).atZone(this.zoneId).toInstant();
                    eUtc = UTC_FORMATTER.format(eInstant);
                } else {
                    eUtc = this.endDate;
                }
            } catch (Exception e) {
                eUtc = this.endDate;
            }
        }
        this.endInstant = eInstant;
        this.endUtc = eUtc;
    }

    /**
     * Returns the client raw start date string for form re-binding.
     * @return start date string
     */
    @Nullable
    public String getStartDate() {
        return startDate;
    }

    /**
     * Returns the client raw end date string for form re-binding.
     * @return end date string
     */
    @Nullable
    public String getEndDate() {
        return endDate;
    }

    /**
     * Returns the client IANA time zone identifier.
     * @return time zone ID string
     */
    @Nullable
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Returns the resolved client ZoneId.
     * @return client ZoneId
     */
    @NonNull
    public ZoneId getZoneId() {
        return zoneId;
    }

    /**
     * Returns the start instant in UTC.
     * @return start Instant
     */
    @Nullable
    public Instant getStartInstant() {
        return startInstant;
    }

    /**
     * Returns the end instant in UTC.
     * @return end Instant
     */
    @Nullable
    public Instant getEndInstant() {
        return endInstant;
    }

    /**
     * Returns normalized UTC start datetime string (yyyy-MM-dd HH:mm:ss) for SQL queries.
     * @return UTC start datetime string
     */
    @Nullable
    public String getStartUtc() {
        return startUtc;
    }

    /**
     * Returns normalized UTC end datetime string (yyyy-MM-dd HH:mm:ss) for SQL queries.
     * @return UTC end datetime string
     */
    @Nullable
    public String getEndUtc() {
        return endUtc;
    }

    /**
     * Returns true if either startDate or endDate is specified.
     * @return true if range has any boundary
     */
    public boolean hasValue() {
        return (startDate != null || endDate != null);
    }

    /**
     * Factory method creating DateRange from current translet request parameters.
     * @param translet current translet
     * @return DateRange instance
     */
    @NonNull
    public static DateRange of(@NonNull Translet translet) {
        String startDate = translet.getParameter("startDate");
        String endDate = translet.getParameter("endDate");
        String timeZone = translet.getParameter("timeZone");
        return of(startDate, endDate, timeZone);
    }

    /**
     * Factory method creating DateRange from parameters.
     * @param startDate start date
     * @param endDate end date
     * @param timeZone time zone ID
     * @return DateRange instance
     */
    @NonNull
    public static DateRange of(String startDate, String endDate, String timeZone) {
        return new DateRange(startDate, endDate, timeZone);
    }

    @Override
    public String toString() {
        return "DateRange{" +
                "startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", timeZone='" + timeZone + '\'' +
                ", startUtc='" + startUtc + '\'' +
                ", endUtc='" + endUtc + '\'' +
                '}';
    }

}
