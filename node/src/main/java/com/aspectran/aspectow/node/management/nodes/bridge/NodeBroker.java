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
package com.aspectran.aspectow.node.management.nodes.bridge;

import org.jspecify.annotations.NonNull;

/**
 * NodeBroker handles the distribution of node management events
 * to connected clients (via WebSockets or Polling).
 */
public class NodeBroker {

    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    /**
     * Instantiates a new NodeBroker.
     */
    public NodeBroker() {
    }

    /**
     * Subscribes a local session to node events.
     * @param session the node session
     */
    public synchronized void subscribe(@NonNull NodeSession session) {
        if (session.isValid()) {
            subscriptionRegistry.addLocalSubscription(session.getId());
        }
    }

    /**
     * Unsubscribes a local session from node events.
     * @param session the node session
     */
    public synchronized void unsubscribe(@NonNull NodeSession session) {
        subscriptionRegistry.removeLocalSubscription(session.getId());
    }

}
