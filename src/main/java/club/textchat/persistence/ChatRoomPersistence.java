package club.textchat.persistence;

/**
 * <p>Created: 2020/05/03</p>
 */
public class ChatRoomPersistence {

    private final ConnectionPool connectionPool;

    public ChatRoomPersistence(RedisConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

}
