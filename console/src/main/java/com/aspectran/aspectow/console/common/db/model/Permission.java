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
package com.aspectran.aspectow.console.common.db.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity representing a granular permission in the system.
 */
public class Permission implements Serializable {

    @Serial
    private static final long serialVersionUID = -2429813955681652495L;

    private Long permId;
    private String permCode;
    private String description;

    /**
     * Gets the permission ID.
     * @return the permission ID
     */
    public Long getPermId() {
        return permId;
    }

    /**
     * Sets the permission ID.
     * @param permId the permission ID
     */
    public void setPermId(Long permId) {
        this.permId = permId;
    }

    /**
     * Gets the permission code.
     * @return the permission code
     */
    public String getPermCode() {
        return permCode;
    }

    /**
     * Sets the permission code.
     * @param permCode the permission code
     */
    public void setPermCode(String permCode) {
        this.permCode = permCode;
    }

    /**
     * Gets the description.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
