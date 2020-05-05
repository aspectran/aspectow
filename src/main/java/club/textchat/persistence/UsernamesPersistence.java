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

    public String get(String username) {
        return super.get(KEY_PREFIX + makeQualified(username));
    }

    public void setByJoin(String username, String httpSessionId) {
        super.set(KEY_PREFIX + makeQualified(username), httpSessionId);
    }

    public void setByLeave(String username, String httpSessionId) {
        super.setex(KEY_PREFIX + makeQualified(username), httpSessionId, expiryPeriodInSeconds);
    }

    private String makeQualified(String username) {
        return username;
    }

}
