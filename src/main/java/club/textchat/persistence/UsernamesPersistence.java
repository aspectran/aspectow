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

    public void acquire(String username, String httpSessionId) {
        set(KEY_PREFIX + qualify(username), httpSessionId);
    }

    public void abandon(String username, String httpSessionId) {
        setex(KEY_PREFIX + qualify(username), httpSessionId, expiryPeriodInSeconds);
    }

    public void abandonIfNotExist(String username, String httpSessionId) {
        setexIfNotExist(KEY_PREFIX + qualify(username), httpSessionId, expiryPeriodInSeconds);
    }

    private String qualify(String username) {
        return username;
    }

}
