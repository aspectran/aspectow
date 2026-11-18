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
package com.aspectran.aspectow.console.notification;

import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.console.common.db.model.NotificationHistory;
import com.aspectran.aspectow.console.common.db.model.PushSubscription;
import com.aspectran.aspectow.console.common.db.model.UserNotificationSetting;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing user notifications, settings, and Web Push subscriptions.
 *
 * <p>Created: 2026-11-17</p>
 */
@Component("/notification")
@Profile("[console.ui, console.custom-ui]")
public class NotificationActivity {

    private final NotificationService notificationService;
    private final WebPushManager webPushManager;

    @Autowired(required = false)
    public NotificationActivity(NotificationService notificationService, WebPushManager webPushManager) {
        this.notificationService = notificationService;
        this.webPushManager = webPushManager;
    }

    private boolean isNotificationEnabled() {
        return notificationService != null && notificationService.isNotificationEnabled();
    }

    private boolean isWebPushEnabled() {
        return isNotificationEnabled() && notificationService.isWebPushEnabled();
    }

    private UserInfo getCurrentUser(@NonNull Translet translet) {
        return translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
    }

    /**
     * Retrieves VAPID public key and current user's notification preferences.
     */
    @Request("/config")
    public RestResponse getConfig(@NonNull Translet translet) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }

        if (!isNotificationEnabled()) {
            Map<String, Object> data = new HashMap<>();
            data.put("enabled", false);
            data.put("webPushEnabled", false);
            return new SuccessResponse(data).ok();
        }

        UserNotificationSetting setting = notificationService.getNotificationSetting(userInfo.getUserId());
        long unreadCount = notificationService.getUnreadNotificationCount(userInfo.getUserId());
        boolean webPushEnabled = isWebPushEnabled();
        String vapidPublicKey = (webPushEnabled && webPushManager != null) ? webPushManager.getVapidPublicKey() : null;

        List<PushSubscription> userSubscriptions = notificationService.getPushSubscriptionsByUserId(userInfo.getUserId());
        List<String> registeredEndpoints = new ArrayList<>();
        if (userSubscriptions != null) {
            for (PushSubscription sub : userSubscriptions) {
                registeredEndpoints.add(sub.getEndpoint());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("enabled", true);
        data.put("webPushEnabled", webPushEnabled);
        data.put("vapidPublicKey", vapidPublicKey);
        data.put("setting", setting);
        data.put("unreadCount", unreadCount);
        data.put("registeredEndpoints", registeredEndpoints);
        data.put("permissions", userInfo.getPermissions());
        data.put("roles", userInfo.getRoles());

        return new SuccessResponse(data).ok();
    }

    /**
     * Saves user notification preferences and thresholds.
     */
    @RequestToPost("/settings")
    public RestResponse saveSettings(@NonNull Translet translet,
                                     String pushEnabled,
                                     Integer cpuThreshold,
                                     Integer memoryThreshold,
                                     String notifyNodeStatus,
                                     String notifySchedulerFail,
                                     String notifyBuildResult,
                                     String notifySecurityAlert,
                                     String notifyCommandFinish) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled()) {
            return new FailureResponse("Notification service is disabled").forbidden();
        }

        UserNotificationSetting setting = notificationService.getNotificationSetting(userInfo.getUserId());
        if (setting == null) {
            setting = new UserNotificationSetting();
            setting.setUserId(userInfo.getUserId());
        }
        if (pushEnabled != null) setting.setPushEnabled(pushEnabled);
        if (cpuThreshold != null && cpuThreshold >= 10 && cpuThreshold <= 100) setting.setCpuThreshold(cpuThreshold);
        if (memoryThreshold != null && memoryThreshold >= 10 && memoryThreshold <= 100) setting.setMemoryThreshold(memoryThreshold);
        if (notifyNodeStatus != null) setting.setNotifyNodeStatus(notifyNodeStatus);
        if (notifySchedulerFail != null) setting.setNotifySchedulerFail(notifySchedulerFail);
        if (notifyBuildResult != null) setting.setNotifyBuildResult(notifyBuildResult);
        if (notifySecurityAlert != null) setting.setNotifySecurityAlert(notifySecurityAlert);
        if (notifyCommandFinish != null) setting.setNotifyCommandFinish(notifyCommandFinish);

        notificationService.saveNotificationSetting(setting);
        return new SuccessResponse(setting).ok();
    }

    /**
     * Registers a Web Push subscription endpoint for the current user's browser.
     */
    @RequestToPost("/subscribe")
    public RestResponse subscribe(@NonNull Translet translet,
                                  String endpoint,
                                  String p256dhKey,
                                  String authKey,
                                  String userAgent) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled() || !isWebPushEnabled()) {
            return new FailureResponse("Web Push is disabled").forbidden();
        }

        if (StringUtils.isEmpty(endpoint) || StringUtils.isEmpty(p256dhKey) || StringUtils.isEmpty(authKey)) {
            return new FailureResponse().setError("required", "Endpoint, p256dhKey, and authKey are required");
        }

        if (userAgent == null) {
            userAgent = translet.getRequestAdapter().getHeader("User-Agent");
        }

        notificationService.registerPushSubscription(userInfo.getUserId(), endpoint, p256dhKey, authKey, userAgent);
        return new SuccessResponse().ok();
    }

    /**
     * Unregisters a Web Push subscription endpoint.
     */
    @RequestToPost("/unsubscribe")
    public RestResponse unsubscribe(@NonNull Translet translet, String endpoint) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled()) {
            return new FailureResponse("Notification service is disabled").forbidden();
        }

        if (!StringUtils.isEmpty(endpoint)) {
            notificationService.unregisterPushSubscription(endpoint);
        }
        return new SuccessResponse().ok();
    }

    /**
     * Retrieves recent in-app notification history.
     */
    @Request("/history")
    public RestResponse getHistory(@NonNull Translet translet, Integer limit) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled()) {
            return new FailureResponse("Notification service is disabled").forbidden();
        }

        int fetchLimit = (limit != null && limit > 0) ? limit : 30;
        List<NotificationHistory> historyList = notificationService.getNotificationHistory(userInfo.getUserId(), fetchLimit);
        long unreadCount = notificationService.getUnreadNotificationCount(userInfo.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("history", historyList);
        data.put("unreadCount", unreadCount);

        return new SuccessResponse(data).ok();
    }

    /**
     * Retrieves unread notification count for the current user.
     */
    @Request("/count")
    public RestResponse getUnreadCount(@NonNull Translet translet) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled()) {
            return new FailureResponse("Notification service is disabled").forbidden();
        }

        long unreadCount = notificationService.getUnreadNotificationCount(userInfo.getUserId());
        return new SuccessResponse(Map.of("unreadCount", unreadCount)).ok();
    }

    /**
     * Marks a notification as read (or all notifications if notificationId is null).
     */
    @RequestToPost("/read")
    public RestResponse markRead(@NonNull Translet translet, Long notificationId) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled()) {
            return new FailureResponse("Notification service is disabled").forbidden();
        }

        if (notificationId != null) {
            notificationService.markAsRead(notificationId, userInfo.getUserId());
        } else {
            notificationService.markAllAsRead(userInfo.getUserId());
        }

        long unreadCount = notificationService.getUnreadNotificationCount(userInfo.getUserId());
        return new SuccessResponse(Map.of("unreadCount", unreadCount)).ok();
    }

    /**
     * Clears all notification history records for the current user.
     */
    @RequestToPost("/clear")
    public RestResponse clearHistory(@NonNull Translet translet) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled()) {
            return new FailureResponse("Notification service is disabled").forbidden();
        }

        notificationService.clearNotificationHistory(userInfo.getUserId());
        return new SuccessResponse().ok();
    }

    /**
     * Sends a test Web Push notification to current user's registered devices.
     */
    @RequestToPost("/test")
    public RestResponse sendTestNotification(@NonNull Translet translet) {
        UserInfo userInfo = getCurrentUser(translet);
        if (userInfo == null) {
            return new FailureResponse().unauthorized();
        }
        if (!isNotificationEnabled()) {
            return new FailureResponse("Notification service is disabled").forbidden();
        }

        int sentCount = notificationService.sendTestNotification(userInfo.getUserId());
        return new SuccessResponse(Map.of("sentCount", sentCount)).ok();
    }

}
