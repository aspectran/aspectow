package club.textchat.redis.persistence;

import club.textchat.server.message.ChatMessage;

import java.util.List;

/**
 * <p>Created: 2020/05/14</p>
 */
public interface ConvosPersistence {

    void put(String roomId, ChatMessage message);

    List<ChatMessage> getRecentConvo(String roomId);

}
