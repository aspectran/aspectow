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
package com.aspectran.aspectow.console.commands;

import com.aspectran.aspectow.console.cluster.NodeConsoleHelper;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RemoteCommandsActivity provides views and REST API endpoints for managing
 * cluster nodes and executing remote file commands.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component("/cluster/commands")
public class RemoteCommandsActivity {

    private final NodeManager nodeManager;

    private final NodeConsoleHelper nodeConsoleHelper;

    @Autowired
    public RemoteCommandsActivity(NodeManager nodeManager,
                                  NodeConsoleHelper nodeConsoleHelper) {
        this.nodeManager = nodeManager;
        this.nodeConsoleHelper = nodeConsoleHelper;
    }

    /**
     * Displays the node commands page.
     * @param nodeId the node ID
     * @return a map of attributes for rendering the view
     */
    @Request
    @Dispatch("cluster/commands")
    @Action("page")
    public Map<String, Object> commands(String nodeId) {
        String clusterMode = nodeManager.getClusterConfig().getMode();
        List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
        NodeInfo nodeInfo = (nodeId != null ? nodeManager.getNodeInfoHolder().getNodeInfo(nodeId) : null);
        if (nodeId != null && nodeInfo == null) {
            throw new IllegalArgumentException("No node found with ID: " + nodeId);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Remote Commands");
        model.put("style", "commands-page");
        model.put("group", "cluster-menu");
        model.put("clusterMode", clusterMode);
        model.put("myNodeId", nodeManager.getNodeId());
        model.put("nodes", nodes);
        if (nodeInfo != null) {
            model.put("node", nodeConsoleHelper.createNodeMap(nodeInfo, true, true));
        }
        return model;
    }

    /**
     * Lists all registered nodes with their current status.
     * @return a list of node information maps
     */
    @Request("/list")
    public RestResponse listNodes() {
        try {
            List<Map<String, Object>> nodes = nodeConsoleHelper.getNodes(true);
            return new SuccessResponse(nodes).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

}
