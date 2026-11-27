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

import com.aspectran.aspectow.appmon.common.support.IPCountryResolver;
import com.aspectran.core.activity.Activity;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.RequestAdapter;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.utils.DigestUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.json.JsonBuilder;
import com.aspectran.web.support.util.WebUtils;
import org.jspecify.annotations.NonNull;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aspectran.aspectow.appmon.engine.exporter.event.session.SessionEventReader.USER_ACTIVITY_COUNT;

/**
 * An advice class that captures events before and after an Activity executes.
 * It measures execution time, counts errors, and gathers session information.
 *
 * <p>Created: 2024-12-19</p>
 */
public class ActivityEventAdvice {

    private final ActivityEventReader activityEventReader;

    private long startTime;

    private String previousSessionId;

    /**
     * Instantiates a new ActivityEventAdvice.
     * @param activityEventReader the reader that will process the captured event data
     */
    public ActivityEventAdvice(@NonNull ActivityEventReader activityEventReader) {
        assert activityEventReader.getEventCount() != null;
        this.activityEventReader = activityEventReader;
    }

    /**
     * Called before the advised activity executes.
     * Records the start time and the existing session ID if available.
     * @param activity the activity that is about to execute
     */
    public void before(@NonNull Activity activity) {
        startTime = System.currentTimeMillis();
        if (activity.hasSessionAdapter()) {
            previousSessionId = activity.getSessionAdapter().getId();
        }
    }

    /**
     * Called after the advised activity has finished.
     * Calculates execution time, updates error counts, and generates a JSON
     * representation of the event.
     * @param activity the activity that has finished executing
     * @return a JSON string representing the captured event data
     */
    public String after(@NonNull Activity activity) {
        Throwable error = activity.getRootCauseOfRaisedException();
        if (error != null && !activity.isExceptionHandled()) {
            activityEventReader.getEventCount().error();
        }

        long interim = activityEventReader.getEventCount().getTallying().getTotal();
        long total = interim + activityEventReader.getEventCount().getTallied().getTotal();
        long errors = activityEventReader.getEventCount().getTallying().getAux();

        long elapsedTime = System.currentTimeMillis() - startTime;

        String sessionId = null;
        int activityCount = 0;
        String clientKey = null;
        String ipAddress = null;
        String countryCode = null;

        if (activity.hasSessionAdapter()) {
            SessionAdapter sessionAdapter = activity.getSessionAdapter();
            sessionId = sessionAdapter.getId();
            if (sessionId != null) {
                if (sessionAdapter.isNew()) {
                    activityCount = 1;
                } else {
                    AtomicInteger counter = sessionAdapter.getAttribute(USER_ACTIVITY_COUNT);
                    if (counter != null) {
                        activityCount = counter.get();
                    }
                }
            }
        }

        boolean sessionTerminated = (sessionId == null && previousSessionId != null);
        if (sessionId == null && !sessionTerminated && activity.hasTranslet()) {
            Translet translet = activity.getTranslet();
            RequestAdapter requestAdapter = translet.getRequestAdapter();
            ipAddress = WebUtils.getRemoteAddr(translet);
            if (StringUtils.hasLength(ipAddress)) {
                String userAgent = (requestAdapter != null ? requestAdapter.getHeader("User-Agent") : null);
                String raw = ipAddress + "|" + (userAgent != null ? userAgent : "");
                clientKey = "anon:" + DigestUtils.md5DigestAsHex(raw).substring(0, 12);
                IPCountryResolver ipCountryResolver = activityEventReader.getIpCountryResolver();
                if (ipCountryResolver != null) {
                    countryCode = ipCountryResolver.resolveCountryCode(ipAddress);
                }
            }
        }

        return new JsonBuilder()
                .prettyPrint(false)
                .nullWritable(false)
                .object()
                    .object("activities")
                        .put("total", total)
                        .put("interim", interim)
                        .put("errors", errors)
                    .endObject()
                    .put("startTime", startTime)
                    .put("elapsedTime", elapsedTime)
                    .put("thread", Thread.currentThread().getName())
                    .put("sessionId", sessionId)
                    .put("activityCount", activityCount)
                    .put("clientKey", clientKey)
                    .put("ipAddress", ipAddress)
                    .put("countryCode", countryCode)
                    .put("error", (error != null && !activity.isExceptionHandled()) ? true : null)
                .endObject()
                .toString();
    }

}
