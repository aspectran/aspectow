package club.textchat.persistence;

import club.textchat.user.UsernameUtils;

/**
 * Persistence for the names of the users in the chat room.
 *
 * inconvo:condense(username) = httpSessionId
 *
 * <p>Created: 2020/05/03</p>
 */
public class UsersInConvoPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "inconvo:";

    public UsersInConvoPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    @Override
    public String get(String username) {
        return super.get(makeKey(username));
    }

    public void put(String username, String httpSessionId) {
        set(makeKey(username), httpSessionId);
    }

    public void remove(String username) {
        del(makeKey(username));
    }

    public boolean exists(String username, String httpSessionId) {
        String httpSessionId2 = get(username);
        if (httpSessionId2 != null) {
            return httpSessionId.equals(httpSessionId2);
        }
        return false;
    }

    private String  makeKey(String username) {
        return KEY_PREFIX + UsernameUtils.condense(username);
    }

}
