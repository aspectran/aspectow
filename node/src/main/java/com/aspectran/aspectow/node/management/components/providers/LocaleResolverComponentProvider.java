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
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.support.i18n.locale.LocaleResolver;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider that extracts specification and configuration details of LocaleResolver components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class LocaleResolverComponentProvider implements ComponentInfoProvider {

    @Override
    @NonNull
    public String getId() {
        return "localeResolver";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Locale Resolver";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Web";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-globe2";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        CoreService masterService = context.getMasterService();
        if (masterService != null && masterService.getLocaleResolver() != null) {
            return true;
        }
        return ComponentUtils.hasLocalBean(context, LocaleResolver.class);
    }

    @Override
    @Nullable
    public ComponentDetails extractDetails(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        List<Map<String, Object>> items = new ArrayList<>();

        CoreService masterService = context.getMasterService();
        LocaleResolver masterResolver = (masterService != null ? masterService.getLocaleResolver() : null);
        if (masterResolver != null) {
            items.add(extractResolverInfo("masterLocaleResolver", masterResolver));
        }

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(LocaleResolver.class);
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
                        if (bean instanceof LocaleResolver resolver && resolver != masterResolver) {
                            String beanName = ComponentUtils.resolveBeanName(rule, "localeResolver");
                            items.add(extractResolverInfo(beanName, resolver));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else if (registry != null) {
            LocaleResolver[] resolvers = registry.getBeansOfType(LocaleResolver.class);
            if (resolvers != null) {
                int index = 1;
                for (LocaleResolver resolver : resolvers) {
                    if (resolver != masterResolver) {
                        items.add(extractResolverInfo("localeResolver" + (index > 1 ? index : ""), resolver));
                        index++;
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
                .setDescription("Internationalization (i18n) Locale and TimeZone Resolution Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractResolverInfo(String beanName, @NonNull LocaleResolver resolver) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = resolver.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        info.put("defaultLocale", ComponentUtils.invokeString(resolver, "getDefaultLocale"));
        info.put("defaultTimeZone", ComponentUtils.invokeString(resolver, "getDefaultTimeZone"));

        Object supported = ComponentUtils.invokeObject(resolver, "getSupportedLocales");
        if (supported instanceof Collection<?> col) {
            List<String> list = new ArrayList<>();
            for (Object obj : col) {
                if (obj != null) {
                    list.add(obj.toString());
                }
            }
            info.put("supportedLocales", list);
        } else if (supported instanceof Object[] arr) {
            List<String> list = new ArrayList<>();
            for (Object obj : arr) {
                if (obj != null) {
                    list.add(obj.toString());
                }
            }
            info.put("supportedLocales", list);
        }

        // Cookie specific attributes
        Object cookieGenerator = ComponentUtils.invokeObject(resolver, "getLocaleCookieGenerator");
        if (cookieGenerator != null) {
            info.put("cookieName", ComponentUtils.invokeString(cookieGenerator, "getCookieName"));
            info.put("cookieMaxAge", ComponentUtils.invokeNumber(cookieGenerator, "getCookieMaxAge"));
            info.put("cookiePath", ComponentUtils.invokeString(cookieGenerator, "getCookiePath"));
            info.put("cookieDomain", ComponentUtils.invokeString(cookieGenerator, "getCookieDomain"));
            info.put("cookieSecure", ComponentUtils.invokeBoolean(cookieGenerator, "isCookieSecure"));
            info.put("cookieHttpOnly", ComponentUtils.invokeBoolean(cookieGenerator, "isCookieHttpOnly"));
            info.put("cookieSameSite", ComponentUtils.invokeString(cookieGenerator, "getCookieSameSite"));
        }

        return info;
    }

}
