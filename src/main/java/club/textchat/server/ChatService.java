package club.textchat.server;

import club.textchat.persistence.ChatMessagePersistence;
import club.textchat.server.model.ChatMessage;
import club.textchat.server.model.payload.BroadcastAvailableUsersPayload;
import club.textchat.server.model.payload.BroadcastConnectedUserPayload;
import club.textchat.server.model.payload.BroadcastDisconnectedUserPayload;
import club.textchat.server.model.payload.BroadcastTextMessagePayload;
import club.textchat.server.model.payload.DuplicatedUserPayload;
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

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final ChatMessagePersistence chatMessagePersistence;

    protected ChatService(ChatMessagePersistence chatMessagePersistence) {
        this.chatMessagePersistence = chatMessagePersistence;
    }

    protected void handle(Session session, ChatMessage chatMessage) throws Exception {
        SendTextMessagePayload payload = chatMessage.getSendTextMessagePayload();
        if (payload != null) {
            String username = getUsername(session);
            switch (payload.getType()) {
                case CHAT:
                    if (username != null) {
                        broadcastTextMessage(session, username, payload.getContent());
                    }
                    break;
                case JOIN:
                    if (username == null) {
                        username = payload.getUsername();
                        if (sessions.containsKey(username)) {
                            duplicatedUser(session, username);
                            return;
                        }
                        setUsername(session, username);
                        sessions.put(username, session);
                        welcomeUser(session, username);
                        broadcastUserConnected(session, username);
                        broadcastAvailableUsers();
                    }
                    break;
                case LEAVE:
                    if (username != null) {
                        leaveUser(username);
                    }
                    break;
            }
        }
    }

    protected void close(Session session, CloseReason reason) throws Exception {
        String username = getUsername(session);
        if (username != null) {
            leaveUser(username);
        }
    }

    protected void error(Session session, Throwable error) {
        logger.error("Error in websocket session: " + session.getId(), error);
        try {
            String username = getUsername(session);
            if (username != null) {
                leaveUser(username);
            }
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getUsername(Session session) {
        if (session.getUserProperties().get("username") != null) {
            return session.getUserProperties().get("username").toString();
        } else {
            return null;
        }
    }

    private void setUsername(Session session, String username) {
        session.getUserProperties().put("username", username);
    }

    private void welcomeUser(Session session, String username) {
        WelcomeUserPayload payload = new WelcomeUserPayload();
        payload.setUsername(username);
        session.getAsyncRemote().sendObject(new ChatMessage(payload));
    }

    private void duplicatedUser(Session session, String username) {
        DuplicatedUserPayload payload = new DuplicatedUserPayload();
        payload.setUsername(username);
        session.getAsyncRemote().sendObject(new ChatMessage(payload));
    }

    private void leaveUser(String username) throws Exception {
        sessions.remove(username);
        broadcastUserDisconnected(username);
        broadcastAvailableUsers();
    }

    private void broadcastUserConnected(Session session, String username) throws Exception {
        BroadcastConnectedUserPayload payload = new BroadcastConnectedUserPayload();
        payload.setUsername(username);
        broadcast(new ChatMessage(payload), session);
    }

    private void broadcastUserDisconnected(String username) throws Exception {
        BroadcastDisconnectedUserPayload payload = new BroadcastDisconnectedUserPayload();
        payload.setUsername(username);
        broadcast(new ChatMessage(payload));
    }

    private void broadcastTextMessage(Session session, String username, String text) throws Exception {
        BroadcastTextMessagePayload payload = new BroadcastTextMessagePayload();
        payload.setContent(text);
        payload.setUsername(username);
        broadcast(new ChatMessage(payload), session);
    }

    private void broadcastAvailableUsers() throws Exception {
        BroadcastAvailableUsersPayload payload = new BroadcastAvailableUsersPayload();
        payload.setUsernames(sessions.keySet());
        broadcast(new ChatMessage(payload));
    }

    private void broadcast(ChatMessage message) throws Exception {
        synchronized (sessions) {
            for (Session session : sessions.values()) {
                broadcast(session, message);
            }
        }
    }

    private void broadcast(ChatMessage message, Session ignoredSession) throws Exception {
        synchronized (sessions) {
            for (Session session : sessions.values()) {
                if (!session.getId().equals(ignoredSession.getId())) {
                    broadcast(session, message);
                }
            }
        }
    }

    private void broadcast(Session session, ChatMessage message) throws Exception {
        if (session.isOpen()) {
            session.getAsyncRemote().sendObject(message);
            chatMessagePersistence.push(message);
        }
    }

}
