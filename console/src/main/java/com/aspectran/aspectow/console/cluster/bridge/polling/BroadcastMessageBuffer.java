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
package com.aspectran.aspectow.console.cluster.bridge.polling;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe buffer for storing and retrieving node management messages for polling clients.
 */
public class BroadcastMessageBuffer {

    private final AtomicInteger lineCounter = new AtomicInteger(0);

    private final List<String> buffer = new LinkedList<>();

    /**
     * Instantiates a new BroadcastMessageBuffer.
     */
    public BroadcastMessageBuffer() {
    }

    /**
     * Pushes a new message (line) into the buffer.
     * @param message the message to add
     * @return the new line index
     */
    public int push(String message) {
        synchronized (buffer) {
            buffer.add(message);
            return lineCounter.incrementAndGet();
        }
    }

    /**
     * Pops new messages for a given session since its last read.
     * @param session the polling session
     * @return an array of new messages, or {@code null} if none are available
     */
    @Nullable
    public String[] pop(@NonNull PollingNodeSession session) {
        synchronized (buffer) {
            int maxLineIndex = lineCounter.get() - 1;
            int lineIndex = session.getLastLineIndex();
            if (lineIndex < 0) {
                session.setLastLineIndex(maxLineIndex);
                return (!buffer.isEmpty() ? buffer.toArray(new String[0]) : null);
            }
            if (lineIndex < maxLineIndex) {
                session.setLastLineIndex(maxLineIndex);
                int offset = maxLineIndex - lineIndex;
                if (offset < buffer.size()) {
                    int start = buffer.size() - offset;
                    return buffer.subList(start, buffer.size()).toArray(new String[0]);
                } else {
                    return buffer.toArray(new String[0]);
                }
            }
            if (lineIndex > maxLineIndex) {
                session.setLastLineIndex(maxLineIndex);
            }
            return null;
        }
    }

    /**
     * Shrinks the buffer by removing messages that are no longer needed by any session.
     * @param minLineIndex the minimum line index currently held by any active session
     */
    public void shrink(int minLineIndex) {
        synchronized (buffer) {
            int lines = lineCounter.get() - minLineIndex + 1;
            if (lines < buffer.size()) {
                buffer.subList(0, buffer.size() - lines).clear();
            } else if (lines == buffer.size()) {
                buffer.clear();
            }
        }
    }

    /**
     * Clears the entire message buffer.
     */
    public void clear() {
        synchronized (buffer) {
            buffer.clear();
        }
    }

}
