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
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class RandomHistoryPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "rhist:";

    private static final String VALUE_SEPARATOR = ":";

    private static final String MET = "1";

    private static final int EXPIRY_PERIOD_IN_SECONDS = 10;

    @Autowired
    public RandomHistoryPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void set(int userNo1, int userNo2) {
        super.setex(makeKey(userNo1, userNo2), MET, EXPIRY_PERIOD_IN_SECONDS);
    }

    public boolean exists(int userNo1, int userNo2) {
        String str = super.get(makeKey(userNo1, userNo2));
        return MET.equals(str);
    }

    private String makeKey(int userNo1, int userNo2) {
        if (userNo1 < userNo2) {
            return (KEY_PREFIX + userNo1 + VALUE_SEPARATOR + userNo2);
        }  else {
            return (KEY_PREFIX + userNo2 + VALUE_SEPARATOR + userNo1);
        }
    }

}
