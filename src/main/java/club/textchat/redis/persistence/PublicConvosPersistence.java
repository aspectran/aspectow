package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.redis.pubsub.PublicMessageSubscriber;
import club.textchat.server.message.ChatMessage;
import com.aspectran.core.util.apon.AponReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2020/05/03</p>
 */
public class PublicConvosPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "convos:";

    private final int maxSaveMessages;

    public PublicConvosPersistence(RedisConnectionPool connectionPool, int maxSaveMessages) {
        super(connectionPool);
        this.maxSaveMessages = maxSaveMessages;
    }

    public void put(String roomId, ChatMessage message) {
        String value = message.toString();
        rpush(makeKey(roomId), value, maxSaveMessages);
        publish(PublicMessageSubscriber.CHANNEL, value);
    }

    public List<ChatMessage> getRecentConvo(String roomId) {
        List<String> list = lrange(makeKey(roomId), maxSaveMessages);
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
