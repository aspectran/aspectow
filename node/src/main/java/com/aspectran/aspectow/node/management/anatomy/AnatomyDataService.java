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
package com.aspectran.aspectow.node.management.anatomy;

import com.aspectran.core.component.bean.BeanRuleRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.component.bean.event.EventListenerRegistry;
import com.aspectran.core.component.bean.event.ListenerMethod;
import com.aspectran.core.component.converter.TypeConverter;
import com.aspectran.core.component.template.DefaultTemplateRenderer;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.AspectRule;
import com.aspectran.core.context.rule.BeanRule;
import com.aspectran.core.context.rule.ScheduleRule;
import com.aspectran.core.context.rule.TemplateRule;
import com.aspectran.core.context.rule.TransletRule;
import com.aspectran.core.context.rule.converter.RulesToParameters;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.AponLines;
import com.aspectran.utils.apon.AponWriter;
import com.aspectran.utils.apon.Parameters;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service that collects and formats framework anatomy data from ActivityContext instances.
 *
 * <p>Created: 2026-11-03</p>
 */
public class AnatomyDataService {

    /**
     * Prepares a map of context names to their corresponding activity contexts
     * available within the local JVM.
     * @return a map of context names to activity contexts
     */
    @NonNull
    public static Map<String, ActivityContext> prepareContextMap() {
        List<CoreService> services = new ArrayList<>(CoreServiceHolder.getAllServices());
        Collections.reverse(services);

        Map<String, ActivityContext> contextMap = new LinkedHashMap<>();
        Set<ActivityContext> seenContexts = new LinkedHashSet<>();
        int index = 0;
        for (CoreService service : services) {
            if (!service.isDerived() || service.getContextName() != null) {
                ActivityContext context = service.getActivityContext();
                if (seenContexts.add(context)) {
                    String name = service.getContextName();
                    if (StringUtils.isEmpty(name)) {
                        name = Integer.toString(index);
                    }
                    contextMap.putIfAbsent(name, context);
                }
                index++;
            }
        }
        return contextMap;
    }

    /**
     * Retrieves all context names available within the local JVM.
     * @return a list of context names
     */
    @NonNull
    public static List<String> getContextNames() {
        return new ArrayList<>(prepareContextMap().keySet());
    }

    /**
     * Retrieves the ActivityContext associated with the specified context name.
     * @param contextName the context name
     * @return the ActivityContext, or null if not found
     */
    @Nullable
    public static ActivityContext getContext(String contextName) {
        return prepareContextMap().get(contextName);
    }

