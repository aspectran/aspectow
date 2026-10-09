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
package com.aspectran.aspectow.appmon.engine.relay;

/**
 * An interface representing a client session for an export service.
 * It provides a protocol-agnostic way to manage session state, such as subscribed apps.
 *
 * <p>Created: 2025. 2. 12.</p>
 */
public interface RelaySession {

    /**
     * Gets the unique identifier of this session.
     * @return the session ID
     */
    String getId();

    /**
     * Gets the time zone of the client.
     * @return the time zone ID string
     */
    String getTimeZone();

    /**
     * Gets the ID of the node that this session has subscribed to.
     * @return the subscribed node ID
     */
    String getSubscribedNodeId();

    /**
     * Sets the ID of the node that this session has subscribed to.
     * @param nodeId the subscribed node ID
     */
    void setSubscribedNodeId(String nodeId);

    /**
     * Gets the ID of the node currently selected dynamically by the client.
     * @return the selected node ID, or {@code null} if in group view mode
     */
    String getSelectedNodeId();

    /**
     * Sets the ID of the node currently selected dynamically by the client.
     * @param nodeId the selected node ID, or {@code null} for group view mode
     */
    void setSelectedNodeId(String nodeId);

    /**
     * Checks whether an indication event for the specified node should be throttled.
     * If not throttled, records the current time as the last indication time.
     * @param nodeId the node ID
     * @param intervalMillis the throttle interval in milliseconds
     * @return {@code true} if throttled (should skip), {@code false} if allowed
     */
    boolean shouldThrottleIndication(String nodeId, long intervalMillis);

    /**
     * Gets the names of the apps that this session has subscribed to.
     * @return an array of app names
     */
    String[] getSubscribedApps();

    /**
     * Sets the names of the apps that this session has subscribed to.
     * @param appIds an array of app IDs
     */
    void setSubscribedApps(String[] appIds);

    /**
     * Removes the subscribed apps from this session.
     */
    void removeSubscribedApps();

    /**
     * Gets the ID of the app that this session is currently focusing on.
     * @return the focused app ID
     */
    String getFocusedAppId();

    /**
     * Sets the ID of the app that this session is currently focusing on.
     * @param appId the focused app ID
     */
    void setFocusedAppId(String appId);

    /**
     * Checks if the session is still valid (e.g., open and not expired).
     * @return {@code true} if the session is valid, {@code false} otherwise
     */
    boolean isValid();

}
