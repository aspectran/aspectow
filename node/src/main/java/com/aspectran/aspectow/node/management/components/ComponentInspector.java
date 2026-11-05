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

import com.aspectran.core.context.ActivityContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface for extracting specification and configuration details
 * of a key framework component.
 *
 * <p>Created: 2026-11-04</p>
 */
public interface ComponentInspector {

    /**
     * Returns the unique component identifier (e.g. "dataSource", "asyncTaskExecutor").
     * @return the component ID
     */
    @NonNull
    String getId();

    /**
     * Returns the human-readable display name (e.g. "Data Source", "Async Task Executor").
     * @return the display name
     */
    @NonNull
    String getDisplayName();

    /**
     * Returns the category of the component (e.g. "Persistence", "Concurrency", "Core").
     * @return the category
     */
    @NonNull
    String getCategory();

    /**
     * Returns the Bootstrap Icon class (e.g. "bi-database", "bi-cpu").
     * @return the icon class
     */
    @NonNull
    String getIcon();

    /**
     * Checks whether this component exists and is available in the given context.
     * @param context the activity context
     * @return true if available, false otherwise
     */
    boolean isAvailable(@NonNull ActivityContext context);

    /**
     * Returns whether this component provides dynamic runtime metrics
     * that can be refreshed on demand.
     * @return true if refreshable, false by default
     */
    default boolean isRefreshable() {
        return false;
    }

    /**
     * Extracts configuration and specification details from the context.
     * @param context the activity context
     * @return the component details, or null if not applicable
     */
    @Nullable
    ComponentDetails inspect(@NonNull ActivityContext context);

}
