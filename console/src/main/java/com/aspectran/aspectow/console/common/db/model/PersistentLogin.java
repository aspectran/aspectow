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

import com.aspectran.utils.ToStringBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a persistent login token for Remember-Me authentication.
 *
 * <p>Created: 2026-10-24</p>
 */
public class PersistentLogin implements Serializable {

    @Serial
    private static final long serialVersionUID = -8730371179551380402L;

    private String series;
    private String username;
    private String token;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime lastUsed;

    /**
     * Gets the series identifier.
     * @return the series
     */
    public String getSeries() {
        return series;
    }

    /**
     * Sets the series identifier.
     * @param series the series
     */
    public void setSeries(String series) {
        this.series = series;
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
     * Gets the current token.
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the current token.
     * @param token the token
     */
    public void setToken(String token) {
        this.token = token;
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
     * Gets the User-Agent header string.
     * @return the User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the User-Agent header string.
     * @param userAgent the User-Agent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets the last used timestamp.
     * @return the last used timestamp
     */
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    /**
     * Sets the last used timestamp.
     * @param lastUsed the last used timestamp
     */
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("series", series);
        tsb.append("username", username);
        tsb.append("token", token);
        tsb.append("ipAddress", ipAddress);
        tsb.append("userAgent", userAgent);
        tsb.append("lastUsed", lastUsed);
        return tsb.toString();
    }

}
