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

import com.aspectran.aspectow.node.management.scheduler.SchedulerManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final String DELIMITER = ":";

    private final SchedulerManager schedulerManager;

    private final NodeMessagePublisher messagePublisher;

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    public SchedulerBroker(@NonNull SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
        this.messagePublisher = schedulerManager.getMessagePublisher();
    }

    public synchronized boolean subscribe(@NonNull SchedulerSession session) {
        if (session.isValid()) {
            boolean alreadyInUse = subscriptionRegistry.isInUse();
            subscriptionRegistry.addLocalSubscription(session.getId());
            String targetNodeId = session.getNodeId();
            if (schedulerManager.isSameNode(targetNodeId)) {
                if (!alreadyInUse) {
                    schedulerManager.startExporters();
                }
                return true;
            } else if (schedulerManager.isGatewayMode()) {
                publishControl(targetNodeId, CONTROL_SUBSCRIBE + schedulerManager.getNodeId() + DELIMITER + session.getId());
            }
        }
        return false;
    }

    public synchronized void subscribeRemotely(String nodeId, String sessionId) {
        logger.info("Received scheduler subscribe request from node: {}, session: {}", nodeId, sessionId);
        boolean alreadyInUse = subscriptionRegistry.isInUse();
        subscriptionRegistry.addRemoteSubscription(nodeId);
        if (!alreadyInUse) {
            schedulerManager.startExporters();
        }
    }

    public synchronized void unsubscribe(@NonNull SchedulerSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        String targetNodeId = session.getNodeId();
        if (schedulerManager.isSameNode(targetNodeId)) {
            if (!subscriptionRegistry.isInUse()) {
                schedulerManager.stopExporters();
            }
        } else if (schedulerManager.isGatewayMode()) {
            publishControl(targetNodeId, CONTROL_UNSUBSCRIBE + schedulerManager.getNodeId() + DELIMITER + session.getId());
        }
    }

    public synchronized void unsubscribeRemotely(String nodeId) {
        logger.info("Received scheduler release request from node: {}", nodeId);
        subscriptionRegistry.removeRemoteSubscription(nodeId);
        if (!subscriptionRegistry.isInUse()) {
            schedulerManager.stopExporters();
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
            schedulerManager.bridge(nodeId, sid, message);
        }

        if (locally) {
            Set<String> remoteNodeIds = subscriptionRegistry.getRemoteSubscriptions();
            if (!remoteNodeIds.isEmpty()) {
                for (String nid : remoteNodeIds) {
                    publishRelay(nid, message);
                }
            }
        }
    }

    public void bridge(String message, boolean locally) {
        Set<String> sessionIds = subscriptionRegistry.getAllSessionIds();
        for (String sid : sessionIds) {
            schedulerManager.bridge(sid, message);
        }

        if (locally) {
            Set<String> remoteNodeIds = subscriptionRegistry.getRemoteSubscriptions();
            if (!remoteNodeIds.isEmpty()) {
                for (String nodeId : remoteNodeIds) {
                    publishRelay(nodeId, message);
                }
            }
        }
    }

    public void bridgeRemotely(String sessionId, String message) {
        Set<String> remoteNodeIds = subscriptionRegistry.getRemoteSubscriptions();
        if (!remoteNodeIds.isEmpty()) {
            for (String nodeId : remoteNodeIds) {
                publishRelay(nodeId, sessionId, message);
            }
        }
    }

}
