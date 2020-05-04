package club.textchat.persistence;

import club.textchat.server.model.ChatMessage;
import com.aspectran.core.util.apon.AponReader;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2020/05/03</p>
 */
public class ChatMessagePersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "conversations:";

    private final int maxMessages;

    public ChatMessagePersistence(RedisConnectionPool connectionPool, int maxMessages) {
        super(connectionPool);
        this.maxMessages = maxMessages;
    }

    public void push(ChatMessage message) throws Exception {
        String roomId = "1";
        lpush(KEY_PREFIX + roomId, message, maxMessages);
    }

    public List<ChatMessage> list() throws Exception {
        String roomId = "1";
        List<String> list = lrange(KEY_PREFIX + roomId, maxMessages);
        List<ChatMessage> result = new ArrayList<>(list.size());
        for (String str : list) {
            ChatMessage message = AponReader.parse(str, ChatMessage.class);
            result.add(message);
        }
        return result;
    }

}
