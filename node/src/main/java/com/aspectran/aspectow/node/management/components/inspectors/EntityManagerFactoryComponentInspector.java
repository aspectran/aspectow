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
 * Inspector that extracts specification and configuration details of JPA
 * {@code EntityManagerFactory} components and QueryDSL {@code EntityQuery} instances.
 *
 * <p>Created: 2026-11-04</p>
 */
public class EntityManagerFactoryComponentInspector implements ComponentInspector {

    private static final String ENTITY_MANAGER_FACTORY_INTERFACE = "jakarta.persistence.EntityManagerFactory";

    private static final String ENTITY_QUERY_INTERFACE = "com.aspectran.jpa.querydsl.EntityQuery";

    @Override
    @NonNull
    public String getId() {
        return "entityManagerFactory";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "JPA EntityManagerFactory";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Persistence";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-diagram-3";
    }

    @Nullable
    private Class<?> getEntityManagerFactoryClass() {
        try {
            return Class.forName(ENTITY_MANAGER_FACTORY_INTERFACE);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Nullable
    private Class<?> getEntityQueryClass() {
        try {
            return Class.forName(ENTITY_QUERY_INTERFACE);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        Class<?> clazz = getEntityManagerFactoryClass();
        if (clazz == null) {
            return false;
        }
        return ComponentUtils.hasLocalBean(context, clazz);
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        Class<?> clazz = getEntityManagerFactoryClass();
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
                            String beanName = ComponentUtils.resolveBeanName(rule, "entityManagerFactory");
                            items.add(extractFactoryInfo(beanName, bean, rule, defaultBeanRegistry, context));
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
                    items.add(extractFactoryInfo("entityManagerFactory" + (index > 1 ? index : ""), factory, null, registry, context));
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
                .setDescription("JPA (Java Persistence API) EntityManagerFactory Specifications, Entities, and QueryDSL Configurations")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractFactoryInfo(String beanName,
                                                   @NonNull Object factory,
                                                   @Nullable BeanRule rule,
                                                   @NonNull BeanRegistry registry,
                                                   @NonNull ActivityContext context) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = factory.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        if (rule != null && rule.getBeanClass() != null) {
            info.put("factoryBeanClassName", rule.getBeanClass().getName());
            info.put("factoryBeanSimpleClassName", rule.getBeanClass().getSimpleName());
        }

        // Status
        Boolean isOpen = ComponentUtils.invokeBoolean(factory, "isOpen");
        info.put("open", (isOpen == null || isOpen));

        // Persistence properties
        Map<?, ?> rawProps = (Map<?, ?>) ComponentUtils.invokeObject(factory, "getProperties");
        String puName = null;
        if (rawProps != null) {
            Object pun = rawProps.get("jakarta.persistence.persistenceUnitName");
            if (pun == null) {
                pun = rawProps.get("hibernate.ejb.persistenceUnitName");
            }
            if (pun == null) {
                pun = rawProps.get("hibernate.persistenceUnitName");
            }
            if (pun != null) {
                puName = pun.toString();
            }
        }
        if (puName == null) {
            puName = ComponentUtils.invokeString(factory, "getPersistenceUnitName");
        }
        info.put("persistenceUnitName", (puName != null ? puName : beanName));

        // Metamodel & Managed Entities
        Object metamodel = ComponentUtils.invokeObject(factory, "getMetamodel");
        List<Map<String, Object>> entitiesList = new ArrayList<>();
        if (metamodel != null) {
            Object entities = ComponentUtils.invokeObject(metamodel, "getEntities");
            if (entities instanceof Collection<?> col) {
                for (Object entityType : col) {
                    Map<String, Object> entityInfo = new LinkedHashMap<>();
                    String entityName = ComponentUtils.invokeString(entityType, "getName");
                    Object javaType = ComponentUtils.invokeObject(entityType, "getJavaType");
                    String javaTypeName = (javaType instanceof Class<?> c ? c.getName() : (javaType != null ? javaType.toString() : null));
                    String simpleName = (javaType instanceof Class<?> c ? c.getSimpleName() : (entityName != null ? entityName : "Entity"));

                    entityInfo.put("name", (entityName != null ? entityName : simpleName));
                    entityInfo.put("className", javaTypeName);
                    entityInfo.put("simpleClassName", simpleName);
                    entitiesList.add(entityInfo);
                }
            }
        }
        entitiesList.sort((a, b) -> String.valueOf(a.get("simpleClassName")).compareToIgnoreCase(String.valueOf(b.get("simpleClassName"))));
        info.put("entityCount", entitiesList.size());
        info.put("entities", entitiesList);

        // Key JPA / Hibernate Properties
        Map<String, Object> keyProps = new LinkedHashMap<>();
        if (rawProps != null) {
            copyProp(rawProps, keyProps, "hibernate.dialect", "dialect");
            copyProp(rawProps, keyProps, "hibernate.hbm2ddl.auto", "hbm2ddlAuto");
            copyProp(rawProps, keyProps, "jakarta.persistence.schema-generation.database.action", "schemaGenAction");
            copyProp(rawProps, keyProps, "hibernate.show_sql", "showSql");
            copyProp(rawProps, keyProps, "hibernate.format_sql", "formatSql");
            copyProp(rawProps, keyProps, "hibernate.transaction.coordinator_class", "transactionCoordinator");
            copyProp(rawProps, keyProps, "jakarta.persistence.jdbc.driver", "driverClass");
            copyProp(rawProps, keyProps, "jakarta.persistence.jdbc.url", "jdbcUrl");
        }
        info.put("properties", keyProps);

        // QueryDSL EntityQuery integration
        Class<?> queryClass = getEntityQueryClass();
        if (queryClass != null && ComponentUtils.hasLocalBean(context, queryClass)) {
            List<Map<String, Object>> queryList = new ArrayList<>();
            if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
                BeanRule[] queryRules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(queryClass);
                if (queryRules != null) {
                    for (BeanRule qRule : queryRules) {
                        try {
                            Object qBean = defaultBeanRegistry.getBean(qRule);
                            if (qBean == null && qRule.getId() != null) {
                                qBean = defaultBeanRegistry.getBean(qRule.getId());
                            }
                            if (qBean == null && qRule.getBeanClass() != null) {
                                qBean = defaultBeanRegistry.getBean(qRule.getBeanClass());
                            }
                            if (qBean != null) {
                                String qName = ComponentUtils.resolveBeanName(qRule, "entityQuery");
                                queryList.add(extractEntityQueryInfo(qName, qBean));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            if (!queryList.isEmpty()) {
                info.put("entityQueries", queryList);
            }
        }

        return info;
    }

    @NonNull
    private Map<String, Object> extractEntityQueryInfo(String beanName, @NonNull Object query) {
        Map<String, Object> qInfo = new LinkedHashMap<>();
        Class<?> clazz = query.getClass();

        qInfo.put("beanName", beanName);
        qInfo.put("className", clazz.getName());
        qInfo.put("simpleClassName", clazz.getSimpleName());

        Object txAspectId = ComponentUtils.getFieldValue(query, "txAspectId");
        qInfo.put("txAspectId", (txAspectId != null ? txAspectId.toString() : null));

        Object templates = ComponentUtils.getFieldValue(query, "templates");
        qInfo.put("templates", (templates != null ? templates.getClass().getSimpleName() : "Default"));

        return qInfo;
    }

    private void copyProp(@NonNull Map<?, ?> src, Map<String, Object> dest, String srcKey, String destKey) {
        Object val = src.get(srcKey);
        if (val != null) {
            if (val instanceof Class<?> clazz) {
                dest.put(destKey, clazz.getName());
            } else if (val instanceof CharSequence || val instanceof Number || val instanceof Boolean) {
                dest.put(destKey, val);
            } else {
                dest.put(destKey, val.toString());
            }
        }
    }

}
