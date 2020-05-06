package club.textchat.server;

import club.textchat.persistence.ConversationsPersistence;
import club.textchat.persistence.UsernamesPersistence;
import club.textchat.server.model.ChatMessage;
import club.textchat.server.model.payload.AbnormalAccessPayload;
import club.textchat.server.model.payload.BroadcastJoinedUsersPayload;
import club.textchat.server.model.payload.BroadcastMessagePayload;
import club.textchat.server.model.payload.BroadcastUserJoinedPayload;
import club.textchat.server.model.payload.BroadcastUserLeavedPayload;
import club.textchat.server.model.payload.SendMessagePayload;
import club.textchat.server.model.payload.WelcomeUserPayload;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Created: 2020/05/03</p>
 */
public abstract class ChatService extends InstantActivitySupport {

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    public static final String USERNAME_PROP = "username";

    public static final String PREV_USERNAME_PROP = "prevUsername";

    public static final String HTTP_SESSION_ID = "httpSessionId";

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final UsernamesPersistence usernamesPersistence;

    private final ConversationsPersistence conversationsPersistence;

    protected ChatService(UsernamesPersistence usernamesPersistence, ConversationsPersistence conversationsPersistence) {
        this.usernamesPersistence = usernamesPersistence;
        this.conversationsPersistence = conversationsPersistence;
    }

    protected void handle(Session session, ChatMessage chatMessage) {
        if (chatMessage.heartBeatPing()) {
            chatMessage.heartBeatPong();
            broadcast(session, chatMessage);
            return;
        }
        SendMessagePayload payload = chatMessage.getSendMessagePayload();
        if (payload != null) {
            switch (payload.getType()) {
                case CHAT:
                    broadcastMessage(session, payload.getContent());
                    break;
                case JOIN:
                    String username = getUsername(session);
                    String username2 = payload.getUsername();
                    if (username == null || !username.equals(username2)) {
                        abort(session, "abnormal");
                        return;
                    }
                    Session session2 = sessions.get(username);
                    if (session2 != null) {
                        String httpSessionId = getHttpSessionId(session);
                        String httpSessionId2 = getHttpSessionId(session2);
                        if (httpSessionId == null || !httpSessionId.equals(httpSessionId2)) {
                            abort(session, "exists");
                            return;
                        }
                        abort(session2, "rejoin");
                        welcome(session, username, true);
                    } else {
                        welcome(session, username, false);
                        String prevUsername = getPrevUsername(session);
                        broadcastUserJoined(session, username, username.equals(prevUsername) ? null : prevUsername);
                    }
                    broadcastJoinedUsers();
                    break;
                case LEAVE:
                    leave(session);
                    break;
            }
        }
    }

    protected void close(Session session, CloseReason reason) {
        leave(session);
    }

    protected void error(Session session, Throwable error) {
        logger.error("Error in websocket session: " + session.getId(), error);
        try {
            abort(session, "abnormal:" + error.getMessage());
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getUsername(Session session) {
        return (String)session.getUserProperties().get(USERNAME_PROP);
    }

    private String getPrevUsername(Session session) {
        return (String)session.getUserProperties().get(PREV_USERNAME_PROP);
    }

    private String getHttpSessionId(Session session) {
        return (String)session.getUserProperties().get(HTTP_SESSION_ID);
    }

    private void welcome(Session session, String username, boolean rejoin) {
        sessions.put(username, session);
        usernamesPersistence.setByJoin(username, getHttpSessionId(session));
        WelcomeUserPayload payload = new WelcomeUserPayload();
        payload.setUsername(username);
        payload.setRecentConversations(conversationsPersistence.getRecentConversations());
        payload.setRejoin(rejoin);
        ChatMessage message = new ChatMessage(payload);
        broadcast(session, message);
    }

    private void abort(Session session, String cause) {
        String username = getUsername(session);
        if (username != null && sessions.remove(username, session)) {
            usernamesPersistence.setByLeave(username, getHttpSessionId(session));
        }
        AbnormalAccessPayload payload = new AbnormalAccessPayload();
        payload.setCause(cause);
        ChatMessage message = new ChatMessage(payload);
        broadcast(session, message);
    }

    private void leave(Session session) {
        String username = getUsername(session);
        if (username != null && sessions.remove(username, session)) {
            usernamesPersistence.setByLeave(username, getHttpSessionId(session));
            broadcastUserLeaved(username);
        }
    }

    private void broadcastUserJoined(Session session, String username, String prevUsername) {
        BroadcastUserJoinedPayload payload = new BroadcastUserJoinedPayload();
        payload.setUsername(username);
        payload.setPrevUsername(prevUsername);
        ChatMessage message = new ChatMessage(payload);
        conversationsPersistence.save(message);
        broadcast(message, session);
    }

    private void broadcastUserLeaved(String username) {
        BroadcastUserLeavedPayload payload = new BroadcastUserLeavedPayload();
        payload.setUsername(username);
        ChatMessage message = new ChatMessage(payload);
        conversationsPersistence.save(message);
        broadcast(message);
    }

    private void broadcastMessage(Session session, String text) {
        String username = getUsername(session);
        if (username != null) {
            BroadcastMessagePayload payload = new BroadcastMessagePayload();
            payload.setContent(text);
            payload.setUsername(username);
            ChatMessage message = new ChatMessage(payload);
            conversationsPersistence.save(message);
            broadcast(message, session);
        }
    }

    private void broadcastJoinedUsers() {
        BroadcastJoinedUsersPayload payload = new BroadcastJoinedUsersPayload();
        payload.setUsernames(sessions.keySet());
        ChatMessage message = new ChatMessage(payload);
        broadcast(message);
    }

    private void broadcast(ChatMessage message) {
        synchronized (sessions) {
            for (Session session : sessions.values()) {
                broadcast(session, message);
            }
        }
    }

    private void broadcast(ChatMessage message, Session ignoredSession) {
        synchronized (sessions) {
            for (Session session : sessions.values()) {
                if (!session.getId().equals(ignoredSession.getId())) {
                    broadcast(session, message);
                }
            }
        }
    }

    private void broadcast(Session session, ChatMessage message) {
        if (session.isOpen()) {
            session.getAsyncRemote().sendObject(message);
        }
    }

}
