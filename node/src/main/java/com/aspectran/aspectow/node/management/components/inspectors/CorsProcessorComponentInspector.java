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
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of CORS Processor components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class CorsProcessorComponentInspector implements ComponentInspector {

    private static final String CORS_PROCESSOR_INTERFACE = "com.aspectran.web.support.cors.CorsProcessor";

    @Override
    @NonNull
    public String getId() {
        return "corsProcessor";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "CORS Processor";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Web";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-shield-check";
    }

    @Nullable
    private Class<?> getCorsProcessorClass() {
        try {
            return Class.forName(CORS_PROCESSOR_INTERFACE);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        Class<?> clazz = getCorsProcessorClass();
        if (clazz == null) {
            return false;
        }
        return ComponentUtils.hasLocalBean(context, clazz);
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        Class<?> clazz = getCorsProcessorClass();
        if (clazz == null) {
            return null;
        }

        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(clazz);
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
                        if (clazz.isInstance(bean)) {
                            String beanName = ComponentUtils.resolveBeanName(rule, "corsProcessor");
                            items.add(extractCorsInfo(beanName, bean));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            Object[] processors = registry.getBeansOfType((Class<?>) clazz);
            if (processors != null) {
                int index = 1;
                for (Object processor : processors) {
                    items.add(extractCorsInfo("corsProcessor" + (index > 1 ? index : ""), processor));
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
                .setDescription("Cross-Origin Resource Sharing (CORS) Security Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractCorsInfo(String beanName, @NonNull Object processor) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = processor.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        info.put("allowedOrigins", toList(ComponentUtils.invokeObject(processor, "getAllowedOrigins")));
        info.put("allowedMethods", toList(ComponentUtils.invokeObject(processor, "getAllowedMethods")));
        info.put("allowedHeaders", toList(ComponentUtils.invokeObject(processor, "getAllowedHeaders")));
        info.put("exposedHeaders", toList(ComponentUtils.invokeObject(processor, "getExposedHeaders")));
        info.put("allowCredentials", ComponentUtils.invokeBoolean(processor, "isAllowCredentials", "getAllowCredentials"));
        info.put("maxAgeSeconds", ComponentUtils.invokeNumber(processor, "getMaxAgeSeconds", "getMaxAge"));

        return info;
    }

    private List<String> toList(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        if (obj instanceof String[] arr) {
            return Arrays.asList(arr);
        }
        if (obj instanceof Collection<?> col) {
            List<String> list = new ArrayList<>();
            for (Object item : col) {
                if (item != null) {
                    list.add(item.toString());
                }
            }
            return list;
        }
        if (obj instanceof String str && !str.isBlank()) {
            return List.of(str);
        }
        return Collections.emptyList();
    }

}
