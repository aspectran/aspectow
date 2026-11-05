/*
 * APON.js - A JavaScript library for APON (Aspectran Parameters Object Notation)
 *
 * Copyright (c) 2024-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
