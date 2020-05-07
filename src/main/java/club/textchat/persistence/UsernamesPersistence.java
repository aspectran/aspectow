package club.textchat.persistence;

/**
 * User names in use.
 *
 * <p>Created: 2020/05/03</p>
 */
public class UsernamesPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "usernames:";

    private final int expiryPeriodInSeconds;

    public UsernamesPersistence(RedisConnectionPool connectionPool, int expiryPeriodInSeconds) {
        super(connectionPool);
        this.expiryPeriodInSeconds = expiryPeriodInSeconds;
    }

    @Override
    public String get(String username) {
        return super.get(KEY_PREFIX + qualify(username));
    }

    public void put(String username, String httpSessionId) {
        super.setex(KEY_PREFIX + qualify(username), httpSessionId, expiryPeriodInSeconds);
    }

    private String qualify(String username) {
        return username;
    }

}
