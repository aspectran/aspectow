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
package com.aspectran.aspectow.console.commands.bridge.websocket;

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.commands.RemoteRequestParameters;
import com.aspectran.aspectow.node.management.commands.RemoteResponseParameters;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBridge;
import com.aspectran.aspectow.node.management.commands.bridge.CommandSession;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.apon.JsonToParameters;
import com.aspectran.utils.security.InvalidPBTokenException;
import com.aspectran.web.websocket.jsr356.AspectranConfigurator;
import com.aspectran.web.websocket.jsr356.SimplifiedEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aspectran.aspectow.node.management.commands.bridge.CommandBroker.CATEGORY_COMMANDS;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * WebsocketCommandBridge provides a WebSocket endpoint for real-time
 * remote command result delivery.
 */
@Component
@ServerEndpoint(
        value = NODES_BASE_PATH + "/{nodeId}/" + CATEGORY_COMMANDS + "/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class WebsocketCommandBridge extends SimplifiedEndpoint implements CommandBridge {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketCommandBridge.class);

    private final RemoteCommandManager remoteCommandManager;

    private final NodeManager nodeManager;

    @Autowired
    public WebsocketCommandBridge(RemoteCommandManager remoteCommandManager, NodeManager nodeManager) {
        this.remoteCommandManager = remoteCommandManager;
        this.nodeManager = nodeManager;
    }

    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
            return true;
        } catch (InvalidPBTokenException e) {
            logger.warn("WebSocket connection rejected: invalid or expired token");
            return false;
        }
    }

    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (session.getMessageHandlers().isEmpty()) {
            session.addMessageHandler(String.class, message -> {
                setLoggingGroup();
                handleMessage(session, message);
            });
        }
    }

    private void handleMessage(Session session, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }

        try {
            RemoteRequestParameters request = JsonToParameters.from(message, RemoteRequestParameters.class);

            String header = request.getHeader();
            if ("execute".equals(header)) {
                execute(session, request);
            } else if ("subscribe".equals(header)) {
                subscribe(session, request);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming remote command message: {}", message, e);
            sendText(session, "[ERROR] Invalid message format");
        }
    }

    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        remoteCommandManager.unregisterSession(session.getId());
        WebsocketCommandSession commandSession = new WebsocketCommandSession(session);
        remoteCommandManager.getBroker().release(commandSession);
        logger.debug("Remote command WebSocket session removed: {} (Total: {})", session.getId(), countSessions());
    }

    private void subscribe(Session session, @NonNull RemoteRequestParameters request) {
        WebsocketCommandSession commandSession = new WebsocketCommandSession(session);
        String targetNodeId = request.getTargetNodeId();
        if (targetNodeId != null && !targetNodeId.isEmpty()) {
            commandSession.setNodeId(targetNodeId);
        } else {
            commandSession.setNodeId(nodeManager.getNodeId());
        }

        if (addSession(session)) {
            remoteCommandManager.registerSession(session.getId(), this);
            remoteCommandManager.getBroker().subscribe(commandSession);
            RemoteResponseParameters response = new RemoteResponseParameters()
                    .setHeader("subscribed")
                    .setNodeId(nodeManager.getNodeId());
            sendText(session, response.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("ConsoleClient joined remote command management: session {}, targetNodeId: {}",
                        session.getId(), commandSession.getNodeId());
            }
        }
    }

    private void pong(Session session) {
        RemoteResponseParameters response = new RemoteResponseParameters()
                .setHeader("pong");
        sendText(session, response.toString());
    }

    private void execute(Session session, @NonNull RemoteRequestParameters request) {
        CommandParameters commandParameters = request.getCommand();
        if (commandParameters != null) {
            request.setSessionId(session.getId());
            try {
                remoteCommandManager.process(request);
                logger.debug("Command execution initiated from session {}: target={}, command={}",
                        session.getId(), request.getTargetNodeId(), commandParameters.getCommandName());
            } catch (Exception e) {
                logger.error("Failed to initiate command execution from session {}", session.getId(), e);
                sendText(session, "[ERROR] " + e.getMessage());
            }
        }
    }

    @Override
    public CommandSession findCommandSession(String sessionId) {
        Session session = findSession(sessionId);
        return (session != null ? new WebsocketCommandSession(session) : null);
    }

    @Override
    public void bridge(String message) {
        if (message != null) {
            broadcast(message);
        }
    }

    @Override
    public void bridge(@NonNull CommandSession session, String message) {
        if (message != null && session instanceof WebsocketCommandSession websocketCommandSession) {
            sendText(websocketCommandSession.getSession(), message);
        }
    }

}
