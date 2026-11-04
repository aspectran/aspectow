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
import com.aspectran.aspectow.node.management.commands.bridge.CommandBridge;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBroker;
import com.aspectran.aspectow.node.management.commands.bridge.CommandSession;
import com.aspectran.aspectow.node.management.commands.remote.RemoteCommandMessageListener;
import com.aspectran.aspectow.node.manager.ClusterEventListener;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.daemon.command.CommandResult;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * RemoteCommandManager orchestrates remote daemon command execution across the cluster.
 * It manages local execution, remote dispatching via Redis, and broadcasting
 * results to connected clients.
 */
public class RemoteCommandManager implements InitializableBean, DisposableBean, ClusterEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCommandManager.class);

    private final Map<String, CommandBridge> sessionBridgeMap = new ConcurrentHashMap<>();

    private final Map<String, CompletableFuture<CommandResponseParameters>> pendingRequests = new ConcurrentHashMap<>();

    private final NodeManager nodeManager;

    private final NodeMessagePublisher messagePublisher;

    private final LocalCommandService localCommandService;

    private final CommandBroker broker;

    private RemoteCommandMessageListener messageListener;

    /**
     * Instantiates a new RemoteCommandManager.
     * @param nodeManager the node manager
     */
    public RemoteCommandManager(@NonNull NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.messagePublisher = nodeManager.getNodeMessagePublisher();
        this.localCommandService = new LocalCommandService(nodeManager);
        this.broker = new CommandBroker(this);
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing RemoteCommandManager for node: {}", getNodeId());

        // Register a listener for command relay messages (commands and results) from Redis
        if (nodeManager.getNodeMessageSubscriber() != null) {
            messageListener = new RemoteCommandMessageListener(this);
            nodeManager.getNodeMessageSubscriber().addListener(this.messageListener);
        }

        // Register as a cluster event listener to handle node join/left events
        if (nodeManager.getClusterEventSubscriber() != null) {
            nodeManager.getClusterEventSubscriber().addListener(this);
        }
    }

    @Override
    public void destroy() {
        if (nodeManager.getNodeMessageSubscriber() != null && messageListener != null) {
            nodeManager.getNodeMessageSubscriber().removeListener(messageListener);
        }
        if (nodeManager.getClusterEventSubscriber() != null) {
            nodeManager.getClusterEventSubscriber().removeListener(this);
        }
        pendingRequests.values().forEach(f -> f.cancel(true));
        pendingRequests.clear();
    }

    /**
     * Returns the message publisher for node communications.
     * @return the message publisher
     */
    public NodeMessagePublisher getMessagePublisher() {
        return messagePublisher;
    }

    /**
     * Returns whether the manager is running in gateway mode (i.e. has a message publisher).
     * @return true if gateway mode, false otherwise
     */
    public boolean isGatewayMode() {
        return (messagePublisher != null);
    }

    /**
     * Returns the unique identifier of the local node.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    /**
     * Returns whether the specified node ID is the local node.
     * @param targetNodeId the node ID to check
     * @return true if it is the local node, false otherwise
     */
    public boolean isSameNode(String targetNodeId) {
        return (targetNodeId != null && targetNodeId.equals(getNodeId()));
    }

    /**
     * Returns the command broker.
     * @return the command broker
     */
    public CommandBroker getBroker() {
        return broker;
    }

    /**
     * Registers a session with its corresponding command bridge.
     * @param sessionId the session identifier
     * @param commandBridge the command bridge
     */
    public void registerSession(String sessionId, CommandBridge commandBridge) {
        sessionBridgeMap.put(sessionId, commandBridge);
    }

    /**
     * Unregisters a session.
     * @param sessionId the session identifier
     */
    public void unregisterSession(String sessionId) {
        sessionBridgeMap.remove(sessionId);
    }

    /**
     * Dispatches a command request to a specific node or handles it locally.
     * @param request the command request parameters
     */
    public void process(@NonNull CommandRequestParameters request) {
        if (request.isTargetServices()) {
            for (NodeInfo nodeInfo : nodeManager.getNodeInfoList()) {
                if (!nodeInfo.isConsole()) {
                    process(nodeInfo.getId(), request.copy());
                }
            }
        } else if (request.isTargetAll()) {
            for (NodeInfo nodeInfo : nodeManager.getNodeInfoList()) {
                process(nodeInfo.getId(), request.copy());
            }
        } else if (StringUtils.hasText(request.getTargetGroup())) {
            String targetGroup = request.getTargetGroup();
            for (NodeInfo nodeInfo : nodeManager.getNodeInfoList()) {
                if (targetGroup.equals(nodeInfo.getGroup())) {
                    process(nodeInfo.getId(), request.copy());
                }
            }
        } else {
            String targetNodeId = request.getTargetNodeId();
            if (StringUtils.isEmpty(targetNodeId)) {
                targetNodeId = getNodeId();
            }
            process(targetNodeId, request);
        }
    }

    private void process(String targetNodeId, @NonNull CommandRequestParameters request) {
        if (isSameNode(targetNodeId)) {
            executeLocally(request);
        } else {
            dispatch(targetNodeId, request);
        }
    }

    private void dispatch(String targetNodeId, @NonNull CommandRequestParameters request) {
        if (messagePublisher != null) {
            try {
                request.setNodeId(getNodeId());
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

    private void executeLocally(@NonNull CommandRequestParameters request) {
        Thread.ofVirtual().start(() -> {
            setVirtualThreadName(request);
            CommandParameters commandParameters = request.getCommand();
            if (commandParameters != null) {
                try {
                    logger.debug("Executing local daemon command: {}", commandParameters);
                    CommandResult result;
                    String commandName = commandParameters.getCommandName();
                    if ("pause".equalsIgnoreCase(commandName) || "resume".equalsIgnoreCase(commandName)) {
                        result = localCommandService.executeControl(commandName);
                    } else if ("anatomy".equalsIgnoreCase(commandName)) {
                        result = localCommandService.executeAnatomy(commandParameters);
                    } else if ("components".equalsIgnoreCase(commandName)) {
                        result = localCommandService.executeComponents(commandParameters);
                    } else {
                        result = localCommandService.execute(commandParameters);
                    }
                    if (result != null && request.getSessionId() != null) {
                        CommandResponseParameters response = new CommandResponseParameters()
                                .setHeader("result")
                                .setNodeId(getNodeId())
                                .setRequestId(request.getRequestId())
                                .setResult(result.getResult())
                                .setError(result.getError());
                        bridge(request.getSessionId(), response.toString());
                    }
                } catch (Exception e) {
                    logger.error("Failed to execute local daemon command", e);
                }
            }
        });
    }

    /**
     * Processes an incoming message received from the cluster relay.
     * @param request the command request parameters
     */
    public void executeRemotely(@NonNull CommandRequestParameters request) {
        Thread.ofVirtual().start(() -> {
            setVirtualThreadName(request);
            try {
                CommandResult result = execute(request);
                if (result != null && messagePublisher != null) {
                    String gatewayNodeId = request.getNodeId();
                    String sessionId = request.getSessionId();
                    CommandResponseParameters response = new CommandResponseParameters()
                            .setHeader("result")
                            .setNodeId(getNodeId())
                            .setRequestId(request.getRequestId())
                            .setResult(result.getResult())
                            .setError(result.getError());
                    String relayMessage = response.toString();
                    if (gatewayNodeId != null && sessionId != null) {
                        messagePublisher.publishRelay(
                                CommandBroker.CATEGORY_COMMANDS, gatewayNodeId, sessionId, relayMessage);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to process daemon command request: {}", request, e);
            }
        });
    }

    @Nullable
    private CommandResult execute(@NonNull CommandRequestParameters request) {
        CommandParameters commandParameters = request.getCommand();
        if (commandParameters != null) {
            String commandName = commandParameters.getCommandName();
            if ("pause".equalsIgnoreCase(commandName) || "resume".equalsIgnoreCase(commandName)) {
                return localCommandService.executeControl(commandName);
            } else if ("components".equalsIgnoreCase(commandName)) {
                return localCommandService.executeComponents(commandParameters);
            } else if ("anatomy".equalsIgnoreCase(commandName)) {
                return localCommandService.executeAnatomy(commandParameters);
            } else {
                return localCommandService.execute(commandParameters);
            }
        }
        return null;
    }

    private void setVirtualThreadName(@NonNull CommandRequestParameters request) {
        CommandParameters commandParameters = request.getCommand();
        if (commandParameters != null) {
            String commandName = commandParameters.getCommandName();
            String reqId = request.getRequestId();
            String idSuffix;
            if (reqId != null && !reqId.isEmpty()) {
                idSuffix = (reqId.length() > 8) ? reqId.substring(reqId.length() - 8) : reqId;
            } else {
                idSuffix = "v" + Thread.currentThread().threadId();
            }
            String threadName = "cmd-" + commandName + "#" + idSuffix;
            Thread.currentThread().setName(threadName);
        }
    }

    /**
     * Bridges a command message to a specific session, or completes an internal
     * pending request future if matched.
     * @param sessionId the session identifier
     * @param message the message to bridge
     */
    public void bridge(String sessionId, String message) {
        Assert.notNull(sessionId, "sessionId must not be null");
        if (message != null && !pendingRequests.isEmpty()) {
            try {
                CommandResponseParameters response = new CommandResponseParameters();
                response.readFrom(message);
                String reqId = response.getRequestId();
                if (reqId != null) {
                    CompletableFuture<CommandResponseParameters> future = pendingRequests.get(reqId);
                    if (future != null) {
                        future.complete(response);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to check pending request for message: {}", message, e);
            }
        }
        CommandBridge bridge = sessionBridgeMap.get(sessionId);
        if (bridge != null) {
            CommandSession session = bridge.findCommandSession(sessionId);
            if (session != null) {
                bridge.bridge(session, message);
            }
        }
    }

    /**
     * Dispatches a remote command to a target node and returns a {@link CompletableFuture}
     * that completes when the target node replies with a response.
     * @param targetNodeId the target node ID
     * @param command the command parameters
     * @param timeoutMs the timeout in milliseconds
     * @return the CompletableFuture holding the response parameters
     */
    public CompletableFuture<CommandResponseParameters> executeRemote(
            @NonNull String targetNodeId, @NonNull CommandParameters command, long timeoutMs) {
        String requestId = "req_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
        CompletableFuture<CommandResponseParameters> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        CommandRequestParameters request = new CommandRequestParameters();
        request.setHeader("execute");
        request.setRequestId(requestId);
        request.setSessionId("internal:rpc");
        request.setTargetNodeId(targetNodeId);
        request.setCommand(command);

        if (isSameNode(targetNodeId)) {
            executeLocally(request);
        } else {
            dispatch(targetNodeId, request);
        }

        return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((res, ex) -> pendingRequests.remove(requestId));
    }

    @Override
    public void onNodeJoined(NodeInfo info) {
        if (sessionBridgeMap.isEmpty()) {
            return;
        }
        try {
            CommandResponseParameters response = new CommandResponseParameters()
                    .setHeader("nodeJoined")
                    .setNodeId(info.getId());
            String message = response.toString();
            for (Map.Entry<String, CommandBridge> entry : sessionBridgeMap.entrySet()) {
                String sessionId = entry.getKey();
                CommandBridge bridge = entry.getValue();
                CommandSession session = bridge.findCommandSession(sessionId);
                if (session != null) {
                    bridge.bridge(session, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast join event of node {}", info.getId(), e);
        }
    }

    @Override
    public void onNodeLeft(String leftNodeId) {
        if (sessionBridgeMap.isEmpty()) {
            return;
        }
        try {
            CommandResponseParameters response = new CommandResponseParameters()
                    .setHeader("nodeLeft")
                    .setNodeId(leftNodeId);
            String message = response.toString();
            for (Map.Entry<String, CommandBridge> entry : sessionBridgeMap.entrySet()) {
                String sessionId = entry.getKey();
                CommandBridge bridge = entry.getValue();
                CommandSession session = bridge.findCommandSession(sessionId);
                if (session != null) {
                    bridge.bridge(session, message);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast left event of node {}", leftNodeId, e);
        }
    }

}
