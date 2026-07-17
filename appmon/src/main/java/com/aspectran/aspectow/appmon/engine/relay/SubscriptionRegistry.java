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
package com.aspectran.aspectow.appmon.engine.relay;

import com.aspectran.utils.Assert;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing application subscriptions from both local sessions
 * and remote nodes in a cluster.
 *
 * <p>Created: 2026-05-02</p>
 */
public class SubscriptionRegistry {

    /** appId -> Set of local session IDs */
    private final Map<String, Set<String>> localSubscriptions = new ConcurrentHashMap<>();

    /** appId -> Set of remote node IDs */
    private final Map<String, Set<String>> remoteSubscriptions = new ConcurrentHashMap<>();

    /** session ID -> Array of subscribed app IDs */
    private final Map<String, String[]> sessionToApps = new ConcurrentHashMap<>();

    /**
     * Adds a local subscription for a specific session and its associated applications.
     * Cleans up any existing subscriptions for the session before registration.
     * @param sessionId the unique session ID of the subscriber
     * @param subscribedApps an array of application IDs to subscribe to
     */
    public void addLocalSubscription(String sessionId, String[] subscribedApps) {
        Assert.hasLength(sessionId, "sessionId must not be null or empty");
        Assert.notEmpty(subscribedApps, "subscribedApps must not be null or empty");

        // Clean up previous subscriptions for this session
        String[] oldApps = sessionToApps.get(sessionId);
        if (oldApps != null) {
            for (String oldAppId : oldApps) {
                removeLocalAppSubscription(oldAppId, sessionId);
            }
        }

        sessionToApps.put(sessionId, subscribedApps);
        for (String appId : subscribedApps) {
            localSubscriptions.computeIfAbsent(appId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }
    }

    /**
     * Removes all local subscriptions associated with the specified session.
     * @param sessionId the unique session ID of the subscriber to remove
     */
    public void removeLocalSubscription(String sessionId) {
        Assert.hasLength(sessionId, "sessionId must not be null or empty");
        String[] appIds = sessionToApps.remove(sessionId);
        if (appIds != null) {
            for (String appId : appIds) {
                removeLocalAppSubscription(appId, sessionId);
            }
        }
    }

    private void removeLocalAppSubscription(String appId, String sessionId) {
        Set<String> sessions = localSubscriptions.get(appId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                localSubscriptions.remove(appId);
            }
        }
    }

    /**
     * Adds a subscription from a remote node in the cluster for a specific application.
     * @param nodeId the ID of the remote node
     * @param appId the ID of the application being monitored by the remote node
     */
    public void addRemoteSubscription(String nodeId, String appId) {
        Assert.hasLength(nodeId, "nodeId must not be null or empty");
        Assert.hasLength(appId, "appId must not be null or empty");
        remoteSubscriptions.computeIfAbsent(appId,
                k -> ConcurrentHashMap.newKeySet()).add(nodeId);
    }

    /**
     * Removes a subscription from a remote node for a specific application.
     * @param nodeId the ID of the remote node
     * @param appId the ID of the application
     */
    public void removeRemoteSubscription(String nodeId, String appId) {
        Assert.hasLength(nodeId, "nodeId must not be null or empty");
        Assert.hasLength(appId, "appId must not be null or empty");
        Set<String> nodes = remoteSubscriptions.get(appId);
        if (nodes != null) {
            nodes.remove(nodeId);
            if (nodes.isEmpty()) {
                remoteSubscriptions.remove(appId);
            }
        }
    }

    /**
     * Returns the set of remote node IDs that have subscribed to the given application.
     * @param appId the application ID
     * @return a set of remote node IDs, or {@code null} if none
     */
    public Set<String> getNodeIdsRemotelySubscribedToApp(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        return remoteSubscriptions.get(appId);
    }

    /**
     * Checks if the specified application is currently in use (monitored) by either
     * local sessions or remote nodes.
     * @param appId the application ID
     * @return true if the application is in use locally or remotely, false otherwise
     */
    public boolean isAppInUse(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        Set<String> sessions = localSubscriptions.get(appId);
        if (sessions != null && !sessions.isEmpty()) {
            return true;
        }
        Set<String> nodes = remoteSubscriptions.get(appId);
        return (nodes != null && !nodes.isEmpty());
    }

    /**
     * Checks if the specified application is currently in use (monitored) by any
     * local sessions.
     * @param appId the application ID
     * @return true if the application is in use locally, false otherwise
     */
    public boolean isAppInUseLocally(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        Set<String> sessions = localSubscriptions.get(appId);
        return (sessions != null && !sessions.isEmpty());
    }

    /**
     * Returns the set of local session IDs subscribed to the specified application.
     * @param appId the application ID
     * @return a set of local session IDs, or an empty set if none
     */
    public Set<String> getSessionsSubscribedToApp(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        Set<String> sessions = localSubscriptions.get(appId);
        return (sessions != null ? sessions : Set.of());
    }

    /**
     * Returns the set of all active local session IDs.
     * @return a set of session IDs
     */
    public Set<String> getAllSessionIds() {
        return sessionToApps.keySet();
    }

    /**
     * Clears all local subscriptions, remote subscriptions, and session mappings.
     */
    public void clear() {
        localSubscriptions.clear();
        remoteSubscriptions.clear();
        sessionToApps.clear();
    }

}
