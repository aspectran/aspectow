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
package com.aspectran.aspectow.console.scheduler.bridge.remote;

import com.aspectran.aspectow.console.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.console.scheduler.manager.SchedulerManager;
import com.aspectran.aspectow.node.manager.NodeMessageListener;
import org.jspecify.annotations.NonNull;

/**
 * SchedulerMessageBridgeHandler listens to Redis relay messages related to
 * scheduler management and forwards them to the SchedulerManager.
 */
public class RemoteSchedulerMessageListener implements NodeMessageListener {

    private final SchedulerManager schedulerManager;

    public RemoteSchedulerMessageListener(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
    }

    @Override
    public String getCategory() {
        return SchedulerBroker.CATEGORY_SCHEDULER;
    }

    @Override
    public void onControlMessage(String nodeId, @NonNull String message) {
        String requesterNodeId = null;
        String sessionId = null;
        String[] parts = message.split(SchedulerBroker.DELIMITER);
        if (parts.length >= 3) {
            requesterNodeId = parts[2];
        }
        if (parts.length >= 4) {
            sessionId = parts[3];
        }
        if (requesterNodeId == null) {
            requesterNodeId = nodeId;
        }

        if (message.startsWith(SchedulerBroker.CONTROL_SUBSCRIBE)) {
            schedulerManager.getBroker().subscribeRemotely(requesterNodeId, sessionId);
        } else if (message.startsWith(SchedulerBroker.CONTROL_RELEASE)) {
            schedulerManager.getBroker().releaseRemotely(requesterNodeId);
        }
    }

    @Override
    public void onRelayMessage(String nodeId, @NonNull String message) {
        if (message.startsWith("command:")) {
            schedulerManager.process(message);
        } else {
            int idx = message.indexOf(SchedulerBroker.DELIMITER);
            if (idx != -1) {
                String sourceNodeId = message.substring(0, idx);
                String content = message.substring(idx + 1);
                schedulerManager.broadcast(sourceNodeId, content);
            } else {
                schedulerManager.broadcast(message);
            }
        }
    }

    @Override
    public void onRelayMessage(String nodeId, String sessionId, @NonNull String message) {
        int idx = message.indexOf(SchedulerBroker.DELIMITER);
        String sourceNodeId;
        String content;
        if (idx != -1) {
            sourceNodeId = message.substring(0, idx);
            content = message.substring(idx + 1);
        } else {
            sourceNodeId = nodeId;
            content = message;
        }

        schedulerManager.getBroker().getSessions().stream()
                .filter(session -> session.getId().equals(sessionId))
                .forEach(session -> schedulerManager.getBroker().bridge(session, sourceNodeId, content));
    }

}
