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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of MyBatis SqlSessionFactory components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class SqlSessionFactoryComponentInspector implements ComponentInspector {

    private static final String SQL_SESSION_FACTORY_INTERFACE = "org.apache.ibatis.session.SqlSessionFactory";

    @Override
    @NonNull
    public String getId() {
        return "sqlSessionFactory";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "MyBatis SqlSessionFactory";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Persistence";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-table";
    }

    @Nullable
    private Class<?> getSqlSessionFactoryClass() {
        try {
            return Class.forName(SQL_SESSION_FACTORY_INTERFACE);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        Class<?> clazz = getSqlSessionFactoryClass();
        if (clazz == null) {
            return false;
        }
        return ComponentUtils.hasLocalBean(context, clazz);
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        Class<?> clazz = getSqlSessionFactoryClass();
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
                            String beanName = ComponentUtils.resolveBeanName(rule, "sqlSessionFactory");
                            items.add(extractFactoryInfo(beanName, bean));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            Object[] factories = registry.getBeansOfType((Class<?>) clazz);
            if (factories != null) {
                int index = 1;
                for (Object factory : factories) {
                    items.add(extractFactoryInfo("sqlSessionFactory" + (index > 1 ? index : ""), factory));
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
                .setDescription("MyBatis SqlSessionFactory and Database Configuration")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractFactoryInfo(String beanName, @NonNull Object factory) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = factory.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        Object configuration = ComponentUtils.invokeObject(factory, "getConfiguration");
        if (configuration != null) {
            Object env = ComponentUtils.invokeObject(configuration, "getEnvironment");
            if (env != null) {
                info.put("environmentId", ComponentUtils.invokeString(env, "getId"));

                Object ds = ComponentUtils.invokeObject(env, "getDataSource");
                if (ds != null) {
                    info.put("dataSourceClassName", ds.getClass().getName());
                    info.put("dataSourceSimpleClassName", ds.getClass().getSimpleName());
                }

                Object txFactory = ComponentUtils.invokeObject(env, "getTransactionFactory");
                if (txFactory != null) {
                    info.put("transactionFactoryClassName", txFactory.getClass().getName());
                    info.put("transactionFactorySimpleClassName", txFactory.getClass().getSimpleName());
                }
            }

            Object mappedStatements = ComponentUtils.invokeObject(configuration, "getMappedStatements");
            if (mappedStatements instanceof Collection<?> col) {
                info.put("mappedStatementsCount", col.size());
            }

            Object typeAliasRegistry = ComponentUtils.invokeObject(configuration, "getTypeAliasRegistry");
            if (typeAliasRegistry != null) {
                Object aliases = ComponentUtils.invokeObject(typeAliasRegistry, "getTypeAliases");
                if (aliases instanceof Map<?, ?> map) {
                    info.put("typeAliasesCount", map.size());
                }
            }

            info.put("cacheEnabled", ComponentUtils.invokeBoolean(configuration, "isCacheEnabled"));
            info.put("lazyLoadingEnabled", ComponentUtils.invokeBoolean(configuration, "isLazyLoadingEnabled"));
            info.put("aggressiveLazyLoading", ComponentUtils.invokeBoolean(configuration, "isAggressiveLazyLoading"));
            info.put("multipleResultSetsEnabled", ComponentUtils.invokeBoolean(configuration, "isMultipleResultSetsEnabled"));
            info.put("useColumnLabel", ComponentUtils.invokeBoolean(configuration, "isUseColumnLabel"));
            info.put("useGeneratedKeys", ComponentUtils.invokeBoolean(configuration, "isUseGeneratedKeys"));
            info.put("defaultExecutorType", ComponentUtils.invokeString(configuration, "getDefaultExecutorType"));
            info.put("defaultStatementTimeout", ComponentUtils.invokeNumber(configuration, "getDefaultStatementTimeout"));
            info.put("autoMappingBehavior", ComponentUtils.invokeString(configuration, "getAutoMappingBehavior"));
            info.put("mapUnderscoreToCamelCase", ComponentUtils.invokeBoolean(configuration, "isMapUnderscoreToCamelCase"));
        }

        return info;
    }

}
