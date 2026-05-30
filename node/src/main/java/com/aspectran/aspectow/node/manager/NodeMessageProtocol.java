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

import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;

/**
 * Defines the Redis key structures and communication patterns used for
 * cluster node management and inter-node communication relay.
 */
public abstract class NodeMessageProtocol {

    public static final String NODES_BASE_PATH = "/nodes";

    public static final String CATEGORY_CLUSTER = "cluster";

    public static final String TYPE_NODES = "nodes";

    public static final String TYPE_PULSE = "pulse";

    public static final String TYPE_CONTROL = "control";

    public static final String TYPE_RELAY = "relay";

    public static final String TYPE_EVENT = "event";

    private static final String KEY_PREFIX = "aspectow:cluster:";

    /**
     * Returns the Redis Hash key for storing node metadata for a specific cluster.
     * @param clusterId the cluster ID
     * @return the Redis key
     */
    @NonNull
    public static String getNodesHashKey(String clusterId) {
        return KEY_PREFIX + TYPE_NODES + ":" + clusterId;
    }

    /**
     * Returns the Redis Hash key for storing node pulse timestamps for a specific cluster.
     * @param clusterId the cluster ID
     * @return the Redis key
     */
    @NonNull
    public static String getPulsesHashKey(String clusterId) {
        return KEY_PREFIX + TYPE_PULSE + ":" + clusterId;
    }

    /**
     * Returns the Redis Pub/Sub channel for cluster-wide events
     * (e.g., node joined, node left).
     * @param clusterId the cluster ID
     * @return the channel name
     */
    @NonNull
    public static String getClusterEventsChannel(String clusterId) {
        return KEY_PREFIX + TYPE_EVENT + ":" + clusterId;
    }

    /**
     * Returns the Redis Pub/Sub channel for management and control messages
     * (e.g., restart, quit, status requests).
     * @param clusterId the cluster ID
     * @param nodeId the node ID
     * @return the channel name
     */
    @NonNull
    public static String getControlChannel(String clusterId, String nodeId) {
        return KEY_PREFIX + TYPE_CONTROL + "::" + clusterId + ":" + nodeId + ":";
    }

    @NonNull
    public static String getControlChannel(String category, String clusterId, String nodeId) {
        return KEY_PREFIX + TYPE_CONTROL + ":" + category + ":" + clusterId + ":" + nodeId + ":";
    }

    /**
     * Returns the Redis Pub/Sub channel for transparently relaying application-specific
     * messages (e.g., AppMon logs, Remote Command data).
     * @param category the category of the relay message
     * @param clusterId the cluster ID
     * @param nodeId the node ID
     * @return the channel name
     */
    @NonNull
    public static String getRelayChannel(String category, String clusterId, String nodeId) {
        return KEY_PREFIX + TYPE_RELAY + ":" + category + ":" + clusterId + ":" + nodeId + ":";
    }

    @NonNull
    public static String getRelayChannel(String category, String clusterId, String nodeId, String sessionId) {
        return KEY_PREFIX + TYPE_RELAY + ":" + category + ":" + clusterId + ":" + nodeId + ":" + StringUtils.nullToEmpty(sessionId);
    }

    /**
     * Returns the Redis Pub/Sub pattern for subscribing to all channels
     * within a specific cluster (both control and relay).
     * @param clusterId the cluster ID
     * @return the subscription pattern
     */
    @NonNull
    public static String getClusterSubscriptionPattern(String clusterId) {
        return KEY_PREFIX + "*:*:" + clusterId + ":*:*";
    }

    /**
     * Returns the Redis Pub/Sub pattern for subscribing to all channels
     * for a specific node within a cluster.
     * @param clusterId the cluster ID
     * @param nodeId the node ID
     * @return the subscription pattern
     */
    @NonNull
    public static String getClusterSubscriptionPattern(String clusterId, String nodeId) {
        return KEY_PREFIX + "*:*:" + clusterId + ":" + nodeId + ":*";
    }

    /**
     * Returns the Redis Pub/Sub pattern for subscribing to relay channels
     * of a specific category for a node within a cluster.
     * @param category the category of the relay message
     * @param clusterId the cluster ID
     * @param nodeId the node ID
     * @return the subscription pattern
     */
    @NonNull
    public static String getRelaySubscriptionPattern(String category, String clusterId, String nodeId) {
        return KEY_PREFIX + TYPE_RELAY + ":" + category + ":" + clusterId + ":" + nodeId + ":*";
    }

}
