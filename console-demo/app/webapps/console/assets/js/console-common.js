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

/**
 * Parses various date value representations (ISO string, epochSecond object,
 * timestamp number, or Dayjs/Date) into a Day.js instance in the client's local time zone.
 *
 * @param {*} dateVal - The date value to parse.
 * @returns {object|null} The parsed Day.js instance, or null if invalid/empty.
 */
function parseToDayjs(dateVal) {
    if (!dateVal) return null;
    if (typeof dayjs === 'undefined') return null;

    if (typeof dateVal === 'object' && dateVal !== null) {
        if (dateVal.year !== undefined && dateVal.monthValue !== undefined && dateVal.dayOfMonth !== undefined) {
            const pad = n => String(n).padStart(2, '0');
            const iso = `${dateVal.year}-${pad(dateVal.monthValue)}-${pad(dateVal.dayOfMonth)}T${pad(dateVal.hour || 0)}:${pad(dateVal.minute || 0)}:${pad(dateVal.second || 0)}`;
            return (typeof dayjs.utc === 'function') ? dayjs.utc(iso).local() : dayjs(iso);
        }
        if (dateVal.epochSecond !== undefined) {
            return (typeof dayjs.unix === 'function') ? dayjs.unix(dateVal.epochSecond).local() : dayjs(dateVal.epochSecond * 1000);
        }
    }
    if (typeof dateVal === 'number') {
        return dayjs(dateVal);
    }
    if (typeof dateVal === 'string') {
        return (typeof dayjs.utc === 'function') ? dayjs.utc(dateVal).local() : dayjs(dateVal);
    }
    const d = dayjs(dateVal);
    return (d && d.isValid()) ? d : null;
}

/**
 * Formats a date value into the client's local time zone.
 *
 * @param {*} dateVal - The date value to format.
 * @param {string} [format='YYYY-MM-DD HH:mm:ss'] - The Day.js format string.
 * @returns {string} The formatted local date/time string, or '-' if invalid/empty.
 */
function formatDateTime(dateVal, format = 'YYYY-MM-DD HH:mm:ss') {
    if (!dateVal) return '-';
    if (typeof dayjs === 'undefined') return String(dateVal);

    const d = parseToDayjs(dateVal);
    return (d && d.isValid()) ? d.format(format) : String(dateVal);
}

/**
 * Calculates relative time difference and returns detailed time metadata.
 *
 * @param {*} dateVal - The date value to inspect.
 * @param {object} [options] - Formatting options.
 * @param {string} [options.lang='auto'] - Language ('ko', 'en', or 'auto').
 * @returns {object} Object containing relativeTime, fullTime, standardTime, daysDiff, minutesDiff, hoursDiff.
 */
function getRelativeTimeInfo(dateVal, options = {}) {
    const d = parseToDayjs(dateVal);
    if (!d || !d.isValid()) {
        return {
            fullTime: '-',
            standardTime: '-',
            relativeTime: '-',
            daysDiff: 999,
            minutesDiff: 999999,
            hoursDiff: 99999
        };
    }

    const fullTime = (typeof d.format === 'function') ? d.format('LLL') : String(dateVal);
    const standardTime = (typeof d.format === 'function') ? d.format('YYYY-MM-DD HH:mm:ss') : String(dateVal);
    const now = dayjs();
    const diffMinutes = Math.max(0, now.diff(d, 'minute'));
    const diffHours = Math.max(0, now.diff(d, 'hour'));
    const diffDays = Math.max(0, now.diff(d, 'day'));

    const isKo = options.lang === 'ko' || (options.lang !== 'en' && typeof navigator !== 'undefined' && navigator.language && navigator.language.startsWith('ko'));

    let relativeTime = '';
    if (diffMinutes < 1) {
        relativeTime = isKo ? '방금 전' : 'Just now';
    } else if (diffMinutes < 60) {
        relativeTime = `${diffMinutes}m ago`;
    } else if (diffHours < 24) {
        relativeTime = `${diffHours}h ago`;
    } else {
        relativeTime = `${diffDays}d ago`;
    }

    return {
        fullTime: fullTime,
        standardTime: standardTime,
        relativeTime: relativeTime,
        daysDiff: diffDays,
        minutesDiff: diffMinutes,
        hoursDiff: diffHours
    };
}

/**
 * Returns a human-readable relative time string (e.g. '방금 전', 'Just now', '5m ago', '3h ago', '2d ago', '45d ago').
 *
 * @param {*} dateVal - The date value to format.
 * @param {object} [options] - Formatting options.
 * @returns {string} The relative time string, or '-' if invalid.
 */
function formatRelativeTime(dateVal, options = {}) {
    return getRelativeTimeInfo(dateVal, options).relativeTime;
}

/**
 * Scans elements with the '.format-local-time' class within a container
 * and converts their 'data-utc' attribute value to the client's local time.
 *
 * @param {Element|jQuery|string} [container=document] - The root container to scan.
 */
function formatLocalTime(container) {
    if (typeof dayjs === 'undefined') return;

    let root = document;
    if (container) {
        if (typeof container === 'string') {
            root = document.querySelector(container) || document;
        } else if (container.nodeType) {
            root = container;
        } else if (container[0] && container[0].nodeType) {
            root = container[0];
        }
    }

    const targets = root.querySelectorAll('.format-local-time');
    targets.forEach(el => {
        const utc = el.getAttribute('data-utc');
        if (!utc) return;

        const relativeMode = el.getAttribute('data-relative');
        const fmt = el.getAttribute('data-format') || 'YYYY-MM-DD HH:mm:ss';

        if (relativeMode) {
            const info = getRelativeTimeInfo(utc);
            if (info.relativeTime === '-') {
                el.textContent = '-';
                return;
            }

            el.setAttribute('title', info.standardTime);

            if (relativeMode === 'both') {
                const dateText = formatDateTime(utc, el.getAttribute('data-format') || 'YYYY-MM-DD HH:mm');
                el.innerHTML = `${dateText} <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle ms-1">${info.relativeTime}</span>`;
            } else if (relativeMode === 'smart') {
                if (info.daysDiff <= 7) {
                    el.textContent = info.relativeTime;
                } else {
                    el.textContent = formatDateTime(utc, el.getAttribute('data-format') || 'YYYY-MM-DD');
                }
            } else {
                // 'true' or standard relative mode
                el.textContent = info.relativeTime;
            }
        } else {
            const formatted = formatDateTime(utc, fmt);
            if (formatted !== '-') {
                el.textContent = formatted;
            }
        }
    });
}

// Automatically format local times once DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => formatLocalTime());
} else {
    formatLocalTime();
}
