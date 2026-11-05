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
 * Formats a date value (ISO string, epochSecond object, timestamp number, or Date)
 * into the client's local time zone.
 *
 * @param {*} dateVal - The date value to format.
 * @param {string} [format='YYYY-MM-DD HH:mm:ss'] - The Day.js format string.
 * @returns {string} The formatted local date/time string, or '-' if invalid/empty.
 */
function formatDateTime(dateVal, format = 'YYYY-MM-DD HH:mm:ss') {
    if (!dateVal) return '-';
    if (typeof dayjs === 'undefined') return String(dateVal);

    let d;
    if (typeof dateVal === 'object' && dateVal !== null && dateVal.epochSecond !== undefined) {
        d = dayjs.unix(dateVal.epochSecond);
    } else if (typeof dateVal === 'number') {
        d = dayjs(dateVal);
    } else if (typeof dateVal === 'string') {
        d = dayjs.utc ? dayjs.utc(dateVal).local() : dayjs(dateVal);
    } else {
        d = dayjs(dateVal);
    }
    return d.isValid() ? d.format(format) : String(dateVal);
}

/**
 * Scans elements with the '.format-local-time' class within a container
 * and converts their 'data-utc' attribute value to the client's local time.
 *
 * @param {Element|jQuery|string} [container=document] - The root container to scan.
 */
function formatLocalTime(container) {
    if (typeof dayjs === 'undefined') return;

    if (typeof jQuery !== 'undefined') {
        const $targets = container ? $(container).find('.format-local-time') : $('.format-local-time');
        $targets.each(function() {
            const utc = $(this).data('utc');
            const fmt = $(this).data('format') || 'YYYY-MM-DD HH:mm:ss';
            if (utc) {
                const formatted = formatDateTime(utc, fmt);
                if (formatted !== '-') {
                    $(this).text(formatted);
                }
            }
        });
    } else {
        const root = (typeof container === 'string' ? document.querySelector(container) : container) || document;
        const targets = root.querySelectorAll('.format-local-time');
        targets.forEach(el => {
            const utc = el.getAttribute('data-utc');
            const fmt = el.getAttribute('data-format') || 'YYYY-MM-DD HH:mm:ss';
            if (utc) {
                const formatted = formatDateTime(utc, fmt);
                if (formatted !== '-') {
                    el.textContent = formatted;
                }
            }
        });
    }
}

// Automatically format local times once DOM is ready
if (typeof jQuery !== 'undefined') {
    $(function() {
        formatLocalTime();
    });
} else if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => formatLocalTime());
} else {
    formatLocalTime();
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
 * @last-modified 2026-09-05
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
        this.activityPath = null;
        this.primaryNodeId = node.id;
        this.mode = 'websocket'; // 'websocket' or 'polling'
        this.wsEverConnected = false;
    }

    /**
     * Connects to the server using the provided node endpoint.
     * @param {string} activityPath - The activity-specific path (e.g., 'nodes', 'commands', 'scheduler')
     */
    start(activityPath) {
        this.activityPath = activityPath;
        this.manualClose = false;
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
     * Opens a new connection.
     */
    openSocket() {
        if (this.manualClose) {
            return;
        }
        if (this.options.onBeforeConnect) {
            Promise.resolve(this.options.onBeforeConnect(this.node)).then((token) => {
                if (this.manualClose) {
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
                    if (!this.manualClose) {
                        this.reconnect();
                    }
                }
            }).catch((err) => {
                if (!this.manualClose) {
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
            this.established = false;
            if (!afterClosing) {
                try {
                    this.socket.close(1000, "Normal closure");
                } catch (e) {
                    // Ignore if already closed
                }
            }
            this.socket = null;
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
        if (this.manualClose) {
            return;
        }
        if (this.node.endpoint && this.node.endpoint.mode === 'polling') {
            this.switchToPolling();
            return;
        }
        if (!this.node.endpoint || !this.node.endpoint.token) {
            console.warn(this.node.id, "cannot connect to websocket without a valid token");
            if (!this.manualClose) {
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
                if (!this.manualClose) {
                    if (this.options.onClose) {
                        setTimeout(() => this.options.onClose(event), 100);
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
        if (this.mode !== 'polling' || this.manualClose) return;

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
        if (this.manualClose) {
            return;
        }
        if (this.established) {
            this.established = false;
            if (this.options.onClose) {
                const closeEvent = { code: 1006, reason: "Connection lost", wasClean: false };
                setTimeout(() => {
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
