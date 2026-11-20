/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.console.admin;

import com.aspectran.aspectow.console.auth.UserInfo;
import com.aspectran.aspectow.console.common.db.model.PushSubscription;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.db.model.VapidKey;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.aspectow.console.notification.NotificationService;
import com.aspectran.aspectow.console.notification.WebPushManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for administrative monitoring and management of Web Push notifications,
 * VAPID infrastructure, device subscriptions, and test dispatches.
 *
 * <p>Created: 2026-11-18</p>
 */
@Component("/admin/notification")
@Profile("[console.ui, console.custom-ui]")
public class NotificationAdminActivity {

    private static final Logger logger = LoggerFactory.getLogger(NotificationAdminActivity.class);

    private final NotificationService notificationService;
    private final WebPushManager webPushManager;
    private final UserService userService;

    @Autowired(required = false)
    public NotificationAdminActivity(NotificationService notificationService,
                                     WebPushManager webPushManager,
                                     UserService userService) {
        this.notificationService = notificationService;
        this.webPushManager = webPushManager;
        this.userService = userService;
    }

    private UserInfo getCurrentUser(@NonNull Translet translet) {
        return translet.getSessionAdapter().getAttribute(UserInfo.USERINFO_KEY);
    }

    /**
     * Renders the Web Push and notification infrastructure management page.
     */
    @Request("/")
    @Dispatch("admin/notification")
    @Action("page")
    public Map<String, Object> view() {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Push & Notifications");
        model.put("group", "admin-menu");
        model.put("style", "admin-notification-page");

        boolean notificationEnabled = notificationService != null && notificationService.isNotificationEnabled();
        boolean webPushEnabled = notificationEnabled && notificationService.isWebPushEnabled();
        model.put("notificationEnabled", notificationEnabled);
        model.put("webPushEnabled", webPushEnabled);

        // VAPID Info
        if (webPushManager != null) {
            VapidKey keyEntity = webPushManager.getVapidKeyEntity();
            model.put("vapidPublicKey", webPushManager.getVapidPublicKey());
            model.put("vapidSubject", webPushManager.getVapidSubject());
            model.put("vapidKeyCreatedAt", keyEntity != null ? keyEntity.getCreatedAt() : null);
        }

        // Subscriptions
        List<PushSubscription> subscriptions = null;
        if (notificationService != null) {
            subscriptions = notificationService.getAllPushSubscriptionsWithUser();
        }
        model.put("subscriptions", subscriptions);
        model.put("totalSubscriptions", subscriptions != null ? subscriptions.size() : 0);

        // User list for test dispatch selection
        List<User> userList = null;
        if (userService != null) {
            userList = userService.getUserList(null);
            if (userList != null) {
                userList.forEach(u -> u.setPassword(null));
            }
        }
        model.put("userList", userList);

        return model;
    }

    /**
     * Sends an administrative test push notification to a specified user or all users.
     */
    @RequestToPost("/test-push")
    public RestResponse testPush(@NonNull Translet translet,
                                 Long targetUserId,
                                 String title,
                                 String message) {
        UserInfo currentUser = getCurrentUser(translet);
        if (currentUser == null) {
            return new FailureResponse().unauthorized();
        }

        if (notificationService == null || !notificationService.isWebPushEnabled()) {
            return new FailureResponse("SERVICE_DISABLED", "Web Push functionality is disabled in this environment.").badRequest();
        }

        try {
            int sentCount = notificationService.sendAdminTestPush(targetUserId, title, message);
            Map<String, Object> result = new HashMap<>();
            result.put("sentCount", sentCount);
            return new SuccessResponse(result).ok();
        } catch (Exception e) {
            logger.error("Failed to send admin test push", e);
            return new FailureResponse("DISPATCH_FAILED", "Failed to send test push: " + e.getMessage()).internalServerError();
        }
    }

