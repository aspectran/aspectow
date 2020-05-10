package club.textchat.persistence;

import club.textchat.server.message.ChatMessage;
import com.aspectran.core.util.apon.AponReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2020/05/03</p>
 */
public class ConversationsPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "convos:";

    private final int maxSaveMessages;

    public ConversationsPersistence(RedisConnectionPool connectionPool, int maxSaveMessages) {
        super(connectionPool);
        this.maxSaveMessages = maxSaveMessages;
    }

    public void put(String roomId, ChatMessage message) {
        rpush(KEY_PREFIX + roomId, message, maxSaveMessages);
        publish(MessageSubscriber.CHANNEL, message.toString());
    }

    public List<ChatMessage> getRecentConversations(String roomId) {
        List<String> list = lrange(KEY_PREFIX + roomId, maxSaveMessages);
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

}
