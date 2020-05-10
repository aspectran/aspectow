package club.textchat.persistence;

import com.aspectran.core.util.apon.Parameters;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanIterator;
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
