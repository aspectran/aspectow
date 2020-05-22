package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class RandomHistoryPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "rhist:";

    private static final String VALUE_SEPARATOR = ":";

    private static final String MET = "1";

    private static final int EXPIRY_PERIOD_IN_SECONDS = 10;

    @Autowired
    public RandomHistoryPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void set(int userNo1, int userNo2) {
        super.setex(makeKey(userNo1, userNo2), MET, EXPIRY_PERIOD_IN_SECONDS);
    }

    public boolean exists(int userNo1, int userNo2) {
        String str = super.get(makeKey(userNo1, userNo2));
        return MET.equals(str);
    }

    private String makeKey(int userNo1, int userNo2) {
        if (userNo1 < userNo2) {
            return (KEY_PREFIX + userNo1 + VALUE_SEPARATOR + userNo2);
        }  else {
            return (KEY_PREFIX + userNo2 + VALUE_SEPARATOR + userNo1);
        }
    }

}
