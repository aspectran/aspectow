package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.redis.subscribe.DefaultMessageSubscriber;
import club.textchat.server.message.ChatMessage;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.apon.AponReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class DefaultConvoPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "convo:";

    private static final int MAX_SAVE_MESSAGES = 50;

    @Autowired
    public DefaultConvoPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void put(String roomId, ChatMessage message) {
        String value = message.toString();
        rpush(makeKey(roomId), value, MAX_SAVE_MESSAGES);
        publish(DefaultMessageSubscriber.CHANNEL, value);
    }

    public List<ChatMessage> getRecentConvo(String roomId) {
        List<String> list = lrange(makeKey(roomId), MAX_SAVE_MESSAGES);
        List<ChatMessage> result = new ArrayList<>(list.size());
        for (String str : list) {
            ChatMessage message;
            try {
                message = AponReader.parse(str, ChatMessage.class);
                result.add(message);
            } catch (IOException e) {
                // ignore
            }
        }
        return result;
    }

    private String makeKey(String roomId) {
        return KEY_PREFIX + roomId;
    }

}
