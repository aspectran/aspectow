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
package com.aspectran.aspectow.console.scheduler.bridge.polling;

import com.aspectran.aspectow.node.management.scheduler.SchedulerManager;
import com.aspectran.aspectow.node.management.scheduler.SchedulerResponseParameters;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBridge;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerSession;
import com.aspectran.core.component.AbstractComponent;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import org.jspecify.annotations.NonNull;

/**
 * PollingSchedulerBridge manages client sessions for HTTP long-polling
 * and uses a central message buffer to distribute scheduler management results.
 */
@Component
public class PollingSchedulerBridge extends AbstractComponent implements SchedulerBridge {

    private final PollingSessionManager sessionManager;

    private final SchedulerManager schedulerManager;

    private final BufferedMessages bufferedMessages;

    @Autowired
    public PollingSchedulerBridge(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
        this.sessionManager = new PollingSessionManager(this);
        this.bufferedMessages = new BufferedMessages(100);
    }

    @Override
    protected void doInitialize() throws Exception {
        sessionManager.initialize();
    }

    @Override
    protected void doDestroy() throws Exception {
        sessionManager.destroy();
        bufferedMessages.clear();
    }

    public PollingSchedulerSession createSession(String nodeId) {
        return sessionManager.createSession(nodeId);
    }

    public PollingSchedulerSession getSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    @Override
    public SchedulerSession findSchedulerSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    public void registerSession(String sessionId) {
        schedulerManager.registerSession(sessionId, this);
    }

    public void unregisterSession(String sessionId) {
        schedulerManager.unregisterSession(sessionId);
    }

    @Override
    public void bridge(SchedulerResponseParameters response) {
        if (response != null && !sessionManager.getSessions().isEmpty()) {
            bufferedMessages.push(response.toString());
        }
    }

    @Override
    public void bridge(@NonNull SchedulerSession session, SchedulerResponseParameters response) {
        if (response != null && session instanceof PollingSchedulerSession pollingSchedulerSession) {
            pollingSchedulerSession.push(response.toString());
        }
    }

    public SchedulerBroker getBroker() {
        return schedulerManager.getBroker();
    }

    public String[] pull(PollingSchedulerSession session) {
        String[] messages = bufferedMessages.pop(session);
        if (messages != null && messages.length > 0) {
            shrinkBuffer();
        }
        return messages;
    }

    public void shrinkBuffer() {
        int minLineIndex = getMinLineIndex();
        if (minLineIndex > -1) {
            bufferedMessages.shrink(minLineIndex);
        }
    }

    private int getMinLineIndex() {
        int minLineIndex = -1;
        for (PollingSchedulerSession session : sessionManager.getSessions().values()) {
            if (minLineIndex == -1) {
                minLineIndex = session.getLastLineIndex();
            } else if (session.getLastLineIndex() < minLineIndex) {
                minLineIndex = session.getLastLineIndex();
            }
        }
        return minLineIndex;
    }

    public BufferedMessages getBufferedMessages() {
        return bufferedMessages;
    }

}
