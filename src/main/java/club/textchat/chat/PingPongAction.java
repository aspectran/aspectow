/*
 * Copyright (c) 2020 The Aspectran Project
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
package club.textchat.chat;

import club.textchat.user.UserManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.web.support.http.HttpHeaders;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Created: 2020/05/17</p>
 */
@Component
public class PingPongAction {

    private static final String PONG = "pong";

    private static final int MAX_INTERVAL = 2000;

    private final Map<String, Long> cache = Collections.synchronizedMap(createLRUMap(64));

    private final UserManager userManager;

    @Autowired
    public PingPongAction(UserManager userManager) {
        this.userManager = userManager;
    }

    @Request("/ping")
    @Transform(FormatType.NONE)
    public void ping(Translet translet) {
        translet.getResponseAdapter().setContentType("text/plain; charset=utf-8");
        translet.getResponseAdapter().setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String sessionId = translet.getSessionAdapter().getId();
        long currentTime = System.currentTimeMillis();
        Long lastAccessedTime = cache.put(sessionId, currentTime);
        if (lastAccessedTime != null) {
            if (lastAccessedTime > currentTime - MAX_INTERVAL) {
                return;
            }
        }
        try {
            userManager.getUserInfo();
            translet.getResponseAdapter().getWriter().write(PONG);
        } catch (Exception e) {
            // ignore
        }
    }

    public static <K, V> Map<K, V> createLRUMap(final int maxSize) {
        return new LinkedHashMap<K, V>(maxSize * 10 / 7, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
    }

}
