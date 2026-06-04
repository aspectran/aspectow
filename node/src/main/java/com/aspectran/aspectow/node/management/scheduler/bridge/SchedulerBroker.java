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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * SchedulerBroker handles the distribution of scheduler management results
 * to connected clients.
 */
public class SchedulerBroker {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerBroker.class);

    public static final String CATEGORY_SCHEDULER = "scheduler";

    public static final String CONTROL_SUBSCRIBE = "subscribe:";

    public static final String CONTROL_RELEASE = "release:";

    public static final String CONTROL_REQUEST = "request:";

    public static final String DELIMITER = ":";

    private final SchedulerManager schedulerManager;

    private final NodeMessagePublisher messagePublisher;

    private final Set<SchedulerBridge> bridges = new CopyOnWriteArraySet<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    public SchedulerBroker(@NonNull SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
        this.messagePublisher = schedulerManager.getMessagePublisher();
    }

    public void addBridge(SchedulerBridge bridge) {
        bridges.add(bridge);
    }

    public void removeBridge(SchedulerBridge bridge) {
        bridges.remove(bridge);
    }

    public Set<SchedulerSession> getSessions() {
        Set<SchedulerSession> sessions = new HashSet<>();
        for (SchedulerBridge bridge : bridges) {
            bridge.getSessions(sessions);
        }
        return sessions;
    }

    public synchronized void subscribe(@NonNull SchedulerSession session) {
        if (session.isValid()) {
            boolean alreadyInUse = subscriptionRegistry.isInUse();
            subscriptionRegistry.addLocalSubscription(session.getId());
            String targetNodeId = session.getNodeId();
            if (schedulerManager.isSameNode(targetNodeId)) {
                if (!alreadyInUse) {
                    schedulerManager.startExporters();
                }
                // Send initial log lines to the new session
                for (String message : schedulerManager.collectLastMessages()) {
                    bridge(session, message);
                }
            } else if (schedulerManager.isGatewayMode()) {
                publishControl(targetNodeId, CONTROL_SUBSCRIBE + schedulerManager.getNodeId() + DELIMITER + session.getId());
            }
        }
    }

    public synchronized void subscribeRemotely(String nodeId, String sessionId) {
        logger.info("Received scheduler subscribe request from node: {}, session: {}", nodeId, sessionId);
        boolean alreadyInUse = subscriptionRegistry.isInUse();
        subscriptionRegistry.addRemoteSubscription(nodeId);
        if (!alreadyInUse) {
            schedulerManager.startExporters();
        }

        // Relay initial log lines back to the requester node
        if (messagePublisher != null) {
            String sourceNodeId = schedulerManager.getNodeId();
            for (String logMessage : schedulerManager.collectLastMessages()) {
                try {
                    String relayMessage = sourceNodeId + DELIMITER + logMessage;
                    if (sessionId != null) {
                        messagePublisher.publishRelay(
                                CATEGORY_SCHEDULER, nodeId, sessionId, relayMessage);
                    } else {
                        messagePublisher.publishRelay(
                                CATEGORY_SCHEDULER, nodeId, relayMessage);
                    }
                } catch (Exception e) {
                    logger.error("Failed to relay initial log message to node: {}", nodeId, e);
                }
            }
        }
    }

    public synchronized void release(@NonNull SchedulerSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        String targetNodeId = session.getNodeId();
        if (schedulerManager.isSameNode(targetNodeId)) {
            if (!subscriptionRegistry.isInUseLocally()) {
                schedulerManager.stopExporters();
            }
        } else if (schedulerManager.isGatewayMode()) {
            publishControl(targetNodeId, CONTROL_RELEASE + schedulerManager.getNodeId() + DELIMITER + session.getId());
        }
    }

    public synchronized void releaseRemotely(String nodeId) {
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

    /**
     * Bridges a scheduler result to all connected clients.
     * @param data the result payload to send
     */
    public void bridge(String data) {
        bridge(schedulerManager.getNodeId(), data);
    }

    /**
     * Bridges a scheduler result to all connected clients.
     * @param sourceNodeId the ID of the node where the message originated
     * @param data the result payload to send
     */
    public void bridge(String sourceNodeId, String data) {
        for (SchedulerBridge bridge : bridges) {
            try {
                bridge.bridge(sourceNodeId, data);
            } catch (Exception e) {
                logger.warn("Failed to bridge scheduler result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Bridges a scheduler result to a specific session.
     * @param session the target session
     * @param data the result payload to send
     */
    public void bridge(@NonNull SchedulerSession session, String data) {
        bridge(session, schedulerManager.getNodeId(), data);
    }

    /**
     * Bridges a scheduler result to a specific session.
     * @param session the target session
     * @param sourceNodeId the ID of the node where the message originated
     * @param data the result payload to send
     */
    public void bridge(@NonNull SchedulerSession session, String sourceNodeId, String data) {
        for (SchedulerBridge bridge : bridges) {
            try {
                bridge.bridge(session, sourceNodeId, data);
            } catch (Exception e) {
                logger.warn("Failed to bridge scheduler result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

}
