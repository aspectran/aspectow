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
package com.aspectran.aspectow.node.manager;

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.redis.RedisConnectionPool;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Specifically listens to the cluster-wide event channel and notifies
 * registered listeners about node join and leave events.
 *
 * <p>Created: 2026-05-24</p>
 */
public class ClusterEventSubscriber extends RedisPubSubAdapter<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(ClusterEventSubscriber.class);

    private final String clusterId;

    private final RedisConnectionPool connectionPool;

    private final Set<ClusterEventListener> listeners = new CopyOnWriteArraySet<>();

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    public ClusterEventSubscriber(String clusterId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.connectionPool = connectionPool;
    }

    public void addListener(ClusterEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClusterEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void message(@NonNull String channel, String message) {
        if (!channel.equals(NodeMessageProtocol.getClusterEventsChannel(clusterId))) {
            return;
        }

        if (message.startsWith("JOINED:")) {
            String aponData = message.substring(7);
            try {
                NodeInfo info = new NodeInfo();
                info.readFrom(aponData);
                for (ClusterEventListener listener : listeners) {
                    listener.onJoined(info);
                }
            } catch (IOException e) {
                logger.warn("Failed to parse JOINED event data", e);
            }
        } else if (message.startsWith("LEFT:")) {
            String leftNodeId = message.substring(5);
            for (ClusterEventListener listener : listeners) {
                listener.onLeft(leftNodeId);
            }
        }
    }

    public void start() {
        this.pubSubConnection = connectionPool.getPubSubConnection();
        this.pubSubConnection.addListener(this);

        String eventsChannel = NodeMessageProtocol.getClusterEventsChannel(clusterId);
        this.pubSubConnection.sync().subscribe(eventsChannel);
        logger.info("ClusterEventSubscriber initialized and subscribed to channel: {}", eventsChannel);
    }

    public void stop() {
        if (pubSubConnection != null) {
            pubSubConnection.removeListener(this);
            pubSubConnection.sync().unsubscribe();
            pubSubConnection.close();
            pubSubConnection = null;
        }
    }

}
