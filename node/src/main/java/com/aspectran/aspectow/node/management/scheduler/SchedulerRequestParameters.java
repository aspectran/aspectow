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
package com.aspectran.aspectow.node.management.scheduler;

import com.aspectran.utils.apon.AponRenderStyle;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Represents a structured request for scheduler management.
 */
public class SchedulerRequestParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey nodeId;
    public static final ParameterKey sessionId;
    public static final ParameterKey targetNodeId;
    public static final ParameterKey command;
    public static final ParameterKey serviceName;
    public static final ParameterKey scheduleId;
    public static final ParameterKey jobName;
    public static final ParameterKey loggingGroup;
    public static final ParameterKey loadedLines;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        sessionId = new ParameterKey("sessionId", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        command = new ParameterKey("command", ValueType.STRING);
        serviceName = new ParameterKey("serviceName", ValueType.STRING);
        scheduleId = new ParameterKey("scheduleId", ValueType.STRING);
        jobName = new ParameterKey("jobName", ValueType.STRING);
        loggingGroup = new ParameterKey("loggingGroup", ValueType.STRING);
        loadedLines = new ParameterKey("loadedLines", ValueType.INT);

        parameterKeys = new ParameterKey[] {
                header,
                nodeId,
                targetNodeId,
                sessionId,
                command,
                serviceName,
                scheduleId,
                jobName,
                loggingGroup,
                loadedLines
        };
    }

    /**
     * Instantiates a new SchedulerRequestParameters.
     */
    public SchedulerRequestParameters() {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
    }

    /**
     * Gets the request header.
     * @return the header
     */
    public String getHeader() {
        return getString(header);
    }

    /**
     * Sets the request header.
     * @param header the header to set
     */
    public void setHeader(String header) {
        putValue(SchedulerRequestParameters.header, header);
    }

    /**
     * Gets the node ID.
     * @return the node ID
     */
    public String getNodeId() {
        return getString(nodeId);
    }

    /**
     * Sets the node ID.
     * @param nodeId the node ID to set
     */
    public void setNodeId(String nodeId) {
        putValue(SchedulerRequestParameters.nodeId, nodeId);
    }

    /**
     * Gets the session ID.
     * @return the session ID
     */
    public String getSessionId() {
        return getString(sessionId);
    }

    /**
     * Sets the session ID.
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) {
        putValue(SchedulerRequestParameters.sessionId, sessionId);
    }

    /**
     * Gets the target node ID.
     * @return the target node ID
     */
    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    /**
     * Sets the target node ID.
     * @param targetNodeId the target node ID to set
     */
    public void setTargetNodeId(String targetNodeId) {
        putValue(SchedulerRequestParameters.targetNodeId, targetNodeId);
    }

    /**
     * Gets the command to execute.
     * @return the command
     */
    public String getCommand() {
        return getString(command);
    }

    /**
     * Sets the command to execute.
     * @param command the command to set
     */
    public void setCommand(String command) {
        putValue(SchedulerRequestParameters.command, command);
    }

    /**
     * Gets the service name.
     * @return the service name
     */
    public String getServiceName() {
        return getString(serviceName);
    }

    /**
     * Sets the service name.
     * @param serviceName the service name to set
     */
    public void setServiceName(String serviceName) {
        putValue(SchedulerRequestParameters.serviceName, serviceName);
    }

    /**
     * Gets the schedule ID.
     * @return the schedule ID
     */
    public String getScheduleId() {
        return getString(scheduleId);
    }

    /**
     * Sets the schedule ID.
     * @param scheduleId the schedule ID to set
     */
    public void setScheduleId(String scheduleId) {
        putValue(SchedulerRequestParameters.scheduleId, scheduleId);
    }

    /**
     * Gets the job name.
     * @return the job name
     */
    public String getJobName() {
        return getString(jobName);
    }

    /**
     * Sets the job name.
     * @param jobName the job name to set
     */
    public void setJobName(String jobName) {
        putValue(SchedulerRequestParameters.jobName, jobName);
    }

    /**
     * Gets the logging group.
     * @return the logging group
     */
    public String getLoggingGroup() {
        return getString(loggingGroup);
    }

    /**
     * Sets the logging group.
     * @param loggingGroup the logging group to set
     */
    public void setLoggingGroup(String loggingGroup) {
        putValue(SchedulerRequestParameters.loggingGroup, loggingGroup);
    }

    /**
     * Gets the number of already loaded lines.
     * @return the loaded lines count
     */
    public int getLoadedLines() {
        return getInt(loadedLines, 0);
    }

    /**
     * Sets the number of already loaded lines.
     * @param loadedLines the loaded lines count to set
     */
    public void setLoadedLines(int loadedLines) {
        putValue(SchedulerRequestParameters.loadedLines, loadedLines);
    }

}
