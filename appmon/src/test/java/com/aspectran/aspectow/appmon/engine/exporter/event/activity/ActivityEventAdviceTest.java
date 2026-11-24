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
package com.aspectran.aspectow.appmon.engine.exporter.event.activity;

import com.aspectran.aspectow.appmon.engine.config.EventInfo;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterManager;
import com.aspectran.aspectow.appmon.engine.exporter.ExporterType;
import com.aspectran.aspectow.appmon.engine.persist.counter.EventCount;
import com.aspectran.core.activity.Activity;
import com.aspectran.core.adapter.DefaultSessionAdapter;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.session.DefaultSessionManager;
import com.aspectran.core.component.session.SessionAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for {@link ActivityEventAdvice}.
 *
 * <p>Created: 2026-11-24</p>
 */
class ActivityEventAdviceTest {

    private DefaultSessionManager sessionManager;
    private ActivityEventReader activityEventReader;

    @BeforeEach
    void setUp() throws Exception {
        sessionManager = new DefaultSessionManager();
        sessionManager.initialize();

        ExporterManager exporterManager = new ExporterManager(ExporterType.EVENT, null, "testApp");
        EventInfo eventInfo = new EventInfo();
        EventCount eventCount = new EventCount();
        activityEventReader = new ActivityEventReader(exporterManager, eventInfo, eventCount);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (sessionManager != null) {
            sessionManager.destroy();
        }
    }

    private Activity createMockActivity(SessionAdapter sessionAdapter) {
        return (Activity) Proxy.newProxyInstance(
                Activity.class.getClassLoader(),
                new Class<?>[]{Activity.class},
                (proxy, method, args) -> {
                    if ("hasSessionAdapter".equals(method.getName())) {
                        return sessionAdapter != null;
                    }
                    if ("getSessionAdapter".equals(method.getName())) {
                        return sessionAdapter;
                    }
                    if ("hasTranslet".equals(method.getName())) {
                        return false;
                    }
                    if ("getRootCauseOfRaisedException".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
        );
    }

    @Test
    void testStatelessRequestDoesNotCreateSession() {
        SessionAgent agent = new SessionAgent(sessionManager);
        SessionAdapter adapter = new DefaultSessionAdapter(agent);
        Activity activity = createMockActivity(adapter);

        ActivityEventAdvice advice = new ActivityEventAdvice(activityEventReader);
        advice.before(activity);

        // Before advice must not create session
        assertFalse(adapter.isValid());

        String json = advice.after(activity);
        assertNotNull(json);

        // After advice must still not create session for stateless requests
        assertFalse(adapter.isValid());
        assertFalse(json.contains("\"sessionId\""));
    }

    @Test
    void testLazySessionResolution() {
        SessionAgent agent = new SessionAgent(sessionManager);
        SessionAdapter adapter = new DefaultSessionAdapter(agent);
        Activity activity = createMockActivity(adapter);

        ActivityEventAdvice advice = new ActivityEventAdvice(activityEventReader);
        advice.before(activity);

        // Before advice: no session
        assertFalse(adapter.isValid());

        // Simulate application creating a session during action execution
        String createdId = adapter.getId(true);
        assertNotNull(createdId);
        assertTrue(adapter.isValid());

        // After advice: captures the lazily created session ID
        String json = advice.after(activity);
        assertNotNull(json);
        assertTrue(json.contains("\"sessionId\":\"" + createdId + "\""));
    }

    @Test
    void testSessionTerminatedRequestDoesNotProduceClientKey() {
        SessionAgent agent = new SessionAgent(sessionManager);
        SessionAdapter adapter = new DefaultSessionAdapter(agent);
        Activity activity = createMockActivity(adapter);

        // Create an existing session prior to request execution
        String existingSessionId = adapter.getId(true);
        assertNotNull(existingSessionId);

        ActivityEventAdvice advice = new ActivityEventAdvice(activityEventReader);
        advice.before(activity);

        // Simulate signoff / session invalidation during action execution
        agent.invalidate();
        assertFalse(adapter.isValid());

        // After advice: session was terminated, should not generate clientKey
        String json = advice.after(activity);
        assertNotNull(json);
        assertFalse(json.contains("\"sessionId\""));
        assertFalse(json.contains("\"clientKey\""));
    }

}
