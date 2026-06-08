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
package com.aspectran.aspectow.console.commands.bridge.polling;

import com.aspectran.aspectow.node.management.commands.RemoteCommandManager;
import com.aspectran.aspectow.node.management.commands.RemoteRequestParameters;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBridge;
import com.aspectran.aspectow.node.management.commands.bridge.CommandBroker;
import com.aspectran.aspectow.node.management.commands.bridge.CommandSession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.Assert;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static com.aspectran.aspectow.node.management.commands.bridge.CommandBroker.CATEGORY_COMMANDS;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * PollingCommandBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute command results.
 */
@Component(NODES_BASE_PATH + "/${thisNodeId}/" + CATEGORY_COMMANDS)
public class PollingCommandBridge extends AbstractComponent implements CommandBridge {

    private final PollingSessionManager pollingSessionManager;

    private final RemoteCommandManager remoteCommandManager;

    @Autowired
    public PollingCommandBridge(RemoteCommandManager remoteCommandManager) {
        this.remoteCommandManager = remoteCommandManager;
        this.pollingSessionManager = new PollingSessionManager(this);
    }

    @Override
    protected void doInitialize() throws Exception {
        pollingSessionManager.initialize();
    }

    @Override
    protected void doDestroy() throws Exception {
        pollingSessionManager.destroy();
    }

    public void registerSession(String sessionId) {
        remoteCommandManager.registerSession(sessionId, this);
    }

    public void unregisterSession(String sessionId) {
        remoteCommandManager.unregisterSession(sessionId);
    }

    public CommandBroker getBroker() {
        return remoteCommandManager.getBroker();
    }

    /**
     * Allows a client to subscribe and start a polling session.
     * @param translet the current translet
     */
    @Request("/polling/subscribe")
    public RestResponse subscribe(@NonNull Translet translet) {
        String nodeId = translet.getParameter("nodeId");
        Assert.hasText(nodeId, "Node ID cannot be empty");

        PollingCommandSession commandSession = pollingSessionManager.getSession(translet);
        if (commandSession == null) {
            commandSession = pollingSessionManager.createSession(translet);
            remoteCommandManager.registerSession(commandSession.getId(), this);
        }

        return new SuccessResponse(Map.of(
                "pollingInterval", commandSession.getPollingInterval(),
                "nodeId", remoteCommandManager.getNodeId(),
                "primary", remoteCommandManager.isSameNode(nodeId)
                )).ok();
    }

    /**
     * Allows a client to pull new messages from the server.
     * @param translet the current translet
     */
    @RequestToGet("/polling/pull")
    public RestResponse pull(@NonNull Translet translet) {
        try {
            PollingCommandSession session = pollingSessionManager.getSession(translet);
            if (session != null) {
                String[] messages = pollingSessionManager.pull(session);
                return new SuccessResponse(messages).ok();
            } else {
                return new FailureResponse().setError("not_found", "Session not found");
            }
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

        /**
     * Creates a command to be sent to a specific node or all nodes.
     * @return a success message
     */
    @RequestToPost("/polling/execute")
    public RestResponse executeCommand(@NonNull Translet translet) {
        PollingCommandSession session = pollingSessionManager.getSession(translet);
        if (session == null) {
            return new FailureResponse().setError("not_found", "Session not found");
        }

        String targetNodeId = translet.getParameter("nodeId");
        String targetGroup = translet.getParameter("targetGroup");
        boolean targetAll = Boolean.parseBoolean(translet.getParameter("targetAll"));
        String command = translet.getParameter("command");

        if (StringUtils.isEmpty(command)) {
            return new FailureResponse().setError("required", "Command is required");
        }

        try {
            RemoteRequestParameters request = new RemoteRequestParameters();
            request.setHeader("execute");
            request.setSessionId(session.getId());
            request.setTargetNodeId(targetNodeId);
            request.setTargetGroup(targetGroup);
            request.setTargetAll(targetAll);
            request.setCommand(command);

            remoteCommandManager.process(request);
            return new SuccessResponse("Command initiated successfully").ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    @Override
    public CommandSession findCommandSession(String sessionId) {
        return pollingSessionManager.getSession(sessionId);
    }

    @Override
    public void bridge(String message) {
        pollingSessionManager.push(message);
    }

    @Override
    public void bridge(@NonNull CommandSession commandSession, String message) {
        pollingSessionManager.push(commandSession, message);
    }

}
