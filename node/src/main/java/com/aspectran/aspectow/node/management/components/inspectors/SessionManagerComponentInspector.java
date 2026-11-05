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
package com.aspectran.aspectow.node.management.components.inspectors;

import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInspector;
import com.aspectran.aspectow.node.management.components.ComponentUtils;
import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.component.session.AbstractSessionManager;
import com.aspectran.core.component.session.HouseKeeper;
import com.aspectran.core.component.session.SessionCache;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.component.session.SessionManagerProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import com.aspectran.utils.DurationUtils;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of SessionManager components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class SessionManagerComponentInspector implements ComponentInspector {

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

    @Nullable
    private String resolveContextName(@NonNull ActivityContext context) {
        String name = context.getName();
        if (StringUtils.hasText(name)) {
            return name;
        }
        if (context.getMasterService() != null) {
            name = context.getMasterService().getContextName();
            if (StringUtils.hasText(name)) {
                return name;
            }
        }
        return null;
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        BeanRegistry beanRegistry = context.getBeanRegistry();
        if (beanRegistry == null) {
            return false;
        }
        if (beanRegistry.containsBean(SessionManagerProvider.class)) {
            try {
                SessionManagerProvider[] providers = beanRegistry.getBeansOfType(SessionManagerProvider.class);
                if (providers != null) {
                    String contextName = resolveContextName(context);
                    for (SessionManagerProvider provider : providers) {
                        Map<String, SessionManager> sessionManagers = provider.getAllSessionManagers();
                        if (sessionManagers != null && !sessionManagers.isEmpty()) {
                            if (contextName == null || sessionManagers.size() == 1 || sessionManagers.containsKey(contextName)) {
                                return true;
                            }
                            for (String name : sessionManagers.keySet()) {
                                if (name.equalsIgnoreCase(contextName)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return ComponentUtils.hasLocalBean(context, SessionManager.class);
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();

        SessionManagerProvider[] providers = registry.getBeansOfType(SessionManagerProvider.class);
        if (providers != null) {
            String contextName = resolveContextName(context);
            for (SessionManagerProvider provider : providers) {
                try {
                    Map<String, SessionManager> sessionManagers = provider.getAllSessionManagers();
                    if (sessionManagers != null && !sessionManagers.isEmpty()) {
                        if (contextName != null && sessionManagers.containsKey(contextName)) {
                            SessionManager manager = sessionManagers.get(contextName);
                            if (manager != null) {
                                items.add(extractSessionManagerInfo(contextName, manager));
                            }
                        } else if (contextName != null) {
                            boolean found = false;
                            for (Map.Entry<String, SessionManager> entry : sessionManagers.entrySet()) {
                                if (entry.getKey().equalsIgnoreCase(contextName)) {
                                    if (entry.getValue() != null) {
                                        items.add(extractSessionManagerInfo(entry.getKey(), entry.getValue()));
                                    }
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                if (sessionManagers.size() == 1) {
                                    Map.Entry<String, SessionManager> entry = sessionManagers.entrySet().iterator().next();
                                    if (entry.getValue() != null) {
                                        items.add(extractSessionManagerInfo(entry.getKey(), entry.getValue()));
                                    }
                                } else {
                                    for (Map.Entry<String, SessionManager> entry : sessionManagers.entrySet()) {
                                        if (entry.getValue() != null) {
                                            items.add(extractSessionManagerInfo(entry.getKey(), entry.getValue()));
                                        }
                                    }
                                }
                            }
                        } else {
                            for (Map.Entry<String, SessionManager> entry : sessionManagers.entrySet()) {
                                if (entry.getValue() != null) {
                                    items.add(extractSessionManagerInfo(entry.getKey(), entry.getValue()));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (items.isEmpty() && registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
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
            return null;
        }

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("HTTP and Web Session Management Specifications")
                .setRefreshable(isRefreshable())
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractSessionManagerInfo(String beanName, @NonNull SessionManager manager) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = manager.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        info.put("defaultMaxIdleSecs", manager.getDefaultMaxIdleSecs());
        info.put("defaultMaxIdleSecsFormatted", formatSeconds(manager.getDefaultMaxIdleSecs()));

        int maxIdleSecsForNew = manager.getMaxIdleSecsForNew();
        info.put("maxIdleSecsForNew", maxIdleSecsForNew);
        info.put("maxIdleSecsForNewFormatted", maxIdleSecsForNew > 0 ? DurationUtils.toHumanReadableSeconds(maxIdleSecsForNew) : "Default");

        if (manager instanceof AbstractSessionManager asm) {
            HouseKeeper houseKeeper = asm.getHouseKeeper();
            if (houseKeeper != null) {
                int scavengingInterval = houseKeeper.getScavengingInterval();
                info.put("scavengingInterval", scavengingInterval);
                info.put("scavengingIntervalFormatted", formatSeconds(scavengingInterval));
            }
        }

        // Cache & Store settings
        SessionCache cache = manager.getSessionCache();
        if (cache != null) {
            info.put("cacheClassName", cache.getClass().getName());
            info.put("cacheSimpleClassName", cache.getClass().getSimpleName());
            info.put("clusterEnabled", cache.isClusterEnabled());
            info.put("maxActiveSessions", cache.getMaxActiveSessions());
            info.put("evictionIdleSecs", cache.getEvictionIdleSecs());
            info.put("evictionIdleSecsFormatted", cache.getEvictionIdleSecs() > 0 ? DurationUtils.toHumanReadableSeconds(cache.getEvictionIdleSecs()) : "Disabled");
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

        return info;
    }

    @NonNull
    private String formatSeconds(int seconds) {
        if (seconds <= 0) {
            return "Unlimited";
        }
        return DurationUtils.toHumanReadableSeconds(seconds);
    }

}
