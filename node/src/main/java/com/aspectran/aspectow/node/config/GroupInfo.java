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
package com.aspectran.aspectow.node.config;

import com.aspectran.utils.apon.DefaultParameters;
import com.aspectran.utils.apon.ParameterKey;
import com.aspectran.utils.apon.ValueType;

/**
 * Contains configuration for a group of nodes.
 *
 * <p>Created: 2026. 05. 27.</p>
 */
public class GroupInfo extends DefaultParameters {

    private static final ParameterKey id;
    private static final ParameterKey title;
    private static final ParameterKey description;

    private static final ParameterKey[] parameterKeys;

    static {
        id = new ParameterKey("id", ValueType.STRING);
        title = new ParameterKey("title", ValueType.STRING);
        description = new ParameterKey("description", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                id,
                title,
                description
        };
    }

    /**
     * Instantiates a new GroupInfo.
     */
    public GroupInfo() {
        super(parameterKeys);
    }

    /**
     * Returns the unique identifier of the group.
     * @return the group identifier
     */
    public String getId() {
        return getString(id);
    }

    /**
     * Sets the unique identifier of the group.
     * @param groupId the group identifier
     */
    public void setId(String groupId) {
        putValue(id, groupId);
    }

    /**
     * Returns the display title of the group.
     * @return the group title
     */
    public String getTitle() {
        return getString(title);
    }

    /**
     * Sets the display title of the group.
     * @param title the group title
     */
    public void setTitle(String title) {
        putValue(GroupInfo.title, title);
    }

    /**
     * Returns the description of the group.
     * @return the group description
     */
    public String getDescription() {
        return getString(description);
    }

    /**
     * Sets the description of the group.
     * @param description the group description
     */
    public void setDescription(String description) {
        putValue(GroupInfo.description, description);
    }

}
