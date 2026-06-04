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

import com.aspectran.aspectow.node.config.NodeInfo;
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
        this.broker = new CommandBroker(this);
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

    public boolean isGatewayMode() {
        return (messagePublisher != null);
    }

    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    public boolean isSameNode(String targetNodeId) {
        return (targetNodeId != null && targetNodeId.equals(getNodeId()));
    }

    public CommandBroker getBroker() {
        return broker;
    }

    /**
     * Dispatches a command request to a specific node or handles it locally.
     * @param request the command request parameters
     */
    public void process(@NonNull RemoteCommandParameters request) {
        if (request.isTargetAll()) {
            for (NodeInfo nodeInfo : nodeManager.getNodeInfoList()) {
                dispatchToNode(nodeInfo.getId(), request);
            }
        } else if (StringUtils.hasText(request.getTargetGroup())) {
            String targetGroup = request.getTargetGroup();
            for (NodeInfo nodeInfo : nodeManager.getNodeInfoList()) {
                if (targetGroup.equals(nodeInfo.getGroup())) {
                    dispatchToNode(nodeInfo.getId(), request);
                }
            }
        } else {
            String targetNodeId = request.getTargetNodeId();
            if (StringUtils.isEmpty(targetNodeId)) {
                targetNodeId = getNodeId();
            }
            dispatchToNode(targetNodeId, request);
        }
    }

    private void dispatchToNode(String targetNodeId, @NonNull RemoteCommandParameters request) {
        if (isSameNode(targetNodeId)) {
            executeLocally(request);
        } else {
            dispatch(targetNodeId, request);
        }
    }

    private void executeLocally(@NonNull RemoteCommandParameters request) {
        Thread.ofVirtual().start(() -> {
            try {
                CommandParameters commandParameters = request.getCommand();
                if (commandParameters != null) {
                    logger.debug("Executing local daemon command: {}", commandParameters);
                    String response = localCommandService.execute(commandParameters.toString());
                    if (response != null) {
                        RemoteCommandResultParameters resultParameters = new RemoteCommandResultParameters()
                                .setHeader("result")
                                .setNodeId(getNodeId())
                                .setRequestId(request.getRequestId())
                                .setResult(response);
                        broadcast(resultParameters);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to execute local daemon command", e);
            }
        });
    }

    private void dispatch(String targetNodeId, @NonNull RemoteCommandParameters request) {
        if (messagePublisher != null) {
            try {
                request.setSourceNodeId(getNodeId());
                String message = CommandBroker.CONTROL_REQUEST + request;
                messagePublisher.publishControl(CommandBroker.CATEGORY_COMMANDS, targetNodeId, message);
                if (logger.isDebugEnabled()) {
                    logger.debug("Daemon command dispatched to node {}: {}", targetNodeId, request.getCommand());
                }
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
                    RemoteCommandResultParameters resultParameters = new RemoteCommandResultParameters()
                            .setHeader("result")
                            .setNodeId(getNodeId())
                            .setRequestId(request.getRequestId())
                            .setResult(response);
                    String relayMessage = resultParameters.toString();
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
    private String execute(@NonNull RemoteCommandParameters request) {
        CommandParameters commandParameters = request.getCommand();
        if (commandParameters != null) {
            return localCommandService.execute(commandParameters.toString());
        }
        return null;
    }

    /**
     * Broadcasts a command execution result to all connected clients on this node.
     * @param resultParameters the command result parameters
     */
    public void broadcast(RemoteCommandResultParameters resultParameters) {
        if (logger.isTraceEnabled()) {
            logger.trace("Broadcasting command result (source: {}, request: {}) to local clients: {}",
                    resultParameters.getString(RemoteCommandResultParameters.nodeId),
                    resultParameters.getString(RemoteCommandResultParameters.requestId),
                    resultParameters.getString(RemoteCommandResultParameters.result));
        }
        if (broker != null) {
            broker.bridge(resultParameters);
        }
    }

}
