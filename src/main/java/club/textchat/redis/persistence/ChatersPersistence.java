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
import club.textchat.server.ChaterInfo;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

import java.util.Set;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class ChatersPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "chaters:";

    private static final String VALUE_SEPARATOR = ":";

    @Autowired
    public ChatersPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void put(ChaterInfo chaterInfo) {
        sadd(makeKey(chaterInfo.getRoomId()), makeValue(chaterInfo));
    }

    public void remove(ChaterInfo chaterInfo) {
        srem(makeKey(chaterInfo.getRoomId()), makeValue(chaterInfo));
    }

    public Set<String> getChaters(String roomId) {
        return smembers(makeKey(roomId));
    }

    public boolean isChater(ChaterInfo chaterInfo) {
        Set<String> chaters = getChaters(chaterInfo.getRoomId());
        if (chaters != null) {
            for (String str : chaters) {
                int index = str.indexOf(VALUE_SEPARATOR);
                if (index > -1) {
                    String userNo = str.substring(0, index);
                    if (chaterInfo.getUserNo() == Integer.parseInt(userNo)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ChaterInfo randomChater(String roomId) {
        String str = srandmember(makeKey(roomId));
        int index = str.indexOf(VALUE_SEPARATOR);
        if (index > -1) {
            String userNo = str.substring(0, index);
            String username = str.substring(index + 1);
            return new ChaterInfo(roomId, Integer.parseInt(userNo), username);
        }
        return null;
    }

    private String makeKey(String roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        return KEY_PREFIX + roomId;
    }

    public static String makeValue(ChaterInfo chaterInfo) {
        return chaterInfo.getUserNo() + VALUE_SEPARATOR + chaterInfo.getUsername();
    }

}
