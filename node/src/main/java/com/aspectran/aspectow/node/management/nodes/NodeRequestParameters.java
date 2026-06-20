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

/**
 * Parameters representing a node management request.
 */
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

    /**
     * Instantiates a new NodeRequestParameters.
     */
    public NodeRequestParameters() {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
    }

    /**
     * Returns the request header.
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
        putValue(NodeRequestParameters.header, header);
    }

    /**
     * Returns the initiating node ID.
     * @return the node ID
     */
    public String getNodeId() {
        return getString(nodeId);
    }

    /**
     * Sets the initiating node ID.
     * @param nodeId the node ID to set
     */
    public void setNodeId(String nodeId) {
        putValue(NodeRequestParameters.nodeId, nodeId);
    }

    /**
     * Returns the target node ID.
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
        putValue(NodeRequestParameters.targetNodeId, targetNodeId);
    }

    /**
     * Returns the node management command to execute.
     * @return the command
     */
    public String getCommand() {
        return getString(command);
    }

    /**
     * Sets the node management command to execute.
     * @param command the command to set
     */
    public void setCommand(String command) {
        putValue(NodeRequestParameters.command, command);
    }

}
