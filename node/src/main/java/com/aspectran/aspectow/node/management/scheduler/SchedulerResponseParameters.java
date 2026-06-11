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
import com.aspectran.utils.json.JsonBuilder;

/**
 * Represents a structured response for scheduler management.
 */
public class SchedulerResponseParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey nodeId;
    public static final ParameterKey owner;
    public static final ParameterKey data;
    public static final ParameterKey error;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        nodeId = new ParameterKey("nodeId", ValueType.STRING);
        owner = new ParameterKey("owner", ValueType.STRING);
        data = new ParameterKey("data", ValueType.OBJECT);
        error = new ParameterKey("error", ValueType.TEXT);

        parameterKeys = new ParameterKey[] {
                header,
                nodeId,
                owner,
                data,
                error
        };
    }

    public SchedulerResponseParameters() {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
    }

    public String getHeader() {
        return getString(header);
    }

    public SchedulerResponseParameters setHeader(String header) {
        putValue(SchedulerResponseParameters.header, header);
        return this;
    }

    public String getNodeId() {
        return getString(nodeId);
    }

    public SchedulerResponseParameters setNodeId(String nodeId) {
        putValue(SchedulerResponseParameters.nodeId, nodeId);
        return this;
    }

    public SchedulerResponseParameters setOwner(String group) {
        putValue(SchedulerResponseParameters.owner, group);
        return this;
    }

    public SchedulerResponseParameters setData(Object data) {
        putValue(SchedulerResponseParameters.data, data);
        return this;
    }

    public SchedulerResponseParameters setError(String error) {
        putValue(SchedulerResponseParameters.error, error);
        return this;
    }

    @Override
    public String toString() {
        try {
            return new JsonBuilder()
                    .prettyPrint(false)
                    .nullWritable(false)
                    .put(this)
                    .toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
