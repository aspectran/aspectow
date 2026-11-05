/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.console.framework;

import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.node.management.anatomy.AnatomyDataService;
import com.aspectran.aspectow.node.management.commands.CommandResponseParameters;
import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInspectorService;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonParser;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Controller that provides framework component specification data for the viewer,
 * supporting both the local node and remote node groups across the cluster.
 *
 * <p>Created: 2026-11-04</p>
 */
@Component("/framework/components")
@Profile("[console.ui, console.custom-ui]")
@Bean("componentsActivity")
public class ComponentsActivity {

    private static final Logger logger = LoggerFactory.getLogger(ComponentsActivity.class);

    private final NodeManager nodeManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    private final RemoteCommandManager remoteCommandManager;

    private final ComponentInspectorService componentInspectorService;

    private final Map<String, Object> componentsCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code ComponentsActivity} with the specified dependencies.
     * @param nodeManager the node manager
     * @param nodeConsoleHelper the node console helper
     * @param remoteCommandManager the remote command manager
     */
    @Autowired
    public ComponentsActivity(NodeManager nodeManager,
                              NodeConsoleHelper nodeConsoleHelper,
                              RemoteCommandManager remoteCommandManager) {
        this.nodeManager = nodeManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
        this.remoteCommandManager = remoteCommandManager;
        this.componentInspectorService = new ComponentInspectorService();
    }

    /**
     * Dispatches to the components viewer page.
     * @param translet the active translet
     * @return the model map containing attributes for rendering the view
     */
    @Request
    @Dispatch("framework/components/viewer")
    @Action("page")
    public Map<String, Object> index(@NonNull Translet translet) {
        return viewer(translet, null);
    }

    /**
     * Dispatches to the components viewer page with a specified context.
     * @param translet the active translet
     * @param contextName the name of the context
     * @return the model map containing attributes for rendering the view
     */
    @Request("/${contextName}")
    @Dispatch("framework/components/viewer")
    @Action("page")
    public Map<String, Object> viewer(@NonNull Translet translet, String contextName) {
        String group = translet.getParameter("group");
        List<Map<String, Object>> groups = getAvailableGroups();

        List<String> allContextNames;
        if (StringUtils.hasText(group) && !isLocalGroup(group)) {
            allContextNames = fetchRemoteContextNames(group);
        } else {
            allContextNames = AnatomyDataService.getContextNames();
        }

        if (contextName == null || !allContextNames.contains(contextName)) {
            if (!allContextNames.isEmpty()) {
                contextName = allContextNames.getFirst();
            }
        }
        if (contextName == null) {
            contextName = "0";
        }

        String clusterMode = (nodeManager != null && nodeManager.getClusterConfig() != null)
                ? nodeManager.getClusterConfig().getMode() : null;
        String myNodeId = (nodeManager != null ? nodeManager.getNodeId() : null);
        String myGroupId = (nodeManager != null ? nodeManager.getGroupId() : null);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("title", "Framework Components");
        model.put("style", "components-page");
        model.put("group", "framework-menu");
        model.put("clusterMode", clusterMode);
        model.put("myNodeId", myNodeId);
        model.put("myGroupId", myGroupId);
        model.put("groups", groups);
        model.put("selectedGroup", group);
        model.put("allContextNames", allContextNames);
        model.put("contextName", contextName);
        return model;
    }

    /**
     * Retrieves the list of context names for the specified group or the local node.
     * @param translet the active translet
     * @return a REST response containing the list of context names
     */
    @Request("/contexts")
    public RestResponse contexts(@NonNull Translet translet) {
        String group = translet.getParameter("group");
        if (!StringUtils.hasText(group) || isLocalGroup(group)) {
            List<String> names = AnatomyDataService.getContextNames();
            return new DefaultRestResponse("allContextNames", names).ok();
        }

        try {
            List<String> names = fetchRemoteContextNames(group);
            return new DefaultRestResponse("allContextNames", names).ok();
        } catch (Exception e) {
            logger.error("Failed to fetch context names for group {}", group, e);
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Provides framework component specification data as JSON for the specified context and group.
     * @param translet the active translet
     * @param contextName the name of the context
     * @return a REST response containing the components data
     */
    @Request("/${contextName}/data")
    public RestResponse data(@NonNull Translet translet, String contextName) {
        String group = translet.getParameter("group");
        if (contextName == null) {
            contextName = translet.getParameter("contextName");
        }
        boolean refresh = "true".equalsIgnoreCase(translet.getParameter("refresh"));

        if (!StringUtils.hasText(group) || isLocalGroup(group)) {
            Map<String, ActivityContext> contexts = AnatomyDataService.prepareContextMap();
            ActivityContext context = (contextName != null ? contexts.get(contextName) : null);
            if (context == null && !contexts.isEmpty()) {
                context = contexts.values().iterator().next();
            }
            if (context == null) {
                return new DefaultRestResponse().notFound();
            }
            Map<String, ComponentDetails> data = componentInspectorService.inspectAll(context);
            return new DefaultRestResponse("componentsData", data).nullWritable(false).ok();
        }

        String cacheKey = group + ":" + (contextName != null ? contextName : "default");
        if (!refresh && componentsCache.containsKey(cacheKey)) {
            return new DefaultRestResponse("componentsData", componentsCache.get(cacheKey)).nullWritable(false).ok();
        }

        if (remoteCommandManager == null) {
            return new FailureResponse().setError("error", "RemoteCommandManager is not available");
        }

        String targetNodeId = findRepresentativeNodeId(group);
        if (targetNodeId == null) {
            return new FailureResponse().setError("error", "No active command manager nodes found in group: " + group);
        }

        try {
            CommandParameters command = new CommandParameters()
                    .setCommandName("components")
                    .setParameter("mode", "data")
                    .setParameter("contextName", contextName);

            CompletableFuture<CommandResponseParameters> future =
                    remoteCommandManager.executeRemote(targetNodeId, command, 10000);
            CommandResponseParameters response = future.get(10, TimeUnit.SECONDS);

            if (response.getError() != null) {
                return new FailureResponse().setError("error", response.getError());
            }

            String jsonResult = response.getResult();
            if (jsonResult == null) {
                return new FailureResponse().setError("error", "Empty response from remote node: " + targetNodeId);
            }

            Object parsed = JsonParser.parse(jsonResult);
            if (parsed instanceof Map<?, ?>) {
                componentsCache.put(cacheKey, parsed);
            }
            return new DefaultRestResponse("componentsData", parsed).nullWritable(false).ok();
        } catch (Exception e) {
            logger.error("Failed to retrieve components data from group: {}", group, e);
            return new FailureResponse().setError("error", "Failed to retrieve components data: " + e.getMessage());
        }
    }

    /**
     * Provides framework component specification data as JSON when contextName is not specified in path.
     * @param translet the active translet
     * @return a REST response containing the components data
     */
    @Request("/data")
    public RestResponse dataWithoutContextName(@NonNull Translet translet) {
        return data(translet, null);
    }

    /**
     * Clears cached components data.
     * @return a REST response indicating success
     */
    @Request("/cache/clear")
    public RestResponse clearCache() {
        componentsCache.clear();
        return new DefaultRestResponse("cleared", true).ok();
    }

    private boolean isLocalGroup(String group) {
        if (nodeManager == null || group == null) {
            return true;
        }
        return group.equals(nodeManager.getGroupId());
    }

    @NonNull
    private List<String> fetchRemoteContextNames(String group) {
        if (remoteCommandManager == null) {
            return Collections.emptyList();
        }
        String targetNodeId = findRepresentativeNodeId(group);
        if (targetNodeId == null) {
            logger.warn("No active command manager nodes found in group: {}", group);
            return Collections.emptyList();
        }

        try {
            CommandParameters command = new CommandParameters()
                    .setCommandName("components")
                    .setParameter("mode", "contexts");

            CompletableFuture<CommandResponseParameters> future =
                    remoteCommandManager.executeRemote(targetNodeId, command, 5000);
            CommandResponseParameters response = future.get(5, TimeUnit.SECONDS);

            if (response.getError() != null) {
                logger.error("Error from remote node {}: {}", targetNodeId, response.getError());
                return Collections.emptyList();
            }

            String result = response.getResult();
            if (result != null) {
                Object parsed = JsonParser.parse(result);
                if (parsed instanceof List<?> list) {
                    List<String> strList = new ArrayList<>(list.size());
                    for (Object item : list) {
                        if (item != null) {
                            strList.add(item.toString());
                        }
                    }
                    return strList;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch context names remotely from group: {}", group, e);
        }
        return Collections.emptyList();
    }

    @Nullable
    private String findRepresentativeNodeId(String groupName) {
        if (nodeConsoleHelper != null) {
            return nodeConsoleHelper.findRepresentativeNodeId(groupName);
        }
        return null;
    }

    @NonNull
    private List<Map<String, Object>> getAvailableGroups() {
        if (nodeConsoleHelper != null) {
            return nodeConsoleHelper.getAvailableGroups();
        }
        return Collections.emptyList();
    }

}
