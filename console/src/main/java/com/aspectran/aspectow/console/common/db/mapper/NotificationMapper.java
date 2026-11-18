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
package com.aspectran.aspectow.console.common.db.mapper;

import com.aspectran.aspectow.console.common.db.model.NotificationHistory;
import com.aspectran.aspectow.console.common.db.model.PushSubscription;
import com.aspectran.aspectow.console.common.db.model.UserNotificationSetting;
import com.aspectran.aspectow.console.common.db.model.VapidKey;
import com.aspectran.aspectow.console.common.db.tx.ConsoleSqlMapperProvider;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.mybatis.SqlMapperAccess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * The MyBatis mapper interface for user notifications and push subscriptions.
 *
 * <p>Created: 2026-11-17</p>
 */
@Mapper
public interface NotificationMapper {

    /**
     * Retrieves the notification setting for a user.
     * @param userId the user ID
     * @return the notification setting, or null if not set
     */
    UserNotificationSetting getNotificationSettingByUserId(Long userId);

    /**
     * Inserts a new user notification setting.
     * @param setting the setting to insert
     * @return the number of affected rows
     */
    int insertNotificationSetting(UserNotificationSetting setting);

    /**
     * Updates an existing user notification setting.
     * @param setting the setting to update
     * @return the number of affected rows
     */
    int updateNotificationSetting(UserNotificationSetting setting);

    /**
     * Retrieves the minimum CPU alert threshold among users who enabled push notifications.
     * @return the minimum CPU threshold (default 85 if none)
     */
    int getMinCpuThreshold();

    /**
     * Retrieves the minimum memory alert threshold among users who enabled push notifications.
     * @return the minimum memory threshold (default 85 if none)
     */
    int getMinMemoryThreshold();

    /**
     * Retrieves the cluster-wide VAPID key pair.
     * @return the VAPID key pair, or null if not yet generated
     */
    VapidKey getVapidKey();

    /**
     * Inserts the cluster-wide VAPID key pair.
     * @param vapidKey the VAPID key pair to insert
     * @return the number of affected rows
     */
    int insertVapidKey(VapidKey vapidKey);

    /**
     * Retrieves a push subscription by its endpoint URL.
     * @param endpoint the push endpoint URL
     * @return the push subscription, or null if not found
     */
    PushSubscription getPushSubscriptionByEndpoint(String endpoint);

    /**
     * Retrieves all push subscriptions for a user.
     * @param userId the user ID
     * @return the list of push subscriptions
     */
    List<PushSubscription> getPushSubscriptionsByUserId(Long userId);

    /**
     * Retrieves all active user IDs eligible for notifications.
     * @return the list of active user IDs
     */
    List<Long> getAllActiveUserIds();

    /**
     * Retrieves active user IDs who have configured notification settings and possess the specified permission.
     * @param permission the permission code (e.g. COMMAND_EXECUTE, BUILD_VIEW)
     * @return the list of user IDs
     */
    List<Long> getUserIdsByPermission(String permission);

    /**
     * Retrieves all active push subscriptions.
     * @return the list of all active push subscriptions
     */
    List<PushSubscription> getAllActivePushSubscriptions();

    /**
     * Inserts a new push subscription.
     * @param subscription the subscription to insert
     * @return the number of affected rows
     */
    int insertPushSubscription(PushSubscription subscription);

    /**
     * Updates the last active timestamp of a push subscription.
     * @param endpoint the push endpoint URL
     * @return the number of affected rows
     */
    int updatePushSubscriptionLastActive(String endpoint);

    /**
     * Deletes a push subscription by its endpoint URL.
     * @param endpoint the push endpoint URL
     * @return the number of affected rows
     */
    int deletePushSubscriptionByEndpoint(String endpoint);

    /**
     * Deletes all push subscriptions for a user.
     * @param userId the user ID
     * @return the number of affected rows
     */
    int deletePushSubscriptionsByUserId(Long userId);

