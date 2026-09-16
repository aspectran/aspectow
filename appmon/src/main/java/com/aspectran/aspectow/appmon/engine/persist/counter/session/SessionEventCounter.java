/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.appmon.engine.persist.counter.session;

import com.aspectran.aspectow.appmon.engine.config.EventInfo;
import com.aspectran.aspectow.appmon.engine.persist.counter.AbstractEventCounter;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCounter;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionManager;
import com.aspectran.core.component.session.SessionManagerProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.core.service.ServiceHoldingListener;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;

/**
 * An {@link EventCounter} for counting session creation events.
 * It registers a {@link SessionListener} to receive notifications when sessions are created.
 *
 * <p>Created: 2025-02-12</p>
 */
public class SessionEventCounter extends AbstractEventCounter {

    private final String serverId;

    private final String contextName;

    /**
     * Instantiates a new SessionEventCounter.
     * @param eventInfo the event configuration
     */
    public SessionEventCounter(@NonNull EventInfo eventInfo) {
        super(eventInfo);

        String[] arr = StringUtils.divide(eventInfo.getTarget(), "/");
        this.serverId = arr[0];
        this.contextName = (arr[1] != null ? arr[1] : "");
    }

    @Override
    public void initialize() throws Exception {
        final SessionListener sessionListener = new SessionEventCountingListener(this);
        ActivityContext context = CoreServiceHolder.findActivityContext(contextName);
        if (context != null) {
            registerSessionListener(context, sessionListener);
        } else {
            CoreServiceHolder.addServiceHoldingListener(new ServiceHoldingListener() {
                @Override
                public void afterServiceHolding(CoreService service) {
                    if (service.getActivityContext() != null) {
                        String serviceContextName = service.getActivityContext().getName();
                        if (serviceContextName != null && serviceContextName.equals(contextName)) {
                            registerSessionListener(service.getActivityContext(), sessionListener);
                        }
                    }
                }
            });
        }
    }

    private void registerSessionListener(@NonNull ActivityContext context, SessionListener sessionListener) {
        SessionManager sessionManager;
        try {
            SessionManagerProvider server = context.getBeanRegistry().getBean(serverId);
            sessionManager = (StringUtils.hasLength(contextName) ?
                    server.getSessionManager(contextName) : server.getSessionManager());
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve session handler with " + getEventInfo().getTarget(), e);
        }
        if (sessionManager != null) {
            sessionManager.addSessionListener(sessionListener);
        }
    }

    /**
     * Called by {@link SessionEventCountingListener} when a session is created.
     */
    void sessionCreated() {
        getEventCount().count();
    }

}
