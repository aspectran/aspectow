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
package com.aspectran.aspectow.node.management.commands.bridge;

import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.commands.RemoteResponseParameters;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * CommandBroker handles the distribution of command results
 * to connected clients (via WebSockets or Polling).
 */
public class CommandBroker {

    private static final Logger logger = LoggerFactory.getLogger(CommandBroker.class);

    public static final String CATEGORY_COMMANDS = "commands";

    public static final String CONTROL_SUBSCRIBE = "subscribe:";

    public static final String CONTROL_RELEASE = "release:";

    public static final String CONTROL_REQUEST = "request:";

    public static final String DELIMITER = ":";

    private final RemoteCommandManager commandManager;

    private final NodeMessagePublisher messagePublisher;

    private final Set<CommandBridge> bridges = new CopyOnWriteArraySet<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    public CommandBroker(@NonNull RemoteCommandManager commandManager) {
        this.commandManager = commandManager;
        this.messagePublisher = commandManager.getMessagePublisher();
    }

    public void addBridge(CommandBridge bridge) {
        bridges.add(bridge);
    }

    public void removeBridge(CommandBridge bridge) {
        bridges.remove(bridge);
    }

    public Set<CommandSession> getSessions() {
        Set<CommandSession> sessions = new HashSet<>();
        for (CommandBridge bridge : bridges) {
            bridge.getSessions(sessions);
        }
        return sessions;
    }

    public synchronized void subscribe(@NonNull CommandSession session) {
        if (session.isValid()) {
            subscriptionRegistry.addLocalSubscription(session.getId());
            String targetNodeId = session.getNodeId();
            if (commandManager.isGatewayMode() && !commandManager.isSameNode(targetNodeId)) {
                publishControl(targetNodeId, CONTROL_SUBSCRIBE + commandManager.getNodeId() + DELIMITER + session.getId());
            }
        }
    }

    public synchronized void subscribeRemotely(String nodeId, String sessionId) {
        logger.info("Received command subscribe request from node: {}, session: {}", nodeId, sessionId);
        subscriptionRegistry.addRemoteSubscription(nodeId);
    }

    public synchronized void release(@NonNull CommandSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        String targetNodeId = session.getNodeId();
        if (commandManager.isGatewayMode() && !commandManager.isSameNode(targetNodeId)) {
            publishControl(targetNodeId, CONTROL_RELEASE + commandManager.getNodeId() + DELIMITER + session.getId());
        }
    }

    public synchronized void releaseRemotely(String nodeId) {
        logger.info("Received command release request from node: {}", nodeId);
        subscriptionRegistry.removeRemoteSubscription(nodeId);
    }

    private void publishControl(String targetNodeId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(CATEGORY_COMMANDS, targetNodeId, message);
            } catch (Exception e) {
                logger.error("Failed to publish control message to Redis", e);
            }
        }
    }

    /**
     * Bridges a command result to all connected clients.
     * @param resultParameters the command result parameters
     */
    public void bridge(RemoteResponseParameters resultParameters) {
        for (CommandBridge bridge : bridges) {
            try {
                bridge.bridge(resultParameters);
            } catch (Exception e) {
                logger.warn("Failed to bridge command result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Bridges a command result to a specific session.
     * @param session the target session
     * @param resultParameters the command result parameters
     */
    public void bridge(@NonNull CommandSession session, RemoteResponseParameters resultParameters) {
        for (CommandBridge bridge : bridges) {
            try {
                bridge.bridge(session, resultParameters);
            } catch (Exception e) {
                logger.warn("Failed to bridge command result via {}: {}",
                        bridge.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

}
