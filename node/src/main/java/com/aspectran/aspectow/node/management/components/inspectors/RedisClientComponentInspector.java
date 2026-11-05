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
import com.aspectran.aspectow.node.redis.RedisConnectionPoolConfig;
import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import com.aspectran.utils.DurationUtils;
import io.lettuce.core.RedisURI;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of Redis Client and Connection Pool components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class RedisClientComponentInspector implements ComponentInspector {

    private static final String REDIS_CLIENT_CLASS = "io.lettuce.core.RedisClient";
    private static final String REDIS_CLUSTER_CLIENT_CLASS = "io.lettuce.core.cluster.RedisClusterClient";

    @Override
    @NonNull
    public String getId() {
        return "redisClient";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Redis Client & Pool";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Persistence";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-layers-half";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        if (ComponentUtils.hasLocalBean(context, RedisConnectionPoolConfig.class)) {
            return true;
        }
        try {
            Class<?> redisClientClass = Class.forName(REDIS_CLIENT_CLASS);
            if (ComponentUtils.hasLocalBean(context, redisClientClass)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class<?> clusterClientClass = Class.forName(REDIS_CLUSTER_CLIENT_CLASS);
            if (ComponentUtils.hasLocalBean(context, clusterClientClass)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            BeanRule[] poolRules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(RedisConnectionPoolConfig.class);
            if (poolRules != null) {
                for (BeanRule rule : poolRules) {
                    try {
                        String beanId = rule.getId();
                        if (beanId != null) {
                            Object bean = defaultBeanRegistry.getBean(beanId);
                            if (bean instanceof RedisConnectionPoolConfig config) {
                                items.add(extractPoolConfigInfo(beanId, config));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (items.isEmpty()) {
                try {
                    Class<?> clientClass = Class.forName(REDIS_CLIENT_CLASS);
                    BeanRule[] clientRules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(clientClass);
                    if (clientRules != null) {
                        for (BeanRule rule : clientRules) {
                            try {
                                String beanId = rule.getId();
                                if (beanId != null) {
                                    Object bean = defaultBeanRegistry.getBean(beanId);
                                    if (bean != null) {
                                        items.add(extractClientInfo(beanId, bean));
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
        } else {
            RedisConnectionPoolConfig[] configs = registry.getBeansOfType(RedisConnectionPoolConfig.class);
            if (configs != null) {
                int index = 1;
                for (RedisConnectionPoolConfig config : configs) {
                    items.add(extractPoolConfigInfo("redisConnectionPoolConfig" + (index > 1 ? index : ""), config));
                    index++;
                }
            }

            if (items.isEmpty()) {
                try {
                    Class<?> clientClass = Class.forName(REDIS_CLIENT_CLASS);
                    Object[] clients = registry.getBeansOfType((Class<?>) clientClass);
                    if (clients != null) {
                        int index = 1;
                        for (Object client : clients) {
                            items.add(extractClientInfo("redisClient" + (index > 1 ? index : ""), client));
                            index++;
                        }
                    }
                } catch (Exception ignored) {
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
                .setDescription("Lettuce Redis Client and Connection Pool Configurations")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractPoolConfigInfo(String beanName, @NonNull RedisConnectionPoolConfig config) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = config.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());
        info.put("poolSize", config.getPoolSize());

        RedisURI uri = config.getRedisURI();
        if (uri != null) {
            info.put("host", uri.getHost());
            info.put("port", uri.getPort());
            info.put("database", uri.getDatabase());
            info.put("ssl", uri.isSsl());
            info.put("timeout", uri.getTimeout() != null ? DurationUtils.toHumanReadable(uri.getTimeout()) : null);
            info.put("clientName", uri.getClientName());

            String uriStr = (uri.toURI() != null ? uri.toURI().toString() : "");
            if (!uriStr.isEmpty()) {
                uriStr = uriStr.replaceAll(":[^@]+@", ":********@");
            }
            info.put("redisUri", uriStr);
        }

        return info;
    }

    @NonNull
    private Map<String, Object> extractClientInfo(String beanName, @NonNull Object client) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = client.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        return info;
    }

}
