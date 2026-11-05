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
package com.aspectran.aspectow.node.management.components;

import com.aspectran.aspectow.node.management.components.inspectors.ApplicationAdapterComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.AsyncTaskExecutorComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.CorsProcessorComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.DataSourceComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.EntityManagerFactoryComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.EnvironmentComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.EventPublisherComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.LocaleResolverComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.MessageSourceComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.MultipartComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.RedisClientComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.SchedulerComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.SessionManagerComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.SqlSessionFactoryComponentInspector;
import com.aspectran.aspectow.node.management.components.inspectors.TemplateRendererComponentInspector;
import com.aspectran.core.context.ActivityContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that coordinates component inspection across all registered inspectors
 * for a given ActivityContext.
 *
 * <p>Created: 2026-11-04</p>
 */
public class ComponentInspectorService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentInspectorService.class);

    private final List<ComponentInspector> inspectors = new ArrayList<>();

    public ComponentInspectorService() {
        registerDefaultInspectors();
    }

    private void registerDefaultInspectors() {
        inspectors.add(new ApplicationAdapterComponentInspector());
        inspectors.add(new EnvironmentComponentInspector());
        inspectors.add(new DataSourceComponentInspector());
        inspectors.add(new SqlSessionFactoryComponentInspector());
        inspectors.add(new EntityManagerFactoryComponentInspector());
        inspectors.add(new RedisClientComponentInspector());
        inspectors.add(new SessionManagerComponentInspector());
        inspectors.add(new TemplateRendererComponentInspector());
        inspectors.add(new SchedulerComponentInspector());
        inspectors.add(new AsyncTaskExecutorComponentInspector());
        inspectors.add(new EventPublisherComponentInspector());
        inspectors.add(new MultipartComponentInspector());
        inspectors.add(new LocaleResolverComponentInspector());
        inspectors.add(new MessageSourceComponentInspector());
        inspectors.add(new CorsProcessorComponentInspector());
    }

    /**
     * Registers a custom component inspector.
     * @param inspector the inspector to register
     */
    public void registerInspector(@NonNull ComponentInspector inspector) {
        inspectors.add(inspector);
    }

    /**
     * Inspects all available key components in the specified context.
     * @param context the activity context
     * @return a map of component ID to ComponentDetails
     */
    @NonNull
    public Map<String, ComponentDetails> inspectAll(@NonNull ActivityContext context) {
        Map<String, ComponentDetails> resultMap = new LinkedHashMap<>();
        for (ComponentInspector inspector : inspectors) {
            try {
                if (inspector.isAvailable(context)) {
                    ComponentDetails details = inspector.inspect(context);
                    if (details != null) {
                        details.setRefreshable(inspector.isRefreshable());
                        resultMap.put(inspector.getId(), details);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to inspect component with inspector [{}]", inspector.getId(), e);
            }
        }
        return resultMap;
    }

    /**
     * Inspects a specific component by its ID.
     * @param context the activity context
     * @param componentId the component ID
     * @return the ComponentDetails, or null if not found
     */
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context, @NonNull String componentId) {
        for (ComponentInspector inspector : inspectors) {
            if (inspector.getId().equals(componentId)) {
                try {
                    if (inspector.isAvailable(context)) {
                        ComponentDetails details = inspector.inspect(context);
                        if (details != null) {
                            details.setRefreshable(inspector.isRefreshable());
                        }
                        return details;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to inspect component [{}]", componentId, e);
                }
                return null;
            }
        }
        return null;
    }

}
