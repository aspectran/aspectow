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

import com.aspectran.aspectow.appmon.common.auth.AppMonTokenIssuer;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.management.nodes.NodeRequestParameters;
import com.aspectran.aspectow.node.management.nodes.NodeResponseParameters;
import com.aspectran.aspectow.node.manager.ClusterEventListener;
import com.aspectran.aspectow.node.manager.ClusterEventSubscriber;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
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

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * NodeGatewayEndpoint provides real-time, bidirectional communication for cluster node management.
 *
 * <p>Created: 2026-04-19</p>
 */
@Component
@ServerEndpoint(
        value = NODES_BASE_PATH + "/{nodeId}/websocket/{token}",
        configurator = AspectranConfigurator.class
)
public class NodeGatewayEndpoint extends SimplifiedEndpoint implements ClusterEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeGatewayEndpoint.class);

    private final NodeManager nodeManager;

    /**
     * Constructs a new {@code NodeGatewayEndpoint} with the specified node manager.
     * @param nodeManager the node manager
     */
    @Autowired
    public NodeGatewayEndpoint(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    /**
     * Registers this endpoint as a cluster event listener when initialized.
     */
    @Initialize
    public void registerListener() {
        ClusterEventSubscriber subscriber = nodeManager.getClusterEventSubscriber();
        if (subscriber != null) {
            subscriber.addListener(this);
            logger.info("NodeGatewayEndpoint registered as ClusterEventListener");
        }
    }

    /**
     * Unregisters this endpoint as a cluster event listener when destroyed.
     */
    @Destroy
    public void unregisterListener() {
        ClusterEventSubscriber subscriber = nodeManager.getClusterEventSubscriber();
        if (subscriber != null) {
            subscriber.removeListener(this);
        }
    }

    /**
     * Checks if the session is authorized using the path parameter token.
     * @param session the websocket session
     * @return true if authorized, false otherwise
     */
    @Override
    protected boolean checkAuthorized(@NonNull Session session) {
        String token = session.getPathParameters().get("token");
        try {
            AppMonTokenIssuer.validateToken(token);
        } catch (InvalidPBTokenException e) {
            logger.error("Invalid token: {}", token);
            return false;
        }
        return true;
    }

    /**
     * Registers the message handler to process incoming text messages from the session.
     * @param session the websocket session
     */
    @Override
    protected void registerMessageHandlers(@NonNull Session session) {
        if (session.getMessageHandlers().isEmpty()) {
            session.addMessageHandler(String.class, message -> handleMessage(session, message));
        }
    }

    /**
     * Handles clean up tasks when a session is removed.
     * @param session the websocket session that was removed
     */
    @Override
    protected void onSessionRemoved(@NonNull Session session) {
        String nodeId = session.getPathParameters().get("nodeId");
        logger.info("Node management session removed: {} (nodeId: {})", session.getId(), nodeId);
    }

    /**
     * Invoked when a new node joins the cluster.
     * Broadcasts the join event to all subscribed sessions.
     * @param nodeInfo the node information of the joined node
     */
    @Override
    public void onNodeJoined(NodeInfo nodeInfo) {
        NodeResponseParameters params = new NodeResponseParameters();
        params.setHeader("joined");
        params.setNode(nodeInfo);
        broadcast(params.toString());
    }

    /**
     * Invoked when a node leaves the cluster.
     * Broadcasts the leave event to all subscribed sessions.
     * @param nodeId the ID of the node that left
     */
    @Override
    public void onNodeLeft(String nodeId) {
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setId(nodeId);
        nodeInfo.setStatus("offline");

        NodeResponseParameters params = new NodeResponseParameters();
        params.setHeader("left");
        params.setNode(nodeInfo);
        broadcast(params.toString());
    }

    private void handleMessage(Session session, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }

        try {
            NodeRequestParameters request = JsonToParameters.from(message, NodeRequestParameters.class);

            String header = request.getHeader();
            if ("subscribe".equals(header)) {
                subscribe(session);
            } else if ("established".equals(header)) {
                established(session);
            } else if ("ping".equals(header)) {
                pong(session);
            }
        } catch (Exception e) {
            logger.error("Failed to parse incoming request message: {}", message, e);
            NodeResponseParameters response = new NodeResponseParameters()
                    .setError("Invalid request format");
            sendText(session, response.toString());
        }
    }

    private void pong(Session session) {
        NodeResponseParameters response = new NodeResponseParameters()
                .setHeader("pong");
        sendText(session, response.toString());
    }

    private void subscribe(Session session) {
        if (addSession(session)) {
            NodeResponseParameters response = new NodeResponseParameters()
                    .setHeader("subscribed");
            sendText(session, response.toString());
        }
    }

    private void established(@NonNull Session session) {
        String nodeId = session.getPathParameters().get("nodeId");
        logger.info("Node management session established: {} (nodeId: {})", session.getId(), nodeId);
    }

}
