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

import club.textchat.chat.ChatAction;
import club.textchat.redis.RedisConnectionPool;
import club.textchat.server.ChaterInfo;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.StringUtils;

/**
 * <p>Created: 2020/06/25</p>
 */
@Component
@Bean
public class RandomChatersByLangPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "rclang:";

    private static final String VALUE_SEPARATOR = ":";

    private static final String ANY_LANGUAGE = "any";

    @Autowired
    public RandomChatersByLangPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void put(ChaterInfo chaterInfo) {
        sadd(makeKey(chaterInfo.getChatLanguage()), makeValue(chaterInfo));
    }

    public void remove(ChaterInfo chaterInfo) {
        srem(makeKey(chaterInfo.getChatLanguage()), makeValue(chaterInfo));
    }

    public ChaterInfo randomChater(String chatLanguage) {
        String str = srandmember(makeKey(chatLanguage));
        if (str != null) {
            int index = str.indexOf(VALUE_SEPARATOR);
            if (index > -1) {
                String json = str.substring(index + 1);
                return new ChaterInfo(ChatAction.RANDOM_CHATROOM_ID, json);
            }
        }
        return null;
    }

    private String makeKey(String language) {
        if (StringUtils.isEmpty(language)) {
            return KEY_PREFIX + ANY_LANGUAGE;
        } else {
            return KEY_PREFIX + language;
        }
    }

    private static String makeValue(ChaterInfo chaterInfo) {
        return chaterInfo.getUserNo() + VALUE_SEPARATOR + chaterInfo.serialize();
    }

}
