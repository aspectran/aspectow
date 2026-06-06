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
package com.aspectran.aspectow.node.management.scheduler.remote;

import com.aspectran.aspectow.node.management.scheduler.SchedulerManager;
import com.aspectran.aspectow.node.management.scheduler.SchedulerRequestParameters;
import com.aspectran.aspectow.node.management.scheduler.SchedulerResponseParameters;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.node.manager.NodeMessageListener;
import com.aspectran.utils.apon.AponParseException;
import com.aspectran.utils.apon.JsonToParameters;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RemoteSchedulerMessageListener listens to Redis relay messages related to
 * scheduler management and forwards them to the SchedulerManager.
 */
public class RemoteSchedulerMessageListener implements NodeMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RemoteSchedulerMessageListener.class);

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
        if (message.startsWith(SchedulerBroker.CONTROL_SUBSCRIBE) || message.startsWith(SchedulerBroker.CONTROL_RELEASE)) {
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
        } else if (message.startsWith(SchedulerBroker.CONTROL_REQUEST)) {
            String requestData = message.substring(SchedulerBroker.CONTROL_REQUEST.length());
            SchedulerRequestParameters request = new SchedulerRequestParameters();
            try {
                request.readFrom(requestData);
            } catch (AponParseException e) {
                logger.error("Failed to parse scheduler request parameters: {}", requestData, e);
            }

            schedulerManager.processRemotely(request);
        }
    }

    @Override
    public void onRelayMessage(String nodeId, @NonNull String message) {
        try {
            SchedulerResponseParameters response = JsonToParameters.from(message, SchedulerResponseParameters.class);
            schedulerManager.bridge(response);
        } catch (Exception e) {
            logger.error("Failed to parse scheduler response: {}", message, e);
        }
    }

    @Override
    public void onRelayMessage(String nodeId, String sessionId, @NonNull String message) {
        try {
            SchedulerResponseParameters response = JsonToParameters.from(message, SchedulerResponseParameters.class);
            schedulerManager.bridge(sessionId, response);
        } catch (Exception e) {
            logger.error("Failed to parse scheduler response: {}", message, e);
        }
    }

}
