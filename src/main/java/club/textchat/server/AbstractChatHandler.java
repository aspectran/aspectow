package club.textchat.server;

import club.textchat.redis.persistence.ChatersPersistence;
import club.textchat.redis.persistence.ConvosPersistence;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.SignedInUsersPersistence;
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
import com.aspectran.core.util.security.InvalidPBTokenException;
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Created: 2020/05/03</p>
 */
public abstract class AbstractChatHandler extends InstantActivitySupport implements ChatHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChatHandler.class);

    protected final Map<ChaterInfo, Session> chaters = new ConcurrentHashMap<>();

    protected final SignedInUsersPersistence signedInUsersPersistence;

    protected final InConvoUsersPersistence inConvoUsersPersistence;

    protected final ChatersPersistence chatersPersistence;

    protected final ConvosPersistence convosPersistence;

    protected AbstractChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                                  InConvoUsersPersistence inConvoUsersPersistence,
                                  ChatersPersistence chatersPersistence,
                                  ConvosPersistence convosPersistence) {
        this.signedInUsersPersistence = signedInUsersPersistence;
        this.inConvoUsersPersistence = inConvoUsersPersistence;
        this.chatersPersistence = chatersPersistence;
        this.convosPersistence = convosPersistence;
    }

    protected void open(String encryptedToken, Session session, EndpointConfig config) throws IOException {
        AdmissionToken admissionToken;
        try {
            admissionToken = TimeLimitedPBTokenIssuer.getPayload(encryptedToken, AdmissionToken.class);
        } catch (InvalidPBTokenException e) {
            logger.warn(e);
            String reason = "Access denied due to invalid admission token";
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            throw new IOException(reason);
        }

        ChaterInfo chaterInfo = (ChaterInfo)config.getUserProperties().get(ChaterInfo.CHATER_INFO_PROP);
        if (chaterInfo == null || chaterInfo.getUserNo() != admissionToken.getUserNo() ||
                !chaterInfo.getUsername().equals(admissionToken.getUsername())) {
            String reason = "User authentication failed";
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            throw new IOException(reason);
        }

        chaterInfo.setRoomId(admissionToken.getRoomId());
        if (logger.isDebugEnabled()) {
            logger.debug("Created chater " + chaterInfo);
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

    protected boolean heartBeat(Session session, ChatMessage message) {
        if (message.heartBeatPing()) {
            message.heartBeatPong();
            send(session, message);
            return true;
        } else {
            return false;
        }
    }

    protected void abort(Session session, ChaterInfo chaterInfo, String cause) {
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

//    private void sendJoinedUsers(Session session, ChaterInfo chaterInfo) {
//        Set<String> chaters = chatersPersistence.getChaters(chaterInfo.getRoomId());
//        ChatersPayload payload = new ChatersPayload();
//        payload.setChaters(chaters);
//        ChatMessage message = new ChatMessage(payload);
//        send(session, message);
//    }

    protected void broadcastUserJoined(ChaterInfo chaterInfo) {
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

    protected void broadcastMessage(ChaterInfo chaterInfo, String content) {
        BroadcastPayload payload = new BroadcastPayload();
        payload.setRoomId(chaterInfo.getRoomId());
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        payload.setContent(content);
        ChatMessage message = new ChatMessage(payload);
        convosPersistence.put(chaterInfo.getRoomId(), message);
    }

    protected void send(Session session, ChatMessage message) {
        if (session.isOpen()) {
            session.getAsyncRemote().sendObject(message);
        }
    }

    @NonNull
    protected ChaterInfo getChaterInfo(Session session) {
        return (ChaterInfo)session.getUserProperties().get(ChaterInfo.CHATER_INFO_PROP);
    }

    protected boolean existsChater(ChaterInfo chaterInfo) {
        if (chaters.containsKey(chaterInfo)) {
            return true;
        }
        return chatersPersistence.isChater(chaterInfo);
    }

    protected boolean checkSameUser(ChaterInfo chaterInfo) {
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
