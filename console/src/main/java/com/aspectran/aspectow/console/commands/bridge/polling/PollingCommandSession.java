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
package com.aspectran.aspectow.console.commands.bridge.polling;

import com.aspectran.aspectow.node.management.commands.bridge.CommandSession;
import com.aspectran.utils.concurrent.AutoLock;
import com.aspectran.utils.timer.CyclicTimeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CommandSession} implementation for HTTP polling.
 * It tracks the last message index retrieved by the client.
 */
public class PollingCommandSession implements CommandSession {

    private static final int MIN_POLLING_INTERVAL = 500;

    private static final int MAX_POLLING_INTERVAL = 5000;

    private static final int SESSION_TIMEOUT_THRESHOLD = 30000;

    private final AutoLock autoLock = new AutoLock();

    private final List<String> messageQueue = new ArrayList<>();

    private final String id;

    private final PollingSessionManager sessionManager;

    private final SessionExpiryTimer expiryTimer;

    private String nodeId;

    private volatile int pollingInterval = MAX_POLLING_INTERVAL;

    private volatile int sessionTimeout = MAX_POLLING_INTERVAL + SESSION_TIMEOUT_THRESHOLD;

    private int lastLineIndex = -1;

    private boolean expired;

    /**
     * Instantiates a new PollingRelaySession.
     * @param id the unique identifier of this session
     * @param sessionManager the session manager that created this session
     */
    public PollingCommandSession(String id, PollingSessionManager sessionManager) {
        this.id = id;
        this.sessionManager = sessionManager;
        this.expiryTimer = new SessionExpiryTimer();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = Math.clamp(pollingInterval, MIN_POLLING_INTERVAL, MAX_POLLING_INTERVAL);
        this.sessionTimeout = pollingInterval + SESSION_TIMEOUT_THRESHOLD;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Gets the index of the last message line that was sent to this session.
     * @return the last line index
     */
    public int getLastLineIndex() {
        return lastLineIndex;
    }

    protected void setLastLineIndex(int lastLineIndex) {
        this.lastLineIndex = lastLineIndex;
    }

    /**
     * Pushes a message to the session's individual queue.
     * @param message the message to push
     */
    public void push(String message) {
        try (AutoLock ignored = autoLock.lock()) {
            if (isValid()) {
                messageQueue.add(message);
            }
        }
    }

    /**
     * Pops all messages from the session's individual queue.
     * @return a list of messages, or {@code null} if the queue is empty
     */
    public List<String> popMessages() {
        try (AutoLock ignored = autoLock.lock()) {
            if (messageQueue.isEmpty()) {
                return null;
            }
            List<String> messages = new ArrayList<>(messageQueue);
            messageQueue.clear();
            return messages;
        }
    }

    @Override
    public boolean isValid() {
        return !isExpired();
    }

    protected boolean isExpired() {
        try (AutoLock ignored = autoLock.lock()) {
            return expired;
        }
    }

    /**
     * Updates the session's last access time and schedules the next expiry check.
     * @param create {@code true} if the session is being created
     */
    protected void access(boolean create) {
        try (AutoLock ignored = autoLock.lock()) {
            if (isValid()) {
                if (!create) {
                    expiryTimer.cancel();
                }
                expiryTimer.schedule(sessionTimeout);
            }
        }
    }

    /**
     * Destroys this session and its expiry timer.
     */
    protected void destroy() {
        try (AutoLock ignored = autoLock.lock()) {
            expiryTimer.destroy();
            messageQueue.clear();
        }
    }

    protected AutoLock lock() {
        return autoLock.lock();
    }

    private void doExpiry() {
        try (AutoLock ignored = lock()) {
            if (!expired) {
                expired = true;
                sessionManager.scavenge();
            }
        }
    }

    /**
     * A timer to handle session expiration.
     */
    public class SessionExpiryTimer {

        private final CyclicTimeout timer;

        SessionExpiryTimer() {
            timer = new CyclicTimeout(sessionManager.getScheduler()) {
                @Override
                public void onTimeoutExpired() {
                    doExpiry();
                }
            };
        }

        public void schedule(long delay) {
            if (delay >= 0) {
                timer.schedule(delay, TimeUnit.MILLISECONDS);
            }
        }

        public void cancel() {
            timer.cancel();
        }

        public void destroy() {
            timer.destroy();
        }

    }

}
