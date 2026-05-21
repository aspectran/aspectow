/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.appmon.engine.relay;

import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.NodeRegistry;
import com.aspectran.aspectow.node.redis.RedisMessagePublisher;
import com.aspectran.utils.Assert;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_SUBSCRIBE;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_UNSUBSCRIBE;

/**
 * Manages all {@link MessageRelayer} and {@link ExporterManager} apps.
 * This class is a central hub for handling client sessions (subscribe/release),
 * collecting messages from exporters, and relaying them to clients.
 *
 * <p>Created: 2025-02-12</p>
 */
public class MessageRelayManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageRelayManager.class);

    public static final String CATEGORY_APPMON = "appmon";

    private final Map<String, MessageRelayer> sessionRelayerMap = new ConcurrentHashMap<>();

    private final List<ExporterManager> exporterManagers = new CopyOnWriteArrayList<>();

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    private final String nodeId;

    private final NodeRegistry nodeRegistry;

    private final RedisMessagePublisher messagePublisher;

    /**
     * Instantiates a new MessageRelayManager.
     * @param messagePublisher the Redis message publisher
     */
    public MessageRelayManager(String nodeId, NodeRegistry nodeRegistry, RedisMessagePublisher messagePublisher) {
        this.nodeId = nodeId;
        this.nodeRegistry = nodeRegistry;
        this.messagePublisher = messagePublisher;
    }

    public SubscriptionRegistry getSubscriptionRegistry() {
        return subscriptionRegistry;
    }

    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isSameNode(String targetNodeId) {
        return (targetNodeId != null && targetNodeId.equals(this.nodeId));
    }

    /**
     * Checks if the manager is running in gateway mode.
     * @return {@code true} if in gateway mode, {@code false} otherwise
     */
    public boolean isGatewayMode() {
        return (messagePublisher != null);
    }

    public void registerSession(String sessionId, MessageRelayer messageRelayer) {
        sessionRelayerMap.put(sessionId, messageRelayer);
    }

    public void unregisterSession(String sessionId) {
        sessionRelayerMap.remove(sessionId);
    }

    /**
     * Adds an exporter manager to this manager.
     * @param exporterManager the exporter manager to add
     */
    public void addExporterManager(ExporterManager exporterManager) {
        exporterManagers.add(exporterManager);
    }

    private void startExporters(String appId) {
        Assert.hasText(appId, "appId must not be null or empty");
        for (ExporterManager exporterManager : exporterManagers) {
            if (exporterManager.getAppId().equals(appId)) {
                exporterManager.start();
            }
        }
    }

    private void stopExporters(String appId) {
        Assert.hasText(appId, "appId must not be null or empty");
        for (ExporterManager exporterManager : exporterManagers) {
            if (exporterManager.getAppId().equals(appId)) {
                exporterManager.stop();
            }
        }
    }

    /**
     * Publishes a local message to Redis and relays it to all registered relayers.
     * @param message the message to publish
     */
    public void broadcast(String message) {
        relayLocally(message);
        if (isGatewayMode()) {
            String appId = extractAppId(message);
            if (appId != null) {
                Set<String> remoteNodeIds = subscriptionRegistry.getNodeIdsRemotelySubscribedToApp(appId);
                if (remoteNodeIds != null) {
                    for (String remoteNodeId : remoteNodeIds) {
                        publishRelay(remoteNodeId, message);
                    }
                }
            } else {
                for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                    if (!isSameNode(nodeInfo.getNodeId())) {
                        publishRelay(nodeInfo.getNodeId(), message);
                    }
                }
            }
        }
    }

    private void publishRelay(String targetNodeId, String message) {
        publishRelay(targetNodeId, null, message);
    }

    private void publishRelay(String targetNodeId, String sessionId, String message) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishRelay(CATEGORY_APPMON, targetNodeId, sessionId, message);
            } catch (Exception e) {
                logger.error("Failed to publish relay message to node {}", targetNodeId, e);
            }
        }
    }

    private void publishControl(String targetNodeId, @NonNull CommandOptions commandOptions) {
        if (messagePublisher != null) {
            try {
                messagePublisher.publishControl(CATEGORY_APPMON, targetNodeId, commandOptions.toString());
            } catch (Exception e) {
                logger.error("Failed to publish control message to node {}", targetNodeId, e);
            }
        }
    }

    /**
     * Relays a message to all registered relayers.
     * This method does not publish the message to Redis.
     * @param message the message to relay
     */
    public void relayLocally(String message) {
        Assert.notNull(message, "message must not be null");
        String appId = extractAppId(message);
        boolean isLog = isLogMessage(message);
        Set<String> sessionIds = subscriptionRegistry.getSessionsSubscribedToApp(appId);
        for (String sid : sessionIds) {
            MessageRelayer relayer = sessionRelayerMap.get(sid);
            if (relayer != null) {
                RelaySession session = relayer.findRelaySession(sid);
                if (session != null) {
                    relayLocally(relayer, session, message, appId, isLog);
                }
            }
        }
    }

    /**
     * Relays a message to all registered relayers.
     * This method does not publish the message to Redis.
     * @param sessionId the target session ID
     * @param message the message to relay
     */
    public void relayLocally(String sessionId, String message) {
        Assert.notNull(sessionId, "sessionId must not be null");
        Assert.notNull(message, "message must not be null");
        MessageRelayer relayer = sessionRelayerMap.get(sessionId);
        if (relayer != null) {
            RelaySession session = relayer.findRelaySession(sessionId);
            if (session != null) {
                String appId = extractAppId(message);
                boolean isLog = isLogMessage(message);
                relayLocally(relayer, session, message, appId, isLog);
            }
        }
    }

    private void relayLocally(
            @NonNull MessageRelayer relayer, @NonNull RelaySession session, @NonNull String message,
            @Nullable String appId, boolean isLog) {
        if (appId != null && isLog) {
            String focusedAppId = session.getFocusedAppId();
            if (focusedAppId != null && !focusedAppId.equals(appId)) {
                return;
            }
        }
        if (session.isValid()) {
            relayer.relay(session, message);
        }
    }

    @Nullable
    private String extractAppId(@NonNull String message) {
        int idx1 = message.indexOf(':');
        if (idx1 != -1) {
            int idx2 = message.indexOf(':', idx1 + 1);
            if (idx2 != -1) {
                String appId = message.substring(idx1 + 1, idx2);
                return (!appId.isEmpty() ? appId : null);
            }
        }
        return null;
    }

    @Nullable
    private String extractType(@NonNull String message) {
        int idx1 = message.indexOf(':');
        if (idx1 != -1) {
            int idx2 = message.indexOf(':', idx1 + 1);
            if (idx2 != -1) {
                int idx3 = message.indexOf(':', idx2 + 1);
                if (idx3 != -1) {
                    return message.substring(idx2 + 1, idx3);
                }
            }
        }
        return null;
    }

    private boolean isLogMessage(@NonNull String message) {
        return "log".equals(extractType(message));
    }

    /**
     * Handles a client subscribing to monitor apps.
     * Starts the necessary exporters for the subscribeed apps.
     * @param session the client session that is subscribing
     * @return {@code true} if the subscribe was successful, {@code false} otherwise
     */
    public synchronized boolean subscribe(@NonNull RelaySession session) {
        if (!session.isValid()) {
            return false;
        }
        String[] subscribedApps = session.getSubscribedApps();
        if (subscribedApps == null || subscribedApps.length == 0) {
            return false;
        }
        for (String appId : subscribedApps) {
            if (!subscriptionRegistry.isAppInUse(appId)) {
                startExporters(appId);
            }
            if (isGatewayMode() && !subscriptionRegistry.isAppInUseLocally(appId)) {
                CommandOptions commandOptions = new CommandOptions();
                commandOptions.setCommand(COMMAND_SUBSCRIBE);
                commandOptions.setNodeId(nodeId);
                commandOptions.setAppId(appId);
                commandOptions.setSessionId(session.getId());
                commandOptions.setTimeZone(session.getTimeZone());
                for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                    if (!isSameNode(nodeInfo.getNodeId())) {
                        publishControl(nodeInfo.getNodeId(), commandOptions);
                    }
                }
            }
        }
        subscriptionRegistry.addLocalSubscription(session.getId(), subscribedApps);
        return true;
    }

    public synchronized void subscribeRemotely(CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        String nodeId = commandOptions.getNodeId();
        String appId = commandOptions.getAppId();
        String sessionId = commandOptions.getSessionId();
        if (!subscriptionRegistry.isAppInUse(appId)) {
            startExporters(appId);
        }
        subscriptionRegistry.addRemoteSubscription(nodeId, appId);
        List<String> messages = getLastMessages(commandOptions);
        if (sessionId != null) {
            for (String message : messages) {
                publishRelay(nodeId, sessionId, message);
            }
        }
    }

    /**
     * Handles a client releasing its monitoring session.
     * Stops exporters that are no longer being monitored by any client.
     * @param session the client session that is being released
     */
    public synchronized void unsubscribe(@NonNull RelaySession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
        String[] subscribedApps = session.getSubscribedApps();
        if (subscribedApps != null) {
            for (String appId : subscribedApps) {
                if (!subscriptionRegistry.isAppInUse(appId)) {
                    stopExporters(appId);
                }
                if (isGatewayMode() && !subscriptionRegistry.isAppInUseLocally(appId)) {
                    CommandOptions commandOptions = new CommandOptions();
                    commandOptions.setCommand(COMMAND_UNSUBSCRIBE);
                    commandOptions.setNodeId(nodeId);
                    commandOptions.setAppId(appId);
                    for (NodeInfo nodeInfo : nodeRegistry.getNodes()) {
                        if (!isSameNode(nodeInfo.getNodeId())) {
                            publishControl(nodeInfo.getNodeId(), commandOptions);
                        }
                    }
                }
            }
        }
        session.removeSubscribedApps();
    }

    public synchronized void unsubscribeRemotely(CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        String nodeId = commandOptions.getNodeId();
        String appId = commandOptions.getAppId();
        subscriptionRegistry.removeRemoteSubscription(nodeId, appId);
        if (!subscriptionRegistry.isAppInUse(appId)) {
            stopExporters(appId);
        }
    }

    @Nullable
    public List<String> refreshData(@NonNull RelaySession session, CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        if (!commandOptions.hasTimeZone()) {
            commandOptions.setTimeZone(session.getTimeZone());
        }
        String targetNodeId = commandOptions.getNodeId();
        if (isSameNode(targetNodeId)) {
            return getNewMessages(session, commandOptions);
        }
        if (isGatewayMode()) {
            commandOptions.setCommand(COMMAND_REFRESH);
            commandOptions.setNodeId(nodeId);
            commandOptions.setSessionId(session.getId());
            publishControl(targetNodeId, commandOptions);
        }
        return null;
    }

    public void refreshDataRemotely(CommandOptions commandOptions) {
        Assert.notNull(commandOptions, "Command options must not be null");
        if (isGatewayMode()) {
            String fromNodeId = commandOptions.getNodeId();
            String appId = commandOptions.getAppId();
            String sessionId = commandOptions.getSessionId();
            List<String> messages = new ArrayList<>();
            collectNewMessages(appId, messages, commandOptions);
            for (String message : messages) {
                publishRelay(fromNodeId, sessionId, message);
            }
        }
    }

    /**
     * Gets the last known messages for the apps subscribeed by the session.
     * @param session the client session
     * @return a list of messages
     */
    public List<String> getLastMessages(@NonNull RelaySession session) {
        if (!session.isValid()) {
            return List.of();
        }
        List<String> messages = new ArrayList<>();
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.setTimeZone(session.getTimeZone());
        String[] subscribedApps = session.getSubscribedApps();
        if (subscribedApps != null && subscribedApps.length > 0) {
            for (String appId : subscribedApps) {
                commandOptions.setAppId(appId);
                collectLastMessages(messages, commandOptions);
            }
        } else {
            collectLastMessages(messages, commandOptions);
        }
        return messages;
    }

    public List<String> getLastMessages(@NonNull CommandOptions commandOptions) {
        List<String> messages = new ArrayList<>();
        collectLastMessages(messages, commandOptions);
        return messages;
    }

    private void collectLastMessages(List<String> messages, @NonNull CommandOptions commandOptions) {
        String appId = commandOptions.getAppId();
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || appId.equals(exporterManager.getAppId())) {
                exporterManager.collectMessages(messages, commandOptions);
            }
        }
    }

    /**
     * Gets new or changed messages based on the provided command options.
     * @param session the client session
     * @param commandOptions the command options specifying what to refresh
     * @return a list of new messages
     */
    @NonNull
    public List<String> getNewMessages(RelaySession session, @NonNull CommandOptions commandOptions) {
        String appId = commandOptions.getAppId();
        List<String> messages = new ArrayList<>();
        if (session == null || session.isValid()) {
            String[] appIds = (session != null ? session.getSubscribedApps() : null);
            if (appIds != null) {
                for (String id : appIds) {
                    if (appId == null || appId.equals(id)) {
                        collectNewMessages(id, messages, commandOptions);
                    }
                }
            } else {
                collectNewMessages(appId, messages, commandOptions);
            }
        }
        return messages;
    }

    private void collectNewMessages(String appId, List<String> messages, CommandOptions commandOptions) {
        for (ExporterManager exporterManager : exporterManagers) {
            if (appId == null || appId.equals(exporterManager.getAppId())) {
                exporterManager.collectNewMessages(messages, commandOptions);
            }
        }
    }

    /**
     * Destroys the manager, stopping all exporters.
     */
    public void destroy() {
        for (ExporterManager exporterManager : exporterManagers) {
            exporterManager.stop();
        }
        exporterManagers.clear();
    }

}
