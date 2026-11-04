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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Provider that extracts specification and configuration details of DataSource components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class DataSourceComponentProvider implements ComponentInfoProvider {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceComponentProvider.class);

    @Override
    @NonNull
    public String getId() {
        return "dataSource";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Data Source";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Persistence";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-database";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        BeanRegistry beanRegistry = context.getBeanRegistry();
        return (beanRegistry != null && beanRegistry.containsBean(DataSource.class));
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
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(DataSource.class);
            if (rules != null) {
                for (BeanRule rule : rules) {
                    try {
                        String beanId = rule.getId();
                        Object bean = (beanId != null ? defaultBeanRegistry.getBean(beanId) : null);
                        if (bean instanceof DataSource ds) {
                            items.add(extractDataSourceInfo(beanId, ds));
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to extract DataSource bean info: {}", rule.getId(), e);
                    }
                }
            }
        }

        if (items.isEmpty()) {
            DataSource[] dataSources = registry.getBeansOfType(DataSource.class);
            if (dataSources != null) {
                int index = 1;
                for (DataSource ds : dataSources) {
                    items.add(extractDataSourceInfo("dataSource" + (index > 1 ? index : ""), ds));
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
                .setDescription("Database Connection Pool Configurations and Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractDataSourceInfo(String beanName, @NonNull DataSource ds) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = ds.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        // Basic connection specifications
        info.put("jdbcUrl", ComponentUtils.invokeString(ds, "getJdbcUrl", "getUrl"));
        info.put("driverClassName", ComponentUtils.invokeString(ds, "getDriverClassName", "getDriverClass"));

        String username = ComponentUtils.invokeString(ds, "getUsername", "getUser");
        if (username == null || username.isBlank()) {
            Object propsObj = ComponentUtils.invokeObject(ds, "getDataSourceProperties", "getConnectionProperties");
            if (propsObj instanceof Properties props) {
                username = props.getProperty("user");
                if (username == null || username.isBlank()) {
                    username = props.getProperty("username");
                }
            } else if (propsObj instanceof Map<?, ?> map) {
                Object userVal = map.get("user");
                if (userVal == null) {
                    userVal = map.get("username");
                }
                if (userVal != null) {
                    username = userVal.toString();
                }
            }
        }
        info.put("username", username);

        // Pool capacity
        info.put("maximumPoolSize", ComponentUtils.invokeNumber(ds, "getMaximumPoolSize", "getMaxTotal", "getMaxActive"));
        info.put("minimumIdle", ComponentUtils.invokeNumber(ds, "getMinimumIdle", "getMinIdle"));

        // Timeouts (ms)
        info.put("connectionTimeout", ComponentUtils.invokeNumber(ds, "getConnectionTimeout", "getMaxWaitMillis", "getMaxWait"));
        info.put("idleTimeout", ComponentUtils.invokeNumber(ds, "getIdleTimeout", "getTimeBetweenEvictionRunsMillis"));
        info.put("maxLifetime", ComponentUtils.invokeNumber(ds, "getMaxLifetime", "getMinEvictableIdleTimeMillis"));

        // Connection attributes
        info.put("autoCommit", ComponentUtils.invokeBoolean(ds, "isAutoCommit", "getDefaultAutoCommit"));
        info.put("readOnly", ComponentUtils.invokeBoolean(ds, "isReadOnly", "getDefaultReadOnly"));
        info.put("transactionIsolation", ComponentUtils.invokeString(ds, "getTransactionIsolationName", "getTransactionIsolation", "getDefaultTransactionIsolation"));
        info.put("poolName", ComponentUtils.invokeString(ds, "getPoolName", "getName"));

        return info;
    }

}
