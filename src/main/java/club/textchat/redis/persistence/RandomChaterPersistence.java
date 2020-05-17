package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.server.ChaterInfo;
import com.aspectran.core.lang.NonNull;

/**
 * <p>Created: 2020/05/03</p>
 */
public class RandomChaterPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "rchat:";

    private static final String VALUE_SEPARATOR = ":";

    private static final String NONE = "0";

    public RandomChaterPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public ChaterInfo get(long userNo) {
        String str = super.get(makeKey(userNo));
        if (str != null && !NONE.equals(str)) {
            int index = str.indexOf(VALUE_SEPARATOR);
            if (index > -1) {
                String userNo2 = str.substring(0, index);
                String username = str.substring(index + 1);
                return new ChaterInfo(Long.parseLong(userNo2), username);
            }
        }
        return null;
    }

    public void set(long userNo) {
        super.set(makeKey(userNo), NONE);
    }

    public void set(@NonNull ChaterInfo chaterInfo, @NonNull ChaterInfo chaterInfo2) {
        super.set(makeKey(chaterInfo.getUserNo()), makeValue(chaterInfo2));
        super.set(makeKey(chaterInfo2.getUserNo()), makeValue(chaterInfo));
    }

    public void unset(long userNo1, long userNo2) {
        ChaterInfo chaterInfo = get(userNo1);
        if (chaterInfo != null && chaterInfo.getUserNo() == userNo2) {
            set(chaterInfo.getUserNo());
        }
    }

    public void remove(long userNo) {
        del(makeKey(userNo));
    }

    public boolean exists(long userNo) {
        String str = super.get(makeKey(userNo));
        return (str != null && !NONE.equals(str));
    }

    private String makeKey(long userNo) {
        return KEY_PREFIX + userNo;
    }

    private String makeValue(ChaterInfo chaterInfo) {
        return chaterInfo.getUserNo() + VALUE_SEPARATOR + chaterInfo.getUsername();
    }

}