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
package com.aspectran.aspectow.node.management.scheduler.bridge;

import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing scheduler management subscriptions from both
 * local sessions and remote nodes in a cluster.
 *
 * <p>Created: 2026-05-02</p>
 */
class SubscriptionRegistry {

    private final Set<String> localSessions = ConcurrentHashMap.newKeySet();

    private final Set<String> localSubscriptions = ConcurrentHashMap.newKeySet();

    private final Set<String> remoteSubscriptions = ConcurrentHashMap.newKeySet();

    /**
     * Adds a local session ID to the registry.
     * @param sessionId the session identifier
     */
    public void addSession(@NonNull String sessionId) {
        localSessions.add(sessionId);
    }

    /**
     * Adds a local subscription by session ID.
     * @param sessionId the session identifier
     */
    public void addLocalSubscription(@NonNull String sessionId) {
        localSubscriptions.add(sessionId);
    }

    /**
     * Removes a local subscription by session ID.
     * @param sessionId the session identifier
     */
    public void removeLocalSubscription(@NonNull String sessionId) {
        localSessions.remove(sessionId);
        localSubscriptions.remove(sessionId);
    }

    /**
     * Adds a remote node subscription.
     * @param nodeId the remote node identifier
     */
    public void addRemoteSubscription(@NonNull String nodeId) {
        remoteSubscriptions.add(nodeId);
    }

    /**
     * Removes a remote node subscription.
     * @param nodeId the remote node identifier
     */
    public void removeRemoteSubscription(@NonNull String nodeId) {
        remoteSubscriptions.remove(nodeId);
    }

    /**
     * Returns a set of all remote subscriptions (node IDs).
     * @return the set of remote subscriptions
     */
    public Set<String> getRemoteSubscriptions() {
        return remoteSubscriptions;
    }

    /**
     * Returns whether there are any active local or remote subscriptions.
     * @return true if active, false otherwise
     */
    public boolean isInUse() {
        return (!localSubscriptions.isEmpty() || !remoteSubscriptions.isEmpty());
    }

    /**
     * Returns whether there are any active local subscriptions.
     * @return true if local subscriptions are active, false otherwise
     */
    public boolean isInUseLocally() {
        return !localSubscriptions.isEmpty();
    }

    /**
     * Returns a set of all local session IDs.
     * @return the set of local session IDs
     */
    public Set<String> getAllSessionIds() {
        return Set.copyOf(localSessions);
    }

    /**
     * Clears all registered sessions and subscriptions.
     */
    public void clear() {
        localSessions.clear();
        localSubscriptions.clear();
        remoteSubscriptions.clear();
    }

}
