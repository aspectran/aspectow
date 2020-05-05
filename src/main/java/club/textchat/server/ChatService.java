package club.textchat.server;

import club.textchat.persistence.ConversationsPersistence;
import club.textchat.persistence.UsernamesPersistence;
import club.textchat.server.model.ChatMessage;
import club.textchat.server.model.payload.AbnormalAccessPayload;
import club.textchat.server.model.payload.BroadcastAvailableUsersPayload;
import club.textchat.server.model.payload.BroadcastConnectedUserPayload;
import club.textchat.server.model.payload.BroadcastDisconnectedUserPayload;
import club.textchat.server.model.payload.BroadcastTextMessagePayload;
import club.textchat.server.model.payload.SendTextMessagePayload;
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
        SendTextMessagePayload payload = chatMessage.getSendTextMessagePayload();
        if (payload != null) {
            switch (payload.getType()) {
                case CHAT:
                    broadcastTextMessage(session, payload.getContent());
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
                        broadcastUserConnected(session, username, username.equals(prevUsername) ? null : prevUsername);
                    }
                    broadcastAvailableUsers();
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
            leave(session);
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getUsername(Session session) {
        return (String)session.getUserProperties().get("username");
    }

    private String getPrevUsername(Session session) {
        return (String)session.getUserProperties().get("prevUsername");
    }

    private String getHttpSessionId(Session session) {
        return (String)session.getUserProperties().get("httpSessionId");
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

    private void leave(Session session) {
        String username = getUsername(session);
        if (username != null && sessions.remove(username, session)) {
            usernamesPersistence.setByLeave(username, getHttpSessionId(session));
            broadcastUserDisconnected(username);
            broadcastAvailableUsers();
        }
    }

    private void abort(Session session, String cause) {
        String username = getUsername(session);
        if (username != null) {
            sessions.remove(username, session);
        }
        AbnormalAccessPayload payload = new AbnormalAccessPayload();
        payload.setCause(cause);
        ChatMessage message = new ChatMessage(payload);
        broadcast(session, message);
    }

    private void broadcastUserConnected(Session session, String username, String prevUsername) {
        BroadcastConnectedUserPayload payload = new BroadcastConnectedUserPayload();
        payload.setUsername(username);
        payload.setPrevUsername(prevUsername);
        ChatMessage message = new ChatMessage(payload);
        conversationsPersistence.save(message);
        broadcast(message, session);
    }

    private void broadcastUserDisconnected(String username) {
        BroadcastDisconnectedUserPayload payload = new BroadcastDisconnectedUserPayload();
        payload.setUsername(username);
        ChatMessage message = new ChatMessage(payload);
        conversationsPersistence.save(message);
        broadcast(message);
    }

    private void broadcastTextMessage(Session session, String text) {
        String username = getUsername(session);
        if (username != null) {
            BroadcastTextMessagePayload payload = new BroadcastTextMessagePayload();
            payload.setContent(text);
            payload.setUsername(username);
            ChatMessage message = new ChatMessage(payload);
            conversationsPersistence.save(message);
            broadcast(message, session);
        }
    }

    private void broadcastAvailableUsers() {
        BroadcastAvailableUsersPayload payload = new BroadcastAvailableUsersPayload();
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
