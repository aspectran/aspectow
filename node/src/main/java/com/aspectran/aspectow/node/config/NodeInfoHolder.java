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
package com.aspectran.aspectow.node.config;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A holder for managing a collection of {@link NodeInfo} objects.
 * Provides convenient and thread-safe access to node information.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class NodeInfoHolder {

    private final Map<String, NodeInfo> nodeInfoMap = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Instantiates a new NodeInfoHolder.
     */
    public NodeInfoHolder() {
    }

    /**
     * Instantiates a new NodeInfoHolder.
     * @param nodeInfoList the list of node information to hold
     */
    public NodeInfoHolder(@NonNull List<NodeInfo> nodeInfoList) {
        for (NodeInfo info : nodeInfoList) {
            nodeInfoMap.put(info.getId(), info);
        }
    }

    /**
     * Gets the list of all held node information.
     * @return a list of {@link NodeInfo} objects
     */
    public List<NodeInfo> getNodeInfoList() {
        synchronized (nodeInfoMap) {
            return new ArrayList<>(nodeInfoMap.values());
        }
    }

    /**
     * Checks if a node with the specified name exists.
     * @param nodeId the name of the node
     * @return {@code true} if the node exists, {@code false} otherwise
     */
    public boolean hasNode(String nodeId) {
        return nodeInfoMap.containsKey(nodeId);
    }

    /**
     * Returns the node configuration for the specified node ID.
     * @param nodeId the node identifier
     * @return the node information, or {@code null} if not found
     */
    public NodeInfo getNodeInfo(String nodeId) {
        return nodeInfoMap.get(nodeId);
    }

    /**
     * Adds or updates node information.
     * @param nodeInfo the node information to add or update
     */
    public void putNodeInfo(NodeInfo nodeInfo) {
        nodeInfoMap.put(nodeInfo.getId(), nodeInfo);
    }

    /**
     * Removes the node information for the specified node ID.
     * @param nodeId the node identifier
     */
    public void removeNode(String nodeId) {
        nodeInfoMap.remove(nodeId);
    }

}
