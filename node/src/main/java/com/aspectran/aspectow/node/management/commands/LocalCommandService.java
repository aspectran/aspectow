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
package com.aspectran.aspectow.node.management.commands;

import com.aspectran.aspectow.node.management.anatomy.AnatomyDataService;
import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.activity.request.ParameterMap;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.daemon.command.CommandResult;
import com.aspectran.daemon.service.DefaultDaemonService;
import com.aspectran.daemon.service.DefaultDaemonServiceBuilder;
import com.aspectran.utils.json.JsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * LocalCommandService handles the actual execution of daemon commands
 * within the local node using DaemonService.
 */
public class LocalCommandService {

    private static final Logger logger = LoggerFactory.getLogger(LocalCommandService.class);

    private final NodeManager nodeManager;

    private final AnatomyDataService anatomyDataService = new AnatomyDataService();

    private DefaultDaemonService daemonService;

    public LocalCommandService(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    private synchronized void setupDaemonService() {
        if (daemonService != null) {
            return;
        }

        for (CoreService service : CoreServiceHolder.getAllServices()) {
            if (service instanceof DefaultDaemonService ds) {
                daemonService = ds;
                break;
            }
        }

        if (daemonService == null) {
            CoreService baseService = null;
            for (CoreService service : CoreServiceHolder.getAllServices()) {
                baseService = service.getRootService();
                break;
            }

            if (baseService != null) {
                logger.info("No active DaemonService found; starting a new one based on root service [{}]",
                        baseService.getServiceName());
                try {
                    daemonService = DefaultDaemonServiceBuilder.build(baseService);
                    if (daemonService.getServiceLifeCycle().isOrphan()) {
                        daemonService.start();
                    }
                } catch (Exception e) {
                    logger.error("Failed to build and start DaemonService", e);
                }
            }
        }
    }

    /**
     * Executes a daemon command on the local node.
     * @param commandParameters the command payload in APON/JSON format
     * @return the execution result
     */
    public CommandResult execute(CommandParameters commandParameters) {
        if (daemonService == null) {
            setupDaemonService();
        }

        if (daemonService != null) {
            try {
                CommandResult commandResult = daemonService.execute(commandParameters);
                if (!commandResult.isSuccess() && commandResult.getError() != null) {
                    logger.error("Local command execution failed: {}", commandResult.getError());
                }
                return commandResult;
            } catch (Exception e) {
                logger.error("Error executing local daemon command", e);
                return new CommandResult(false, "[FAILED] Error executing local daemon command", e.getMessage());
            }
        } else {
            logger.warn("DaemonService is not available for local command processing");
            return new CommandResult(false, "[FAILED] Local DaemonService is not available");
        }
    }

    /**
     * Executes a node control command (e.g. pause, resume) on the local node.
     * @param command the command payload parameters
     * @return the execution result
     */
    public CommandResult executeControl(String command) {
        if (command != null) {
            String commandTrimmed = command.trim();
            if ("pause".equalsIgnoreCase(commandTrimmed)) {
                boolean found = false;
                for (CoreService service : CoreServiceHolder.getAllServices()) {
                    if (service.isWebService()) {
                        if ("console".equals(service.getContextName())) {
                            continue;
                        }
                        try {
                            service.getServiceLifeCycle().pause();
                            found = true;
                        } catch (Exception e) {
                            logger.error("Failed to pause WebService: {}", service.getServiceName(), e);
                        }
                    }
                }
                if (found) {
                    if (nodeManager != null && nodeManager.getNodeReporter() != null) {
                        nodeManager.getNodeReporter().updateStatus("paused");
                    }
                    return new CommandResult(true, "[SUCCESS] Web service has been paused");
                } else {
                    return new CommandResult(false, "[FAILED] WebService instance not found");
                }
            } else if ("resume".equalsIgnoreCase(commandTrimmed)) {
                boolean found = false;
                for (CoreService service : CoreServiceHolder.getAllServices()) {
                    if (service.isWebService()) {
                        if ("console".equals(service.getContextName())) {
                            continue;
                        }
                        try {
                            service.getServiceLifeCycle().resume();
                            found = true;
                        } catch (Exception e) {
                            logger.error("Failed to resume WebService: {}", service.getServiceName(), e);
                        }
                    }
                }
                if (found) {
                    if (nodeManager != null && nodeManager.getNodeReporter() != null) {
                        nodeManager.getNodeReporter().updateStatus("live");
                    }
                    return new CommandResult(true, "[SUCCESS] Web service has been resumed");
                } else {
                    return new CommandResult(false, "[FAILED] WebService instance not found");
                }
            }
        }
        return new CommandResult(false, "[FAILED] Unknown node control command: " + command);
    }

    /**
     * Executes an anatomy command to collect ActivityContext information on the local node.
     * @param commandParameters the command parameters
     * @return the execution result
     */
    public CommandResult executeAnatomy(CommandParameters commandParameters) {
        try {
            ParameterMap parameterMap = (commandParameters != null ? commandParameters.getParameterMap() : null);
            String mode = (parameterMap != null ? parameterMap.getParameter("mode") : null);
            if ("contexts".equalsIgnoreCase(mode)) {
                List<String> names = AnatomyDataService.getContextNames();
                JsonBuilder jsonBuilder = new JsonBuilder().array();
                for (String name : names) {
                    jsonBuilder.put(name);
                }
                jsonBuilder.endArray();
                return new CommandResult(true, jsonBuilder.toString());
            } else if ("data".equalsIgnoreCase(mode)) {
                String contextName = (parameterMap != null ? parameterMap.getParameter("contextName") : null);
                Map<String, ActivityContext> contextMap = AnatomyDataService.prepareContextMap();
                ActivityContext context = null;
                if (contextName != null) {
                    context = contextMap.get(contextName);
                }
                if (context == null && !contextMap.isEmpty()) {
                    context = contextMap.values().iterator().next();
                }
                if (context == null) {
                    return new CommandResult(false, null, "ActivityContext not found: " + contextName);
                }
                Map<String, Object> data = anatomyDataService.getAnatomyData(context);
                JsonBuilder jsonBuilder = new JsonBuilder().nullWritable(false).put(data);
                return new CommandResult(true, jsonBuilder.toString());
            } else {
                return new CommandResult(false, null, "Unsupported anatomy command mode: " + mode);
            }
        } catch (Exception e) {
            logger.error("Failed to execute anatomy command", e);
            return new CommandResult(false, null, "Failed to execute anatomy command: " + e.getMessage());
        }
    }

}
