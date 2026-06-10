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
package com.aspectran.aspectow.console.scheduler.bridge.polling;

import com.aspectran.aspectow.node.management.scheduler.SchedulerManager;
import com.aspectran.aspectow.node.management.scheduler.SchedulerRequestParameters;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBridge;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerSession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker.CATEGORY_SCHEDULER;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * PollingSchedulerBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute scheduler management results.
 */
@Component(NODES_BASE_PATH + "/${thisNodeId}/" + CATEGORY_SCHEDULER)
public class PollingSchedulerBridge extends AbstractComponent implements SchedulerBridge {

    private final PollingSessionManager sessionManager;

    private final SchedulerManager schedulerManager;

    @Autowired
    public PollingSchedulerBridge(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
        this.sessionManager = new PollingSessionManager(this);
    }

    @Override
    protected void doInitialize() throws Exception {
        sessionManager.initialize();
    }

    @Override
    protected void doDestroy() throws Exception {
        sessionManager.destroy();
    }

    public SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }

    public SchedulerBroker getBroker() {
        return schedulerManager.getBroker();
    }

    /**
     * Allows a client to subscribe and start a polling session.
     * @param translet the current translet
     */
    @Request("/polling/subscribe")
    public RestResponse subscribe(@NonNull Translet translet) {
        String nodeId = translet.getParameter("nodeId");
        if (StringUtils.isEmpty(nodeId)) {
            return new FailureResponse("Node ID cannot be empty");
        }

        PollingSchedulerSession schedulerSession = sessionManager.getSession(translet);
        if (schedulerSession == null) {
            schedulerSession = sessionManager.createSession(translet);
            schedulerManager.registerSession(schedulerSession.getId(), this);
            schedulerManager.getBroker().subscribe(schedulerSession);
        }

        return new SuccessResponse(Map.of(
                "pollingInterval", schedulerSession.getPollingInterval(),
                "nodeId", schedulerManager.getNodeId(),
                "primary", schedulerManager.isSameNode(nodeId)
                )).ok();
    }

    /**
     * Allows a client to pull new messages from the server.
     * @param translet the current translet
     */
    @RequestToGet("/polling/pull")
    public RestResponse pull(@NonNull Translet translet) {
        try {
            PollingSchedulerSession session = sessionManager.getSession(translet);
            if (session != null) {
                String[] messages = sessionManager.pull(session);
                return new SuccessResponse(messages).ok();
            } else {
                return new FailureResponse().setError("session_not_found", "Session not found");
            }
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    /**
     * Creates a scheduler command to be sent to a specific node or all nodes.
     * @return a success message
     */
    @RequestToPost("/polling/push")
    public RestResponse push(@NonNull Translet translet, @NonNull SchedulerRequestParameters request) {
        PollingSchedulerSession session = sessionManager.getSession(translet);
        if (session == null) {
            return new FailureResponse().setError("not_found", "Session not found");
        }

        if (request.getCommand() == null) {
            return new FailureResponse().setError("required", "Command is required");
        }

        try {
            request.setSessionId(session.getId());
            schedulerManager.process(request);
            return new SuccessResponse("Scheduler command initiated successfully").ok();
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    @Override
    public SchedulerSession findSchedulerSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    @Override
    public void bridge(String message) {
        sessionManager.push(message);
    }

    @Override
    public void bridge(@NonNull SchedulerSession schedulerSession, String message) {
        sessionManager.push(schedulerSession, message);
    }

}
