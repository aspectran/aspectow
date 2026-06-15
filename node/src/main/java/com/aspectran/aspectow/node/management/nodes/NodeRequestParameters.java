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
package com.aspectran.aspectow.node.management.nodes;

import com.aspectran.utils.apon.AponRenderStyle;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

public class NodeRequestParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey nodeId;
    public static final ParameterKey targetNodeId;
    public static final ParameterKey command;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        targetNodeId = new ParameterKey("targetNodeId", ValueType.STRING);
        command = new ParameterKey("command", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                header,
                nodeId,
                targetNodeId,
                command
        };
    }

    public NodeRequestParameters() {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
    }

    public String getHeader() {
        return getString(header);
    }

    public void setHeader(String header) {
        putValue(NodeRequestParameters.header, header);
    }

    public String getNodeId() {
        return getString(nodeId);
    }

    public void setNodeId(String nodeId) {
        putValue(NodeRequestParameters.nodeId, nodeId);
    }

    public String getTargetNodeId() {
        return getString(targetNodeId);
    }

    public void setTargetNodeId(String targetNodeId) {
        putValue(NodeRequestParameters.targetNodeId, targetNodeId);
    }

    public String getCommand() {
        return getString(command);
    }

    public void setCommand(String command) {
        putValue(NodeRequestParameters.command, command);
    }

}
