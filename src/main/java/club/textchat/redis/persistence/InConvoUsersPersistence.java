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
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

/**
 * Persistence for the names of the users in the chat room.
 *
 * inconvo:condense(username) = httpSessionId
 *
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class InConvoUsersPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "inconvo:";

    @Autowired
    public InConvoUsersPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    @Override
    public String get(String httpSessionId) {
        return super.get(makeKey(httpSessionId));
    }

    public void put(String httpSessionId, String roomId) {
        set(makeKey(httpSessionId), roomId);
    }

    public void remove(String httpSessionId) {
        del(makeKey(httpSessionId));
    }

    public boolean exists(String httpSessionId) {
        return (get(httpSessionId) != null);
    }

    public boolean exists(String httpSessionId, String roomId) {
        String roomId2 = get(httpSessionId);
        if (roomId2 != null) {
            return roomId2.equals(roomId);
        }
        return false;
    }

    private String makeKey(String httpSessionId) {
        if (httpSessionId == null) {
            throw new IllegalArgumentException("httpSessionId must not be null");
        }
        return KEY_PREFIX + httpSessionId;
    }

}
