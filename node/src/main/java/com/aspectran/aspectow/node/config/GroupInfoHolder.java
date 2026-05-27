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

import com.aspectran.utils.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holder for managing a collection of {@link GroupInfo} objects.
 * Provides easy access to group metadata for the entire cluster.
 *
 * <p>Created: 2026-05-27</p>
 */
public class GroupInfoHolder {

    private final Map<String, GroupInfo> groupInfoMap = new LinkedHashMap<>();

    /**
     * Instantiates a new GroupInfoHolder.
     */
    public GroupInfoHolder() {
    }

    /**
     * Instantiates a new GroupInfoHolder.
     * @param groupInfoList the list of group information
     */
    public GroupInfoHolder(List<GroupInfo> groupInfoList) {
        if (groupInfoList != null) {
            for (GroupInfo groupInfo : groupInfoList) {
                groupInfoMap.put(groupInfo.getId(), groupInfo);
            }
        }
    }

    /**
     * Returns a specific group configuration by ID.
     * @param groupId the group identifier
     * @return the group information, or {@code null} if not found
     */
    public GroupInfo getGroupInfo(String groupId) {
        Assert.hasLength(groupId, "groupId must not be null or empty");
        return groupInfoMap.get(groupId);
    }

    /**
     * Adds group information to the holder.
     * @param groupInfo the group information to add
     */
    public void putGroupInfo(GroupInfo groupInfo) {
        Assert.notNull(groupInfo, "groupInfo must not be null");
        groupInfoMap.put(groupInfo.getId(), groupInfo);
    }

    /**
     * Returns all group configurations.
     * @return a collection of {@link GroupInfo}
     */
    public Collection<GroupInfo> getGroupInfos() {
        return groupInfoMap.values();
    }

    /**
     * Returns the number of groups managed by this holder.
     * @return the group count
     */
    public int getGroupCount() {
        return groupInfoMap.size();
    }

}
