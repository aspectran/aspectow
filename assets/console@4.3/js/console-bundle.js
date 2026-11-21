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

(function(global) {
    'use strict';

    const APON = {};

    const COMMENT_CHAR = '#';
    const NAME_VALUE_SEPARATOR = ':';
    const COMMA_CHAR = ',';
    const BLOCK_OPEN = '{';
    const BLOCK_CLOSE = '}';
    const ARRAY_OPEN = '[';
    const ARRAY_CLOSE = ']';
    const TEXT_OPEN = '(';
    const TEXT_CLOSE = ')';
    const TEXT_LINE_START = '|';
    const DOUBLE_QUOTE_CHAR = '"';
    const SINGLE_QUOTE_CHAR = "'";
    const ESCAPE_CHAR = '\\';

    /**
     * Parses an APON (Aspectran Parameters Object Notation) string, constructing the JavaScript value or object described by the string.
     * @param {string} apon The string to parse as APON.
     * @returns {object} The object corresponding to the given APON text.
     */
    APON.parse = function(apon) {
        if (typeof apon !== 'string' || !apon.trim()) {
            return {};
        }

        let lineNumber = 1;
        let linePos = 0;
        let pos = 0;

        function peekChar() {
            if (pos < apon.length) {
                const char = apon[pos];
                if (char === '\r' || char === '\n') {
                    return '\n';
                }
                return char;
            }
            return null;
        }

        function readChar() {
            const char = apon[pos++];
            if (char === '\n') {
                lineNumber++;
                linePos = 0;
            } else if (char === '\r') {
                if (apon[pos] === '\n') {
                    pos++;
                }
                lineNumber++;
                linePos = 0;
                return '\n';
            } else {
                linePos++;
            }
            return char;
        }

        function skipWhitespace() {
            while (pos < apon.length) {
                const char = apon[pos];
                if (char === COMMENT_CHAR) {
                    while (pos < apon.length && apon[pos] !== '\n' && apon[pos] !== '\r') {
                        pos++;
                    }
                    continue;
                }
                if (char === ' ' || char === '\t' || char === '\f' || char === '\v') {
                    pos++;
                    linePos++;
                    continue;
                }
                break;
            }
        }

        function skipWhitespaceAndCommas() {
            while (pos < apon.length) {
                const char = apon[pos];
                if (char === COMMENT_CHAR) {
                    while (pos < apon.length && apon[pos] !== '\n' && apon[pos] !== '\r') {
                        pos++;
                    }
                    continue;
                }
                if (char === ' ' || char === '\t' || char === '\r' || char === '\n' || char === COMMA_CHAR || char === '\f' || char === '\v') {
                    readChar();
                    continue;
                }
                break;
            }
        }

        function syntaxError(message) {
            const error = new Error('Invalid APON format: ' + message + ' at [lineNumber: ' + lineNumber + ', linePos: ' + (linePos + 1) + ']');
            error.lineNumber = lineNumber;
            error.linePos = linePos + 1;
            return error;
        }

        function readQuotedString(quoteChar) {
            let str = '';
            while (pos < apon.length) {
                const char = readChar();
                if (char === quoteChar) {
                    return str;
                }
                if (char === ESCAPE_CHAR) {
                    const next = readChar();
                    if (next === 'n') str += '\n';
                    else if (next === 'r') str += '\r';
                    else if (next === 't') str += '\t';
                    else if (next === 'b') str += '\b';
                    else if (next === 'f') str += '\f';
                    else if (next === quoteChar) str += quoteChar;
                    else if (next === ESCAPE_CHAR) str += ESCAPE_CHAR;
                    else if (next === 'u') {
                        const hex = apon.substring(pos, pos + 4);
                        pos += 4;
                        linePos += 4;
                        str += String.fromCharCode(parseInt(hex, 16));
                    } else str += next;
                } else {
                    str += char;
                }
            }
            throw syntaxError('Unclosed quotation mark');
        }

        function readName() {
            skipWhitespace();
            const char = peekChar();
            if (char === DOUBLE_QUOTE_CHAR || char === SINGLE_QUOTE_CHAR) {
                readChar();
                return readQuotedString(char);
            }
            let name = '';
            while (pos < apon.length) {
                const c = peekChar();
                if (c === null || c === '\n' || c === NAME_VALUE_SEPARATOR || c === COMMENT_CHAR) {
                    break;
                }
                if (name === '') {
                    if (c === COMMA_CHAR || c === BLOCK_OPEN || c === BLOCK_CLOSE || c === ARRAY_OPEN || c === ARRAY_CLOSE) {
                        break;
                    }
                }
                if (/\s/.test(c)) {
                    const savedPos = pos;
                    const savedLinePos = linePos;
                    const savedLineNumber = lineNumber;
                    skipWhitespace();
                    if (peekChar() === NAME_VALUE_SEPARATOR) {
                        break;
                    }
                    pos = savedPos;
                    linePos = savedLinePos;
                    lineNumber = savedLineNumber;
                }
                name += readChar();
            }
            return name.trim();
        }

        function parseValue(typeHint, inArray) {
            skipWhitespace();
            const char = peekChar();
            if (char === null) return null;

            if (char === BLOCK_OPEN) {
                readChar();
                return parseObject(BLOCK_CLOSE);
            }
            if (char === ARRAY_OPEN) {
                readChar();
                return parseArray();
            }
            if (char === TEXT_OPEN) {
                const savedPos = pos;
                const savedLinePos = linePos;
                const savedLineNumber = lineNumber;
                readChar();
                // Check if it's a text block start: ( followed by newline
                while (pos < apon.length) {
                    const c = apon[pos];
                    if (c === ' ' || c === '\t') { pos++; linePos++; continue; }
                    if (c === '\r' || c === '\n') {
                        return parseTextBlock();
                    }
                    break;
                }
                pos = savedPos;
                linePos = savedLinePos;
                lineNumber = savedLineNumber;
            }

            if (char === BLOCK_CLOSE || char === ARRAY_CLOSE) return null;

            let valueStr = readToken(inArray);
            if (valueStr === null) return null;

            if (valueStr === '{}') return {};
            if (valueStr === '[]') return [];
            if (valueStr === 'null') return null;

            if (typeHint) {
                return castValue(valueStr, typeHint);
            }

            if (valueStr === 'true') return true;
            if (valueStr === 'false') return false;

            if (!isNaN(valueStr) && valueStr.trim() !== '') {
                if (valueStr.includes('.') || valueStr.toLowerCase().includes('e')) {
                    return parseFloat(valueStr);
                } else {
                    return parseInt(valueStr, 10);
                }
            }
            return valueStr;
        }

        function readToken(inArray) {
            skipWhitespace();
            const firstChar = peekChar();
            if (firstChar === DOUBLE_QUOTE_CHAR || firstChar === SINGLE_QUOTE_CHAR) {
                readChar();
                return readQuotedString(firstChar);
            }
            let token = '';
            while (pos < apon.length) {
                const c = peekChar();
                if (c === null || c === '\n' || c === COMMENT_CHAR) break;
                if (token === '') {
                    if (c === BLOCK_OPEN || c === BLOCK_CLOSE || c === ARRAY_OPEN || c === ARRAY_CLOSE) break;
                } else {
                    if (c === BLOCK_CLOSE || c === ARRAY_CLOSE) break;
                }
                if (c === COMMA_CHAR) {
                    if (inArray || hasColonAheadOnLine()) break;
                }
                if (/\s/.test(c)) {
                    const savedPos = pos;
                    const savedLinePos = linePos;
                    const savedLineNumber = lineNumber;
                    let hasNewline = false;
                    while (pos < apon.length && /\s/.test(apon[pos])) {
                        if (readChar() === '\n') { hasNewline = true; break; }
                    }
                    const next = peekChar();
                    if (hasNewline || next === BLOCK_CLOSE || next === ARRAY_CLOSE || next === COMMA_CHAR || next === COMMENT_CHAR) {
                        pos = savedPos;
                        linePos = savedLinePos;
                        lineNumber = savedLineNumber;
                        break;
                    }
                    pos = savedPos;
                    linePos = savedLinePos;
                    lineNumber = savedLineNumber;
                }
                if (c === ESCAPE_CHAR) {
                    readChar();
                    const next = readChar();
                    if (next === null) break;
                    if (next === COMMA_CHAR || next === ESCAPE_CHAR) token += next;
                    else token += ESCAPE_CHAR + next;
                    continue;
                }
                token += readChar();
            }
            return token.trim() || null;
        }

        function hasColonAheadOnLine() {
            let p = pos;
            while (p < apon.length) {
                const c = apon[p];
                if (c === '\r' || c === '\n' || c === COMMENT_CHAR) break;
                if (c === NAME_VALUE_SEPARATOR) return true;
                if (c === BLOCK_OPEN || c === BLOCK_CLOSE || c === ARRAY_OPEN || c === ARRAY_CLOSE) break;
                p++;
            }
            return false;
        }

        function parseTextBlock() {
            let text = '';
            let firstLine = true;
            // consume the opening line
            while (pos < apon.length && apon[pos] !== '\n' && apon[pos] !== '\r') pos++;
            readChar();

            while (pos < apon.length) {
                // skip leading whitespace on line
                while (pos < apon.length && (apon[pos] === ' ' || apon[pos] === '\t')) { pos++; linePos++; }
                const c = apon[pos];
                if (c === TEXT_CLOSE) {
                    readChar();
                    return text;
                }
                if (c === TEXT_LINE_START) {
                    if (!firstLine) text += '\n';
                    pos++; linePos++;
                    let lineEnd = pos;
                    while (lineEnd < apon.length && apon[lineEnd] !== '\n' && apon[lineEnd] !== '\r') lineEnd++;
                    text += apon.substring(pos, lineEnd);
                    pos = lineEnd;
                    readChar();
                    firstLine = false;
                    continue;
                }
                if (pos >= apon.length) break;
                if (apon[pos] === '\n' || apon[pos] === '\r') {
                    readChar();
                    continue;
                }
                throw syntaxError('Text block lines must start with a "|" character or end with ")"');
            }
            throw syntaxError('Unclosed text block');
        }

        function parseObject(endChar) {
            const obj = {};
            while (true) {
                skipWhitespaceAndCommas();
                const char = peekChar();
                if (char === null) break;
                if (char === endChar) {
                    readChar();
                    return obj;
                }
                const nameToken = readName();
                if (!nameToken) {
                    if (peekChar() === endChar) continue;
                    throw syntaxError('Parameter name is missing');
                }
                skipWhitespace();
                if (readChar() !== NAME_VALUE_SEPARATOR) {
                    throw syntaxError('Missing name-value separator ":"');
                }
                
                let name = nameToken;
                let typeHint = null;
                const typeHintIndex = name.indexOf(TEXT_OPEN);
                if (typeHintIndex > 0 && name.endsWith(TEXT_CLOSE)) {
                    typeHint = name.substring(typeHintIndex + 1, name.length - 1).toLowerCase();
                    name = name.substring(0, typeHintIndex);
                }

                const val = parseValue(typeHint, false);
                if (obj.hasOwnProperty(name)) {
                    if (Array.isArray(obj[name])) {
                        obj[name].push(val);
                    } else {
                        obj[name] = [obj[name], val];
                    }
                } else {
                    obj[name] = val;
                }
            }
            if (endChar) throw syntaxError('Unclosed block, missing "' + endChar + '"');
            return obj;
        }

        function parseArray() {
            const arr = [];
            while (true) {
                skipWhitespaceAndCommas();
                const char = peekChar();
                if (char === null) throw syntaxError('Unclosed array bracket, missing "]"');
                if (char === ARRAY_CLOSE) {
                    readChar();
                    return arr;
                }
                arr.push(parseValue(null, true));
            }
        }

        function castValue(value, typeHint) {
            switch (typeHint) {
                case 'string': return String(value);
                case 'int':
                case 'long': return parseInt(value, 10);
                case 'float':
                case 'double': return parseFloat(value);
                case 'boolean': return String(value).toLowerCase() === 'true';
                default: return value;
            }
        }

        skipWhitespaceAndCommas();
        const firstChar = peekChar();
        if (firstChar === null) return {};
        if (firstChar === BLOCK_OPEN) {
            readChar();
            const res = parseObject(BLOCK_CLOSE);
            skipWhitespaceAndCommas();
            if (peekChar() !== null) throw syntaxError('Unexpected content after closing brace "}" of root object');
            return res;
        } else if (firstChar === ARRAY_OPEN) {
            readChar();
            const res = parseArray();
            skipWhitespaceAndCommas();
            if (peekChar() !== null) throw syntaxError('Unexpected content after closing bracket "]" of root array');
            return res;
        } else {
            return parseObject(null);
        }
    };

    /**
     * Converts a JavaScript value to an APON (Aspectran Parameters Object Notation) string.
     * @param {object} obj The value to convert to an APON string.
     * @param {object|number} [options] An object that contains options, or the number of spaces to use for indentation.
     * @param {string} [options.indent="  "] The string to use for indentation.
     * @param {string} [options.style="PRETTY"] The output style ("PRETTY", "SINGLE_LINE", or "COMPACT").
     * @returns {string} The APON string representation of the value.
     */
    APON.stringify = function(obj, options) {
        if (typeof obj !== 'object' || obj === null) {
            throw new Error('APON.stringify input must be an object or an array.');
        }

        let indentString = '  ';
        let style = 'PRETTY';

        if (typeof options === 'number') {
            indentString = ' '.repeat(options);
        } else if (typeof options === 'object' && options !== null) {
            if (typeof options.indent === 'string') indentString = options.indent;
            if (typeof options.style === 'string') style = options.style.toUpperCase();
        }

        const isPretty = style === 'PRETTY';
        const isCompact = style === 'COMPACT';

        function needsQuotes(str) {
            if (str.length === 0) return true;
            if (str.trim() !== str) return true;
            if (/[{}:#"',\[\]]/.test(str) || /^\(/.test(str)) return true;
            const lower = str.toLowerCase();
            if (lower === 'null' || lower === 'true' || lower === 'false') return true;
            if (!isNaN(str) && str.trim() !== '') return true;
            return false;
        }

        function escapeString(str) {
            return str.replace(/\\/g, '\\\\')
                      .replace(/"/g, '\\"')
                      .replace(/\n/g, '\\n')
                      .replace(/\r/g, '\\r')
                      .replace(/\t/g, '\\t')
                      .replace(/\x08/g, '\\b')
                      .replace(/\x0c/g, '\\f');
        }

        function stringifyValue(value, indent) {
            if (value === null) return 'null';
            if (typeof value === 'string') {
                if (isPretty && value.includes('\n')) {
                    const lines = value.split(/\r?\n/);
                    const content = lines.map(line => `${indent}${indentString}${TEXT_LINE_START}${line}`);
                    return `(\n${content.join('\n')}\n${indent})`;
                } else {
                    return needsQuotes(value) || value.includes('\n') ? `"${escapeString(value)}"` : value;
                }
            }
            if (typeof value === 'object') {
                return stringifyObject(value, indent);
            }
            return String(value);
        }

        function stringifyObject(obj, indent) {
            const nextIndent = isPretty ? indent + indentString : '';
            const separator = isPretty ? '\n' : (isCompact ? ',' : ', ');
            const kvSeparator = isCompact ? ':' : ': ';
            const openPad = (isPretty || isCompact) ? '' : ' ';
            const closePad = (isPretty || isCompact) ? '' : ' ';

            if (Array.isArray(obj)) {
                if (obj.length === 0) return '[]';
                const content = obj.map(item => (isPretty ? nextIndent : '') + stringifyValue(item, nextIndent));
                if (isPretty) {
                    return `[\n${content.join('\n')}\n${indent}]`;
                } else {
                    return `[${openPad}${content.join(separator)}${closePad}]`;
                }
            } else {
                const keys = Object.keys(obj);
                if (keys.length === 0) return '{}';
                const content = keys.map(key => {
                    const val = obj[key];
                    return (isPretty ? nextIndent : '') + key + kvSeparator + stringifyValue(val, nextIndent);
                });
                if (isPretty) {
                    return `{\n${content.join('\n')}\n${indent}}`;
                } else {
                    return `{${openPad}${content.join(separator)}${closePad}}`;
                }
            }
        }

        if (Array.isArray(obj)) {
            return stringifyObject(obj, '');
        } else {
            const keys = Object.keys(obj);
            const separator = isPretty ? '\n' : (isCompact ? ',' : ', ');
            const kvSeparator = isCompact ? ':' : ': ';
            const lines = keys.map(key => key + kvSeparator + stringifyValue(obj[key], ''));
            return lines.join(separator);
        }
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = APON;
    } else {
        global.APON = APON;
    }

})(typeof window !== 'undefined' ? window : this);

/**
 * APON (Aspectran Parameter Object Notation) Syntax Highlighter.
 * Parses raw APON text and returns HTML marked up with syntax classes.
 * 
 * @param {string} text - The raw APON format string.
 * @returns {string} The syntax-highlighted HTML string.
 *
 * @version 1.0
 * @last-modified 2026-06-24
 */
function highlightApon(text) {
    if (!text) return '';

    let i = 0;
    let result = '';
    const len = text.length;

    function escapeHtml(str) {
        return str
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }

    while (i < len) {
        const char = text[i];

        // 1. Comments: starting with # until end of line
        if (char === '#') {
            let comment = '';
            while (i < len && text[i] !== '\n') {
                comment += text[i];
                i++;
            }
            result += `<span class="apon-comment">${escapeHtml(comment)}</span>`;
            continue;
        }

        // 2. Multiline String: ''' ... '''
        if (text.startsWith("'''", i)) {
            let multilineStr = "'''";
            i += 3;
            while (i < len && !text.startsWith("'''", i)) {
                multilineStr += text[i];
                i++;
            }
            if (i < len) {
                multilineStr += "'''";
                i += 3;
            }
            result += `<span class="apon-string">${escapeHtml(multilineStr)}</span>`;
            continue;
        }

        // 3. String literal: " ... "
        if (char === '"') {
            let strLiteral = '"';
            i++;
            let escaped = false;
            while (i < len) {
                const c = text[i];
                strLiteral += c;
                i++;
                if (escaped) {
                    escaped = false;
                } else if (c === '\\') {
                    escaped = true;
                } else if (c === '"') {
                    break;
                }
            }
            result += `<span class="apon-string">${escapeHtml(strLiteral)}</span>`;
            continue;
        }

        // 4. Text block: ( ... )
        if (char === '(') {
            result += `<span class="apon-bracket">(</span>`;
            i++;
            let contentStart = i;
            
            // Find the true closing parenthesis for the text block
            while (i < len) {
                if (text[i] === ')') {
                    // Backtrack to check if this line starts with '|'
                    let isInsidePipeLine = false;
                    let k = i - 1;
                    while (k >= contentStart && text[k] !== '\n' && text[k] !== '\r') {
                        if (text[k] === '|') {
                            isInsidePipeLine = true;
                            break;
                        }
                        k--;
                    }
                    if (!isInsidePipeLine) {
                        // Found the closing parenthesis (no pipe character on this line)
                        break;
                    }
                }
                i++;
            }
            
            let content = text.substring(contentStart, i);
            result += `<span class="apon-string">${escapeHtml(content)}</span>`;
            if (i < len && text[i] === ')') {
                result += `<span class="apon-bracket">)</span>`;
                i++;
            }
            continue;
        }

        // 5. Brackets and Braces (curly and square)
        if (char === '{' || char === '}' || char === '[' || char === ']') {
            result += `<span class="apon-bracket">${char}</span>`;
            i++;
            continue;
        }

        // 6. White spaces
        if (char === ' ' || char === '\t' || char === '\n' || char === '\r') {
            result += char;
            i++;
            continue;
        }

        // 7. Words (Keys, Keywords, Values)
        let start = i;
        while (i < len && text[i] !== ' ' && text[i] !== '\t' && text[i] !== '\n' && text[i] !== '\r' && 
               text[i] !== '{' && text[i] !== '}' && text[i] !== '[' && text[i] !== ']' && 
               text[i] !== '(' && text[i] !== ')' &&
               text[i] !== ',' && text[i] !== ':' && text[i] !== '"' && text[i] !== '#') {
            i++;
        }

        if (start === i) {
            result += escapeHtml(text[i]);
            i++;
            continue;
        }

        let word = text.substring(start, i);

        if (i < len && text[i] === ':') {
            result += `<span class="apon-key">${escapeHtml(word)}</span>:`;
            i++; // consume ':'
            
            // Consume spaces after colon
            while (i < len && (text[i] === ' ' || text[i] === '\t')) {
                result += text[i];
                i++;
            }
            
            // If there's an unquoted value after colon, scan it as a block
            if (i < len && 
                text[i] !== '{' && text[i] !== '[' && text[i] !== '(' && 
                text[i] !== '"' && text[i] !== '\'' && text[i] !== '#' && 
                text[i] !== '\n' && text[i] !== '\r' && text[i] !== ',' &&
                text[i] !== '}' && text[i] !== ']') {
                
                let valStart = i;
                while (i < len && 
                       text[i] !== '\n' && text[i] !== '\r' && 
                       text[i] !== ',' && 
                       text[i] !== '}' && text[i] !== ']') {
                    
                    if (text[i] === '#' && (text[i-1] === ' ' || text[i-1] === '\t')) {
                        break;
                    }
                    i++;
                }
                
                let val = text.substring(valStart, i);
                let trimmedVal = val.trimEnd();
                let actualVal = val;
                if (trimmedVal.length < val.length) {
                    i = valStart + trimmedVal.length;
                    actualVal = trimmedVal;
                }
                
                if (actualVal === 'true' || actualVal === 'false' || actualVal === 'null') {
                    result += `<span class="apon-keyword">${actualVal}</span>`;
                } else if (!isNaN(actualVal) && !isNaN(parseFloat(actualVal))) {
                    result += `<span class="apon-number">${actualVal}</span>`;
                } else {
                    result += `<span class="apon-string">${escapeHtml(actualVal)}</span>`;
                }
            }
            continue;
        }

        // Unquoted value without a following colon (e.g. array element)
        if (word === 'true' || word === 'false' || word === 'null') {
            result += `<span class="apon-keyword">${word}</span>`;
        } else if (!isNaN(word) && !isNaN(parseFloat(word))) {
            result += `<span class="apon-number">${word}</span>`;
        } else {
            result += `<span class="apon-string">${escapeHtml(word)}</span>`;
        }
    }

    return result;
}

/**
 * ConsoleClient provides a unified interface for real-time communication with Console activities,
 * automatically falling back to HTTP long-polling if WebSockets are unavailable.
 *
 * @version 4.3
 * @last-modified 2026-11-21
 */
class ConsoleClient {

    constructor(node, options = {}) {
        this.node = node;
        this.options = Object.assign({
            heartbeatInterval: 50000,
            pollingInterval: 3000,
            maxRetries: 10,
            retryInterval: 5000,
            token: null,
            onBeforeConnect: null,
            onOpen: null,
            onClose: null,
            onRetry: null,
            onFailed: null,
            onEstablished: null,
            onMessage: null
        }, options);

        if (this.options.token && this.node.endpoint) {
            this.node.endpoint.token = this.options.token;
        }

        this.socket = null;
        this.heartbeatTimer = null;
        this.pollingTimer = null;
        this.reconnectTimer = null;
        this.retryCount = 0;
        this.established = false;
        this.manualClose = false;
        this.unloading = false;
        this.activityPath = null;
        this.primaryNodeId = node.id;
        this.mode = 'websocket'; // 'websocket' or 'polling'
        this.wsEverConnected = false;

        this.handlePageHide = () => {
            this.unloading = true;
            if (this.reconnectTimer) {
                clearTimeout(this.reconnectTimer);
                this.reconnectTimer = null;
            }
            if (this.pollingTimer) {
                clearTimeout(this.pollingTimer);
                this.pollingTimer = null;
            }
        };

        if (typeof window !== "undefined") {
            window.addEventListener("beforeunload", this.handlePageHide);
            window.addEventListener("pagehide", this.handlePageHide);
        }
    }

    /**
     * Connects to the server using the provided node endpoint.
     * @param {string} activityPath - The activity-specific path (e.g., 'nodes', 'commands', 'scheduler')
     */
    start(activityPath) {
        this.activityPath = activityPath;
        this.manualClose = false;
        this.unloading = false;
        this.openSocket();
    }

    /**
     * Closes the connection manually.
     */
    stop() {
        this.manualClose = true;
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
        this.closeSocket(false);
        this.stopPolling();
    }

    /**
     * Destroys the client and removes event listeners.
     */
    destroy() {
        this.stop();
        if (typeof window !== "undefined") {
            window.removeEventListener("beforeunload", this.handlePageHide);
            window.removeEventListener("pagehide", this.handlePageHide);
        }
    }

    /**
     * Opens a new connection.
     */
    openSocket() {
        if (this.manualClose || this.unloading) {
            return;
        }
        if (this.options.onBeforeConnect) {
            Promise.resolve(this.options.onBeforeConnect(this.node)).then((token) => {
                if (this.manualClose || this.unloading) {
                    return;
                }
                if (token) {
                    if (this.node.endpoint) {
                        this.node.endpoint.token = token;
                    }
                }
                if (this.node.endpoint && this.node.endpoint.token) {
                    this.connect();
                } else {
                    console.warn(this.node.id, "failed to obtain token before connect");
                    if (!this.manualClose && !this.unloading) {
                        this.reconnect();
                    }
                }
            }).catch((err) => {
                if (!this.manualClose && !this.unloading) {
                    console.error(this.node.id, "failed to prepare connection:", err);
                    this.reconnect();
                }
            });
        } else {
            this.connect();
        }
    }

    /**
     * Closes the socket and clears timers.
     * @param {boolean} afterClosing - whether this is called after the socket is already closed
     * @private
     */
    closeSocket(afterClosing) {
        if (this.socket) {
            const socket = this.socket;
            this.socket = null;
            this.established = false;
            socket.onopen = null;
            socket.onmessage = null;
            socket.onerror = null;
            socket.onclose = null;
            if (!afterClosing) {
                try {
                    socket.close(1000, "Normal closure");
                } catch (e) {
                    // Ignore if already closed
                }
            }
        }
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    endpointPath() {
        let path = this.node.endpoint.path;
        if (this.node.port && (location.hostname === "localhost" || location.hostname === "127.0.0.1")) {
            const url = new URL(path, location.href);
            url.port = this.node.port;
            path = url.origin + url.pathname;
        }
        if (this.activityPath) {
            const p = (path.endsWith('/') ? path : path + '/');
            const a = (this.activityPath.startsWith('/') ? this.activityPath.substring(1) : this.activityPath);
            path = p + a;
        }
        return path;
    }

    /**
     * Actually opens a new WebSocket connection.
     * @private
     */
    connect() {
        if (this.manualClose || this.unloading) {
            return;
        }
        if (this.node.endpoint && this.node.endpoint.mode === 'polling') {
            this.switchToPolling();
            return;
        }
        if (!this.node.endpoint || !this.node.endpoint.token) {
            console.warn(this.node.id, "cannot connect to websocket without a valid token");
            if (!this.manualClose && !this.unloading) {
                this.reconnect();
            }
            return;
        }
        this.mode = 'websocket';
        this.closeSocket(false);

        const url = new URL(this.endpointPath() + "/websocket/" + this.node.endpoint.token, location.href);
        url.protocol = url.protocol.replace("https:", "wss:").replace("http:", "ws:");

        console.log(this.node.id, "connecting to websocket:", url.href);
        try {
            this.socket = new WebSocket(url.href);

            this.socket.onopen = (event) => {
                console.log(this.node.id, "websocket connected");
                this.wsEverConnected = true;
                const subscribeRequest = Object.assign({ header: "subscribe", targetNodeId: this.node.id }, this.options.subscribeParams);
                this.socket.send(JSON.stringify(subscribeRequest));
                this.sendPing();

                if (this.options.onOpen) {
                    this.options.onOpen(event);
                }
            };

            this.socket.onmessage = (event) => {
                if (typeof event.data === "string") {
                    try {
                        const response = JSON.parse(event.data);
                        const header = response.header;
                        if (header === 'subscribed') {
                            const primaryNodeId = response.nodeId;
                            console.log("subscribed", primaryNodeId);
                            this.establish(primaryNodeId);
                            this.sendMessage({ header: "established" });
                        } else if (header === 'pong') {
                            this.sendPing();
                        } else {
                            this.handleMessage(response);
                        }
                    } catch (e) {
                        console.error("Failed to parse incoming WebSocket message:", event.data, e);
                    }
                }
            };

            this.socket.onclose = (event) => {
                this.closeSocket(true);
                if (this.mode === 'polling') return;
                if (!this.manualClose && !this.unloading) {
                    if (this.options.onClose) {
                        setTimeout(() => {
                            if (!this.manualClose && !this.unloading) {
                                this.options.onClose(event);
                            }
                        }, 100);
                    }
                    if (event.code === 1003) {
                        console.warn("Websocket connection refused: ", event.code, (event.reason || "Unauthorized"));
                        return;
                    }
                    if (event.code === 1011) {
                        console.log("Websocket connection closed: ", event.code);
                        return;
                    }
                    if (event.code === 1000 || this.retryCount === 0) {
                        console.log("Websocket connection closed: ", event.code);
                    }
                    if (event.code !== 1000) {
                        setTimeout(() => this.reconnect(), 1000);
                    }
                }
            };

            this.socket.onerror = (event) => {
                if (this.manualClose || this.unloading) {
                    return;
                }
                console.error(this.node.id, "websocket error:", event);
                if (!this.wsEverConnected) {
                    this.switchToPolling();
                }
            };
        } catch (e) {
            console.error(this.node.id, "failed to create websocket:", e);
            if (!this.wsEverConnected) {
                this.switchToPolling();
            }
        }
    }

    switchToPolling() {
        if (this.mode === 'polling' || this.manualClose || this.wsEverConnected) return;
        console.warn(this.node.id, "switching to HTTP polling mode");
        this.mode = 'polling';
        this.closeSocket(false);
        this.startPolling();
    }

    startPolling() {
        this.stopPolling();

        let subscribeUrl = this.endpointPath() + "/polling/subscribe?targetNodeId=" + this.node.id;
        const token = (this.node.endpoint ? this.node.endpoint.token : null) || this.options.token;
        if (token) {
            subscribeUrl += "&token=" + encodeURIComponent(token);
        }
        fetch(subscribeUrl, {
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(res => {
                if (!res.ok) {
                    throw new Error("HTTP error " + res.status);
                }
                return res.json();
            })
            .then(res => {
                if (res.success) {
                    const primaryNodeId = res.data.nodeId;
                    console.log("subscribed", primaryNodeId);
                    if (res.data.pollingInterval) {
                        this.options.pollingInterval = res.data.pollingInterval;
                        console.log("polling interval:", this.options.pollingInterval);
                    }
                    this.establish(primaryNodeId);
                    if (this.options.onOpen) {
                        try {
                            this.options.onOpen();
                        } catch (e) {
                            console.error(this.node.id, "Error in onOpen callback:", e);
                        }
                    }
                    this.poll();
                } else {
                    throw new Error(res.error.message);
                }
            })
            .catch(err => {
                console.error("Failed to start polling:", err);
                this.reconnect();
            });
    }

    poll() {
        if (this.mode !== 'polling' || this.manualClose || this.unloading) return;

        const pullUrl = this.endpointPath() + "/polling/pull";
        fetch(pullUrl, {
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(res => {
                if (!res.ok) {
                    throw new Error("HTTP error " + res.status);
                }
                return res.json();
            })
            .then(res => {
                if (res.success) {
                    if (res.data) {
                        res.data.forEach(msg => {
                            try {
                                // Clean unescaped control characters before parsing JSON
                                const cleaned = msg.replace(/[\u0000-\u001f]/g, function(ch) {
                                    if (ch === '\n') return '\\n';
                                    if (ch === '\r') return '\\r';
                                    if (ch === '\t') return '\\t';
                                    return '';
                                });
                                const response = JSON.parse(cleaned);
                                try {
                                    this.handleMessage(response);
                                } catch (ex) {
                                    console.error(this.node.id, "Error in handleMessage callback:", ex);
                                }
                            } catch (e) {
                                console.error(this.node.id, "failed to parse poll message:", msg, e);
                            }
                        });
                    }
                    this.pollingTimer = setTimeout(() => this.poll(), this.options.pollingInterval);
                } else {
                    if (res.error && res.error.code === 'session_not_found') {
                        console.warn(this.node.id, "Session lost (not found). Re-subscribing...");
                        this.reconnect();
                    } else {
                        throw new Error(res.error ? res.error.message : "Polling failed");
                    }
                }
            })
            .catch(err => {
                console.error(this.node.id, "polling error:", err);
                this.reconnect();
            });
    }

    stopPolling() {
        if (this.pollingTimer) {
            clearTimeout(this.pollingTimer);
            this.pollingTimer = null;
        }
    }

    /**
     * Completes the connection process after receiving the 'subscribed' message.
     */
    establish(primaryNodeId) {
        this.retryCount = 0;
        this.established = true;
        this.primaryNodeId = primaryNodeId;
        if (this.options.onEstablished) {
            this.options.onEstablished(this.node);
        }
    }

    handleMessage(response) {
        if (this.options.onMessage) {
            this.options.onMessage(response);
        }
    }

    /**
     * Sends a raw message to the server.
     * @param {Object} request - the message data to send
     */
    sendMessage(request) {
        if (this.mode === 'websocket' && this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(request));
        } else if (this.mode === 'polling') {
            try {
                this.pushMessage(request);
            } catch (e) {
                console.error("Failed to push message:", request);
            }
        }
    }

    pushMessage(request) {
        if (this.mode !== 'polling' || this.manualClose) return;
        fetch(this.endpointPath() + "/polling/push", {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(request)
        }).then(res => res.json())
        .then(res => {
            if (res.success) {
                console.log("message pushed:", request, res.data);
            } else {
                if (res.error && res.error.code === 'session_not_found') {
                    console.warn(this.node.id, "Session lost (not found)");
                } else {
                    console.error("message processing failed:", request, res.error.message);
                }
            }
        })
        .catch(err => console.error("message pushing failed:", err));
    }

    /**
     * Sends a heartbeat ping to the server.
     * @private
     */
    sendPing() {
        if (this.heartbeatTimer) {
            clearTimeout(this.heartbeatTimer);
        }
        this.heartbeatTimer = setTimeout(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.socket.send(JSON.stringify({ header: "ping" }));
            }
        }, this.options.heartbeatInterval);
    }

    /**
     * Retries the connection with exponential backoff.
     * @private
     */
    reconnect() {
        if (this.manualClose || this.unloading) {
            return;
        }
        if (this.established) {
            this.established = false;
            if (this.options.onClose) {
                const closeEvent = { code: 1006, reason: "Connection lost", wasClean: false };
                setTimeout(() => {
                    if (this.manualClose || this.unloading) return;
                    try {
                        this.options.onClose(closeEvent);
                    } catch (e) {
                        console.error(this.node.id, "Error in onClose callback:", e);
                    }
                }, 100);
            }
        }

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        if (this.retryCount < this.options.maxRetries) {
            this.retryCount++;
            const jitter = Math.floor(Math.random() * 1000);
            const interval = (this.options.retryInterval * this.retryCount) + jitter;
            const status = "(" + this.retryCount + "/" + this.options.maxRetries + ", interval=" + interval + "ms)";
            console.log(this.node.id, "reconnect attempt", status);
            if (this.options.onRetry) {
                this.options.onRetry(this.retryCount, this.options.maxRetries, interval);
            }
            this.reconnectTimer = setTimeout(() => {
                this.reconnectTimer = null;
                if (this.manualClose) return;
                if (this.mode === 'websocket') {
                    this.openSocket();
                } else if (this.mode === 'polling') {
                    this.startPolling();
                }
            }, interval);
        } else {
            if (this.mode === 'websocket') {
                if (this.wsEverConnected) {
                    console.log(this.node.id, "max reconnect attempts reached for websocket; retrying...");
                    if (this.options.onFailed) {
                        this.options.onFailed(this.node);
                    }
                    this.reconnectTimer = setTimeout(() => {
                        this.reconnectTimer = null;
                        if (this.manualClose) return;
                        this.retryCount = Math.max(0, this.options.maxRetries - 2);
                        this.openSocket();
                    }, this.options.retryInterval);
                } else {
                    console.log(this.node.id, "abort reconnect attempt, switching to polling");
                    this.switchToPolling();
                }
            } else if (this.mode === 'polling') {
                console.log(this.node.id, "abort reconnect attempt, connection failed");
                this.stopPolling();
                if (this.options.onFailed) {
                    this.options.onFailed(this.node);
                }
            }
        }
    }

}

/**
 * Aspectow Console Notification & Web Push Client
 */
const NotificationClient = (function () {
    let vapidPublicKey = null;
    let currentSetting = null;
    let userPermissions = null;
    let swRegistration = null;
    let webPushEnabled = true;
    let unreadCount = 0;
    let hasNewNotification = false;
    let registeredEndpoints = null;

    function getContextPath() {
        if (typeof CONTEXT_PATH !== 'undefined' && CONTEXT_PATH !== null) {
            return CONTEXT_PATH;
        }
        if (typeof window.CONTEXT_PATH !== 'undefined' && window.CONTEXT_PATH !== null) {
            return window.CONTEXT_PATH;
        }
        return '';
    }

    function urlB64ToUint8Array(base64String) {
        const padding = '='.repeat((4 - base64String.length % 4) % 4);
        const base64 = (base64String + padding)
            .replace(/\-/g, '+')
            .replace(/_/g, '/');
        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);
        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }

    function updateBadge(count) {
        const newCount = typeof count === 'number' ? count : parseInt(count, 10) || 0;
        if (newCount > unreadCount) {
            hasNewNotification = true;
        }
        unreadCount = newCount;
        const badge = document.getElementById('notif-badge');
        if (!badge) return;
        if (unreadCount > 0) {
            badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
            badge.classList.remove('d-none');
        } else {
            badge.textContent = '0';
            badge.classList.add('d-none');
        }
    }

    function refreshUnreadCount() {
        const ctx = getContextPath();
        fetch(ctx + '/notification/count')
            .then(res => res.json())
            .then(res => {
                if (res && res.data && typeof res.data.unreadCount !== 'undefined') {
                    updateBadge(res.data.unreadCount);
                }
            })
            .catch(() => {});
    }

    function animateBell() {
        const bellIcon = document.querySelector('#notif-btn i, [data-bs-target="#notificationModal"] i');
        if (bellIcon) {
            bellIcon.classList.add('text-danger');
            setTimeout(() => bellIcon.classList.remove('text-danger'), 1500);
        }
    }

    async function init() {
        const notifBtn = document.getElementById('notif-btn');
        if (!notifBtn) {
            // Notification feature is not enabled or rendered on this page
            return;
        }

        if ('serviceWorker' in navigator) {
            try {
                swRegistration = await navigator.serviceWorker.ready;
            } catch (e) {
                console.warn('[NotificationClient] Service worker not ready:', e);
            }

            navigator.serviceWorker.addEventListener('message', (event) => {
                if (event.data && event.data.type === 'PUSH_NOTIFICATION_RECEIVED') {
                    console.log('[NotificationClient] Realtime push notification received:', event.data);
                    hasNewNotification = true;
                    refreshUnreadCount();
                    animateBell();
                    const modal = document.getElementById('notificationModal');
                    if (modal && modal.classList.contains('show')) {
                        loadHistory(true);
                    }
                }
            });
        }

        // Tab focus / visibility change
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                refreshUnreadCount();
            }
        });

        // Periodic background sync every 30 seconds
        //setInterval(refreshUnreadCount, 30000);

        loadConfig();
        bindEvents();
    }

    function loadConfig() {
        const ctx = getContextPath();
        return fetch(ctx + '/notification/config')
            .then(res => res.json())
            .then(res => {
                if (res && res.data) {
                    if (res.data.enabled === false) {
                        const notifBtn = document.getElementById('notif-btn');
                        if (notifBtn) notifBtn.classList.add('d-none');
                        return;
                    }
                    webPushEnabled = (res.data.webPushEnabled !== false);
                    vapidPublicKey = res.data.vapidPublicKey;
                    currentSetting = res.data.setting;
                    if (res.data.permissions && Array.isArray(res.data.permissions)) {
                        userPermissions = new Set(res.data.permissions);
                    }
                    if (res.data.registeredEndpoints && Array.isArray(res.data.registeredEndpoints)) {
                        registeredEndpoints = new Set(res.data.registeredEndpoints);
                    } else {
                        registeredEndpoints = new Set();
                    }
                    updateBadge(res.data.unreadCount || 0);
                    populateSettingsForm(currentSetting);
                }
            })
            .catch(err => {
                console.warn('[NotificationClient] Failed to load notification config:', err);
            });
    }

    function hasPermission(requiredPermission) {
        return (userPermissions && userPermissions.has(requiredPermission));
    }

    function populateSettingsForm(setting) {
        if (setting) {
            const cpuInput = document.getElementById('notif-cpu-threshold');
            const memInput = document.getElementById('notif-mem-threshold');
            const nodeChk = document.getElementById('notif-node-status');
            const schedChk = document.getElementById('notif-sched-fail');
            const buildChk = document.getElementById('notif-build-result');
            const secChk = document.getElementById('notif-security-alert');
            const cmdChk = document.getElementById('notif-cmd-finish');

            if (cpuInput) cpuInput.value = setting.cpuThreshold || 85;
            if (memInput) memInput.value = setting.memoryThreshold || 85;
            if (nodeChk) nodeChk.checked = setting.notifyNodeStatus === 'Y';
            if (schedChk) schedChk.checked = setting.notifySchedulerFail === 'Y';
            if (buildChk) buildChk.checked = setting.notifyBuildResult === 'Y';
            if (secChk) secChk.checked = setting.notifySecurityAlert === 'Y';
            if (cmdChk) cmdChk.checked = setting.notifyCommandFinish === 'Y';
        }

        applyPermissionFilters();
        checkPushSubscriptionState();
    }

    function applyPermissionFilters() {
        // Monitoring alert thresholds section
        const thresholdsSec = document.getElementById('notif-thresholds-sec');
        const canViewMonitoring = hasPermission('MONITOR_VIEW') || hasPermission('MONITOR_CONTROL');
        if (thresholdsSec) {
            if (canViewMonitoring) {
                thresholdsSec.classList.remove('d-none');
            } else {
                thresholdsSec.classList.add('d-none');
            }
        }

        // Notification event source items
        const eventItems = [
            { id: 'notif-item-node-status', chkId: 'notif-node-status', perm: 'MONITOR_VIEW' },
            { id: 'notif-item-sched-fail', chkId: 'notif-sched-fail', perm: 'NODE_MANAGE' },
            { id: 'notif-item-build-result', chkId: 'notif-build-result', perm: 'BUILD_VIEW' },
            { id: 'notif-item-security-alert', chkId: 'notif-security-alert', perm: 'USER_MANAGE' },
            { id: 'notif-item-cmd-finish', chkId: 'notif-cmd-finish', perm: 'COMMAND_EXECUTE' }
        ];

        let visibleCount = 0;
        eventItems.forEach(item => {
            const container = document.getElementById(item.id);
            const chk = document.getElementById(item.chkId);
            const allowed = hasPermission(item.perm);

            if (container) {
                if (allowed) {
                    container.classList.remove('d-none');
                    visibleCount++;
                } else {
                    container.classList.add('d-none');
                    if (chk) {
                        chk.checked = false;
                    }
                }
            }
        });

        // Notice when no event sources are available for this user's role
        const noEventsNotice = document.getElementById('notif-no-events-notice');
        if (noEventsNotice) {
            if (visibleCount === 0) {
                noEventsNotice.classList.remove('d-none');
            } else {
                noEventsNotice.classList.add('d-none');
            }
        }
    }

    async function checkPushSubscriptionState() {
        const toggle = document.getElementById('notif-push-toggle');
        const testBtn = document.getElementById('notif-test-btn');
        const permNotice = document.getElementById('notif-perm-notice');

        if (!webPushEnabled) {
            if (toggle) {
                toggle.checked = false;
                toggle.disabled = true;
            }
            if (testBtn) testBtn.disabled = true;
            if (permNotice) {
                permNotice.textContent = 'Web Push is disabled by corporate policy or firewall configuration. In-app notifications remain active.';
                permNotice.className = 'small text-warning mt-1';
                permNotice.classList.remove('d-none');
            }
            return;
        }

        if (!('Notification' in window) || !('PushManager' in window)) {
            if (toggle) toggle.disabled = true;
            if (permNotice) {
                permNotice.textContent = 'Web Push notifications are not supported by this browser.';
                permNotice.className = 'small text-danger mt-1';
                permNotice.classList.remove('d-none');
            }
            return;
        }

        if (Notification.permission === 'denied') {
            if (toggle) {
                toggle.checked = false;
                toggle.disabled = true;
            }
            if (testBtn) testBtn.disabled = true;
            if (permNotice) {
                permNotice.textContent = 'Browser notification permission is blocked. Please allow notifications in site settings.';
                permNotice.className = 'small text-danger mt-1';
                permNotice.classList.remove('d-none');
            }
            return;
        }

        if (swRegistration) {
            const sub = await swRegistration.pushManager.getSubscription();
            if (sub) {
                // Verify if this local subscription is still registered on the server for the current user
                if (registeredEndpoints !== null && !registeredEndpoints.has(sub.endpoint)) {
                    console.warn('[NotificationClient] Push subscription endpoint is not registered on server. Unsubscribing stale local subscription...');
                    try {
                        await sub.unsubscribe();
                    } catch (e) {
                        console.warn('[NotificationClient] Error unsubscribing stale subscription:', e);
                    }
                    if (toggle) toggle.checked = false;
                    if (testBtn) testBtn.disabled = true;
                    if (permNotice) permNotice.classList.add('d-none');
                    return;
                }

                // Verify if existing subscription matches current server VAPID public key
                if (vapidPublicKey && sub.options && sub.options.applicationServerKey) {
                    const currentKeyBytes = new Uint8Array(sub.options.applicationServerKey);
                    const expectedKeyBytes = urlB64ToUint8Array(vapidPublicKey);
                    let mismatch = (currentKeyBytes.length !== expectedKeyBytes.length);
                    if (!mismatch) {
                        for (let i = 0; i < currentKeyBytes.length; i++) {
                            if (currentKeyBytes[i] !== expectedKeyBytes[i]) {
                                mismatch = true;
                                break;
                            }
                        }
                    }
                    if (mismatch) {
                        console.warn('[NotificationClient] VAPID public key mismatch detected. Re-subscribing with updated key...');
                        try {
                            const oldEndpoint = sub.endpoint;
                            await sub.unsubscribe();
                            if (oldEndpoint) {
                                const formData = new URLSearchParams();
                                formData.append('endpoint', oldEndpoint);
                                const ctx = getContextPath();
                                await fetch(ctx + '/notification/unsubscribe', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                    body: formData
                                }).catch(() => {});
                            }
                            await handlePushToggle(true);
                            return;
                        } catch (reSubErr) {
                            console.error('[NotificationClient] Failed to re-subscribe after VAPID key mismatch:', reSubErr);
                        }
                    }
                }

                if (toggle) toggle.checked = true;
                if (testBtn) testBtn.disabled = false;
                if (permNotice) {
                    permNotice.textContent = 'Web Push notifications are active on this device.';
                    permNotice.className = 'small text-success mt-1';
                    permNotice.classList.remove('d-none');
                }
            } else {
                if (toggle) toggle.checked = false;
                if (testBtn) testBtn.disabled = true;
                if (permNotice) permNotice.classList.add('d-none');
            }
        }
    }

    async function handlePushToggle(enabled, silent = false) {
        if (!webPushEnabled) {
            if (!silent) alert('Web Push is disabled by corporate policy or firewall configuration.');
            checkPushSubscriptionState();
            return;
        }

        const testBtn = document.getElementById('notif-test-btn');
        const permNotice = document.getElementById('notif-perm-notice');

        if (!swRegistration) {
            swRegistration = await navigator.serviceWorker.ready;
        }

        if (enabled) {
            let permission = Notification.permission;
            if (permission !== 'granted') {
                if (silent) {
                    return;
                }
                permission = await Notification.requestPermission();
                if (permission !== 'granted') {
                    checkPushSubscriptionState();
                    return;
                }
            }

            if (!vapidPublicKey) {
                if (!silent) alert('Failed to retrieve VAPID Public Key.');
                return;
            }

            try {
                const sub = await swRegistration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlB64ToUint8Array(vapidPublicKey)
                });

                const rawKey = sub.getKey ? sub.getKey('p256dh') : null;
                const rawAuth = sub.getKey ? sub.getKey('auth') : null;
                const p256dhKey = rawKey ? btoa(String.fromCharCode.apply(null, new Uint8Array(rawKey))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '') : '';
                const authKey = rawAuth ? btoa(String.fromCharCode.apply(null, new Uint8Array(rawAuth))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '') : '';

                const formData = new URLSearchParams();
                formData.append('endpoint', sub.endpoint);
                formData.append('p256dhKey', p256dhKey);
                formData.append('authKey', authKey);
                formData.append('userAgent', navigator.userAgent);

                const ctx = getContextPath();
                await fetch(ctx + '/notification/subscribe', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: formData
                });

                if (registeredEndpoints) {
                    registeredEndpoints.add(sub.endpoint);
                }

                const toggle = document.getElementById('notif-push-toggle');
                if (toggle) toggle.checked = true;
                if (currentSetting) currentSetting.pushEnabled = 'Y';
                if (testBtn) testBtn.disabled = false;
                if (permNotice) {
                    permNotice.textContent = 'Web Push notifications have been activated on this device.';
                    permNotice.className = 'small text-success mt-1';
                    permNotice.classList.remove('d-none');
                }
            } catch (err) {
                console.error('[NotificationClient] Failed to subscribe to push:', err);
                if (!silent) alert('Error subscribing to push notifications: ' + err.message);
                checkPushSubscriptionState();
            }
        } else {
            try {
                const sub = await swRegistration.pushManager.getSubscription();
                if (sub) {
                    const endpoint = sub.endpoint;
                    await sub.unsubscribe();

                    if (registeredEndpoints && endpoint) {
                        registeredEndpoints.delete(endpoint);
                    }

                    const formData = new URLSearchParams();
                    formData.append('endpoint', endpoint);
                    const ctx = getContextPath();
                    await fetch(ctx + '/notification/unsubscribe', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: formData
                    });
                }
                const toggle = document.getElementById('notif-push-toggle');
                if (toggle) toggle.checked = false;
                if (currentSetting) currentSetting.pushEnabled = 'N';
                if (testBtn) testBtn.disabled = true;
                if (permNotice) permNotice.classList.add('d-none');
            } catch (err) {
                console.error('[NotificationClient] Failed to unsubscribe from push:', err);
            }
        }
    }

    function saveSettings() {
        const form = document.getElementById('notif-settings-form');
        if (!form) return;

        const formData = new URLSearchParams();
        formData.append('pushEnabled', document.getElementById('notif-push-toggle').checked ? 'Y' : 'N');
        formData.append('cpuThreshold', document.getElementById('notif-cpu-threshold').value);
        formData.append('memoryThreshold', document.getElementById('notif-mem-threshold').value);
        formData.append('notifyNodeStatus', document.getElementById('notif-node-status').checked ? 'Y' : 'N');
        formData.append('notifySchedulerFail', document.getElementById('notif-sched-fail').checked ? 'Y' : 'N');
        formData.append('notifyBuildResult', document.getElementById('notif-build-result').checked ? 'Y' : 'N');
        formData.append('notifySecurityAlert', document.getElementById('notif-security-alert').checked ? 'Y' : 'N');
        formData.append('notifyCommandFinish', document.getElementById('notif-cmd-finish').checked ? 'Y' : 'N');

        const ctx = getContextPath();
        fetch(ctx + '/notification/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        })
        .then(res => res.json())
        .then(res => {
            const saveBtn = document.getElementById('notif-save-btn');
            if (saveBtn) {
                const originHtml = saveBtn.innerHTML;
                saveBtn.innerHTML = '<i class="bi bi-check-lg"></i> Saved';
                saveBtn.classList.replace('btn-primary', 'btn-success');
                setTimeout(() => {
                    saveBtn.innerHTML = originHtml;
                    saveBtn.classList.replace('btn-success', 'btn-primary');
                }, 2000);
            }
        })
        .catch(err => {
            alert('Failed to save settings: ' + err.message);
        });
    }

    function loadHistory(silent = false) {
        const listContainer = document.getElementById('notif-history-list');
        const emptyNotice = document.getElementById('notif-history-empty');
        if (!listContainer) return Promise.resolve();

        const hasExistingItems = listContainer.children.length > 0 && !listContainer.querySelector('.spinner-border');
        if (!silent && !hasExistingItems) {
            listContainer.innerHTML = '<div class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm me-2"></div>Loading notifications...</div>';
            if (emptyNotice) emptyNotice.classList.add('d-none');
        }

        const ctx = getContextPath();
        return fetch(ctx + '/notification/history?limit=40')
            .then(res => res.json())
            .then(res => {
                const list = (res && res.data && res.data.history) || [];
                const latestUnreadCount = (res && res.data) ? res.data.unreadCount : 0;
                updateBadge(latestUnreadCount);

                if (list.length === 0) {
                    listContainer.innerHTML = '';
                    listContainer.classList.add('d-none');
                    if (emptyNotice) emptyNotice.classList.remove('d-none');
                    return;
                }

                listContainer.classList.remove('d-none');
                if (emptyNotice) emptyNotice.classList.add('d-none');

                const fragment = document.createDocumentFragment();
                list.forEach(item => {
                    const card = createNotificationCard(item);
                    fragment.appendChild(card);
                });
                if (typeof listContainer.replaceChildren === 'function') {
                    listContainer.replaceChildren(fragment);
                } else {
                    listContainer.innerHTML = '';
                    listContainer.appendChild(fragment);
                }
            })
            .catch(err => {
                if (!hasExistingItems) {
                    listContainer.innerHTML = '<div class="text-center py-4 text-danger">Failed to load notifications.</div>';
                }
            });
    }

    function createNotificationCard(item) {
        const isUnread = (item.readYn === 'N');
        const div = document.createElement('div');
        div.className = 'list-group-item list-group-item-action p-3 border-bottom rounded-0 notif-item ' + (isUnread ? 'list-group-item-primary' : '');
        div.style.cursor = 'pointer';

        let iconHtml = '<i class="bi bi-info-circle-fill text-primary fs-5"></i>';
        if (item.severity === 'CRITICAL') {
            iconHtml = '<i class="bi bi-exclamation-octagon-fill text-danger fs-5"></i>';
        } else if (item.severity === 'WARN') {
            iconHtml = '<i class="bi bi-exclamation-triangle-fill text-warning fs-5"></i>';
        } else if (item.severity === 'RESOLVED') {
            iconHtml = '<i class="bi bi-check-circle-fill text-success fs-5"></i>';
        }

        const timeInfo = (typeof getRelativeTimeInfo === 'function')
            ? getRelativeTimeInfo(item.createdAt)
            : {
                relativeTime: (typeof formatRelativeTime === 'function' ? formatRelativeTime(item.createdAt) : (typeof formatDateTime === 'function' ? formatDateTime(item.createdAt) : item.createdAt)),
                fullTime: (typeof formatDateTime === 'function' ? formatDateTime(item.createdAt) : item.createdAt)
            };
        const relativeTime = timeInfo.relativeTime || '-';
        const fullTime = timeInfo.fullTime || timeInfo.standardTime || relativeTime;

        div.innerHTML = `
            <div class="d-flex align-items-start gap-3">
                <div class="flex-shrink-0 mt-1">${iconHtml}</div>
                <div class="flex-grow-1 overflow-hidden">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <div class="d-flex align-items-center gap-1">
                            <span class="badge bg-secondary-subtle text-secondary-emphasis small">${escapeHtml(item.category)}</span>
                            ${isUnread ? '<span class="badge rounded-pill bg-primary" style="font-size: 0.6rem;">NEW</span>' : ''}
                        </div>
                        <div class="d-flex align-items-center gap-1">
                            <small class="text-muted" style="font-size: 0.75rem;" title="${escapeHtml(fullTime)}">${escapeHtml(relativeTime)}</small>
                            ${isUnread ? '<span class="bg-primary rounded-circle ms-1" style="width: 7px; height: 7px; display: inline-block;" title="Unread"></span>' : ''}
                        </div>
                    </div>
                    <div class="text-truncate mb-1 ${isUnread ? 'fw-bold text-body' : 'text-body-secondary'}" style="font-size: 0.9rem;">${escapeHtml(item.title)}</div>
                    <div class="small ${isUnread ? 'text-body-secondary' : 'text-muted'} text-break" style="font-size: 0.8rem; line-height: 1.4;">${escapeHtml(item.message)}</div>
                </div>
            </div>
        `;

        div.addEventListener('click', () => {
            markRead(item.notificationId, item.targetUrl);
        });

        return div;
    }

    function resolveTargetUrl(targetUrl) {
        if (!targetUrl) return null;
        if (targetUrl.startsWith('http://') || targetUrl.startsWith('https://')) {
            return targetUrl;
        }
        const ctx = getContextPath();
        const cleanTarget = targetUrl.replace(/^\.?\//, '');
        if (!cleanTarget) {
            return ctx ? ctx + '/' : '/';
        }
        return (ctx ? ctx + '/' : '/') + cleanTarget;
    }

    function markRead(notificationId, targetUrl) {
        const formData = new URLSearchParams();
        if (notificationId) {
            formData.append('notificationId', notificationId);
        }

        const ctx = getContextPath();
        return fetch(ctx + '/notification/read', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        })
        .then(res => res.json())
        .then(res => {
            if (res && res.data) {
                updateBadge(res.data.unreadCount || 0);
                if ((res.data.unreadCount || 0) === 0) {
                    hasNewNotification = false;
                }
            }
            if (targetUrl) {
                const resolved = resolveTargetUrl(targetUrl);
                if (resolved) {
                    location.href = resolved;
                    return;
                }
            }
            if (notificationId) {
                loadHistory(true);
            }
        });
    }

    function markAllRead() {
        hasNewNotification = false;
        // Optimistic UI update: instantly clear unread styling without flickering
        const listContainer = document.getElementById('notif-history-list');
        if (listContainer) {
            listContainer.querySelectorAll('.notif-item').forEach(card => {
                card.classList.remove('bg-primary-subtle', 'bg-opacity-10');
                const title = card.querySelector('.fw-bold');
                if (title) {
                    title.classList.remove('fw-bold', 'text-body');
                    title.classList.add('text-body-secondary');
                }
                const msg = card.querySelector('.small.text-body-secondary');
                if (msg) {
                    msg.classList.remove('text-body-secondary');
                    msg.classList.add('text-muted');
                }
                const newBadge = card.querySelector('.badge.bg-primary');
                if (newBadge) newBadge.remove();
                const unreadDot = card.querySelector('.bg-primary.rounded-circle');
                if (unreadDot) unreadDot.remove();
            });
        }
        updateBadge(0);

        markRead(null, null)
            .then(() => {
                loadHistory(true);
            })
            .catch(() => {});
    }

    function clearHistory() {
        if (!confirm('Are you sure you want to clear all notification history?')) return;

        const ctx = getContextPath();
        fetch(ctx + '/notification/clear', { method: 'POST' })
            .then(() => {
                hasNewNotification = false;
                updateBadge(0);
                const listContainer = document.getElementById('notif-history-list');
                const emptyNotice = document.getElementById('notif-history-empty');
                if (listContainer) listContainer.innerHTML = '';
                listContainer.classList.add('d-none');
                if (emptyNotice) emptyNotice.classList.remove('d-none');
            })
            .catch(err => {
                alert('Failed to clear notification history: ' + err.message);
            });
    }

    function sendTestNotification() {
        const testBtn = document.getElementById('notif-test-btn');
        if (testBtn) {
            testBtn.disabled = true;
            testBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Sending...';
        }

        const ctx = getContextPath();
        fetch(ctx + '/notification/test', { method: 'POST' })
            .then(res => res.json())
            .then(res => {
                const count = res && res.data ? res.data.sentCount : 0;
                alert(`Test push notification sent (notified devices: ${count}).\nPlease check your browser/system notification tray.`);
                loadHistory();
            })
            .catch(err => {
                alert('Error sending test notification: ' + err.message);
            })
            .finally(() => {
                if (testBtn) {
                    testBtn.disabled = false;
                    testBtn.innerHTML = '<i class="bi bi-send me-1"></i> Send Test Notification';
                }
            });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function activateHistoryTab() {
        const historyTabBtn = document.getElementById('notif-history-tab');
        if (!historyTabBtn) return;
        if (typeof bootstrap !== 'undefined' && bootstrap.Tab) {
            const tabInstance = bootstrap.Tab.getOrCreateInstance(historyTabBtn);
            tabInstance.show();
        } else {
            historyTabBtn.click();
        }
    }

    function bindEvents() {
        const modal = document.getElementById('notificationModal');
        if (modal) {
            modal.addEventListener('show.bs.modal', () => {
                if (hasNewNotification || unreadCount > 0) {
                    activateHistoryTab();
                    hasNewNotification = false;
                }
                loadHistory();
                loadConfig().then(() => checkPushSubscriptionState());
            });
        }

        const pushToggle = document.getElementById('notif-push-toggle');
        if (pushToggle) {
            pushToggle.addEventListener('change', (e) => {
                handlePushToggle(e.target.checked);
            });
        }

        const saveBtn = document.getElementById('notif-save-btn');
        if (saveBtn) {
            saveBtn.addEventListener('click', saveSettings);
        }

        const markAllBtn = document.getElementById('notif-mark-all-read');
        if (markAllBtn) {
            markAllBtn.addEventListener('click', markAllRead);
        }

        const clearBtn = document.getElementById('notif-clear-history');
        if (clearBtn) {
            clearBtn.addEventListener('click', clearHistory);
        }

        const testBtn = document.getElementById('notif-test-btn');
        if (testBtn) {
            testBtn.addEventListener('click', sendTestNotification);
        }
    }

    function removeRegisteredEndpoint(endpoint) {
        if (registeredEndpoints && endpoint) {
            registeredEndpoints.delete(endpoint);
        }
    }

    return {
        init: init,
        loadHistory: loadHistory,
        markAllRead: markAllRead,
        clearHistory: clearHistory,
        sendTestNotification: sendTestNotification,
        activateHistoryTab: activateHistoryTab,
        checkPushSubscriptionState: checkPushSubscriptionState,
        removeRegisteredEndpoint: removeRegisteredEndpoint
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    NotificationClient.init();
});
