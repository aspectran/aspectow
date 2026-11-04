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
import com.aspectran.core.component.bean.async.AsyncTaskExecutor;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Provider that extracts specification and configuration details of AsyncTaskExecutor components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class AsyncTaskExecutorComponentProvider implements ComponentInfoProvider {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskExecutorComponentProvider.class);

    @Override
    @NonNull
    public String getId() {
        return "asyncTaskExecutor";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Async Task Executor";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Concurrency";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-cpu";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        return (ComponentUtils.hasLocalBean(context, AsyncTaskExecutor.class) ||
                ComponentUtils.hasLocalBean(context, Executor.class));
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
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(AsyncTaskExecutor.class);
            if (rules == null) {
                rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(Executor.class);
            }
            if (rules != null) {
                for (BeanRule rule : rules) {
                    try {
                        Object bean = defaultBeanRegistry.getBean(rule);
                        if (bean == null && rule.getId() != null) {
                            bean = defaultBeanRegistry.getBean(rule.getId());
                        }
                        if (bean == null && rule.getBeanClass() != null) {
                            bean = defaultBeanRegistry.getBean(rule.getBeanClass());
                        }
                        if (bean instanceof Executor executor) {
                            String beanName = ComponentUtils.resolveBeanName(rule, "taskExecutor");
                            items.add(extractExecutorInfo(beanName, executor));
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to extract AsyncTaskExecutor bean info: {}", rule.getId(), e);
                    }
                }
            }
        } else {
            Executor[] executors = registry.getBeansOfType(Executor.class);
            if (executors != null) {
                int index = 1;
                for (Executor exec : executors) {
                    items.add(extractExecutorInfo("taskExecutor" + (index > 1 ? index : ""), exec));
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
                .setDescription("Thread Pool and Asynchronous Task Execution Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractExecutorInfo(String beanName, @NonNull Executor executor) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = executor.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        // Pool sizes
        info.put("corePoolSize", ComponentUtils.invokeNumber(executor, "getCorePoolSize"));
        info.put("maxPoolSize", ComponentUtils.invokeNumber(executor, "getMaxPoolSize", "getMaximumPoolSize"));
        info.put("keepAliveSeconds", ComponentUtils.invokeNumber(executor, "getKeepAliveSeconds"));
        info.put("allowCoreThreadTimeOut", ComponentUtils.invokeBoolean(executor, "isAllowCoreThreadTimeOut", "allowsCoreThreadTimeOut"));

        // Queue
        info.put("queueCapacity", ComponentUtils.invokeNumber(executor, "getQueueCapacity"));
        info.put("queueType", invokeQueueType(executor));

        // Thread naming & rejection policy
        info.put("threadNamePrefix", ComponentUtils.invokeString(executor, "getThreadNamePrefix"));
        info.put("rejectionPolicy", invokeRejectionPolicy(executor));
        info.put("concurrencyLimit", ComponentUtils.invokeNumber(executor, "getConcurrencyLimit"));

        return info;
    }

    @Nullable
    private String invokeQueueType(Object target) {
        try {
            Method method = ComponentUtils.findMethod(target.getClass(), "getThreadPoolExecutor");
            if (method != null) {
                Object tpe = method.invoke(target);
                if (tpe != null) {
                    Method getQueue = ComponentUtils.findMethod(tpe.getClass(), "getQueue");
                    if (getQueue != null) {
                        Object queue = getQueue.invoke(tpe);
                        if (queue != null) {
                            return queue.getClass().getSimpleName();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private String invokeRejectionPolicy(Object target) {
        try {
            Method method = ComponentUtils.findMethod(target.getClass(), "getRejectedExecutionHandler");
            if (method != null) {
                Object handler = method.invoke(target);
                if (handler != null) {
                    return handler.getClass().getSimpleName();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}
