package club.textchat.persistence;

import club.textchat.server.model.ChatMessage;
import com.aspectran.core.util.apon.AponReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2020/05/03</p>
 */
public class ConversationsPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "conversations:";

    private final int maxSaveMessages;

    public ConversationsPersistence(RedisConnectionPool connectionPool, int maxSaveMessages) {
        super(connectionPool);
        this.maxSaveMessages = maxSaveMessages;
    }

    public void save(ChatMessage message) {
        String roomId = "1";
        rpush(KEY_PREFIX + roomId, message, maxSaveMessages);
    }

    public List<ChatMessage> getRecentConversations() {
        String roomId = "1";
        List<String> list = lrange(KEY_PREFIX + roomId, maxSaveMessages);
        List<ChatMessage> result = new ArrayList<>(list.size());
        for (String str : list) {
            ChatMessage message = null;
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
