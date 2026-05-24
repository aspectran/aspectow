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
package com.aspectran.aspectow.node.redis;

import com.aspectran.aspectow.node.manager.NodeMessageProtocol;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.TYPE_CONTROL;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.TYPE_RELAY;

/**
 * Listens to management control and transparent relay channels and notifies
 * registered listeners based on the message category.
 */
public class RedisMessageSubscriber extends RedisPubSubAdapter<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final String clusterId;

    private final String nodeId;

    private final RedisConnectionPool connectionPool;

    private final Set<RedisMessageListener> listeners = new CopyOnWriteArraySet<>();

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private String subscribePattern;

    public RedisMessageSubscriber(String clusterId, String nodeId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.connectionPool = connectionPool;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void addListener(RedisMessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RedisMessageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the Redis Pub/Sub pattern to subscribe to.
     * If not set, the default node-specific pattern will be used.
     * @param subscribePattern the subscription pattern
     */
    public void setSubscribePattern(String subscribePattern) {
        this.subscribePattern = subscribePattern;
    }

    @Override
    public void message(String pattern, String channel, String message) {
        message(channel, message);
    }

    @Override
    public void message(@NonNull String channel, String message) {
        if (channel.equals(NodeMessageProtocol.getClusterEventsChannel(clusterId))) {
            for (RedisMessageListener listener : listeners) {
                listener.onClusterEvent(message);
            }
            return;
        }

        // Expected patterns:
        // aspectow:nodes:control:<category>:<clusterId>:<nodeId>
        // aspectow:nodes:relay:<category>:<clusterId>:<nodeId>:<sessionId>

        String[] parts = channel.split(":");
        if (parts.length < 6) {
            return;
        }

        String type = parts[2]; // control or relay
        String category = parts[3]; // appmon, commands, etc.
        String targetNodeId = parts[5]; // node ID
        String sessionId = (parts.length > 6 ? parts[6] : null); // session ID

        if (!nodeId.equals(targetNodeId)) {
            return;
        }

        if (TYPE_CONTROL.equals(type)) {
            for (RedisMessageListener listener : listeners) {
                String listenerCategory = listener.getCategory();
                if (listenerCategory == null || listenerCategory.equals(category)) {
                    listener.onControlMessage(targetNodeId, message);
                }
            }
        } else if (TYPE_RELAY.equals(type)) {
            for (RedisMessageListener listener : listeners) {
                String listenerCategory = listener.getCategory();
                if (listenerCategory == null || listenerCategory.equals(category)) {
                    if (sessionId != null) {
                        listener.onRelayMessage(targetNodeId, sessionId, message);
                    } else {
                        listener.onRelayMessage(targetNodeId, message);
                    }
                }
            }
        }
    }

    public void start() {
        this.pubSubConnection = connectionPool.getPubSubConnection();
        this.pubSubConnection.addListener(this);

        String pattern = (subscribePattern != null ? subscribePattern :
                NodeMessageProtocol.getClusterSubscriptionPattern(clusterId, nodeId));
        this.pubSubConnection.sync().psubscribe(pattern);

        String eventsChannel = NodeMessageProtocol.getClusterEventsChannel(clusterId);
        this.pubSubConnection.sync().subscribe(eventsChannel);
        logger.info("RedisMessageSubscriber initialized and subscribed to pattern: {} and channel: {}",
                pattern, eventsChannel);
    }

    public void stop() {
        if (pubSubConnection != null) {
            pubSubConnection.removeListener(this);
            pubSubConnection.sync().punsubscribe();
            pubSubConnection.close();
        }
    }

}
