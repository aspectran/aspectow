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
package com.aspectran.aspectow.console.cluster.bridge.websocket;

import com.aspectran.aspectow.node.management.nodes.bridge.NodeSession;
import com.aspectran.web.websocket.jsr356.WrappedSession;
import jakarta.websocket.Session;

/**
 * A {@link NodeSession} implementation that wraps a JSR-356 {@link Session}.
 * It stores session-specific data in the WebSocket session's user properties.
 */
public class WebsocketNodeSession extends WrappedSession implements NodeSession {

    private static final String NODE_ID_PROPERTY = "console:nodeId";

    /**
     * Instantiates a new WebsocketNodeSession.
     * @param session the underlying WebSocket session
     */
    public WebsocketNodeSession(Session session) {
        super(session);
    }

    /**
     * Returns the unique identifier of the WebSocket session.
     * @return the session ID
     */
    @Override
    public String getId() {
        return getSession().getId();
    }

    /**
     * Returns the target node ID associated with this node session.
     * @return the target node ID
     */
    @Override
    public String getNodeId() {
        return (String)getSession().getUserProperties().get(NODE_ID_PROPERTY);
    }

    /**
     * Sets the target node ID associated with this node session.
     * @param nodeId the target node ID
     */
    @Override
    public void setNodeId(String nodeId) {
        getSession().getUserProperties().put(NODE_ID_PROPERTY, nodeId);
    }

    /**
     * Returns whether the underlying WebSocket session is valid and open.
     * @return {@code true} if the session is open; {@code false} otherwise
     */
    @Override
    public boolean isValid() {
        return getSession().isOpen();
    }

}
