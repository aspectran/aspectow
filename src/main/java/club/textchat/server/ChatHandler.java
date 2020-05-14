package club.textchat.server;

import club.textchat.server.message.ChatMessage;

/**
 * <p>Created: 2020/05/14</p>
 */
public interface ChatHandler {

    void broadcast(ChatMessage message, String roomId, long userNo);

}
