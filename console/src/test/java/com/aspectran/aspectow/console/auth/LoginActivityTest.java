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
package com.aspectran.aspectow.console.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginActivityTest {

    @Test
    void testIsSafeLocalUrl() {
        // Safe relative paths
        assertTrue(LoginActivity.isSafeLocalUrl("/"));
        assertTrue(LoginActivity.isSafeLocalUrl("/cluster/"));
        assertTrue(LoginActivity.isSafeLocalUrl("/nodes/detail?nodeId=1"));
        assertTrue(LoginActivity.isSafeLocalUrl("/nodes/detail?time=12:30:00"));

        // Unsafe URLs - external schemes
        assertFalse(LoginActivity.isSafeLocalUrl("http://evil.com"));
        assertFalse(LoginActivity.isSafeLocalUrl("https://evil.com"));
        assertFalse(LoginActivity.isSafeLocalUrl("ftp://evil.com"));
        assertFalse(LoginActivity.isSafeLocalUrl("javascript:alert(1)"));
        assertFalse(LoginActivity.isSafeLocalUrl("data:text/html,<script>alert(1)</script>"));

        // Unsafe URLs - protocol-relative and backslash bypasses
        assertFalse(LoginActivity.isSafeLocalUrl("//evil.com"));
        assertFalse(LoginActivity.isSafeLocalUrl("/\\evil.com"));
        assertFalse(LoginActivity.isSafeLocalUrl("\\\\evil.com"));
        assertFalse(LoginActivity.isSafeLocalUrl("evil.com"));

        // Unsafe URLs - control characters and CRLF
        assertFalse(LoginActivity.isSafeLocalUrl("/cluster/\r\nSet-Cookie:malicious=true"));
        assertFalse(LoginActivity.isSafeLocalUrl("/cluster/%00"));
        assertFalse(LoginActivity.isSafeLocalUrl("/cluster/%0d%0a"));

        // Null and blank
        assertFalse(LoginActivity.isSafeLocalUrl(null));
        assertFalse(LoginActivity.isSafeLocalUrl(""));
    }

}
