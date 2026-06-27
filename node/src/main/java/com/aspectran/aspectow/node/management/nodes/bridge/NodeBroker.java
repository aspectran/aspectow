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
package com.aspectran.aspectow.node.management.nodes.bridge;

import com.aspectran.aspectow.node.management.nodes.RemoteNodeManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NodeBroker handles the distribution of node management events
 * to connected clients (via WebSockets or Polling).
 */
public class NodeBroker {

    private static final Logger logger = LoggerFactory.getLogger(NodeBroker.class);

    public static final String CATEGORY_NODES = "nodes";

    public static final String CONTROL_SUBSCRIBE = "subscribe:";

    public static final String CONTROL_RELEASE = "release:";

    public static final String CONTROL_REQUEST = "request:";

    public static final String DELIMITER = ":";

    private final RemoteNodeManager nodeManager;

    private final NodeMessagePublisher messagePublisher;

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    /**
     * Instantiates a new NodeBroker.
     * @param nodeManager the remote node manager
     */
    public NodeBroker(@NonNull RemoteNodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.messagePublisher = nodeManager.getMessagePublisher();
    }

    /**
     * Subscribes a local session to node events.
     * @param session the node session
     */
    public synchronized void subscribe(@NonNull NodeSession session) {
        if (session.isValid()) {
            subscriptionRegistry.addLocalSubscription(session.getId());
            String targetNodeId = session.getNodeId();
            if (nodeManager.isGatewayMode() && !nodeManager.isSameNode(targetNodeId)) {
                publishControl(targetNodeId, CONTROL_SUBSCRIBE + nodeManager.getNodeId() + DELIMITER + session.getId());
            }
        }
    }

    /**
     * Subscribes a remote node to node events.
     * @param nodeId the remote node ID
     * @param sessionId the remote session ID
     */
    public synchronized void subscribeRemotely(String nodeId, String sessionId) {
        logger.info("Received node subscribe request from node: {}, session: {}", nodeId, sessionId);
        subscriptionRegistry.addRemoteSubscription(nodeId);
    }

    /**
     * Unsubscribes a local session from node events.
     * @param session the node session
     */
    public synchronized void unsubscribe(@NonNull NodeSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        String targetNodeId = session.getNodeId();
        if (nodeManager.isGatewayMode() && !nodeManager.isSameNode(targetNodeId)) {
            publishControl(targetNodeId, CONTROL_RELEASE + nodeManager.getNodeId() + DELIMITER + session.getId());
        }
    }

    /**
     * Unsubscribes a remote node from node events.
     * @param nodeId the remote node ID
     */
    public synchronized void unsubscribeRemotely(String nodeId) {
        logger.info("Received node unsubscribe request from node: {}", nodeId);
        subscriptionRegistry.removeRemoteSubscription(nodeId);
    }

    private void publishControl(String targetNodeId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(CATEGORY_NODES, targetNodeId, message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to Redis", e);
            }
        }
    }

}
