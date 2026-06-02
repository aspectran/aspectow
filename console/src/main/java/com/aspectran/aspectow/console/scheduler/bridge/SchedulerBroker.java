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
package com.aspectran.aspectow.console.scheduler.bridge;

import com.aspectran.aspectow.console.scheduler.bridge.polling.PollingSchedulerBridge;
import com.aspectran.aspectow.console.scheduler.bridge.websocket.WebsocketSchedulerBridge;
import com.aspectran.aspectow.console.scheduler.manager.SchedulerManager;
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

    public static final String CONTROL_SUBSCRIBE = "scheduler:subscribe";

    public static final String CONTROL_RELEASE = "scheduler:release";

    private final NodeMessagePublisher messagePublisher;

    private final SchedulerManager schedulerManager;

    private final Set<SchedulerBridge> bridges = new CopyOnWriteArraySet<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    public SchedulerBroker(NodeMessagePublisher messagePublisher, SchedulerManager schedulerManager) {
        this.messagePublisher = messagePublisher;
        this.schedulerManager = schedulerManager;
    }

    public NodeMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    public SubscriptionRegistry getSubscriptionRegistry() {
        return subscriptionRegistry;
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
            if (bridge instanceof WebsocketSchedulerBridge websocketBridge) {
                websocketBridge.getSessions(sessions);
            } else if (bridge instanceof PollingSchedulerBridge pollingBridge) {
                sessions.addAll(pollingBridge.getSessions());
            }
        }
        return sessions;
    }

    public synchronized void subscribe(@NonNull SchedulerSession session) {
        if (session.isValid()) {
            subscriptionRegistry.addLocalSubscription(session.getId());
            String targetNodeId = session.getNodeId();
            if (schedulerManager.getNodeId().equals(targetNodeId)) {
                schedulerManager.startExporters();
                // Send initial log lines to the new session
                for (String message : schedulerManager.collectLastMessages()) {
                    bridge(session, message);
                }
            } else {
                publishControl(targetNodeId, CONTROL_SUBSCRIBE + ":" + session.getId());
            }
        }
    }

    public synchronized void release(@NonNull SchedulerSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        String targetNodeId = session.getNodeId();
        if (schedulerManager.getNodeId().equals(targetNodeId)) {
            if (!subscriptionRegistry.isInUseLocally()) {
                schedulerManager.stopExporters();
            }
        } else {
            publishControl(targetNodeId, CONTROL_RELEASE + ":" + session.getId());
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
        if (messagePublisher != null && !subscriptionRegistry.getRemoteSubscriptions().isEmpty()) {
            String relayMessage = sourceNodeId + "\0" + data;
            for (String remoteNodeId : subscriptionRegistry.getRemoteSubscriptions()) {
                try {
                    messagePublisher.publishRelay(CATEGORY_SCHEDULER, remoteNodeId, relayMessage);
                } catch (Exception e) {
                    logger.error("Failed to relay scheduler result to remote node: {}", remoteNodeId, e);
                }
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
        if (messagePublisher != null && session.isRemote()) {
            try {
                String relayMessage = sourceNodeId + "\0" + data;
                messagePublisher.publishRelay(CATEGORY_SCHEDULER, session.getNodeId(), session.getId(), relayMessage);
            } catch (Exception e) {
                logger.error("Failed to relay scheduler result to remote session: {}", session.getId(), e);
            }
        }
    }

}
