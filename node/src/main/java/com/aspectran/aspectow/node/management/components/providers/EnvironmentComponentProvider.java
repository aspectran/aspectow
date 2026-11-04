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
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.env.ActivityEnvironment;
import com.aspectran.core.context.env.Environment;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Provider that extracts specification and configuration details of Environment components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class EnvironmentComponentProvider implements ComponentInfoProvider {

    @Override
    @NonNull
    public String getId() {
        return "environment";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Environment";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Core";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-gear-wide-connected";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        return (context.getEnvironment() != null);
    }

    @Override
    @Nullable
    public ComponentDetails extractDetails(@NonNull ActivityContext context) {
        Environment env = context.getEnvironment();
        if (env == null) {
            return null;
        }

        Map<String, Object> props = new LinkedHashMap<>();

        // Profiles
        String[] activeProfiles = env.getActiveProfiles();
        String[] defaultProfiles = env.getDefaultProfiles();
        props.put("activeProfiles", (activeProfiles != null ? Arrays.asList(activeProfiles) : List.of()));
        props.put("defaultProfiles", (defaultProfiles != null ? Arrays.asList(defaultProfiles) : List.of()));

        // System Properties Summary
        Map<String, String> sysProps = new LinkedHashMap<>();
        sysProps.put("java.version", System.getProperty("java.version"));
        sysProps.put("java.vendor", System.getProperty("java.vendor"));
        sysProps.put("os.name", System.getProperty("os.name"));
        sysProps.put("os.arch", System.getProperty("os.arch"));
        sysProps.put("file.encoding", System.getProperty("file.encoding"));
        sysProps.put("user.timezone", System.getProperty("user.timezone"));
        props.put("systemSummary", sysProps);

        // Application Properties (Sorted & Masked)
        Map<String, String> appProperties = new TreeMap<>();
        if (env instanceof ActivityEnvironment activityEnv) {
            java.util.Iterator<String> it = activityEnv.getPropertyNames();
            if (it != null) {
                while (it.hasNext()) {
                    String key = it.next();
                    Object val = activityEnv.getProperty(key);
                    appProperties.put(key, ComponentUtils.maskIfSensitive(key, val != null ? val.toString() : null));
                }
            }
        }
        props.put("properties", appProperties);

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(props);

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("Application Profiles, Property Sources, and Environment Settings")
                .setItems(items)
                .setProperties(props);
    }

}
