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

    /** session ID -> Array of subscribeed app IDs */
    private final Map<String, String[]> sessionToApps = new ConcurrentHashMap<>();

    public void addLocalSubscription(String sessionId, String[] subscribedApps) {
        Assert.hasLength(sessionId, "sessionId must not be null or empty");
        Assert.notEmpty(subscribedApps, "subscribedApps must not be null or empty");
        sessionToApps.put(sessionId, subscribedApps);
        for (String appId : subscribedApps) {
            localSubscriptions.computeIfAbsent(appId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }
    }

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

    public void addRemoteSubscription(String nodeId, String appId) {
        Assert.hasLength(nodeId, "nodeId must not be null or empty");
        Assert.hasLength(appId, "appId must not be null or empty");
        remoteSubscriptions.computeIfAbsent(appId,
                k -> ConcurrentHashMap.newKeySet()).add(nodeId);
    }

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

    public Set<String> getNodeIdsRemotelySubscribedToApp(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        return remoteSubscriptions.get(appId);
    }

    public boolean isAppInUse(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        Set<String> sessions = localSubscriptions.get(appId);
        if (sessions != null && !sessions.isEmpty()) {
            return true;
        }
        Set<String> nodes = remoteSubscriptions.get(appId);
        return (nodes != null && !nodes.isEmpty());
    }

    public boolean isAppInUseLocally(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        Set<String> sessions = localSubscriptions.get(appId);
        return (sessions != null && !sessions.isEmpty());
    }

    public Set<String> getSessionsSubscribedToApp(String appId) {
        Assert.hasLength(appId, "appId must not be null or empty");
        Set<String> sessions = localSubscriptions.get(appId);
        return (sessions != null ? sessions : Set.of());
    }

    public Set<String> getAllSessionIds() {
        return sessionToApps.keySet();
    }

    public void clear() {
        localSubscriptions.clear();
        remoteSubscriptions.clear();
        sessionToApps.clear();
    }

}
