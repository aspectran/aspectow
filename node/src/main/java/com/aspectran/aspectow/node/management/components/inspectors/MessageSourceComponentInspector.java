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
import com.aspectran.core.support.i18n.message.AbstractMessageSource;
import com.aspectran.core.support.i18n.message.MessageSource;
import com.aspectran.core.support.i18n.message.ResourceBundleMessageSource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of MessageSource components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class MessageSourceComponentInspector implements ComponentInspector {

    @Override
    @NonNull
    public String getId() {
        return "messageSource";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Message Source";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Core";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-translate";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        return ComponentUtils.hasLocalBean(context, MessageSource.class);
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
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(MessageSource.class);
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
                        if (bean instanceof MessageSource ms) {
                            String beanName = ComponentUtils.resolveBeanName(rule, "messageSource");
                            items.add(extractMessageSourceInfo(beanName, ms));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            MessageSource[] sources = registry.getBeansOfType(MessageSource.class);
            if (sources != null) {
                int index = 1;
                for (MessageSource ms : sources) {
                    items.add(extractMessageSourceInfo("messageSource" + (index > 1 ? index : ""), ms));
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
                .setDescription("Internationalization (i18n) and Resource Bundle Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractMessageSourceInfo(String beanName, @NonNull MessageSource ms) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = ms.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        if (ms instanceof ResourceBundleMessageSource rbms) {
            String[] basenames = rbms.getBasenames();
            if (basenames != null && basenames.length > 0) {
                info.put("basenames", Arrays.asList(basenames));
            }
            info.put("defaultEncoding", rbms.getDefaultEncoding());
            info.put("cacheMillis", rbms.getCacheMillis());
            info.put("fallbackToSystemLocale", rbms.isFallbackToSystemLocale());
        }

        if (ms instanceof AbstractMessageSource ams) {
            info.put("useCodeAsDefaultMessage", ams.isUseCodeAsDefaultMessage());
        } else {
            info.put("useCodeAsDefaultMessage", ComponentUtils.invokeBoolean(ms, "isUseCodeAsDefaultMessage"));
        }

        return info;
    }

}
