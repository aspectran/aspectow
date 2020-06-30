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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class UsersByCountryPersistence extends AbstractPersistence {

    private static final String KEY = "usersbycntry";

    @Autowired
    public UsersByCountryPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void increase(String country) {
        hincrby(KEY, country, 1);
    }

    public void decrease(String country) {
        Long value = hincrby(KEY, country, -1);
        if (value <= 0) {
            hdel(KEY, country);
        }
    }

    public Map<String, Long> getCounters() {
        Map<String, String> unSortedMap = hgetall(KEY);
        LinkedHashMap<String, Long> reverseSortedMap = new LinkedHashMap<>();
        unSortedMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), Long.parseLong(x.getValue())));
        return reverseSortedMap;
    }

}
