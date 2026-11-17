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
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    @Override
    public String toString() {
        return "PushSubscription{" +
                "subscriptionId=" + subscriptionId +
                ", userId=" + userId +
                ", endpoint='" + endpoint + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", lastActiveAt=" + lastActiveAt +
                '}';
    }

}
