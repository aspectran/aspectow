package club.textchat.server;

import club.textchat.persistence.ConversationsPersistence;
import club.textchat.persistence.TalkersPersistence;
import club.textchat.persistence.UsernamesPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.AbortPayload;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.JoinPayload;
import club.textchat.server.message.payload.JoinedUsersPayload;
import club.textchat.server.message.payload.MessagePayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.lang.NonNull;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Created: 2020/05/03</p>
 */
public abstract class ChatHandler extends InstantActivitySupport {

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private final Map<TalkerInfo, Session> talkers = new ConcurrentHashMap<>();

    private final UsernamesPersistence usernamesPersistence;

    private final TalkersPersistence talkersPersistence;

    private final ConversationsPersistence conversationsPersistence;

    protected ChatHandler(UsernamesPersistence usernamesPersistence,
                          TalkersPersistence talkersPersistence,
                          ConversationsPersistence conversationsPersistence) {
        this.usernamesPersistence = usernamesPersistence;
        this.talkersPersistence = talkersPersistence;
        this.conversationsPersistence = conversationsPersistence;
    }

    protected void handle(Session session, ChatMessage chatMessage) {
        if (chatMessage.heartBeatPing()) {
            chatMessage.heartBeatPong();
            send(session, chatMessage);
            return;
        }
        MessagePayload payload = chatMessage.getMessagePayload();
        if (payload != null) {
            TalkerInfo talkerInfo = getTalkerInfo(session);
            switch (payload.getType()) {
                case CHAT:
                    broadcastMessage(talkerInfo, payload.getContent());
                    break;
                case JOIN:
                    String username = talkerInfo.getUsername();
                    String username2 = payload.getUsername();
                    if (!username.equals(username2)) {
                        abort(session, talkerInfo, "abnormal");
                        return;
                    }
                    if (existsTalker(talkerInfo)) {
                        if (!checkSameUser(talkerInfo)) {
                            abort(session, talkerInfo, "exists");
                            return;
                        }
                        Session session2 = talkers.get(talkerInfo);
                        if (session2 != null) {
                            abort(session2, talkerInfo, "rejoin");
                        }
                        if (!join(session, talkerInfo, true)) {
                            broadcastUserJoined(talkerInfo);
                        }
                    } else {
                        join(session, talkerInfo, false);
                        broadcastUserJoined(talkerInfo);
                    }
                    sendJoinedUsers(session, talkerInfo);
                    break;
                default:
                    abort(session, talkerInfo, "abnormal");
            }
        }
    }

    protected void close(Session session, CloseReason reason) {
        leave(session);
    }

