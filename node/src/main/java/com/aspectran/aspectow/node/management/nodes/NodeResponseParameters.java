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

import com.aspectran.aspectow.node.config.NodeInfo;
import com.aspectran.utils.apon.AponRenderStyle;
import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;
import com.aspectran.utils.json.JsonBuilder;

public class NodeResponseParameters extends DefaultParameters {

    public static final ParameterKey header;
    public static final ParameterKey node;

    private static final ParameterKey[] parameterKeys;

    static {
        header = new ParameterKey("header", ValueType.STRING);
        node = new ParameterKey("node", NodeInfo.class);

        parameterKeys = new ParameterKey[] {
                header,
                node
        };
    }

    public NodeResponseParameters() {
        super(parameterKeys);
        setRenderStyle(AponRenderStyle.COMPACT);
    }

    public String getHeader() {
        return getString(header);
    }

    public NodeResponseParameters setHeader(String header) {
        putValue(NodeResponseParameters.header, header);
        return this;
    }

    public NodeInfo getNode() {
        return getParameters(node);
    }

    public NodeResponseParameters setNode(NodeInfo node) {
        putValue(NodeResponseParameters.node, node);
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
