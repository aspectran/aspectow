package club.textchat.server;

import club.textchat.redis.persistence.ChatersPersistence;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.SignedInUsersPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.AbortPayload;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Created: 2020/05/03</p>
 */
public abstract class AbstractChatHandler extends InstantActivitySupport {

    private static final Logger logger = LoggerFactory.getLogger(AbstractChatHandler.class);

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    protected final Map<ChaterInfo, Session> chaters = new ConcurrentHashMap<>();

    protected final SignedInUsersPersistence signedInUsersPersistence;

    protected final InConvoUsersPersistence inConvoUsersPersistence;

    protected final ChatersPersistence chatersPersistence;

    protected AbstractChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                                  InConvoUsersPersistence inConvoUsersPersistence,
                                  ChatersPersistence chatersPersistence) {
        this.signedInUsersPersistence = signedInUsersPersistence;
        this.inConvoUsersPersistence = inConvoUsersPersistence;
        this.chatersPersistence = chatersPersistence;
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

    protected boolean heartBeat(Session session, ChatMessage message) {
        if (message.heartBeatPing()) {
            message.heartBeatPong();
            send(session, message);
            return true;
        } else {
            return false;
        }
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

    protected String getCurrentDatetime(ChaterInfo chaterInfo) {
        if (chaterInfo.getZoneId() != null) {
            return dateTimeFormatter.format(ZonedDateTime.now(chaterInfo.getZoneId()));
        } else {
            return null;
        }
    }

}
