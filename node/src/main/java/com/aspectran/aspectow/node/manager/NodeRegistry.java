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
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides an API for retrieving information about registered cluster nodes
 * from the Redis storage.
 */
public class NodeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);

    private final String clusterId;

    private final RedisConnectionPool connectionPool;

    public NodeRegistry(String clusterId, RedisConnectionPool connectionPool) {
        this.clusterId = clusterId;
        this.connectionPool = connectionPool;
    }

    /**
     * Retrieves all registered nodes as NodeInfo objects.
     * @return a list of NodeInfo objects
     */
    public List<NodeInfo> getNodes() {
        Map<String, String> rawNodes = getAllNodes();
        List<NodeInfo> nodes = new ArrayList<>(rawNodes.size());
        for (String aponData : rawNodes.values()) {
            try {
                NodeInfo nodeInfo = new NodeInfo();
                nodeInfo.readFrom(aponData);
                nodes.add(nodeInfo);
            } catch (IOException e) {
                logger.warn("Failed to parse node info APON data", e);
            }
        }
        return nodes;
    }

    /**
     * Retrieves all registered nodes from Redis as raw APON strings.
     * @return a map of node IDs to their metadata (APON strings)
     */
    public Map<String, String> getAllNodes() {
        String key = NodeMessageProtocol.getNodesHashKey(clusterId);
        logger.debug("Retrieving all nodes from Redis hash: {}", key);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            logger.error("Failed to retrieve nodes from Redis registry", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves all registered groups from Redis for the current cluster.
     * @return a map of group IDs to their metadata (APON strings)
     */
    public Map<String, String> getAllGroups() {
        String key = NodeMessageProtocol.getGroupsHashKey(clusterId);
        logger.debug("Retrieving all groups from Redis hash: {}", key);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            logger.error("Failed to retrieve groups from Redis registry", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves all registered applications from Redis for a specific group.
     * @param groupId the group ID
     * @return a map of application IDs to their metadata (APON strings)
     */
    public Map<String, String> getAllApps(String groupId) {
        String key = NodeMessageProtocol.getAppsHashKey(groupId);
        logger.debug("Retrieving all apps for group: {} from Redis hash: {}", groupId, key);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            logger.error("Failed to retrieve apps for group {} from Redis registry", groupId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves the last pulse timestamps for all nodes.
     * @return a map of node IDs to their last pulse timestamps
     */
    public Map<String, String> getAllPulses() {
        String key = NodeMessageProtocol.getPulsesHashKey(clusterId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hgetall(key);
        } catch (Exception e) {
            logger.error("Failed to retrieve node pulses from Redis registry", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves a specific node's information.
     * @param nodeId the node ID
     * @return the node metadata string, or null if not found
     */
    public String getNode(String nodeId) {
        String key = NodeMessageProtocol.getNodesHashKey(clusterId);
        logger.debug("Retrieving node info for: {} from {}", nodeId, key);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            return connection.sync().hget(key, nodeId);
        } catch (Exception e) {
            logger.error("Failed to retrieve node info for {} from Redis registry", nodeId, e);
            return null;
        }
    }

    /**
     * Retrieves a specific node's information as a NodeInfo object.
     * @param nodeId the node ID
     * @return the node information as a NodeInfo object, or null if not found
     */
    public NodeInfo getNodeInfo(String nodeId) {
        String aponData = getNode(nodeId);
        if (aponData == null) {
            return null;
        }
        try {
            NodeInfo nodeInfo = new NodeInfo();
            nodeInfo.readFrom(aponData);
            return nodeInfo;
        } catch (IOException e) {
            logger.warn("Failed to parse node info APON data", e);
            return null;
        }
    }

    public boolean isFound(String nodeId) {
        return getNode(nodeId) != null;
    }

    /**
     * Checks if a node is considered 'live' based on its last pulse timestamp.
     * @param nodeId the node ID
     * @param timeoutMillis the timeout threshold in milliseconds
     * @return true if the node is live, false otherwise
     */
    public boolean isLive(String nodeId, long timeoutMillis) {
        String key = NodeMessageProtocol.getPulsesHashKey(clusterId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            String pulse = connection.sync().hget(key, nodeId);
            if (pulse != null) {
                try {
                    long lastPulse = Long.parseLong(pulse);
                    return (System.currentTimeMillis() - lastPulse <= timeoutMillis);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to check liveness for node {} from Redis registry", nodeId, e);
        }
        return false;
    }

    /**
     * Evicts nodes that have not sent a pulse within the specified timeout.
     * Also cleans up orphaned group and app metadata.
     * @param timeoutMillis the timeout threshold in milliseconds
     */
    public void evictZombieNodes(long timeoutMillis) {
        String nodesKey = NodeMessageProtocol.getNodesHashKey(clusterId);
        String pulsesKey = NodeMessageProtocol.getPulsesHashKey(clusterId);
        String groupsKey = NodeMessageProtocol.getGroupsHashKey(clusterId);
        try (StatefulRedisConnection<String, String> connection = connectionPool.getConnection()) {
            RedisCommands<String, String> sync = connection.sync();
            Map<String, String> pulses = sync.hgetall(pulsesKey);
            long now = System.currentTimeMillis();
            boolean evicted = false;
            for (Map.Entry<String, String> entry : pulses.entrySet()) {
                String nodeId = entry.getKey();
                try {
                    long lastPulse = Long.parseLong(entry.getValue());
                    if (now - lastPulse > timeoutMillis) {
                        logger.info("Evicting zombie node '{}' from cluster '{}'", nodeId, clusterId);
                        sync.hdel(nodesKey, nodeId);
                        sync.hdel(pulsesKey, nodeId);
                        evicted = true;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            if (evicted) {
                // Metadata Garbage Collection: Remove groups and apps that no longer have active nodes
                Map<String, String> remainingNodes = sync.hgetall(nodesKey);
                java.util.Set<String> activeGroups = new java.util.HashSet<>();
                for (String aponData : remainingNodes.values()) {
                    try {
                        NodeInfo info = new NodeInfo();
                        info.readFrom(aponData);
                        if (info.getGroup() != null) {
                            activeGroups.add(info.getGroup());
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }

                Map<String, String> registeredGroups = sync.hgetall(groupsKey);
                for (String gid : registeredGroups.keySet()) {
                    if (!activeGroups.contains(gid)) {
                        logger.info("Cleaning up orphaned group metadata: {} (Cluster: {})", gid, clusterId);
                        sync.hdel(groupsKey, gid);
                        sync.del(NodeMessageProtocol.getAppsHashKey(gid));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to evict zombie nodes and metadata from cluster '{}'", clusterId, e);
        }
    }

}
