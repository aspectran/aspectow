package club.textchat.persistence;

/**
 * <p>Created: 2020/05/03</p>
 */
public class RoomsPersistence {

    private final ConnectionPool connectionPool;

    public RoomsPersistence(RedisConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

}
