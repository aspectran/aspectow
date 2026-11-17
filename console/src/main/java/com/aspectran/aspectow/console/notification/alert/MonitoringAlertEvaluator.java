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
package com.aspectran.aspectow.console.notification.alert;

import com.aspectran.aspectow.console.common.db.mapper.NotificationMapper;
import com.aspectran.aspectow.console.notification.NotificationService;
import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.aspectow.node.manager.ClusterEventListener;
import com.aspectran.aspectow.node.manager.ClusterEventSubscriber;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitors cluster node membership events and resource metric thresholds (CPU, Memory).
 * Implements state-machine alert throttling to ensure alerts fire only once upon entering
 * an alerting state, and once upon normal recovery.
 *
 * <p>Created: 2026-11-17</p>
 */
@Component
public class MonitoringAlertEvaluator extends InstantActivitySupport
        implements InitializableBean, DisposableBean, ClusterEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringAlertEvaluator.class);

    public enum AlertState {
        NORMAL,
        ALERTING
    }

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final NodeManager nodeManager;

    // Key: nodeId + ":" + metricType (e.g. "node-1:CPU", "node-1:MEMORY")
    private final Map<String, AlertState> alertStates = new ConcurrentHashMap<>();
    private final Map<String, Double> lastValues = new ConcurrentHashMap<>();
    private final Map<String, Long> localEventTimestamps = new ConcurrentHashMap<>();

    private ScheduledExecutorService metricScheduler;

    @Autowired(required = false)
    public MonitoringAlertEvaluator(NotificationService notificationService,
                                    NotificationMapper notificationMapper,
                                    NodeManager nodeManager) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
        this.nodeManager = nodeManager;
    }

    @Override
    public void initialize() {
        if (nodeManager != null) {
            ClusterEventSubscriber subscriber = nodeManager.getClusterEventSubscriber();
            if (subscriber != null) {
                subscriber.addListener(this);
                logger.info("MonitoringAlertEvaluator registered as ClusterEventListener");
            }
        }
        metricScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MonitoringAlertEvaluator");
            t.setDaemon(true);
            return t;
        });
        metricScheduler.scheduleWithFixedDelay(this::sampleLocalMetrics, 15, 30, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        if (metricScheduler != null) {
            metricScheduler.shutdownNow();
            metricScheduler = null;
        }
        if (nodeManager != null) {
            ClusterEventSubscriber subscriber = nodeManager.getClusterEventSubscriber();
            if (subscriber != null) {
                subscriber.removeListener(this);
            }
        }
        alertStates.clear();
        lastValues.clear();
        localEventTimestamps.clear();
    }

    /**
     * Samples local host/JVM CPU and memory metrics and evaluates them against user thresholds.
     */
    private void sampleLocalMetrics() {
        try {
            String nodeId = (nodeManager != null && nodeManager.getNodeId() != null)
                    ? nodeManager.getNodeId() : "console";
            java.lang.management.OperatingSystemMXBean baseOsBean = ManagementFactory.getOperatingSystemMXBean();
            if (baseOsBean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                double cpuLoad = osBean.getCpuLoad();
                if (cpuLoad >= 0.0) {
                    evaluateMetric(nodeId, "CPU", cpuLoad * 100.0);
                }
                long totalMem = osBean.getTotalMemorySize();
                long freeMem = osBean.getFreeMemorySize();
                if (totalMem > 0) {
                    double memUsage = ((double) (totalMem - freeMem) / totalMem) * 100.0;
                    evaluateMetric(nodeId, "MEMORY", memUsage);
                }
            }
        } catch (Throwable t) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to sample local metrics for alert evaluation: {}", t.getMessage());
            }
        }
    }

    // --- Cluster Membership Events (Node Down / Up) ---

    @Override
    public void onNodeLeft(String nodeId) {
        logger.warn("Cluster node offline detected: {}", nodeId);
        if (acquireClusterEventLock("nodeLeft", nodeId)) {
            if (notificationService != null) {
                notificationService.dispatchNotification(
                        "MONITORING",
                        "CRITICAL",
                        "Cluster Node Offline",
                        "Node '" + nodeId + "' has disconnected from the cluster.",
                        "/cluster/nodes/"
                );
            }
        } else {
            logger.debug("Skipping duplicate offline notification for node '{}' (already handled by another node)", nodeId);
        }
    }

    @Override
    public void onNodeJoined(NodeInfo nodeInfo) {
        String nodeId = nodeInfo.getId();
        logger.info("Cluster node joined/reconnected: {}", nodeId);
        if (acquireClusterEventLock("nodeJoined", nodeId)) {
            if (notificationService != null) {
                notificationService.dispatchNotification(
                        "MONITORING",
                        "RESOLVED",
                        "Cluster Node Connected",
                        "Node '" + nodeId + "' has joined/reconnected to the cluster.",
                        "/cluster/nodes/"
                );
            }
        } else {
            logger.debug("Skipping duplicate joined notification for node '{}' (already handled by another node)", nodeId);
        }
    }

    /**
     * Attempts to acquire a distributed cluster event lock to ensure that only
     * a single node dispatches membership alerts across the entire cluster.
     * @param eventType the event type (e.g. "nodeLeft", "nodeJoined")
     * @param targetNodeId the target node identifier
     * @return {@code true} if this node acquired the lock and should dispatch; {@code false} otherwise
     */
    private boolean acquireClusterEventLock(String eventType, String targetNodeId) {
        if (nodeManager != null && nodeManager.getRedisConnectionPool() != null) {
            try (StatefulRedisConnection<String, String> connection = nodeManager.getRedisConnectionPool().getConnection()) {
                RedisCommands<String, String> sync = connection.sync();
                String lockKey = "aspectow:notif:dedup:" + eventType + ":" + targetNodeId;
                String result = sync.set(lockKey, nodeManager.getNodeId(), SetArgs.Builder.nx().ex(15));
                return "OK".equalsIgnoreCase(result);
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Redis deduplication lock error, falling back to local memory: {}", e.getMessage());
                }
            }
        }
        return acquireLocalEventLock(eventType, targetNodeId);
    }

    private boolean acquireLocalEventLock(String eventType, String targetNodeId) {
        String key = eventType + ":" + targetNodeId;
        long now = System.currentTimeMillis();
        Long last = localEventTimestamps.get(key);
        if (last != null && (now - last) < 15000L) {
            return false;
        }
        localEventTimestamps.put(key, now);
        return true;
    }

    @Override
    public void onNodeStatusChanged(NodeInfo nodeInfo) {
        // Optional status updates
    }

    // --- Resource Metric Threshold Evaluation (CPU, Memory) ---

    /**
     * Evaluates a resource metric sample for a specific node and triggers alert or recovery notification
     * if the state transitions between NORMAL and ALERTING.
     * @param nodeId the node identifier
     * @param metricType "CPU" or "MEMORY"
     * @param currentValue the current percentage value (0.0 to 100.0)
     */
    public void evaluateMetric(String nodeId, String metricType, double currentValue) {
        if (nodeId == null || metricType == null || notificationService == null) {
            return;
        }

        String stateKey = nodeId + ":" + metricType.toUpperCase();
        AlertState currentState = alertStates.getOrDefault(stateKey, AlertState.NORMAL);
        lastValues.put(stateKey, currentValue);

        // Determine effective threshold across subscribed users (or default 85%)
        int effectiveThreshold = determineThreshold(metricType);

        if (currentValue >= effectiveThreshold) {
            if (currentState == AlertState.NORMAL) {
                // Transition: NORMAL -> ALERTING (Fire alert once)
                alertStates.put(stateKey, AlertState.ALERTING);
                String title = "[Alert] High " + metricType.toUpperCase() + " Usage on " + nodeId;
                String msg = metricType.toUpperCase() + " usage reached " + Math.round(currentValue) +
                        "% (Threshold: " + effectiveThreshold + "%).";

                logger.warn("Metric alert triggered: {} -> {}%", stateKey, currentValue);
                notificationService.dispatchNotification(
                        "MONITORING",
                        "CRITICAL",
                        title,
                        msg,
                        "/appmon/dashboard/?nodeId=" + nodeId
                );
            }
            // If already ALERTING, suppress repeated alerts (Throttling)
        } else {
            if (currentState == AlertState.ALERTING) {
                // Transition: ALERTING -> NORMAL (Fire resolved notification once)
                alertStates.put(stateKey, AlertState.NORMAL);
                String title = "[Resolved] " + metricType.toUpperCase() + " Usage Normalized on " + nodeId;
                String msg = metricType.toUpperCase() + " usage has returned to normal (" +
                        Math.round(currentValue) + "%).";

                logger.info("Metric alert resolved: {} -> {}%", stateKey, currentValue);
                notificationService.dispatchNotification(
                        "MONITORING",
                        "RESOLVED",
                        title,
                        msg,
                        "/appmon/dashboard/?nodeId=" + nodeId
                );
            }
        }
    }

    private int determineThreshold(String metricType) {
        int defaultThreshold = 85;
        if (notificationMapper != null) {
            try {
                return instantActivity(() -> {
                    if ("CPU".equalsIgnoreCase(metricType)) {
                        int val = notificationMapper.getMinCpuThreshold();
                        return val > 0 ? val : defaultThreshold;
                    } else if ("MEMORY".equalsIgnoreCase(metricType)) {
                        int val = notificationMapper.getMinMemoryThreshold();
                        return val > 0 ? val : defaultThreshold;
                    }
                    return defaultThreshold;
                });
            } catch (Exception e) {
                // fallback to default
            }
        }
        return defaultThreshold;
    }

    public AlertState getAlertState(String nodeId, String metricType) {
        return alertStates.getOrDefault(nodeId + ":" + metricType.toUpperCase(), AlertState.NORMAL);
    }

}
