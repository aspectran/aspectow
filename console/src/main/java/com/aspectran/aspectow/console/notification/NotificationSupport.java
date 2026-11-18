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
package com.aspectran.aspectow.console.notification;

import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.context.env.Environment;
import com.aspectran.utils.StringUtils;

/**
 * Base support class for notification-related components.
 * Provides unified access to notification and Web Push configuration properties
 * via {@link InstantActivitySupport#getEnvironment()}.
 *
 * <p>Created: 2026-11-18</p>
 */
public abstract class NotificationSupport extends InstantActivitySupport {

    public static final String NOTIFICATION_ENABLED_PROPERTY = "aspectow.console.notification.enabled";

    public static final String WEBPUSH_ENABLED_PROPERTY = "aspectow.console.notification.webPush.enabled";

    private Boolean notificationEnabled;

    private Boolean webPushEnabled;

    public void setNotificationEnabled(Boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public void setWebPushEnabled(Boolean webPushEnabled) {
        this.webPushEnabled = webPushEnabled;
    }

    /**
     * Checks if the notification system is enabled in the current environment.
     * Requires explicit opt-in (enabled=true). Defaults to false.
     * @return true if explicitly enabled, false otherwise
     */
    public boolean isNotificationEnabled() {
        if (notificationEnabled != null) {
            return notificationEnabled;
        }
        return getBooleanProperty(NOTIFICATION_ENABLED_PROPERTY, false);
    }

    /**
     * Checks if external Web Push dispatching is enabled in the current environment.
     * Requires notification to be enabled and explicit opt-in (webPush.enabled=true). Defaults to false.
     * @return true if explicitly enabled, false otherwise
     */
    public boolean isWebPushEnabled() {
        if (webPushEnabled != null) {
            return isNotificationEnabled() && webPushEnabled;
        }
        return isNotificationEnabled() && getBooleanProperty(WEBPUSH_ENABLED_PROPERTY, false);
    }

    private boolean getBooleanProperty(String name, boolean defaultValue) {
        try {
            Environment environment = getEnvironment();
            Object prop = environment.getProperty(name);
            if (prop instanceof Boolean bool) {
                return bool;
            }
            if (prop instanceof String str && StringUtils.hasText(str)) {
                return Boolean.parseBoolean(str.trim());
            }
            if (prop != null) {
                return Boolean.parseBoolean(prop.toString().trim());
            }
            return defaultValue;
        } catch (IllegalStateException e) {
            // ActivityContext has not been injected yet (e.g. standalone unit testing)
            return defaultValue;
        }
    }

}
