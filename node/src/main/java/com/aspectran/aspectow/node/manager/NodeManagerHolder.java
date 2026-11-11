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
package com.aspectran.aspectow.node.manager;

import com.aspectran.utils.Assert;
import com.aspectran.utils.SystemUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.aspectran.aspectow.node.manager.NodeManagerBuilder.MY_GROUP_ID_PROPERTY;
import static com.aspectran.aspectow.node.manager.NodeManagerBuilder.MY_NODE_ID_PROPERTY;

/**
 * A central holder for the {@link NodeManager} instance and current node metadata.
 * <p>Provides static global access to the current node's information across
 * multiple contexts within the same JVM.</p>
 *
 * <p>Created: 2026. 11. 11.</p>
 */
public final class NodeManagerHolder {

    private static volatile NodeManager nodeManager;

    private NodeManagerHolder() {
        // not to be instantiated
    }

    /**
     * Returns the held {@link NodeManager} instance.
     * @return the held NodeManager, or {@code null} if not yet initialized
     */
    @Nullable
    public static NodeManager getNodeManager() {
        return nodeManager;
    }

    /**
     * Returns the held {@link NodeManager} instance, throwing an exception if not yet initialized.
     * @return the held NodeManager
     * @throws IllegalStateException if NodeManager has not been initialized
     */
    @NonNull
    public static NodeManager getRequiredNodeManager() {
        NodeManager manager = nodeManager;
        Assert.state(manager != null, "No NodeManager is currently available");
        return manager;
    }

    /**
     * Sets the {@link NodeManager} instance and synchronizes the node ID and group ID
     * with the system properties.
     * @param nodeManager the NodeManager instance to hold, or {@code null} to clear
     */
    public static void setNodeManager(@Nullable NodeManager nodeManager) {
        NodeManagerHolder.nodeManager = nodeManager;
        if (nodeManager != null) {
            if (nodeManager.getNodeId() != null) {
                System.setProperty(MY_NODE_ID_PROPERTY, nodeManager.getNodeId());
            }
            if (nodeManager.getGroupId() != null) {
                System.setProperty(MY_GROUP_ID_PROPERTY, nodeManager.getGroupId());
            }
        }
    }

    /**
     * Returns whether a {@link NodeManager} is currently held.
     * @return {@code true} if a NodeManager is held, otherwise {@code false}
     */
    public static boolean hasNodeManager() {
        return nodeManager != null;
    }

    /**
     * Returns the current node ID.
     * <p>If a {@link NodeManager} is available, its node ID is returned.
     * Otherwise, the value configured via system property is returned.</p>
     * @return the current node ID, or {@code null} if unknown
     */
    @Nullable
    public static String getNodeId() {
        if (nodeManager != null) {
            return nodeManager.getNodeId();
        }
        return SystemUtils.getProperty(MY_NODE_ID_PROPERTY);
    }

    /**
     * Returns the current group ID.
     * <p>If a {@link NodeManager} is available, its group ID is returned.
     * Otherwise, the value configured via system property is returned.</p>
     * @return the current group ID, or {@code null} if unknown
     */
    @Nullable
    public static String getGroupId() {
        if (nodeManager != null) {
            return nodeManager.getGroupId();
        }
        return SystemUtils.getProperty(MY_GROUP_ID_PROPERTY);
    }

    /**
     * Clears the held {@link NodeManager} instance.
     */
    public static void clear() {
        nodeManager = null;
    }

}