    /**
     * Retrieves the current list of push subscriptions.
     */
    @RequestToGet("/subscriptions")
    public RestResponse getSubscriptions(@NonNull Translet translet) {
        UserInfo currentUser = getCurrentUser(translet);
        if (currentUser == null) {
            return new FailureResponse().unauthorized();
        }

        List<PushSubscription> subscriptions = null;
        if (notificationService != null) {
            subscriptions = notificationService.getAllPushSubscriptionsWithUser();
            if (subscriptions != null) {
                for (PushSubscription sub : subscriptions) {
                    sub.setP256dhKey(null);
                    sub.setAuthKey(null);
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptions", subscriptions != null ? subscriptions : Collections.emptyList());
        data.put("totalSubscriptions", subscriptions != null ? subscriptions.size() : 0);
        return new SuccessResponse(data).ok();
    }

    /**
     * Deletes a specific push subscription by ID.
     */
    @RequestToPost("/delete-subscription")
    public RestResponse deleteSubscription(@NonNull Translet translet, Long subscriptionId) {
        UserInfo currentUser = getCurrentUser(translet);
        if (currentUser == null) {
            return new FailureResponse().unauthorized();
        }
        if (currentUser.hasRole("DEMO")) {
            return new FailureResponse().forbidden();
        }
        if (subscriptionId == null) {
            return new FailureResponse("REQUIRED_FIELD", "Missing subscriptionId").badRequest();
        }

        if (notificationService != null) {
            notificationService.deletePushSubscription(subscriptionId);
        }
        return new SuccessResponse().ok();
    }

    /**
     * Sends an administrative test push notification to a single specific device subscription.
     */
    @RequestToPost("/test-device")
    public RestResponse testDevice(@NonNull Translet translet, Long subscriptionId) {
        UserInfo currentUser = getCurrentUser(translet);
        if (currentUser == null) {
            return new FailureResponse().unauthorized();
        }
        if (currentUser.hasRole("DEMO")) {
            return new FailureResponse().forbidden();
        }
        if (subscriptionId == null) {
            return new FailureResponse("REQUIRED_FIELD", "Missing subscriptionId").badRequest();
        }

        if (notificationService == null || !notificationService.isWebPushEnabled()) {
            return new FailureResponse("SERVICE_DISABLED", "Web Push functionality is disabled.").badRequest();
        }

        try {
            int result = notificationService.sendAdminTestPushToSubscription(subscriptionId);
            Map<String, Object> data = new HashMap<>();
            data.put("result", result);
            if (result == 1) {
                data.put("message", "Ping notification sent successfully.");
                return new SuccessResponse(data).ok();
            } else if (result == -1) {
                data.put("message", "Device subscription was expired and has been automatically removed.");
                return new SuccessResponse(data).ok();
            } else {
                return new FailureResponse("PING_FAILED", "Failed to send ping notification to the device.").badRequest();
            }
        } catch (Exception e) {
            logger.error("Failed to send device ping push", e);
            return new FailureResponse("DISPATCH_FAILED", "Failed to send ping: " + e.getMessage()).internalServerError();
        }
    }

    /**
     * Purges stale push subscriptions inactive for more than the specified number of days (default 30 days).
     */
    @RequestToPost("/prune-stale")
    public RestResponse pruneStale(@NonNull Translet translet, Integer days) {
        UserInfo currentUser = getCurrentUser(translet);
        if (currentUser == null) {
            return new FailureResponse().unauthorized();
        }
        if (!currentUser.hasRole("SUPER_ADMIN")) {
            return new FailureResponse().forbidden();
        }

        if (notificationService == null) {
            return new FailureResponse("SERVICE_UNAVAILABLE", "NotificationService is not available.").badRequest();
        }

        int thresholdDays = (days != null && days > 0) ? days : 30;
        try {
            int prunedCount = notificationService.pruneStalePushSubscriptions(thresholdDays);
            Map<String, Object> data = new HashMap<>();
            data.put("prunedCount", prunedCount);
            data.put("days", thresholdDays);
            data.put("message", "Pruned " + prunedCount + " stale device subscription(s).");
            return new SuccessResponse(data).ok();
        } catch (Exception e) {
            logger.error("Failed to prune stale push subscriptions", e);
            return new FailureResponse("PRUNE_FAILED", "Failed to prune stale devices: " + e.getMessage()).internalServerError();
        }
    }

    /**
     * Regenerates the cluster-wide VAPID key pair and purges all previous device subscriptions.
     */
    @RequestToPost("/regenerate-key")
    public RestResponse regenerateKey(@NonNull Translet translet) {
        UserInfo currentUser = getCurrentUser(translet);
        if (currentUser == null) {
            return new FailureResponse().unauthorized();
        }
        if (!currentUser.hasRole("SUPER_ADMIN")) {
            return new FailureResponse().forbidden();
        }

        if (notificationService == null || !notificationService.isWebPushEnabled()) {
            return new FailureResponse("SERVICE_DISABLED", "Web Push functionality is disabled.").badRequest();
        }

        try {
            String newPublicKey = notificationService.regenerateVapidKey();
            Map<String, Object> data = new HashMap<>();
            data.put("publicKey", newPublicKey);
            return new SuccessResponse(data).ok();
        } catch (Exception e) {
            logger.error("Failed to regenerate VAPID key", e);
            return new FailureResponse("REGEN_FAILED", "Failed to regenerate VAPID key: " + e.getMessage()).internalServerError();
        }
    }

}
