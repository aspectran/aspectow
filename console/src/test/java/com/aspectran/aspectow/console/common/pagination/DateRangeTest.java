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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    void testSeoulTimeZoneConversion() {
        // Asia/Seoul is UTC+9.
        // 2026-11-21 00:00:00 KST -> 2026-11-20 15:00:00 UTC
        // 2026-11-21 23:59:59.999 KST -> 2026-11-21 14:59:59 UTC
        DateRange dateRange = DateRange.of("2026-11-21", "2026-11-21", "Asia/Seoul");

        assertEquals("2026-11-21", dateRange.getStartDate());
        assertEquals("2026-11-21", dateRange.getEndDate());
        assertEquals("Asia/Seoul", dateRange.getTimeZone());

        assertEquals("2026-11-20 15:00:00", dateRange.getStartUtc());
        assertEquals("2026-11-21 14:59:59", dateRange.getEndUtc());
        assertTrue(dateRange.hasValue());
    }

    @Test
    void testNewYorkTimeZoneConversion() {
        // America/New_York in standard time is UTC-5.
        // 2026-11-21 00:00:00 EST -> 2026-11-21 05:00:00 UTC
        // 2026-11-21 23:59:59.999 EST -> 2026-11-22 04:59:59 UTC
        DateRange dateRange = DateRange.of("2026-11-21", "2026-11-21", "America/New_York");

        assertEquals("2026-11-21 05:00:00", dateRange.getStartUtc());
        assertEquals("2026-11-22 04:59:59", dateRange.getEndUtc());
    }

    @Test
    void testEmptyAndNullDateRange() {
        DateRange dateRange = DateRange.of(null, "", null);
        assertNull(dateRange.getStartDate());
        assertNull(dateRange.getEndDate());
        assertNull(dateRange.getStartUtc());
        assertNull(dateRange.getEndUtc());
        assertFalse(dateRange.hasValue());
    }

}