    /**
     * Retrieves the recent notification history for a user.
     * @param userId the user ID
     * @param limit maximum number of records to retrieve
     * @return the list of notification history records
     */
    List<NotificationHistory> getNotificationHistoryList(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Retrieves the count of unread notifications for a user.
     * @param userId the user ID
     * @return the unread notification count
     */
    long getUnreadNotificationCount(Long userId);

    /**
     * Inserts a new notification history record.
     * @param history the notification history to insert
     * @return the number of affected rows
     */
    int insertNotificationHistory(NotificationHistory history);

    /**
     * Marks a specific notification as read.
     * @param notificationId the notification ID
     * @param userId the user ID
     * @return the number of affected rows
     */
    int markNotificationAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    /**
     * Marks all notifications as read for a user.
     * @param userId the user ID
     * @return the number of affected rows
     */
    int markAllNotificationsAsRead(Long userId);

    /**
     * Deletes all notification history for a user.
     * @param userId the user ID
     * @return the number of affected rows
     */
    int deleteAllNotificationHistory(Long userId);

    /**
     * Data Access Object (DAO) for {@link NotificationMapper}.
     */
    @Component
    @Bean("console.notificationDao")
    class Dao extends SqlMapperAccess<NotificationMapper> implements NotificationMapper {

        @Autowired
        public Dao(ConsoleSqlMapperProvider sqlMapperProvider) {
            super(sqlMapperProvider);
        }

        @Override
        public UserNotificationSetting getNotificationSettingByUserId(Long userId) {
            return mapper().getNotificationSettingByUserId(userId);
        }

        @Override
        public int insertNotificationSetting(UserNotificationSetting setting) {
            return mapper().insertNotificationSetting(setting);
        }

        @Override
        public int updateNotificationSetting(UserNotificationSetting setting) {
            return mapper().updateNotificationSetting(setting);
        }

        @Override
        public int getMinCpuThreshold() {
            return mapper().getMinCpuThreshold();
        }

        @Override
        public int getMinMemoryThreshold() {
            return mapper().getMinMemoryThreshold();
        }

        @Override
        public VapidKey getVapidKey() {
            return mapper().getVapidKey();
        }

        @Override
        public int insertVapidKey(VapidKey vapidKey) {
            return mapper().insertVapidKey(vapidKey);
        }

        @Override
        public PushSubscription getPushSubscriptionByEndpoint(String endpoint) {
            return mapper().getPushSubscriptionByEndpoint(endpoint);
        }

        @Override
        public List<PushSubscription> getPushSubscriptionsByUserId(Long userId) {
            return mapper().getPushSubscriptionsByUserId(userId);
        }

        @Override
        public List<Long> getAllActiveUserIds() {
            return mapper().getAllActiveUserIds();
        }

        @Override
        public List<Long> getUserIdsByPermission(String permission) {
            return mapper().getUserIdsByPermission(permission);
        }

        @Override
        public List<PushSubscription> getAllActivePushSubscriptions() {
            return mapper().getAllActivePushSubscriptions();
        }

        @Override
        public int insertPushSubscription(PushSubscription subscription) {
            return mapper().insertPushSubscription(subscription);
        }

        @Override
        public int updatePushSubscriptionLastActive(String endpoint) {
            return mapper().updatePushSubscriptionLastActive(endpoint);
        }

        @Override
        public int deletePushSubscriptionByEndpoint(String endpoint) {
            return mapper().deletePushSubscriptionByEndpoint(endpoint);
        }

        @Override
        public int deletePushSubscriptionsByUserId(Long userId) {
            return mapper().deletePushSubscriptionsByUserId(userId);
        }

        @Override
        public List<NotificationHistory> getNotificationHistoryList(Long userId, int limit) {
            return mapper().getNotificationHistoryList(userId, limit);
        }

        @Override
        public long getUnreadNotificationCount(Long userId) {
            return mapper().getUnreadNotificationCount(userId);
        }

        @Override
        public int insertNotificationHistory(NotificationHistory history) {
            return mapper().insertNotificationHistory(history);
        }

        @Override
        public int markNotificationAsRead(Long notificationId, Long userId) {
            return mapper().markNotificationAsRead(notificationId, userId);
        }

        @Override
        public int markAllNotificationsAsRead(Long userId) {
            return mapper().markAllNotificationsAsRead(userId);
        }

        @Override
        public int deleteAllNotificationHistory(Long userId) {
            return mapper().deleteAllNotificationHistory(userId);
        }
    }

}
