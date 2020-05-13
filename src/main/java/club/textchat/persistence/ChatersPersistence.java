package club.textchat.persistence;

import club.textchat.server.ChaterInfo;

import java.util.Set;

/**
 * <p>Created: 2020/05/03</p>
 */
public class ChatersPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "chaters:";

    private static final String VALUE_SEPARATOR = ":";

    public ChatersPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void put(ChaterInfo chaterInfo) {
        sadd(makeKey(chaterInfo.getRoomId()), makeValue(chaterInfo));
    }

    public void remove(ChaterInfo chaterInfo) {
        srem(makeKey(chaterInfo.getRoomId()), makeValue(chaterInfo));
    }

    public Set<String> getChaters(String roomId) {
        return smembers(makeKey(roomId));
    }

    public boolean isChater(ChaterInfo chaterInfo) {
        Set<String> chaters = getChaters(chaterInfo.getRoomId());
        if (chaters != null) {
            for (String str : chaters) {
                int index = str.indexOf(VALUE_SEPARATOR);
                if (index > -1) {
                    String userNo = str.substring(0, index);
                    if (chaterInfo.getUserNo() == Long.parseLong(userNo)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String makeKey(String roomId) {
        return KEY_PREFIX + roomId;
    }

    private String makeValue(ChaterInfo chaterInfo) {
        return chaterInfo.getUserNo() + VALUE_SEPARATOR + chaterInfo.getUsername();
    }

}
