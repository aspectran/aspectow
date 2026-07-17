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
package com.aspectran.aspectow.console.common.db.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a login history record.
 */
public class LoginHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = -2429813955681652496L;

    private Long historyId;
    private String username;
    private LocalDateTime loginAt;
    private String ipAddress;
    private String userAgent;
    private String successYn;

    /**
     * Gets the login history ID.
     * @return the history ID
     */
    public Long getHistoryId() {
        return historyId;
    }

    /**
     * Sets the login history ID.
     * @param historyId the history ID
     */
    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    /**
     * Gets the username.
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the login timestamp.
     * @return the login timestamp
     */
    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    /**
     * Sets the login timestamp.
     * @param loginAt the login timestamp
     */
    public void setLoginAt(LocalDateTime loginAt) {
        this.loginAt = loginAt;
    }

    /**
     * Gets the IP address.
     * @return the IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address.
     * @param ipAddress the IP address
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the User-Agent string.
     * @return the User-Agent string
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the User-Agent string.
     * @param userAgent the User-Agent string
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets the success indicator flag ("Y" or "N").
     * @return the success indicator flag
     */
    public String getSuccessYn() {
        return successYn;
    }

    /**
     * Sets the success indicator flag ("Y" or "N").
     * @param successYn the success indicator flag
     */
    public void setSuccessYn(String successYn) {
        this.successYn = successYn;
    }

}
