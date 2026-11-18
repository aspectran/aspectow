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

import com.aspectran.aspectow.console.common.db.model.NotificationHistory;
import com.aspectran.aspectow.console.common.db.model.PushSubscription;
import com.aspectran.aspectow.console.common.db.model.UserNotificationSetting;

import java.util.List;

/**
 * Service interface for managing user notifications, push subscriptions, and dispatching alerts.
 *
 * <p>Created: 2026-11-17</p>
 */
public interface NotificationService {

    /**
     * Checks if the notification system is enabled.
     * @return true if enabled, false otherwise
     */
    boolean isNotificationEnabled();

    /**
     * Checks if external Web Push dispatching is enabled.
     * @return true if enabled, false otherwise
     */
    boolean isWebPushEnabled();

    /**
     * Retrieves notification preferences for a user, or null if not yet configured.
     * @param userId the user ID
     * @return the notification setting, or null if not configured
     */
    UserNotificationSetting getNotificationSetting(Long userId);

    /**
     * Saves (inserts or updates) notification preferences and thresholds for a user.
     * @param setting the setting to save
     */
    void saveNotificationSetting(UserNotificationSetting setting);

    /**
     * Updates notification preferences and thresholds for a user.
     * @param setting the updated setting
     */
    void updateNotificationSetting(UserNotificationSetting setting);

    /**
     * Registers a new or existing push subscription for a user.
     * @param userId the user ID
     * @param endpoint the push endpoint URL
     * @param p256dhKey the client public key
     * @param authKey the client auth secret
     * @param userAgent the browser/device user agent
     * @return the registered push subscription
     */
    PushSubscription registerPushSubscription(Long userId, String endpoint, String p256dhKey, String authKey, String userAgent);

    /**
     * Unregisters a push subscription by its endpoint.
     * @param endpoint the push endpoint URL
     */
    void unregisterPushSubscription(String endpoint);

    /**
     * Retrieves the recent notification history for a user.
     * @param userId the user ID
     * @param limit maximum number of records
     * @return the notification history list
     */
    List<NotificationHistory> getNotificationHistory(Long userId, int limit);

    /**
     * Retrieves the unread notification count for a user.
     * @param userId the user ID
     * @return the unread count
     */
    long getUnreadNotificationCount(Long userId);

    /**
     * Marks a specific notification as read.
     * @param notificationId the notification ID
     * @param userId the user ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * Marks all notifications as read for a user.
     * @param userId the user ID
     */
    void markAllAsRead(Long userId);

    /**
     * Clears all notification history records for a user.
     * @param userId the user ID
     */
    void clearNotificationHistory(Long userId);

    /**
     * Sends a test push notification to all devices registered by the specified user.
     * @param userId the user ID
     * @return number of devices successfully notified
     */
    int sendTestNotification(Long userId);

    /**
     * Dispatches a notification to all subscribed users who have enabled the given category.
     * Also saves the record to the in-app notification history.
     * @param category the notification category (MONITORING, SCHEDULER, BUILD, SECURITY, COMMAND)
     * @param severity the severity level (INFO, WARN, CRITICAL, RESOLVED)
     * @param title the notification title
     * @param message the notification message
     * @param targetUrl internal URL to navigate when clicked
     */
    void dispatchNotification(String category, String severity, String title, String message, String targetUrl);

}
