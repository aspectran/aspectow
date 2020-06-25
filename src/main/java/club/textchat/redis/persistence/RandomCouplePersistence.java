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
import com.aspectran.core.lang.NonNull;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class RandomCouplePersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "rcouple:";

    private static final String VALUE_SEPARATOR = ":";

    private static final String NONE = "0";

    @Autowired
    public RandomCouplePersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public ChaterInfo get(int userNo) {
        String str = super.get(makeKey(userNo));
        if (str != null && !NONE.equals(str)) {
            int index = str.indexOf(VALUE_SEPARATOR);
            if (index > -1) {
                String json = str.substring(index + 1);
                return new ChaterInfo(ChatAction.RANDOM_CHATROOM_ID, json);
            }
        }
        return null;
    }

    public void set(int userNo) {
        super.set(makeKey(userNo), NONE);
    }

    public void set(@NonNull ChaterInfo chaterInfo, @NonNull ChaterInfo chaterInfo2) {
        super.set(makeKey(chaterInfo.getUserNo()), makeValue(chaterInfo2));
        super.set(makeKey(chaterInfo2.getUserNo()), makeValue(chaterInfo));
    }

    public void unset(int userNo1, int userNo2) {
        ChaterInfo chaterInfo = get(userNo1);
        if (chaterInfo != null && chaterInfo.getUserNo() == userNo2) {
            set(chaterInfo.getUserNo());
        }
    }

    public void remove(int userNo) {
        del(makeKey(userNo));
    }

    public boolean exists(int userNo) {
        String str = super.get(makeKey(userNo));
        return (str != null && !NONE.equals(str));
    }

    private String makeKey(int userNo) {
        return KEY_PREFIX + userNo;
    }

    private String makeValue(ChaterInfo chaterInfo) {
        return chaterInfo.getUserNo() + VALUE_SEPARATOR + chaterInfo.serialize();
    }

}
