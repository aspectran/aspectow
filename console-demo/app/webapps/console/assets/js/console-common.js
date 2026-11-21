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

/**
 * Sets a cookie with Lax same-site attribute.
 * @param {string} name - Cookie name
 * @param {string} value - Cookie value
 * @param {number} [days] - Number of days to expire
 */
function setCookie(name, value, days) {
    let expires = "";
    if (days) {
        const date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "") + expires + "; path=/; SameSite=Lax";
}

/**
 * Gets a cookie value by name.
 * @param {string} name - Cookie name
 * @returns {string|null} Cookie value or null
 */
function getCookie(name) {
    const nameEQ = name + "=";
    const ca = document.cookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
    }
    return null;
}

/**
 * Returns client IANA time zone identifier (e.g. 'Asia/Seoul').
 * @returns {string} Client time zone
 */
function getClientTimeZone() {
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    } catch (e) {
        return 'UTC';
    }
}

/**
 * Calculates optimal page size based on available window height.
 * @param {number} [reservedHeight=310]
 * @param {number} [rowHeight=41]
 * @returns {number} Calculated page size (5~50)
 */
function calculateAutoPageSize(reservedHeight = 310, rowHeight = 41) {
    const availableHeight = window.innerHeight - reservedHeight;
    let count = Math.floor(availableHeight / rowHeight);
    if (count < 5) count = 5;
    if (count > 50) count = 50;
    return count;
}

/**
 * Initializes and manages page size selector with cookie persistence and auto-height adaptation.
 * @param {object} options
 * @param {string} options.cookiePrefix - Cookie prefix (e.g. 'login_history', 'audit_log')
 * @param {string} [options.selector='#pageSizeSelect'] - Dropdown select element selector
 * @param {number} [options.renderedSize] - Currently rendered page size
 * @param {number} [options.reservedHeight=310]
 * @param {number} [options.rowHeight=41]
 */
function initPageSizeControl(options) {
    if (!options || !options.cookiePrefix) return;
    const selector = options.selector || '#pageSizeSelect';
    const $select = $(selector);
    if (!$select.length) return;

    const sizeCookieKey = options.cookiePrefix + '_page_size';
    const modeCookieKey = options.cookiePrefix + '_page_size_mode';

    const urlParams = new URLSearchParams(window.location.search);
    const currentSizeParam = urlParams.get('size');
    const renderedSize = options.renderedSize || parseInt($select.val()) || 10;

    let savedMode = getCookie(modeCookieKey) || 'auto';
    let savedSize = getCookie(sizeCookieKey);

    if (currentSizeParam) {
        if (savedMode === 'manual' && savedSize !== currentSizeParam) {
            setCookie(sizeCookieKey, currentSizeParam, 365);
        }
        $select.val(savedMode === 'auto' ? 'auto' : currentSizeParam);
    } else {
        if (savedMode === 'auto') {
            $select.val('auto');
            const optimalSize = calculateAutoPageSize(options.reservedHeight, options.rowHeight);
            if (optimalSize !== renderedSize) {
                setCookie(sizeCookieKey, optimalSize, 365);
                setCookie(modeCookieKey, 'auto', 365);
                urlParams.set('size', optimalSize);
                window.location.search = urlParams.toString();
                return;
            }
        } else {
            $select.val(renderedSize);
        }
    }

    $select.on('change', function() {
        const val = $(this).val();
        if (val === 'auto') {
            setCookie(modeCookieKey, 'auto', 365);
            const optimalSize = calculateAutoPageSize(options.reservedHeight, options.rowHeight);
            setCookie(sizeCookieKey, optimalSize, 365);
            urlParams.set('size', optimalSize);
        } else {
            setCookie(modeCookieKey, 'manual', 365);
            setCookie(sizeCookieKey, val, 365);
            urlParams.set('size', val);
        }
        urlParams.set('page', '1');
        window.location.search = urlParams.toString();
    });
}

/**
 * Navigates to a specific page number while preserving all existing query parameters
 * and ensuring timeZone is included.
 * @param {number} page
 */
function gotoPage(page) {
    const urlParams = new URLSearchParams(window.location.search);
    if (!urlParams.has('timeZone')) {
        urlParams.set('timeZone', getClientTimeZone());
    }
    urlParams.set('page', page);
    window.location.search = urlParams.toString();
}
