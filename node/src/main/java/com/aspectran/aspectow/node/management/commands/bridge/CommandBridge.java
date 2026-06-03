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
package com.aspectran.aspectow.node.management.commands.bridge;

import java.util.Collection;

/**
 * Interface for bridging command results to clients.
 * This can be implemented using various communication protocols like WebSocket or polling.
 */
public interface CommandBridge {

    /**
     * Collects all active sessions managed by this bridge.
     * @param sessions the collection to add sessions to
     */
    void getSessions(Collection<CommandSession> sessions);

    /**
     * Bridges a message to all connected sessions.
     * @param message the message to bridge
     */
    void bridge(String message);

    /**
     * Bridges a message to a specific session.
     * @param session the session to send the message to
     * @param message the message to bridge
     */
    void bridge(CommandSession session, String message);

}
