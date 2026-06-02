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
package com.aspectran.aspectow.console.scheduler.manager;

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.console.scheduler.bridge.SchedulerRequestParameters;
import com.aspectran.aspectow.console.scheduler.bridge.SubscriptionRegistry;
import com.aspectran.aspectow.console.scheduler.bridge.remote.RemoteSchedulerMessageListener;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.aspectow.node.manager.NodeMessagePublisher;
import com.aspectran.core.adapter.ApplicationAdapter;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.ApplicationAdapterAware;
import com.aspectran.core.scheduler.service.SchedulerService;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.logging.LoggingDefaults;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker.DELIMITER;

/**
 * SchedulerManager orchestrates scheduler management across the cluster.
 * It manages local execution, remote dispatching via Redis, and broadcasting
 * results to connected clients.
 */
@Component
@Bean(id = "schedulerManager")
public class SchedulerManager implements ApplicationAdapterAware, InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

    private static final String OP_LIST = "list";
    private static final String OP_ENABLE = "enable";
    private static final String OP_DISABLE = "disable";
    private static final String OP_PREVIOUS = "previousLines";

    private final Map<String, SchedulerLogExporter> logExporters = new ConcurrentHashMap<>();

    private final NodeManager nodeManager;

    private final LocalSchedulerService localSchedulerService;

    private final SchedulerBroker broker;

    private ApplicationAdapter applicationAdapter;

    @Autowired
    public SchedulerManager(@NonNull NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.localSchedulerService = new LocalSchedulerService();
        this.broker = new SchedulerBroker(this);
    }

    @Override
    public void setApplicationAdapter(ApplicationAdapter applicationAdapter) {
        this.applicationAdapter = applicationAdapter;
    }

    @Override
    public void initialize() throws Exception {
        logger.info("Initializing SchedulerManager for node: {}", getNodeId());

        if (nodeManager.getNodeMessageSubscriber() != null) {
            RemoteSchedulerMessageListener bridgeHandler = new RemoteSchedulerMessageListener(this);
            nodeManager.getNodeMessageSubscriber().addListener(bridgeHandler);
        }
    }

    public SubscriptionRegistry getSubscriptionRegistry() {
        return broker.getSubscriptionRegistry();
    }

    public NodeMessagePublisher getNodeMessagePublisher() {
        return nodeManager.getNodeMessagePublisher();
    }

    public String getNodeId() {
        return nodeManager.getNodeId();
    }

    public synchronized void startExporters() {
        discoverLogFiles();
        for (SchedulerLogExporter exporter : logExporters.values()) {
            try {
                exporter.start();
            } catch (Exception e) {
                logger.error("Failed to start scheduler log exporter for context: {}", exporter.getLoggingGroup(), e);
            }
        }
    }

    /**
     * Collects the last known log lines from all active exporters.
     * @return a list of log messages
     */
    public List<String> collectLastMessages() {
        List<String> messages = new ArrayList<>();
        for (SchedulerLogExporter exporter : logExporters.values()) {
            if (exporter.isStarted()) {
                exporter.read(messages);
            }
        }
        return messages;
    }

    public synchronized void stopExporters() {
        for (SchedulerLogExporter exporter : logExporters.values()) {
            try {
                exporter.stop();
            } catch (Exception e) {
                logger.error("Failed to stop scheduler log exporter for context: {}", exporter.getLoggingGroup(), e);
            }
        }
    }

    private void discoverLogFiles() {
        String baseLogDir = System.getProperty(LoggingDefaults.LOGS_DIR_PROPERTY);
        File logsDir;
        if (StringUtils.hasText(baseLogDir)) {
            logsDir = applicationAdapter.getRealPath(baseLogDir).toFile();
        } else {
            logsDir = applicationAdapter.getRealPath(LoggingDefaults.DEFAULT_LOGS_DIR).toFile();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Discovering scheduler log files in: {}", logsDir.getAbsolutePath());
        }

        String logCharset = System.getProperty(LoggingDefaults.LOG_CHARSET_PROPERTY);

        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service.getServiceLifeCycle().isActive()) {
                SchedulerService schedulerService = service.getSchedulerService();
                if (schedulerService != null) {
                    String loggingGroup = schedulerService.getLoggingGroup();
                    if (!logExporters.containsKey(loggingGroup)) {
                        File logFile = new File(logsDir, loggingGroup + "-scheduler.log");
                        if (logFile.exists()) {
                            logger.info("Discovered scheduler log file: {}", logFile.getAbsolutePath());
                            // Currently using default settings; future: apply injected overrides
                            SchedulerLogExporter exporter = new SchedulerLogExporter(
                                    loggingGroup, logFile, broker, logCharset);
                            logExporters.put(loggingGroup, exporter);
                        } else if (logger.isDebugEnabled()) {
                            logger.debug("Scheduler log file not found: {}", logFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    public SchedulerBroker getBroker() {
        return broker;
    }

    /**
     * Dispatches a management request to a specific node or handles it locally.
     * @param targetNodeId the ID of the node to receive the request
     * @param request the structured request parameters
     */
    public void dispatch(String targetNodeId, @NonNull SchedulerRequestParameters request) {
        if (getNodeId().equals(targetNodeId)) {
            logger.debug("Executing local scheduler request: {}", request.getCommand());
            String response = execute(request);
            if (response != null) {
                broadcast(response);
            }
        } else {
            if (nodeManager.getNodeMessagePublisher() != null) {
                try {
                    request.setSourceNodeId(getNodeId());
                    String message = "command:" + request;
                    nodeManager.getNodeMessagePublisher().publishRelay(SchedulerBroker.CATEGORY_SCHEDULER, targetNodeId, message);
                    logger.debug("Scheduler request dispatched to node {}: {}", targetNodeId, request.getCommand());
                } catch (Exception e) {
                    logger.error("Failed to dispatch scheduler request to node {}", targetNodeId, e);
                }
            } else {
                logger.warn("Cannot dispatch request to node {}: Redis publisher not available", targetNodeId);
            }
        }
    }

    /**
     * Processes an incoming message received from the cluster relay.
     * @param message the raw relay message
     */
    public void process(String message) {
        if (StringUtils.isEmpty(message) || !message.startsWith("command:")) {
            return;
        }

        String requestData = message.substring(8);
        try {
            SchedulerRequestParameters request = new SchedulerRequestParameters();
            request.readFrom(requestData);

            String response = execute(request);
            if (response != null && nodeManager.getNodeMessagePublisher() != null) {
                String requesterNodeId = request.getSourceNodeId();
                String sessionId = request.getSessionId();
                String relayMessage = getNodeId() + DELIMITER + response;
                if (requesterNodeId != null) {
                    if (sessionId != null) {
                        nodeManager.getNodeMessagePublisher().publishRelay(
                                SchedulerBroker.CATEGORY_SCHEDULER, requesterNodeId, sessionId, relayMessage);
                    } else {
                        nodeManager.getNodeMessagePublisher().publishRelay(
                                SchedulerBroker.CATEGORY_SCHEDULER, requesterNodeId, relayMessage);
                    }
                } else {
                    nodeManager.getNodeMessagePublisher().publishRelay(
                            SchedulerBroker.CATEGORY_SCHEDULER, relayMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process scheduler relay message", e);
        }
    }

    /**
     * Executes the management logic for a given request on the local node.
     * @param request the structured request parameters
     * @return the execution result as JSON string, or null if unhandled
     */
    @Nullable
    private String execute(SchedulerRequestParameters request) {
        try {
            String command = request.getCommand();
            if (OP_LIST.equals(command)) {
                return localSchedulerService.getSchedulesAsJson();
            } else if (OP_ENABLE.equals(command)) {
                return performStateChange(request, false);
            } else if (OP_DISABLE.equals(command)) {
                return performStateChange(request, true);
            } else if (OP_PREVIOUS.equals(command)) {
                return readPreviousLines(request);
            }
        } catch (Exception e) {
            logger.error("Failed to execute scheduler request", e);
        }
        return null;
    }

    @Nullable
    private String readPreviousLines(@NonNull SchedulerRequestParameters request) {
        String loggingGroup = request.getLoggingGroup();
        int loadedLines = request.getLoadedLines();
        if (loggingGroup != null) {
            List<String> lines = null;
            SchedulerLogExporter exporter = logExporters.get(loggingGroup);
            if (exporter != null) {
                lines = exporter.readPreviousLines(loadedLines);
            }
            if (lines == null) {
                lines = Collections.emptyList();
            }
            return new JsonBuilder().object()
                    .put("type", "previousLines")
                    .put("loggingGroup", loggingGroup)
                    .put("lines", lines)
                    .endObject()
                    .toString();
        }
        return null;
    }

    @Nullable
    private String performStateChange(@NonNull SchedulerRequestParameters request, boolean disabled) {
        String serviceName = request.getServiceName();
        String scheduleId = request.getScheduleId();
        String jobName = request.getJobName();

        if (jobName != null) {
            return localSchedulerService.updateState(serviceName, "job", jobName, disabled);
        } else if (scheduleId != null) {
            return localSchedulerService.updateState(serviceName, "schedule", scheduleId, disabled);
        }
        return null;
    }

    public void broadcast(String response) {
        broadcast(getNodeId(), response);
    }

    /**
     * Broadcasts a management result to all connected clients on this node.
     * @param sourceNodeId the ID of the node where the message originated
     * @param response the result payload in JSON format
     */
    public void broadcast(String sourceNodeId, String response) {
        if (logger.isTraceEnabled()) {
            logger.trace("Broadcasting scheduler result (source: {}) to local clients: {}", sourceNodeId, response);
        }
        if (broker != null) {
            broker.bridge(sourceNodeId, response);
        }
    }

}
