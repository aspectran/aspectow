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
 * Entity representing user notification preferences and monitoring thresholds.
 *
 * <p>Created: 2026-11-17</p>
 */
public class UserNotificationSetting implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String pushEnabled = "Y";
    private int cpuThreshold = 85;
    private int memoryThreshold = 85;
    private String notifyNodeStatus = "Y";
    private String notifySchedulerFail = "Y";
    private String notifyBuildResult = "Y";
    private String notifySecurityAlert = "Y";
    private String notifyCommandFinish = "Y";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(String pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isPushAlertEnabled() {
        return "Y".equalsIgnoreCase(pushEnabled);
    }

    public int getCpuThreshold() {
        return cpuThreshold;
    }

    public void setCpuThreshold(int cpuThreshold) {
        this.cpuThreshold = cpuThreshold;
    }

    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    public void setMemoryThreshold(int memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }

    public String getNotifyNodeStatus() {
        return notifyNodeStatus;
    }

    public void setNotifyNodeStatus(String notifyNodeStatus) {
        this.notifyNodeStatus = notifyNodeStatus;
    }

    public boolean isNotifyNodeStatusEnabled() {
        return "Y".equalsIgnoreCase(notifyNodeStatus);
    }

    public String getNotifySchedulerFail() {
        return notifySchedulerFail;
    }

    public void setNotifySchedulerFail(String notifySchedulerFail) {
        this.notifySchedulerFail = notifySchedulerFail;
    }

    public boolean isNotifySchedulerFailEnabled() {
        return "Y".equalsIgnoreCase(notifySchedulerFail);
    }

    public String getNotifyBuildResult() {
        return notifyBuildResult;
    }

    public void setNotifyBuildResult(String notifyBuildResult) {
        this.notifyBuildResult = notifyBuildResult;
    }

    public boolean isNotifyBuildResultEnabled() {
        return "Y".equalsIgnoreCase(notifyBuildResult);
    }

    public String getNotifySecurityAlert() {
        return notifySecurityAlert;
    }

    public void setNotifySecurityAlert(String notifySecurityAlert) {
        this.notifySecurityAlert = notifySecurityAlert;
    }

    public boolean isNotifySecurityAlertEnabled() {
        return "Y".equalsIgnoreCase(notifySecurityAlert);
    }

    public String getNotifyCommandFinish() {
        return notifyCommandFinish;
    }

    public void setNotifyCommandFinish(String notifyCommandFinish) {
        this.notifyCommandFinish = notifyCommandFinish;
    }

    public boolean isNotifyCommandFinishEnabled() {
        return "Y".equalsIgnoreCase(notifyCommandFinish);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserNotificationSetting{" +
                "userId=" + userId +
                ", pushEnabled='" + pushEnabled + '\'' +
                ", cpuThreshold=" + cpuThreshold +
                ", memoryThreshold=" + memoryThreshold +
                ", notifyNodeStatus='" + notifyNodeStatus + '\'' +
                ", notifySchedulerFail='" + notifySchedulerFail + '\'' +
                ", notifyBuildResult='" + notifyBuildResult + '\'' +
                ", notifySecurityAlert='" + notifySecurityAlert + '\'' +
                ", notifyCommandFinish='" + notifyCommandFinish + '\'' +
                '}';
    }

}
