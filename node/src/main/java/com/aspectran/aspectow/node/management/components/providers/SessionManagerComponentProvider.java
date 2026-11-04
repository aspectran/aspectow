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
package com.aspectran.aspectow.node.management.components.providers;

import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInfoProvider;
import com.aspectran.aspectow.node.management.components.ComponentUtils;
import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.component.session.SessionCache;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.component.session.SessionStatistics;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import com.aspectran.utils.DurationUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provider that extracts specification and configuration details of SessionManager components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class SessionManagerComponentProvider implements ComponentInfoProvider {

    @Override
    @NonNull
    public String getId() {
        return "sessionManager";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Session Manager";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Web";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-clock-history";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        BeanRegistry beanRegistry = context.getBeanRegistry();
        return (beanRegistry != null && beanRegistry.containsBean(SessionManager.class));
    }

    @Override
    @Nullable
    public ComponentDetails extractDetails(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(SessionManager.class);
            if (rules != null) {
                for (BeanRule rule : rules) {
                    try {
                        String beanId = rule.getId();
                        if (beanId != null) {
                            Object bean = defaultBeanRegistry.getBean(beanId);
                            if (bean instanceof SessionManager manager) {
                                items.add(extractSessionManagerInfo(beanId, manager));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (items.isEmpty()) {
            SessionManager[] managers = registry.getBeansOfType(SessionManager.class);
            if (managers != null) {
                int index = 1;
                for (SessionManager manager : managers) {
                    items.add(extractSessionManagerInfo("sessionManager" + (index > 1 ? index : ""), manager));
                    index++;
                }
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("HTTP and Web Session Management Specifications and Statistics")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractSessionManagerInfo(String beanName, @NonNull SessionManager manager) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = manager.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        info.put("routeId", manager.getRouteId());
        info.put("defaultMaxIdleSecs", manager.getDefaultMaxIdleSecs());
        info.put("defaultMaxIdleSecsFormatted", formatSeconds(manager.getDefaultMaxIdleSecs()));
        info.put("maxIdleSecsForNew", manager.getMaxIdleSecsForNew());

        // Cache & Store settings
        SessionCache cache = manager.getSessionCache();
        if (cache != null) {
            info.put("cacheClassName", cache.getClass().getName());
            info.put("cacheSimpleClassName", cache.getClass().getSimpleName());
            info.put("clusterEnabled", cache.isClusterEnabled());
            info.put("maxActiveSessions", cache.getMaxActiveSessions());
            info.put("evictionIdleSecs", cache.getEvictionIdleSecs());
            info.put("saveOnCreate", cache.isSaveOnCreate());
            info.put("removeUnloadableSessions", cache.isRemoveUnloadableSessions());

            Object store = ComponentUtils.invokeObject(cache, "getSessionStore");
            if (store == null) {
                store = ComponentUtils.getFieldValue(cache, "sessionStore");
            }
            if (store != null) {
                info.put("sessionStoreClassName", store.getClass().getName());
                info.put("sessionStoreSimpleClassName", store.getClass().getSimpleName());
                info.put("storeDir", ComponentUtils.invokeString(store, "getStoreDir"));
                info.put("redisUri", ComponentUtils.invokeString(store, "getRedisURI", "getUri"));
            }
        }

        // Active sessions
        Set<String> activeSessions = manager.getActiveSessions();
        info.put("activeSessionsCount", activeSessions != null ? activeSessions.size() : 0);

        // Statistics
        SessionStatistics statistics = manager.getStatistics();
        if (statistics != null) {
            info.put("statsActive", statistics.getNumberOfActives());
            info.put("statsHighest", statistics.getHighestNumberOfActives());
            info.put("statsCreated", statistics.getNumberOfCreated());
            info.put("statsExpired", statistics.getNumberOfExpired());
            info.put("statsRejected", statistics.getNumberOfRejected());
            info.put("statsAvgAliveTime", statistics.getAverageSessionAliveTime());
        }

        return info;
    }

    @NonNull
    private String formatSeconds(int seconds) {
        if (seconds <= 0) {
            return "Unlimited";
        }
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int remainingSecs = seconds % 60;
        if (minutes < 60) {
            return minutes + "m" + (remainingSecs > 0 ? " " + remainingSecs + "s" : "");
        }
        int hours = minutes / 60;
        int remainingMins = minutes % 60;
        return hours + "h" + (remainingMins > 0 ? " " + remainingMins + "m" : "");
    }

}
