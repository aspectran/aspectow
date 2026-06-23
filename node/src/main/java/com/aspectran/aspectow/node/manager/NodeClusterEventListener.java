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
package com.aspectran.aspectow.node.manager;

import com.aspectran.aspectow.node.config.NodeInfo;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClusterEventListener implementation for NodeManager.
 *
 * <p>Created: 2026-06-22</p>
 */
public class NodeClusterEventListener implements ClusterEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeClusterEventListener.class);

    private final NodeManager nodeManager;

    public NodeClusterEventListener(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public void onNodeJoined(@NonNull NodeInfo info) {
        if (nodeManager.getNodeId().equals(info.getId())) {
            return;
        }
        // 1. Validate Token
        try {
            nodeManager.validateToken(info.getToken());
        } catch (Exception e) {
            logger.warn("Rejected join request from node '{}' due to invalid token", info.getId());
            return;
        }

        if (nodeManager.getClusterConfig().isGatewayMode()) {
            NodeInfo existingInfo = nodeManager.getNodeInfoHolder().getNodeInfo(info.getId());
            if (existingInfo != null) {
                // 2. Partial update for Gateway Mode: keep static config, update dynamic state
                // Create a new NodeInfo instance to ensure atomic update for readers
                NodeInfo newInfo = existingInfo.copyWithUpdatedState(info);
                nodeManager.getNodeInfoHolder().putNodeInfo(newInfo);
                if (logger.isDebugEnabled()) {
                    logger.debug("Updated dynamic state for joined node: {}", info.getId());
                }
            } else {
                // 3. Full update for dynamic join
                nodeManager.getNodeInfoHolder().putNodeInfo(info);
                if (logger.isDebugEnabled()) {
                    logger.debug("Added new node info for joined node: {}", info.getId());
                }
            }
        }
    }

    @Override
    public void onNodeLeft(String leftNodeId) {
        if (nodeManager.getNodeId().equals(leftNodeId)) {
            return;
        }
        if (nodeManager.getClusterConfig().isGatewayMode()) {
            NodeInfo existingInfo = nodeManager.getNodeInfoHolder().getNodeInfo(leftNodeId);
            if (existingInfo != null) {
                // Clone and update status to 'offline' for atomic update
                NodeInfo newInfo = existingInfo.copy();
                newInfo.setStatus("offline");

                nodeManager.getNodeInfoHolder().putNodeInfo(newInfo);
                if (logger.isDebugEnabled()) {
                    logger.debug("Set node status to 'offline' for left node: {}", leftNodeId);
                }
            }
        }
    }

}