    /**
     * Retrieves all framework anatomy data, including translets, beans, aspects, schedules,
     * templates, and type converters, formatted for presentation.
     * @param context the current activity context
     * @return a map containing categorized framework anatomy data
     */
    public Map<String, Object> getAnatomyData(ActivityContext context) {
        Assert.notNull(context, "context must not be null");

        Map<String, Object> anatomyData = new LinkedHashMap<>();

        // 1. Translet Rules
        if (context.getTransletRuleRegistry() != null) {
            Map<String, TransletRule> ruleMap = context.getTransletRuleRegistry().getTransletRuleMap();
            List<Map<String, Object>> ruleData = ruleMap.entrySet().stream()
                    .map(entry -> convertTransletRuleToApon(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            anatomyData.put("Translet Rules", ruleData);
        }

        // 2. Bean Rules
        if (context.getBeanRegistry() instanceof DefaultBeanRegistry registry) {
            BeanRuleRegistry beanRuleRegistry = registry.getBeanRuleRegistry();
            Set<BeanRule> allBeanRules = new HashSet<>(beanRuleRegistry.getIdBasedBeanRules());
            beanRuleRegistry.getTypeBasedBeanRules().forEach(allBeanRules::addAll);
            allBeanRules.addAll(beanRuleRegistry.getConfigurableBeanRules());

            List<Map<String, Object>> ruleData = allBeanRules.stream()
                    .map(this::convertBeanRuleToApon)
                    .collect(Collectors.toList());
            anatomyData.put("Bean Rules", ruleData);

            // Event Listeners
            EventListenerRegistry eventListenerRegistry = registry.getEventListenerRegistry();
            if (eventListenerRegistry != null) {
                Map<Class<?>, List<ListenerMethod>> listenerMap = eventListenerRegistry.getListenerMap();
                List<Map<String, Object>> listenerData = listenerMap.entrySet().stream()
                        .map(entry -> convertEventListenerToApon(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());
                anatomyData.put("Event Listeners", listenerData);
            }
        }

        // 3. Aspect Rules
        if (context.getAspectRuleRegistry() != null) {
            Collection<AspectRule> rules = context.getAspectRuleRegistry().getAspectRules();
            List<Map<String, Object>> ruleData = rules.stream()
                    .map(this::convertAspectRuleToApon)
                    .collect(Collectors.toList());
            anatomyData.put("Aspect Rules", ruleData);
        }

        // 4. Schedule Rules
        if (context.getScheduleRuleRegistry() != null) {
            Collection<ScheduleRule> rules = context.getScheduleRuleRegistry().getScheduleRules();
            List<Map<String, Object>> ruleData = rules.stream()
                    .map(this::convertScheduleRuleToApon)
                    .collect(Collectors.toList());
            anatomyData.put("Schedule Rules", ruleData);
        }

        // 5. Template Rules
        if (context.getTemplateRenderer() instanceof DefaultTemplateRenderer renderer) {
            Collection<TemplateRule> rules = renderer.getTemplateRules();
            List<Map<String, Object>> ruleData = rules.stream()
                    .map(this::convertTemplateRuleToApon)
                    .collect(Collectors.toList());
            anatomyData.put("Template Rules", ruleData);
        }

        // 6. Type Converters
        if (context.getTypeConverterRegistry() != null) {
            Map<Class<?>, TypeConverter<?>> converters = context.getTypeConverterRegistry().getConverters();
            List<Map<String, Object>> ruleData = converters.entrySet().stream()
                    .map(entry -> convertTypeConverterToApon(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            anatomyData.put("Type Converters", ruleData);
        }

        return anatomyData;
    }

    @NonNull
    private Map<String, Object> convertTransletRuleToApon(String ruleName, @NonNull TransletRule rule) {
        Parameters params = RulesToParameters.toTransletParameters(rule);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", ruleName);
        map.put("apon", paramsToString(params));
        return map;
    }

    @NonNull
    private Map<String, Object> convertBeanRuleToApon(@NonNull BeanRule rule) {
        Parameters params = RulesToParameters.toBeanParameters(rule);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("className", rule.getClassName());
        map.put("apon", paramsToString(params));
        return map;
    }

    @NonNull
    private Map<String, Object> convertAspectRuleToApon(@NonNull AspectRule rule) {
        Parameters params = RulesToParameters.toAspectParameters(rule);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("apon", paramsToString(params));
        return map;
    }

    @NonNull
    private Map<String, Object> convertScheduleRuleToApon(@NonNull ScheduleRule rule) {
        Parameters params = RulesToParameters.toScheduleParameters(rule);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("apon", paramsToString(params));
        return map;
    }

    @NonNull
    private Map<String, Object> convertTemplateRuleToApon(@NonNull TemplateRule rule) {
        Parameters params = RulesToParameters.toTemplateParameters(rule);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("apon", paramsToString(params));
        return map;
    }

    @NonNull
    private Map<String, Object> convertTypeConverterToApon(@NonNull Class<?> type, @NonNull TypeConverter<?> converter) {
        AponLines apon = new AponLines();
        apon.line("type", type.getName());
        apon.line("converter", converter.getClass().getName());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", type.getName());
        map.put("apon", apon.toString());
        return map;
    }

    @NonNull
    private Map<String, Object> convertEventListenerToApon(@NonNull Class<?> eventType, @NonNull List<ListenerMethod> listenerMethods) {
        AponLines apon = new AponLines();
        apon.array(eventType.getName());
        for (ListenerMethod listenerMethod : listenerMethods) {
            apon.line(listenerMethod.toString());
        }
        apon.end();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", eventType.getName());
        map.put("apon", apon.toString());
        return map;
    }

    private String paramsToString(Parameters params) {
        try {
            String apon = new AponWriter().nullWritable(false).write(params).toString();
            return maskSensitiveApon(apon);
        } catch (IOException e) {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Masks sensitive data in the APON configuration string.
     * @param apon the APON configuration string
     * @return the masked APON string
     */
    public static String maskSensitiveApon(String apon) {
        if (apon == null) {
            return null;
        }
        String regex = "(?i)([\\w\\-\\.]*(?:password|secret|passphrase|privatekey|secretkey)[\\w\\-\\.]*)\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|[^\\s,{}#\\[\\]\\(\\)]+)";
        return apon.replaceAll(regex, "$1: ********");
    }

}
