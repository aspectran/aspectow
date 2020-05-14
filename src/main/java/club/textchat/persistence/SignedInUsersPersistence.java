package club.textchat.persistence;

import club.textchat.user.UsernameUtils;

/**
 * Persistence for names of users who are not in conversation yet.
 *
 * signed:condense(username) = httpSessionId
 *
 * <p>Created: 2020/05/03</p>
 */
public class SignedInUsersPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "signed:";

    private final int expiryPeriodInSeconds;

    public SignedInUsersPersistence(RedisConnectionPool connectionPool, int expiryPeriodInSeconds) {
        super(connectionPool);
        this.expiryPeriodInSeconds = expiryPeriodInSeconds;
    }

    @Override
    public String get(String username) {
        return super.get(makeKey(username));
    }

    public void put(String username, String httpSessionId) {
        set(makeKey(username), httpSessionId);
    }

    public void tryAbandon(String username, String httpSessionId) {
        setexIfNotExist(makeKey(username), httpSessionId, expiryPeriodInSeconds);
    }

    public void abandon(String username, String httpSessionId) {
        setex(makeKey(username), httpSessionId, expiryPeriodInSeconds);
    }

    public boolean exists(String username, String httpSessionId) {
        String httpSessionId2 = get(username);
        if (httpSessionId2 != null) {
            return httpSessionId2.equals(httpSessionId);
        }
        return false;
    }

    private String  makeKey(String username) {
        return KEY_PREFIX + UsernameUtils.condense(username);
    }

}
