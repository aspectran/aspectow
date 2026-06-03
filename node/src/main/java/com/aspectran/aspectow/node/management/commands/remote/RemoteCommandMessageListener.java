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
package com.aspectran.aspectow.node.management.commands.remote;

import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.commands.RemoteCommandParameters;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBroker;
import com.aspectran.aspectow.node.manager.NodeMessageListener;
import com.aspectran.utils.apon.AponParseException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommandMessageBridgeHandler listens to Redis relay messages and forwards
 * them to the RemoteCommandManager.
 */
public class RemoteCommandMessageListener implements NodeMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCommandMessageListener.class);

    private final RemoteCommandManager remoteCommandManager;

    public RemoteCommandMessageListener(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
    }

    @Override
    public String getCategory() {
        return CommandBroker.CATEGORY_COMMANDS;
    }

    @Override
    public void onControlMessage(String nodeId, @NonNull String message) {
        if (message.startsWith(CommandBroker.CONTROL_SUBSCRIBE) || message.startsWith(CommandBroker.CONTROL_RELEASE)) {
            String requesterNodeId = null;
            String sessionId = null;
            String[] parts = message.split(CommandBroker.DELIMITER);
            if (parts.length >= 2) {
                requesterNodeId = parts[1];
            }
            if (parts.length >= 3) {
                sessionId = parts[2];
            }
            if (requesterNodeId == null) {
                requesterNodeId = nodeId;
            }

            if (message.startsWith(CommandBroker.CONTROL_SUBSCRIBE)) {
                remoteCommandManager.getBroker().subscribeRemotely(requesterNodeId, sessionId);
            } else if (message.startsWith(CommandBroker.CONTROL_RELEASE)) {
                remoteCommandManager.getBroker().releaseRemotely(requesterNodeId);
            }
        } else if (message.startsWith(CommandBroker.CONTROL_REQUEST)) {
            String requestData = message.substring(CommandBroker.CONTROL_REQUEST.length());
            RemoteCommandParameters request = new RemoteCommandParameters();
            try {
                request.readFrom(requestData);
            } catch (AponParseException e) {
                logger.error("Failed to parse command request parameters: {}", requestData, e);
            }

            remoteCommandManager.processRemotely(request);
        }
    }

    @Override
    public void onRelayMessage(String nodeId, @NonNull String message) {
        int idx = message.indexOf(CommandBroker.DELIMITER);
        if (idx != -1) {
            String sourceNodeId = message.substring(0, idx);
            String content = message.substring(idx + 1);
            remoteCommandManager.broadcast(sourceNodeId, content);
        } else {
            remoteCommandManager.broadcast(message);
        }
    }

    @Override
    public void onRelayMessage(String nodeId, String sessionId, @NonNull String message) {
        int idx = message.indexOf(CommandBroker.DELIMITER);
        String sourceNodeId;
        String content;
        if (idx != -1) {
            sourceNodeId = message.substring(0, idx);
            content = message.substring(idx + 1);
        } else {
            sourceNodeId = nodeId;
            content = message;
        }

        remoteCommandManager.getBroker().getSessions().stream()
                .filter(session -> session.getId().equals(sessionId))
                .forEach(session -> remoteCommandManager.getBroker().bridge(session, sourceNodeId, content));
    }

}
