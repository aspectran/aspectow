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

    <R> R sync(Function<RedisCommands<String, String>, R> func) throws Exception {
        try (StatefulRedisConnection<String, String> conn = connectionPool.getConnection()) {
            return func.apply(conn.sync());
        }
    }

    public long rpush(String key, Parameters value) throws Exception {
        return sync(c -> c.rpush(key, value.toString()));
    }

    public void rpush(String key, Parameters value, int limit) throws Exception {
        sync(c -> {
            long len = c.rpush(key, value.toString());
            if (len > limit) {
                c.ltrim(key, -limit, -1);
            }
            return null;
        });
    }

    public List<String> lrange(String key, int limit) throws Exception {
        return sync(c -> c.lrange(key, 0, limit - 1));
    }

}
