/*
 * Copyright (c) 2020-present The Aspectran Project
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
import java.util.List;

/**
 * Entity representing a user role in the system.
 */
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 5824905139046648187L;

    private Long roleId;
    private String roleName;
    private String description;

    private List<Permission> permissions;

    /**
     * Gets the role ID.
     * @return the role ID
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * Sets the role ID.
     * @param roleId the role ID
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    /**
     * Gets the role name.
     * @return the role name
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Sets the role name.
     * @param roleName the role name
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
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

    /**
     * Gets the list of permissions associated with this role.
     * @return the list of permissions
     */
    public List<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Sets the list of permissions associated with this role.
     * @param permissions the list of permissions
     */
    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

}
