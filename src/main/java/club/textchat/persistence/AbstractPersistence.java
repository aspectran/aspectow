package club.textchat.persistence;

import com.aspectran.core.util.apon.Parameters;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;
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

    <R> R sync(Function<RedisCommands<String, String>, R> func) {
        try (StatefulRedisConnection<String, String> conn = connectionPool.getConnection()) {
            return func.apply(conn.sync());
        } catch (Exception e) {
            throw new PersistenceException("Data manipulation failure with Redis", e);
        }
    }

    protected String get(String key) {
        return sync(c -> c.get(key));
    }

    protected void set(String key, String value) {
        sync(c -> c.set(key, value));
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

    protected long rpush(String key, Parameters value) {
        return rpush(key, value.toString());
    }

    protected void rpush(String key, Parameters value, int limit) {
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

    protected long sadd(String key, String value) {
        return sync(c -> c.sadd(key, value));
    }

    protected long srem(String key, String value) {
        return sync(c -> c.srem(key, value));
    }

    protected Set<String> smembers(String key) {
        return sync(c -> c.smembers(key));
    }

}
