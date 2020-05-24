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
package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.user.UsernameUtils;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

/**
 * Persistence for names of users who are not in conversation yet.
 *
 * signed:condense(username) = httpSessionId
 *
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class SignedInUsersPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "signed:";

    private static final int EXPIRY_PERIOD_IN_SECONDS = 5;

    @Autowired
    public SignedInUsersPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    @Override
    public String get(String username) {
        return super.get(makeKey(username));
    }

    public void put(String username, String httpSessionId) {
        set(makeKey(username), httpSessionId);
    }

    public void tryAbandon(String username, String httpSessionId) {
        setexIfNotExist(makeKey(username), httpSessionId, EXPIRY_PERIOD_IN_SECONDS);
    }

    public void abandon(String username, String httpSessionId) {
        setex(makeKey(username), httpSessionId, EXPIRY_PERIOD_IN_SECONDS);
    }

    public boolean exists(String username, String httpSessionId) {
        String httpSessionId2 = get(username);
        if (httpSessionId2 != null) {
            return httpSessionId2.equals(httpSessionId);
        }
        return false;
    }

    private String  makeKey(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username must not be null");
        }
        return KEY_PREFIX + UsernameUtils.condense(username);
    }

}
