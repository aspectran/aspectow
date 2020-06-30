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

import club.textchat.redis.ConnectionPool;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanIterator;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * <p>Created: 2020/05/04</p>
 */
public class AbstractPersistence {

    private final ConnectionPool connectionPool;

    public AbstractPersistence(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    private StatefulRedisConnection<String, String> getConnection() {
        try {
            return connectionPool.getConnection();
        } catch (Exception e) {
            throw RedisConnectionException.create(e);
        }
    }

    <R> R sync(Function<RedisCommands<String, String>, R> func) {
        try (StatefulRedisConnection<String, String> conn = getConnection()) {
            return func.apply(conn.sync());
        }
    }

    protected void publish(String channel, String message) {
        sync(c -> c.publish(channel, message));
    }

    protected String get(String key) {
        return sync(c -> c.get(key));
    }

    protected void set(String key, String value) {
        sync(c -> c.set(key, value));
    }

    protected void setnx(String key, String value) {
        sync(c -> c.setnx(key, value));
    }

    protected long del(String key) {
        return sync(c -> c.del(key));
    }

    protected void setex(String key, String value, int timeout) {
        sync(c -> c.setex(key, timeout, value));
    }

    protected void setexIfNotExist(String key, String value, int timeout) {
        sync(c -> {
            if (c.get(key) == null) {
                c.setex(key, timeout, value);
            }
            return null;
        });
    }

    protected long rpush(String key, String value) {
        return sync(c -> c.rpush(key, value));
    }

    protected void rpush(String key, String value, int limit) {
        sync(c -> {
            long len = rpush(key, value);
            if (len > limit) {
                c.ltrim(key, -limit, -1);
            }
            return null;
        });
    }

    protected List<String> lrange(String key) {
        return sync(c -> c.lrange(key, 0, -1));
    }

    protected List<String> lrange(String key, int limit) {
        return sync(c -> c.lrange(key, -limit, -1));
    }

    protected long sadd(String key, String member) {
        return sync(c -> c.sadd(key, member));
    }

    protected long srem(String key, String member) {
        return sync(c -> c.srem(key, member));
    }

    protected Set<String> smembers(String key) {
        return sync(c -> c.smembers(key));
    }

    protected boolean sismember(String key, String member) {
        return sync(c -> c.sismember(key, member));
    }

    protected String srandmember(String key) {
        return sync(c -> c.srandmember(key));
    }

    protected Long hincrby(String key, String field, long amount) {
        return sync(c -> c.hincrby(key, field, amount));
    }

    protected Long hdel(String key, String field) {
        return sync(c -> c.hdel(key, field));
    }

    protected Map<String, String> hgetall(String key) {
        return sync(c -> c.hgetall(key));
    }

    protected boolean smismember(String keyPattern, String member) {
        return sync(c -> {
            boolean exists = false;
            ScanIterator<String> scanIterator = ScanIterator.scan(c, ScanArgs.Builder.matches(keyPattern));
            while (scanIterator.hasNext()) {
                String key = scanIterator.next();
                exists = c.sismember(key, member);
                if (exists) {
                    break;
                }
            }
            return exists;
        });
    }

}
