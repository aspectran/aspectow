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
package com.aspectran.aspectow.node.management.commands;

import com.aspectran.aspectow.node.management.commands.bridge.CommandBroker;
import com.aspectran.aspectow.node.management.commands.remote.RemoteCommandMessageListener;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aspectran.aspectow.node.management.commands.bridge.CommandBroker.DELIMITER;

/**
 * RemoteCommandManager orchestrates remote daemon command execution across the cluster.
 * It manages local execution, remote dispatching via Redis, and broadcasting
 * results to connected clients.
 */
public class RemoteCommandManager implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCommandManager.class);

    private final NodeManager nodeManager;

    private final NodeMessagePublisher messagePublisher;

    private final LocalCommandService localCommandService;

    private final CommandBroker broker;

    public RemoteCommandManager(@NonNull NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.messagePublisher = nodeManager.getNodeMessagePublisher();
        this.localCommandService = new LocalCommandService();
        this.broker = new CommandBroker(getNodeId(), messagePublisher, this);
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RemoteCommandManager for node: {}", getNodeId());

        // Register a listener for command relay messages (commands and results) from Redis
        if (nodeManager.getNodeMessageSubscriber() != null) {
            RemoteCommandMessageListener bridgeHandler = new RemoteCommandMessageListener(this);
            nodeManager.getNodeMessageSubscriber().addListener(bridgeHandler);
        }
    }

    public NodeMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    public boolean isSameNode(String targetNodeId) {
        return (targetNodeId != null && targetNodeId.equals(getNodeId()));
    }

    public synchronized void startExporters() {
        // Future implementation: Start command-related exporters (e.g., log streaming)
    }

    public synchronized void stopExporters() {
        // Future implementation: Stop command-related exporters
    }

    public CommandBroker getBroker() {
        return broker;
    }

    /**
     * Dispatches a command request to a specific node or handles it locally.
     * @param request the command request parameters
     */
    public void process(@NonNull RemoteCommandParameters request) {
        String targetNodeId = request.getTargetNodeId();
        if (StringUtils.isEmpty(targetNodeId)) {
            targetNodeId = getNodeId();
        }
        if (isSameNode(targetNodeId)) {
            Thread.ofVirtual().start(() -> {
                try {
                    CommandParameters commandParameters = request.getCommand();
                    if (commandParameters != null) {
                        logger.debug("Executing local daemon command: {}", commandParameters);
                        String response = localCommandService.execute(commandParameters.toString());
                        if (response != null) {
                            broadcast(response);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to execute local daemon command", e);
                }
            });
        } else {
            dispatch(targetNodeId, request);
        }
    }

    private void dispatch(String targetNodeId, @NonNull RemoteCommandParameters request) {
        if (messagePublisher != null) {
            try {
                request.setSourceNodeId(getNodeId());
                String message = CommandBroker.CONTROL_REQUEST + request;
                messagePublisher.publishControl(CommandBroker.CATEGORY_COMMANDS, targetNodeId, message);
                logger.debug("Daemon command dispatched to node {}: {}", targetNodeId, request.getCommand());
            } catch (Exception e) {
                logger.error("Failed to dispatch daemon command to node {}", targetNodeId, e);
            }
        } else {
            logger.warn("Cannot dispatch command to node {}: Redis publisher not available", targetNodeId);
        }
    }

    /**
     * Processes an incoming message received from the cluster relay.
     */
    public void processRemotely(RemoteCommandParameters request) {
        if (request == null) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            try {
                String response = execute(request);
                if (response != null && messagePublisher != null) {
                    String requesterNodeId = request.getSourceNodeId();
                    String sessionId = request.getSessionId();
                    String relayMessage = getNodeId() + DELIMITER + response;
                    if (requesterNodeId != null) {
                        if (sessionId != null) {
                            messagePublisher.publishRelay(
                                    CommandBroker.CATEGORY_COMMANDS, requesterNodeId, sessionId, relayMessage);
                        } else {
                            messagePublisher.publishRelay(
                                    CommandBroker.CATEGORY_COMMANDS, requesterNodeId, relayMessage);
                        }
                    } else {
                        messagePublisher.publishRelay(CommandBroker.CATEGORY_COMMANDS, relayMessage);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to process daemon command request: {}", request, e);
            }
        });
    }

    @Nullable
    private String execute(RemoteCommandParameters request) {
        CommandParameters commandParameters = request.getCommand();
        if (commandParameters != null) {
            return localCommandService.execute(commandParameters.toString());
        }
        return null;
    }

    /**
     * Broadcasts a command execution result to all connected clients on this node.
     * @param response the result payload
     */
    public void broadcast(String response) {
        broadcast(getNodeId(), response);
    }

    /**
     * Broadcasts a command execution result to all connected clients on this node.
     * @param sourceNodeId the ID of the node where the message originated
     * @param response the result payload
     */
    public void broadcast(String sourceNodeId, String response) {
        if (logger.isTraceEnabled()) {
            logger.trace("Broadcasting command result (source: {}) to local clients: {}", sourceNodeId, response);
        }
        if (broker != null) {
            broker.bridge(sourceNodeId, response);
        }
    }

}
