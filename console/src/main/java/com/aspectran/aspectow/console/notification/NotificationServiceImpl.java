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

import com.aspectran.aspectow.console.common.db.mapper.NotificationMapper;
import com.aspectran.aspectow.console.common.db.model.NotificationHistory;
import com.aspectran.aspectow.console.common.db.model.PushSubscription;
import com.aspectran.aspectow.console.common.db.model.UserNotificationSetting;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of the {@link NotificationService}.
 *
 * <p>Created: 2026-11-17</p>
 */
@Component
public class NotificationServiceImpl extends InstantActivitySupport implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;
    private final WebPushManager webPushManager;
    private final ExecutorService dispatchExecutor;

    @Autowired
    public NotificationServiceImpl(NotificationMapper notificationMapper, WebPushManager webPushManager) {
        this.notificationMapper = notificationMapper;
        this.webPushManager = webPushManager;
        this.dispatchExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "WebPushDispatcher");
            t.setDaemon(true);
            return t;
        });
    }

    @Destroy
    public void destroy() {
        dispatchExecutor.shutdown();
    }

    @Override
    public UserNotificationSetting getOrCreateNotificationSetting(Long userId) {
        UserNotificationSetting setting = notificationMapper.getNotificationSettingByUserId(userId);
        if (setting == null) {
            setting = new UserNotificationSetting();
            setting.setUserId(userId);
            setting.setPushEnabled("Y");
            setting.setCpuThreshold(85);
            setting.setMemoryThreshold(85);
            setting.setNotifyNodeStatus("Y");
            setting.setNotifySchedulerFail("Y");
            setting.setNotifyBuildResult("Y");
            setting.setNotifySecurityAlert("Y");
            setting.setNotifyCommandFinish("Y");
            notificationMapper.insertNotificationSetting(setting);
        }
        return setting;
    }

    @Override
    public void updateNotificationSetting(UserNotificationSetting setting) {
        if (setting.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        UserNotificationSetting existing = notificationMapper.getNotificationSettingByUserId(setting.getUserId());
        if (existing == null) {
            notificationMapper.insertNotificationSetting(setting);
        } else {
            notificationMapper.updateNotificationSetting(setting);
        }
    }

    @Override
    public PushSubscription registerPushSubscription(Long userId, String endpoint, String p256dhKey, String authKey, String userAgent) {
        PushSubscription existing = notificationMapper.getPushSubscriptionByEndpoint(endpoint);
        if (existing != null) {
            existing.setUserId(userId);
            existing.setP256dhKey(p256dhKey);
            existing.setAuthKey(authKey);
            existing.setUserAgent(userAgent);
            notificationMapper.updatePushSubscriptionLastActive(endpoint);
            return existing;
        }

        PushSubscription subscription = new PushSubscription();
        subscription.setUserId(userId);
        subscription.setEndpoint(endpoint);
        subscription.setP256dhKey(p256dhKey);
        subscription.setAuthKey(authKey);
        subscription.setUserAgent(userAgent);
        notificationMapper.insertPushSubscription(subscription);
        return subscription;
    }

    @Override
    public void unregisterPushSubscription(String endpoint) {
        notificationMapper.deletePushSubscriptionByEndpoint(endpoint);
    }

    @Override
    public List<NotificationHistory> getNotificationHistory(Long userId, int limit) {
        if (limit <= 0) {
            limit = 30;
        }
        return notificationMapper.getNotificationHistoryList(userId, limit);
    }

    @Override
    public long getUnreadNotificationCount(Long userId) {
        return notificationMapper.getUnreadNotificationCount(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        notificationMapper.markNotificationAsRead(notificationId, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllNotificationsAsRead(userId);
    }

    @Override
    public void clearNotificationHistory(Long userId) {
        notificationMapper.deleteAllNotificationHistory(userId);
    }

    @Override
    public int sendTestNotification(Long userId) {
        List<PushSubscription> subscriptions = notificationMapper.getPushSubscriptionsByUserId(userId);
        if (subscriptions.isEmpty()) {
            logger.info("No push subscriptions found for user ID: {}", userId);
            return 0;
        }

        String payloadJson = buildPayloadJson(
                "Aspectow Console",
                "Test notification received successfully!",
                "./",
                "test-alert"
        );

        int sentCount = 0;
        for (PushSubscription sub : subscriptions) {
            try {
                int status = webPushManager.sendPushNotification(sub, payloadJson);
                if (status >= 200 && status < 300) {
                    notificationMapper.updatePushSubscriptionLastActive(sub.getEndpoint());
                    sentCount++;
                } else if (status == 401 || status == 404 || status == 410) {
                    // Endpoint invalid (VAPID key mismatch) or expired
                    logger.info("Removing invalid/expired push subscription endpoint: {} (status: {})", sub.getEndpoint(), status);
                    notificationMapper.deletePushSubscriptionByEndpoint(sub.getEndpoint());
                }
            } catch (Exception e) {
                logger.error("Failed to send test push notification to endpoint: " + sub.getEndpoint(), e);
            }
        }

        // Also record to in-app notification history
        NotificationHistory history = new NotificationHistory();
        history.setUserId(userId);
        history.setCategory("TEST");
        history.setSeverity("INFO");
        history.setTitle("Test Notification");
        history.setMessage("Web Push test notification was successfully sent.");
        history.setTargetUrl("./");
        notificationMapper.insertNotificationHistory(history);

        return sentCount;
    }

    @Override
    public void dispatchNotification(String category, String severity, String title, String message, String targetUrl) {
        dispatchExecutor.submit(() -> {
            try {
                // Step 1: Query subscriptions and record in-app history within short DB transaction
                List<PushSubscription> targetsToSend = new ArrayList<>();
                instantActivity(() -> {
                    List<PushSubscription> activeSubscriptions = notificationMapper.getAllActivePushSubscriptions();
                    if (activeSubscriptions == null || activeSubscriptions.isEmpty()) {
                        return null;
                    }

                    Set<Long> recordedUserIds = new HashSet<>();
                    for (PushSubscription sub : activeSubscriptions) {
                        UserNotificationSetting setting = notificationMapper.getNotificationSettingByUserId(sub.getUserId());
                        if (setting == null || !setting.isPushAlertEnabled()) {
                            continue;
                        }

                        // Check category-level permission
                        if (!isCategoryAllowed(setting, category)) {
                            continue;
                        }

                        // Record in-app notification once per user
                        if (recordedUserIds.add(sub.getUserId())) {
                            NotificationHistory history = new NotificationHistory();
                            history.setUserId(sub.getUserId());
                            history.setCategory(category);
                            history.setSeverity(severity);
                            history.setTitle(title);
                            history.setMessage(message);
                            history.setTargetUrl(targetUrl);
                            notificationMapper.insertNotificationHistory(history);
                        }

                        targetsToSend.add(sub);
                    }
                    return null;
                });

                if (targetsToSend.isEmpty()) {
                    return;
                }

                // Step 2: Send Web Push (Outside DB transaction to avoid connection starvation / timeout)
                String payloadJson = buildPayloadJson(
                        title,
                        message,
                        targetUrl,
                        category != null ? category.toLowerCase() : "default"
                );
                List<String> successEndpoints = new ArrayList<>();
                List<String> expiredEndpoints = new ArrayList<>();

                for (PushSubscription sub : targetsToSend) {
                    try {
                        int status = webPushManager.sendPushNotification(sub, payloadJson);
                        if (status >= 200 && status < 300) {
                            successEndpoints.add(sub.getEndpoint());
                        } else if (status == 401 || status == 404 || status == 410) {
                            expiredEndpoints.add(sub.getEndpoint());
                        }
                    } catch (Exception e) {
                        logger.error("Error dispatching push notification to {}", sub.getEndpoint(), e);
                    }
                }

                // Step 3: Update subscription statuses in DB
                if (!successEndpoints.isEmpty() || !expiredEndpoints.isEmpty()) {
                    instantActivity(() -> {
                        for (String ep : successEndpoints) {
                            try {
                                notificationMapper.updatePushSubscriptionLastActive(ep);
                            } catch (Exception ignored) {
                            }
                        }
                        for (String ep : expiredEndpoints) {
                            try {
                                notificationMapper.deletePushSubscriptionByEndpoint(ep);
                            } catch (Exception ignored) {
                            }
                        }
                        return null;
                    });
                }
            } catch (Exception e) {
                logger.error("Error in dispatchNotification task", e);
            }
        });
    }

    private boolean isCategoryAllowed(UserNotificationSetting setting, String category) {
        if (category == null) return true;
        return switch (category.toUpperCase()) {
            case "MONITORING" -> setting.isNotifyNodeStatusEnabled();
            case "SCHEDULER" -> setting.isNotifySchedulerFailEnabled();
            case "BUILD" -> setting.isNotifyBuildResultEnabled();
            case "SECURITY" -> setting.isNotifySecurityAlertEnabled();
            case "COMMAND" -> setting.isNotifyCommandFinishEnabled();
            default -> true;
        };
    }

    private String buildPayloadJson(String title, String body, String targetUrl, String tag) {
        return "{" +
                "\"title\":\"" + escapeJson(title) + "\"," +
                "\"body\":\"" + escapeJson(body) + "\"," +
                "\"icon\":\"./assets/icons/icon-192x192.png\"," +
                "\"badge\":\"./assets/icons/icon-192x192.png\"," +
                "\"targetUrl\":\"" + escapeJson(targetUrl != null ? targetUrl : "./") + "\"," +
                "\"tag\":\"" + escapeJson(tag != null ? tag : "aspectow-notification") + "\"" +
                "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
