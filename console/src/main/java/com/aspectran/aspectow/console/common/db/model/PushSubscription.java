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
import java.time.Instant;

/**
 * Entity representing a Web Push subscription endpoint and encryption keys for a device.
 *
 * <p>Created: 2026-11-17</p>
 */
public class PushSubscription implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long subscriptionId;
    private Long userId;
    private String endpoint;
    private String p256dhKey;
    private String authKey;
    private String userAgent;
    private Instant createdAt;
    private Instant lastActiveAt;
    private String username;
    private String nickname;

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getP256dhKey() {
        return p256dhKey;
    }

    public void setP256dhKey(String p256dhKey) {
        this.p256dhKey = p256dhKey;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("subscriptionId", subscriptionId);
        tsb.append("userId", userId);
        tsb.append("username", username);
        tsb.append("nickname", nickname);
        tsb.append("endpoint", endpoint);
        tsb.append("p256dhKey", p256dhKey);
        tsb.append("authKey", authKey);
        tsb.append("userAgent", userAgent);
        tsb.append("createdAt", createdAt);
        tsb.append("lastActiveAt", lastActiveAt);
        return tsb.toString();
    }

}
