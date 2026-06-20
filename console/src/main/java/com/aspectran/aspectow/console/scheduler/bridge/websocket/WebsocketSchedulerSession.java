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
package com.aspectran.aspectow.console.scheduler.bridge.websocket;

import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerSession;
import com.aspectran.web.websocket.jsr356.WrappedSession;
import jakarta.websocket.Session;

/**
 * A {@link SchedulerSession} implementation that wraps a JSR-356 {@link Session}.
 */
public class WebsocketSchedulerSession extends WrappedSession implements SchedulerSession {

    private static final String NODE_ID_PROPERTY = "console:scheduler:nodeId";

    /**
     * Constructs a new {@code WebsocketSchedulerSession} wrapping the given JSR-356 {@link Session}.
     * @param session the WebSocket session
     */
    public WebsocketSchedulerSession(Session session) {
        super(session);
    }

    /**
     * Returns the unique identifier of this WebSocket session.
     * @return the session ID
     */
    @Override
    public String getId() {
        return getSession().getId();
    }

    /**
     * Gets the node identifier associated with this session.
     * @return the node ID, or {@code null} if not set
     */
    @Override
    public String getNodeId() {
        return (String)getSession().getUserProperties().get(NODE_ID_PROPERTY);
    }

    /**
     * Sets the node identifier associated with this session.
     * @param nodeId the node ID to associate with this session
     */
    @Override
    public void setNodeId(String nodeId) {
        getSession().getUserProperties().put(NODE_ID_PROPERTY, nodeId);
    }

    /**
     * Checks if the underlying WebSocket session is open and valid.
     * @return {@code true} if the session is open; {@code false} otherwise
     */
    @Override
    public boolean isValid() {
        return getSession().isOpen();
    }

}
