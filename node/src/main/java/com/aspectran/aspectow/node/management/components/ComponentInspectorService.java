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

import com.aspectran.aspectow.node.management.components.providers.ApplicationAdapterComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.AsyncTaskExecutorComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.CorsProcessorComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.DataSourceComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.EnvironmentComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.EventPublisherComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.LocaleResolverComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.MessageSourceComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.MultipartComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.RedisClientComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.SchedulerComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.SessionManagerComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.SqlSessionFactoryComponentProvider;
import com.aspectran.aspectow.node.management.components.providers.TemplateRendererComponentProvider;
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
 * Service that coordinates component inspection across all registered providers
 * for a given ActivityContext.
 *
 * <p>Created: 2026-11-04</p>
 */
public class ComponentInspectorService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentInspectorService.class);

    private final List<ComponentInfoProvider> providers = new ArrayList<>();

    public ComponentInspectorService() {
        registerDefaultProviders();
    }

    private void registerDefaultProviders() {
        providers.add(new ApplicationAdapterComponentProvider());
        providers.add(new EnvironmentComponentProvider());
        providers.add(new DataSourceComponentProvider());
        providers.add(new SqlSessionFactoryComponentProvider());
        providers.add(new RedisClientComponentProvider());
        providers.add(new SessionManagerComponentProvider());
        providers.add(new TemplateRendererComponentProvider());
        providers.add(new SchedulerComponentProvider());
        providers.add(new AsyncTaskExecutorComponentProvider());
        providers.add(new EventPublisherComponentProvider());
        providers.add(new MultipartComponentProvider());
        providers.add(new LocaleResolverComponentProvider());
        providers.add(new MessageSourceComponentProvider());
        providers.add(new CorsProcessorComponentProvider());
    }

    /**
     * Registers a custom component info provider.
     * @param provider the provider to register
     */
    public void registerProvider(@NonNull ComponentInfoProvider provider) {
        providers.add(provider);
    }

    /**
     * Inspects all available key components in the specified context.
     * @param context the activity context
     * @return a map of component ID to ComponentDetails
     */
    @NonNull
    public Map<String, ComponentDetails> inspectAll(@NonNull ActivityContext context) {
        Map<String, ComponentDetails> resultMap = new LinkedHashMap<>();
        for (ComponentInfoProvider provider : providers) {
            try {
                if (provider.isAvailable(context)) {
                    ComponentDetails details = provider.extractDetails(context);
                    if (details != null) {
                        resultMap.put(provider.getId(), details);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to inspect component with provider [{}]", provider.getId(), e);
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
        for (ComponentInfoProvider provider : providers) {
            if (provider.getId().equals(componentId)) {
                try {
                    if (provider.isAvailable(context)) {
                        return provider.extractDetails(context);
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
