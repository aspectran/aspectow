package club.textchat.server;

import club.textchat.persistence.ChatersPersistence;
import club.textchat.persistence.ConvosPersistence;
import club.textchat.persistence.InConvoUsersPersistence;
import club.textchat.persistence.SignedInUsersPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.AbortPayload;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.ChatersPayload;
import club.textchat.server.message.payload.JoinPayload;
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

    private final Map<ChaterInfo, Session> chaters = new ConcurrentHashMap<>();

    private final SignedInUsersPersistence signedInUsersPersistence;

    private final InConvoUsersPersistence inConvoUsersPersistence;

    private final ChatersPersistence chatersPersistence;

    private final ConvosPersistence convosPersistence;

    protected ChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                          InConvoUsersPersistence inConvoUsersPersistence,
                          ChatersPersistence chatersPersistence,
                          ConvosPersistence convosPersistence) {
        this.signedInUsersPersistence = signedInUsersPersistence;
        this.inConvoUsersPersistence = inConvoUsersPersistence;
        this.chatersPersistence = chatersPersistence;
        this.convosPersistence = convosPersistence;
    }

    protected void handle(Session session, ChatMessage chatMessage) {
        if (chatMessage.heartBeatPing()) {
            chatMessage.heartBeatPong();
            send(session, chatMessage);
            return;
        }
        MessagePayload payload = chatMessage.getMessagePayload();
        if (payload != null) {
            ChaterInfo chaterInfo = getChaterInfo(session);
            switch (payload.getType()) {
                case CHAT:
                    broadcastMessage(chaterInfo, payload.getContent());
                    break;
                case JOIN:
                    String username = chaterInfo.getUsername();
                    String username2 = payload.getUsername();
                    if (!username.equals(username2)) {
                        abort(session, chaterInfo, "abnormal");
                        return;
                    }
                    if (existsChater(chaterInfo)) {
                        if (!checkSameUser(chaterInfo)) {
                            abort(session, chaterInfo, "exists");
                            return;
                        }
                        Session session2 = chaters.get(chaterInfo);
                        if (session2 != null) {
                            abort(session2, chaterInfo, "rejoin");
                        }
                        if (!join(session, chaterInfo, true)) {
                            broadcastUserJoined(chaterInfo);
                        }
                    } else {
                        join(session, chaterInfo, false);
                        broadcastUserJoined(chaterInfo);
                    }
                    sendJoinedUsers(session, chaterInfo);
                    break;
                default:
                    abort(session, chaterInfo, "abnormal");
            }
        }
    }

    protected void close(Session session, CloseReason reason) {
        leave(session);
    }

    protected void error(Session session, Throwable error) {
        logger.error("Error in websocket session: " + session.getId(), error);
        try {
            ChaterInfo chaterInfo = getChaterInfo(session);
            abort(session, chaterInfo, "abnormal:" + error.getMessage());
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean join(Session session, ChaterInfo chaterInfo, boolean rejoin) {
        boolean replaced = false;
        if (session.isOpen()) {
            if (chaters.put(chaterInfo, session) != null) {
                replaced = true;
            }
            chatersPersistence.put(chaterInfo);
            inConvoUsersPersistence.put(chaterInfo.getUsername(), chaterInfo.getHttpSessionId());
            JoinPayload payload = new JoinPayload();
            payload.setUsername(chaterInfo.getUsername());
            payload.setRecentConvos(convosPersistence.getRecentConvos(chaterInfo.getRoomId()));
            payload.setRejoin(rejoin);
            ChatMessage message = new ChatMessage(payload);
            send(session, message);
        }
        return replaced;
    }

    private void abort(Session session, ChaterInfo chaterInfo, String cause) {
        if (chaters.remove(chaterInfo, session)) {
            chatersPersistence.remove(chaterInfo);
            signedInUsersPersistence.tryAbandon(chaterInfo.getUsername(), chaterInfo.getHttpSessionId());
            inConvoUsersPersistence.remove(chaterInfo.getUsername());
        }
        AbortPayload payload = new AbortPayload();
        payload.setCause(cause);
        ChatMessage message = new ChatMessage(payload);
        send(session, message);
    }

    private void leave(Session session) {
        ChaterInfo chaterInfo = getChaterInfo(session);
        if (chaters.remove(chaterInfo, session)) {
            chatersPersistence.remove(chaterInfo);
            signedInUsersPersistence.tryAbandon(chaterInfo.getUsername(), chaterInfo.getHttpSessionId());
            inConvoUsersPersistence.remove(chaterInfo.getUsername());
            broadcastUserLeft(chaterInfo);
        }
    }

    private void sendJoinedUsers(Session session, ChaterInfo chaterInfo) {
        Set<String> chaters = chatersPersistence.getChaters(chaterInfo.getRoomId());
        ChatersPayload payload = new ChatersPayload();
        payload.setChaters(chaters);
        ChatMessage message = new ChatMessage(payload);
        send(session, message);
    }

    private void broadcastUserJoined(ChaterInfo chaterInfo) {
        UserJoinedPayload payload = new UserJoinedPayload();
        payload.setRoomId(chaterInfo.getRoomId());
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        payload.setPrevUsername(chaterInfo.getPrevUsername());
        ChatMessage message = new ChatMessage(payload);
        convosPersistence.put(chaterInfo.getRoomId(), message);
    }

    private void broadcastUserLeft(ChaterInfo chaterInfo) {
        UserLeftPayload payload = new UserLeftPayload();
        payload.setRoomId(chaterInfo.getRoomId());
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        ChatMessage message = new ChatMessage(payload);
        convosPersistence.put(chaterInfo.getRoomId(), message);
    }

    private void broadcastMessage(ChaterInfo chaterInfo, String content) {
        BroadcastPayload payload = new BroadcastPayload();
        payload.setRoomId(chaterInfo.getRoomId());
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        payload.setContent(content);
        ChatMessage message = new ChatMessage(payload);
        convosPersistence.put(chaterInfo.getRoomId(), message);
    }

    public void broadcast(ChatMessage message, String roomId, final long excludedUserNo) {
        for (Map.Entry<ChaterInfo, Session> entry : chaters.entrySet()) {
            ChaterInfo chaterInfo = entry.getKey();
            Session session = entry.getValue();
            if (chaterInfo.getRoomId().equals(roomId) &&
                    (excludedUserNo == 0L || excludedUserNo != chaterInfo.getUserNo())) {
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
    private ChaterInfo getChaterInfo(Session session) {
        return (ChaterInfo)session.getUserProperties().get(ChaterInfo.CHATER_INFO_PROP);
    }

    private boolean existsChater(ChaterInfo chaterInfo) {
        if (chaters.containsKey(chaterInfo)) {
            return true;
        }
        return chatersPersistence.isChater(chaterInfo);
    }

    private boolean checkSameUser(ChaterInfo chaterInfo) {
        Session session = chaters.get(chaterInfo);
        String httpSessionId;
        if (session != null && session.isOpen()) {
            ChaterInfo chaterInfo2 = getChaterInfo(session);
            httpSessionId = chaterInfo2.getHttpSessionId();
        } else {
            httpSessionId = signedInUsersPersistence.get(chaterInfo.getUsername());
        }
        return (httpSessionId != null && httpSessionId.equals(chaterInfo.getHttpSessionId()));
    }

}
