package club.textchat.persistence;

import java.util.Set;

/**
 * <p>Created: 2020/05/03</p>
 */
public class TalkersPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "talkers:";

    public TalkersPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void put(String roomId, String username) {
        sadd(KEY_PREFIX + roomId, username);
    }

    public void remove(String roomId, String username) {
        srem(KEY_PREFIX + roomId, username);
    }

    public Set<String> getUsernames(String roomId) {
        return smembers(KEY_PREFIX + roomId);
    }

}