    protected void error(Session session, Throwable error) {
        logger.error("Error in websocket session: " + session.getId(), error);
        try {
            TalkerInfo talkerInfo = getTalkerInfo(session);
            abort(session, talkerInfo, "abnormal:" + error.getMessage());
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean join(Session session, TalkerInfo talkerInfo, boolean rejoin) {
        boolean replaced = false;
        if (session.isOpen()) {
            if (talkers.put(talkerInfo, session) != null) {
                replaced = true;
            }
            talkersPersistence.put(talkerInfo.getRoomId(), talkerInfo.getUsername());
            usernamesPersistence.acquire(talkerInfo.getUsername(), talkerInfo.getHttpSessionId());
            JoinPayload payload = new JoinPayload();
            payload.setUsername(talkerInfo.getUsername());
            payload.setRecentConversations(conversationsPersistence.getRecentConversations(talkerInfo.getRoomId()));
            payload.setRejoin(rejoin);
            ChatMessage message = new ChatMessage(payload);
            send(session, message);
        }
        return replaced;
    }

    private void abort(Session session, TalkerInfo talkerInfo, String cause) {
        if (talkers.remove(talkerInfo, session)) {
            talkersPersistence.remove(talkerInfo.getRoomId(), talkerInfo.getUsername());
            usernamesPersistence.abandonIfNotExist(talkerInfo.getUsername(), talkerInfo.getHttpSessionId());
        }
        AbortPayload payload = new AbortPayload();
        payload.setCause(cause);
        ChatMessage message = new ChatMessage(payload);
        send(session, message);
    }

    private void leave(Session session) {
        TalkerInfo talkerInfo = getTalkerInfo(session);
        if (talkers.remove(talkerInfo, session)) {
            talkersPersistence.remove(talkerInfo.getRoomId(), talkerInfo.getUsername());
            usernamesPersistence.abandonIfNotExist(talkerInfo.getUsername(), talkerInfo.getHttpSessionId());
            broadcastUserLeft(talkerInfo);
        }
    }

    private void sendJoinedUsers(Session session, TalkerInfo talkerInfo) {
        Set<String> usernames = talkersPersistence.getUsernames(talkerInfo.getRoomId());
        JoinedUsersPayload payload = new JoinedUsersPayload();
        payload.setUsernames(usernames);
        ChatMessage message = new ChatMessage(payload);
        send(session, message);
    }

    private void broadcastUserJoined(TalkerInfo talkerInfo) {
        UserJoinedPayload payload = new UserJoinedPayload();
        payload.setRoomId(talkerInfo.getRoomId());
        payload.setUsername(talkerInfo.getUsername());
        payload.setPrevUsername(talkerInfo.getPrevUsername());
        ChatMessage message = new ChatMessage(payload);
        conversationsPersistence.put(talkerInfo.getRoomId(), message);
    }

    private void broadcastUserLeft(TalkerInfo talkerInfo) {
        UserLeftPayload payload = new UserLeftPayload();
        payload.setRoomId(talkerInfo.getRoomId());
        payload.setUsername(talkerInfo.getUsername());
        ChatMessage message = new ChatMessage(payload);
        conversationsPersistence.put(talkerInfo.getRoomId(), message);
    }

    private void broadcastMessage(TalkerInfo talkerInfo, String content) {
        BroadcastPayload payload = new BroadcastPayload();
        payload.setRoomId(talkerInfo.getRoomId());
        payload.setContent(content);
        payload.setUsername(talkerInfo.getUsername());
        ChatMessage message = new ChatMessage(payload);
        conversationsPersistence.put(talkerInfo.getRoomId(), message);
    }

    public void broadcast(ChatMessage message, String roomId, String excluded) {
        for (Map.Entry<TalkerInfo, Session> entry : talkers.entrySet()) {
            TalkerInfo talkerInfo = entry.getKey();
            Session session = entry.getValue();
            if (talkerInfo.getRoomId().equals(roomId) &&
                    (excluded == null || !excluded.equals(talkerInfo.getUsername()))) {
                send(session, message);
            }
        }
    }

    private void send(Session session, ChatMessage message) {
        if (session.isOpen()) {
            session.getAsyncRemote().sendObject(message);
        }
    }

    @NonNull
    private TalkerInfo getTalkerInfo(Session session) {
        return (TalkerInfo)session.getUserProperties().get(TalkerInfo.TALKER_INFO_PROP);
    }

    private boolean existsTalker(TalkerInfo talkerInfo) {
        if (talkers.containsKey(talkerInfo)) {
            return true;
        }
        Set<String> usernames = talkersPersistence.getUsernames(talkerInfo.getRoomId());
        return (usernames != null && usernames.contains(talkerInfo.getUsername()));
    }

    private boolean checkSameUser(TalkerInfo talkerInfo) {
        Session session = talkers.get(talkerInfo);
        String httpSessionId;
        if (session != null && session.isOpen()) {
            TalkerInfo talkerInfo2 = getTalkerInfo(session);
            httpSessionId = talkerInfo2.getHttpSessionId();
        } else {
            httpSessionId = usernamesPersistence.get(talkerInfo.getUsername());
        }
        return (httpSessionId != null && httpSessionId.equals(talkerInfo.getHttpSessionId()));
    }

}
