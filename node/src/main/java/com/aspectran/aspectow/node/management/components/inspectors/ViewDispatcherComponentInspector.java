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
import com.aspectran.core.activity.response.dispatch.ViewDispatcher;
import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.AspectRule;
import com.aspectran.core.context.rule.BeanRule;
import com.aspectran.core.context.rule.JoinpointRule;
import com.aspectran.core.context.rule.SettingsAdviceRule;
import com.aspectran.core.context.rule.params.JoinpointParameters;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of ViewDispatcher components.
 *
 * <p>Created: 2026-11-06</p>
 */
public class ViewDispatcherComponentInspector implements ComponentInspector {

    private static final Logger logger = LoggerFactory.getLogger(ViewDispatcherComponentInspector.class);

    @Override
    @NonNull
    public String getId() {
        return "viewDispatcher";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "View Dispatcher";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "View";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-layout-text-window-reverse";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        return ComponentUtils.hasLocalBean(context, ViewDispatcher.class);
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return null;
        }

        Map<String, List<String>> targetsMap = resolveDispatcherTargets(context);
        List<Map<String, Object>> items = new ArrayList<>();

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(ViewDispatcher.class);
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
                        if (bean instanceof ViewDispatcher vd) {
                            String beanName = ComponentUtils.resolveBeanName(rule, "viewDispatcher");
                            List<String> targets = targetsMap.get(beanName);
                            if (targets == null && rule.getId() != null) {
                                targets = targetsMap.get(rule.getId());
                            }
                            items.add(extractViewDispatcherInfo(beanName, vd, targets));
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to extract ViewDispatcher bean info: {}", rule.getId(), e);
                    }
                }
            }
        } else {
            ViewDispatcher[] dispatchers = registry.getBeansOfType(ViewDispatcher.class);
            if (dispatchers != null) {
                int index = 1;
                for (ViewDispatcher vd : dispatchers) {
                    String beanName = "viewDispatcher" + (index > 1 ? index : "");
                    List<String> targets = targetsMap.get(beanName);
                    items.add(extractViewDispatcherInfo(beanName, vd, targets));
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
                .setDescription("View Dispatcher Configurations and Layout Template Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, List<String>> resolveDispatcherTargets(@NonNull ActivityContext context) {
        Map<String, List<String>> targetsMap = new LinkedHashMap<>();
        if (context.getAspectRuleRegistry() != null) {
            Collection<AspectRule> aspectRules = context.getAspectRuleRegistry().getAspectRules();
            if (aspectRules != null) {
                for (AspectRule aspectRule : aspectRules) {
                    SettingsAdviceRule sar = aspectRule.getSettingsAdviceRule();
                    if (sar != null && sar.getSettings() != null) {
                        Object val = sar.getSetting(ViewDispatcher.VIEW_DISPATCHER_SETTING_NAME);
                        if (val != null) {
                            String dispatcherName = val.toString();
                            JoinpointRule joinpointRule = aspectRule.getJoinpointRule();
                            String targetStr = null;
                            if (joinpointRule != null) {
                                JoinpointParameters joinpointParameters = joinpointRule.getJoinpointParameters();
                                if (joinpointParameters != null) {
                                    targetStr = joinpointParameters.toString().trim();
                                } else {
                                    targetStr = joinpointRule.toString().trim();
                                }
                            }
                            if (targetStr != null && !targetStr.isEmpty()) {
                                targetsMap.computeIfAbsent(dispatcherName, k -> new ArrayList<>()).add(targetStr);
                            }
                        }
                    }
                }
            }
        }
        return targetsMap;
    }

    @NonNull
    private Map<String, Object> extractViewDispatcherInfo(
            String beanName,
            @NonNull ViewDispatcher vd,
            @Nullable List<String> targets
    ) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = vd.getClass();
        String simpleName = clazz.getSimpleName();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", simpleName);
        info.put("isSingleton", vd.isSingleton());
        info.put("contentType", vd.getContentType());

        // Target scopes (pointcuts)
        if (targets != null && !targets.isEmpty()) {
            info.put("targets", targets);
        }

        // View technology type
        String dispatcherType;
        if (simpleName.contains("JspTemplate")) {
            dispatcherType = "JSP Template";
        } else if (simpleName.contains("Jsp")) {
            dispatcherType = "JSP";
        } else if (simpleName.contains("Thymeleaf")) {
            dispatcherType = "Thymeleaf";
        } else if (simpleName.contains("FreeMarker")) {
            dispatcherType = "FreeMarker";
        } else if (simpleName.contains("Pebble")) {
            dispatcherType = "Pebble";
        } else {
            dispatcherType = simpleName;
        }
        info.put("dispatcherType", dispatcherType);

        // Prefix & Suffix
        String prefix = ComponentUtils.invokeString(vd, "getPrefix");
        if (prefix != null) {
            info.put("prefix", prefix);
        }
        String suffix = ComponentUtils.invokeString(vd, "getSuffix");
        if (suffix != null) {
            info.put("suffix", suffix);
        }

        // JspTemplateViewDispatcher specific properties
        String template = ComponentUtils.invokeString(vd, "getTemplate");
        if (template != null) {
            info.put("template", template);
        }
        String includePageKey = ComponentUtils.invokeString(vd, "getIncludePageKey");
        if (includePageKey != null) {
            info.put("includePageKey", includePageKey);
        }

        // Template engine inspection for engine-based dispatchers
        Object engine = ComponentUtils.invokeObject(vd, "getTemplateEngine");
        if (engine == null) {
            engine = ComponentUtils.getFieldValue(vd, "templateEngine");
        }
        if (engine == null) {
            engine = ComponentUtils.getFieldValue(vd, "configuration");
        }
        if (engine != null) {
            info.put("templateEngineClassName", engine.getClass().getName());
            info.put("templateEngineSimpleClassName", engine.getClass().getSimpleName());
        }

        return info;
    }

}
