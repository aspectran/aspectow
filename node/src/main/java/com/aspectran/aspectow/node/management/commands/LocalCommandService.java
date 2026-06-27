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

import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.daemon.command.CommandResult;
import com.aspectran.daemon.service.DefaultDaemonService;
import com.aspectran.daemon.service.DefaultDaemonServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LocalCommandService handles the actual execution of daemon commands
 * within the local node using DaemonService.
 */
public class LocalCommandService {

    private static final Logger logger = LoggerFactory.getLogger(LocalCommandService.class);

    private final NodeManager nodeManager;

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
     * @param commandData the command payload in APON/JSON format
     * @return the execution result
     */
    public CommandResult execute(String commandData) {
        if (daemonService == null) {
            setupDaemonService();
        }

        if (daemonService != null) {
            try {
                CommandResult commandResult = daemonService.execute(commandData);
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
     * @param command the node control command
     * @return the execution result
     */
    public CommandResult executeControl(String command) {
        if (command != null) {
            String commandTrimmed = command.trim();
            if ("pause".equalsIgnoreCase(commandTrimmed)) {
                boolean found = false;
                for (CoreService service : CoreServiceHolder.getAllServices()) {
                    if (service.getClass().getName().endsWith("WebService") ||
                            service.getClass().getSimpleName().contains("WebService")) {
                        try {
                            java.lang.reflect.Method getContextNameMethod = service.getClass().getMethod("getContextName");
                            String contextName = (String) getContextNameMethod.invoke(service);
                            if ("console".equals(contextName)) {
                                continue;
                            }
                        } catch (Exception ignored) {
                            // ignore
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
                    if (service.getClass().getName().endsWith("WebService") ||
                            service.getClass().getSimpleName().contains("WebService")) {
                        try {
                            java.lang.reflect.Method getContextNameMethod = service.getClass().getMethod("getContextName");
                            String contextName = (String) getContextNameMethod.invoke(service);
                            if ("console".equals(contextName)) {
                                continue;
                            }
                        } catch (Exception ignored) {
                            // ignore
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

}
