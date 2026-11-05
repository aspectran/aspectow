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

import com.aspectran.aspectow.node.management.anatomy.AnatomyDataService;
import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInspector;
import com.aspectran.aspectow.node.management.components.ComponentUtils;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inspector that extracts specification and configuration details of ApplicationAdapter components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class ApplicationAdapterComponentInspector implements ComponentInspector {

    @Override
    @NonNull
    public String getId() {
        return "applicationAdapter";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Application Adapter";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Core";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-window-stack";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        return (context.getApplicationAdapter() != null);
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        ApplicationAdapter adapter = context.getApplicationAdapter();
        if (adapter == null) {
            return null;
        }

        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = adapter.getClass();

        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        CoreService coreService = AnatomyDataService.findCoreService(context);
        CoreService rootService = (coreService != null ? coreService.getRootService() : null);
        if (rootService == null) {
            rootService = coreService;
        }

        if (rootService != null) {
            info.put("serviceName", rootService.getServiceName());
            info.put("serviceClass", rootService.getClass().getName());
            info.put("serviceType", rootService.getClass().getSimpleName());

            String envType;
            String envDesc;
            boolean servletEnv = false;
            String serverInfo = null;

            if (rootService.isWebService()) {
                envType = "Web";
                servletEnv = true;

                Object servletContext = ComponentUtils.invokeObject(rootService, "getServletContext");
                if (servletContext == null) {
                    servletContext = ComponentUtils.invokeObject(coreService, "getServletContext");
                }
                if (servletContext != null) {
                    serverInfo = ComponentUtils.invokeString(servletContext, "getServerInfo");
                    info.put("serverInfo", serverInfo);
                    info.put("servletContextName", ComponentUtils.invokeString(servletContext, "getServletContextName"));
                }

                if (serverInfo != null && !serverInfo.isBlank()) {
                    envDesc = "Servlet Web Environment (" + serverInfo + ")";
                } else if (rootService.getClass().getName().contains("Netty")) {
                    envDesc = "Netty Web Environment";
                } else if (rootService.getClass().getName().contains("Tow")) {
                    envDesc = "Undertow Web Environment";
                } else {
                    envDesc = "Servlet Web Environment";
                }
            } else if (rootService.isDaemonService()) {
                envType = "Daemon";
                envDesc = "Background Daemon Service";
            } else if (rootService.isShellService()) {
                envType = "Shell";
                envDesc = "Interactive Command-Line Shell";
            } else {
                envType = "Standard";
                envDesc = "Standard Standalone Core Service";
            }

            info.put("environmentType", envType);
            info.put("environmentDescription", envDesc);
            info.put("servletEnvironment", servletEnv);
        }

        String basePath = adapter.getBasePathString();
        info.put("basePath", basePath);

        Set<String> attributeNames = adapter.getAttributeNames();
        if (attributeNames != null) {
            info.put("attributeCount", attributeNames.size());
            info.put("attributeNames", new ArrayList<>(attributeNames));
        } else {
            info.put("attributeCount", 0);
            info.put("attributeNames", Collections.emptyList());
        }

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(info);

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("Application Base Path, Execution Environment, and Application-scoped Attribute Specifications")
                .setItems(items)
                .setProperties(info);
    }

}
