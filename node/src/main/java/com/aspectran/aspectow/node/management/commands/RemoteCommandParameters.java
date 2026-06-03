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

import com.aspectran.daemon.command.CommandParameters;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Represents a structured message for remote command execution.
 * It encapsulates the command type, routing information, and the command payload.
 */
public class RemoteCommandParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey targetNodeId;
    public static final ParameterKey sourceNodeId;
    public static final ParameterKey sessionId;
    public static final ParameterKey targetGroup;
    public static final ParameterKey targetAll;
    public static final ParameterKey command;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        sourceNodeId = new ParameterKey("sourceNodeId", ValueType.STRING);
        sessionId = new ParameterKey("sessionId", ValueType.STRING);
        targetGroup = new ParameterKey("targetGroup", ValueType.STRING);
        targetAll = new ParameterKey("targetAll", ValueType.BOOLEAN);
        command = new ParameterKey("command", CommandParameters.class);

        parameterKeys = new ParameterKey[] {
                header,
                targetNodeId,
                sourceNodeId,
                sessionId,
                targetGroup,
                targetAll,
                command
        };
    }

    public RemoteCommandParameters() {
        super(parameterKeys);
    }

    public String getHeader() {
        return getString(header);
    }

    public void setHeader(String header) {
        putValue(RemoteCommandParameters.header, header);
    }

    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    public void setTargetNodeId(String targetNodeId) {
        putValue(RemoteCommandParameters.targetNodeId, targetNodeId);
    }

    public String getSourceNodeId() {
        return getString(sourceNodeId);
    }

    public void setSourceNodeId(String sourceNodeId) {
        putValue(RemoteCommandParameters.sourceNodeId, sourceNodeId);
    }

    public String getSessionId() {
        return getString(sessionId);
    }

    public void setSessionId(String sessionId) {
        putValue(RemoteCommandParameters.sessionId, sessionId);
    }

    public String getTargetGroup() {
        return getString(targetGroup);
    }

    public boolean isTargetAll() {
        return getBoolean(targetAll, false);
    }

    public CommandParameters getCommand() {
        return getParameters(command);
    }

    public void setCommand(CommandParameters command) {
        putValue(RemoteCommandParameters.command, command);
    }

    public void setCommand(String command) {
        putValue(RemoteCommandParameters.command, command);
    }

}
