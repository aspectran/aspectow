package club.textchat.persistence;

import com.aspectran.core.util.apon.Parameters;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;
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

    public String get(String key) {
        return sync(c -> c.get(key));
    }

    public void set(String key, String value) {
        sync(c -> c.set(key, value));
    }

    public void setex(String key, String value, int timeout) {
        sync(c -> c.setex(key, timeout, value));
    }

    public long rpush(String key, Parameters value) {
        return sync(c -> c.rpush(key, value.toString()));
    }

    public void rpush(String key, Parameters value, int limit) {
        sync(c -> {
            long len = c.rpush(key, value.toString());
            if (len > limit) {
                c.ltrim(key, -limit, -1);
            }
            return null;
        });
    }

    public List<String> lrange(String key, int limit) {
        return sync(c -> c.lrange(key, -limit, -1));
    }

}
