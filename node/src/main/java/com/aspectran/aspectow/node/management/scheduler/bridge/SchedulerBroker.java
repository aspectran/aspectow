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
package com.aspectran.aspectow.node.management.scheduler.bridge;

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.management.scheduler.RemoteSchedulerManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * SchedulerBroker handles the distribution of scheduler management results
 * to connected clients.
 */
public class SchedulerBroker {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerBroker.class);

    public static final String CATEGORY_SCHEDULER = "scheduler";

    public static final String CONTROL_SUBSCRIBE = "subscribe:";

    public static final String CONTROL_UNSUBSCRIBE = "unsubscribe:";

    public static final String CONTROL_REQUEST = "request:";

    private final RemoteSchedulerManager remoteSchedulerManager;

    private final NodeMessagePublisher messagePublisher;

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    /**
     * Constructs a new SchedulerBroker.
     * @param remoteSchedulerManager the scheduler manager to associate with this broker
     */
    public SchedulerBroker(@NonNull RemoteSchedulerManager remoteSchedulerManager) {
        this.remoteSchedulerManager = remoteSchedulerManager;
        this.messagePublisher = remoteSchedulerManager.getMessagePublisher();
    }

    /**
     * Subscribes a session to scheduler management updates across all nodes.
     * @param session the session to subscribe
     * @return true if the subscription was successful, false otherwise
     */
    public synchronized boolean subscribe(@NonNull SchedulerSession session) {
        if (session.isValid()) {
            boolean alreadyInUse = subscriptionRegistry.isInUse();
            subscriptionRegistry.addSession(session.getId());
            subscriptionRegistry.addLocalSubscription(session.getId());
            if (!alreadyInUse) {
                remoteSchedulerManager.startExporters();
            }
            if (remoteSchedulerManager.isGatewayMode()) {
                subscribeAllRemoteNodes(session.getId());
            }
            return true;
        }
        return false;
    }

    /**
     * Sends a subscription request to all remote nodes in the cluster for the given session.
     * @param sessionId the session identifier
     */
    public void subscribeAllRemoteNodes(String sessionId) {
        if (remoteSchedulerManager.isGatewayMode() && remoteSchedulerManager.getNodeManager() != null) {
            List<NodeInfo> nodes = remoteSchedulerManager.getNodeManager().getNodeInfoList();
            for (NodeInfo node : nodes) {
                if (!remoteSchedulerManager.isSameNode(node.getId())) {
                    subscribeRemoteNode(node.getId(), sessionId);
                }
            }
        }
    }

    /**
     * Sends a subscription request to a single remote node for the given session.
     * @param targetNodeId the target node ID
     * @param sessionId the session identifier
     */
    public void subscribeRemoteNode(String targetNodeId, String sessionId) {
        if (remoteSchedulerManager.isGatewayMode()) {
            publishControl(targetNodeId, CONTROL_SUBSCRIBE + remoteSchedulerManager.getNodeId() + (sessionId != null ? ":" + sessionId : ""));
        }
    }

    /**
     * Sends a subscription request to a single remote node.
     * @param targetNodeId the target node ID
     */
    public void subscribeRemoteNode(String targetNodeId) {
        subscribeRemoteNode(targetNodeId, null);
    }

    /**
     * Registers a remote node subscription.
     * @param nodeId the ID of the remote node
     * @param sessionId the ID of the remote session
     */
    public synchronized void subscribeRemotely(String nodeId, String sessionId) {
        logger.info("Received scheduler subscribe request from node: {}, session: {}", nodeId, sessionId);
        boolean alreadyInUse = subscriptionRegistry.isInUse();
        subscriptionRegistry.addRemoteSubscription(nodeId);
        if (!alreadyInUse) {
            remoteSchedulerManager.startExporters();
        }
    }

    /**
     * Unsubscribes a session from scheduler management updates.
     * @param session the session to unsubscribe
     */
    public synchronized void unsubscribe(@NonNull SchedulerSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        if (!subscriptionRegistry.isInUse()) {
            remoteSchedulerManager.stopExporters();
        }
        if (remoteSchedulerManager.isGatewayMode()) {
            unsubscribeAllRemoteNodes(session.getId());
        }
    }

    /**
     * Sends an unsubscribe request to all remote nodes for the given session.
     * @param sessionId the session identifier
     */
    public void unsubscribeAllRemoteNodes(String sessionId) {
        if (remoteSchedulerManager.isGatewayMode() && remoteSchedulerManager.getNodeManager() != null) {
            List<NodeInfo> nodes = remoteSchedulerManager.getNodeManager().getNodeInfoList();
            for (NodeInfo node : nodes) {
                if (!remoteSchedulerManager.isSameNode(node.getId())) {
                    publishControl(node.getId(), CONTROL_UNSUBSCRIBE + remoteSchedulerManager.getNodeId() + (sessionId != null ? ":" + sessionId : ""));
                }
            }
        }
    }

    /**
     * Unsubscribes a remote node.
     * @param nodeId the ID of the remote node to unsubscribe
     */
    public synchronized void unsubscribeRemotely(String nodeId) {
        logger.info("Received scheduler release request from node: {}", nodeId);
        subscriptionRegistry.removeRemoteSubscription(nodeId);
        if (!subscriptionRegistry.isInUse()) {
            remoteSchedulerManager.stopExporters();
        }
    }

    private void publishControl(String targetNodeId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(CATEGORY_SCHEDULER, targetNodeId, message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to Redis", e);
            }
        }
    }

    private void publishRelay(String targetNodeId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(CATEGORY_SCHEDULER, targetNodeId, message);
            } catch (Exception e) {
                logger.error("Failed to publish relay message to node: {}", targetNodeId, e);
            }
        }
    }

    private void publishRelay(String targetNodeId, String sessionId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(CATEGORY_SCHEDULER, targetNodeId, sessionId, message);
            } catch (Exception e) {
                logger.error("Failed to publish relay message to node: {}", targetNodeId, e);
            }
        }
    }

    /**
     * Bridges a scheduler result to all connected clients.
     * @param message the result payload to send
     */
    public void bridgeLog(String nodeId, String message, boolean locally) {
        Set<String> sessionIds = subscriptionRegistry.getAllSessionIds();
        for (String sid : sessionIds) {
            remoteSchedulerManager.bridge(nodeId, sid, message);
        }

        if (locally) {
            Set<String> remoteNodeIds = subscriptionRegistry.getRemoteSubscriptions();
            for (String nid : remoteNodeIds) {
                publishRelay(nid, message);
            }
        }
    }

    /**
     * Bridges a scheduler response message to connected local sessions, and optionally
     * relays it to remote nodes.
     * @param message the message payload
     * @param locally if true, also relays the message to remote nodes
     */
    public void bridge(String message, boolean locally) {
        Set<String> sessionIds = subscriptionRegistry.getAllSessionIds();
        for (String sid : sessionIds) {
            remoteSchedulerManager.bridge(sid, message);
        }

        if (locally) {
            Set<String> remoteNodeIds = subscriptionRegistry.getRemoteSubscriptions();
            for (String nodeId : remoteNodeIds) {
                publishRelay(nodeId, message);
            }
        }
    }

    /**
     * Bridges a scheduler response message to remote nodes.
     * @param sessionId the session ID
     * @param message the message payload
     */
    public void bridgeRemotely(String sessionId, String message) {
        Set<String> remoteNodeIds = subscriptionRegistry.getRemoteSubscriptions();
        if (!remoteNodeIds.isEmpty()) {
            for (String nodeId : remoteNodeIds) {
                publishRelay(nodeId, sessionId, message);
            }
        }
    }

    /**
     * Bridges a scheduler response message to a specific remote node and session.
     * @param nodeId the remote node ID
     * @param sessionId the remote session ID
     * @param message the message payload
     */
    public void bridgeRemotely(String nodeId, String sessionId, String message) {
        publishRelay(nodeId, sessionId, message);
    }

}
