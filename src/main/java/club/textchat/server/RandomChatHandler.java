package club.textchat.server;

import club.textchat.redis.persistence.ChatersPersistence;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.RandomChaterPersistence;
import club.textchat.redis.persistence.RandomConvosPersistence;
import club.textchat.redis.persistence.RandomHistoryPersistence;
import club.textchat.redis.persistence.SignedInUsersPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.JoinPayload;
import club.textchat.server.message.payload.MessagePayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.lang.NonNull;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <p>Created: 2020/05/14</p>
 */
@Component
@Bean
public class RandomChatHandler extends AbstractChatHandler {

    private final ChatersPersistence chatersPersistence;

    private final RandomChaterPersistence randomChaterPersistence;

    private final RandomHistoryPersistence randomHistoryPersistence;

    private final RandomConvosPersistence randomConvosPersistence;

    private final RandomChatCoupler randomChatCoupler;

    @Autowired
    public RandomChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                             InConvoUsersPersistence inConvoUsersPersistence,
                             ChatersPersistence chatersPersistence,
                             RandomChaterPersistence randomChaterPersistence,
                             RandomHistoryPersistence randomHistoryPersistence,
                             RandomConvosPersistence randomConvosPersistence) {
        super(signedInUsersPersistence, inConvoUsersPersistence, chatersPersistence);
        this.chatersPersistence = chatersPersistence;
        this.randomChaterPersistence = randomChaterPersistence;
        this.randomHistoryPersistence = randomHistoryPersistence;
        this.randomConvosPersistence = randomConvosPersistence;
        this.randomChatCoupler = new RandomChatCoupler(this);
    }

    protected void handle(Session session, ChatMessage chatMessage) {
        if (heartBeat(session, chatMessage)) {
            return;
        }
        MessagePayload payload = chatMessage.getMessagePayload();
        if (payload != null) {
            ChaterInfo chaterInfo = getChaterInfo(session);
            chaterInfo.setRoomId(null);
            switch (payload.getType()) {
                case CHAT:
                    ChaterInfo chaterInfo2 = getPartner(payload.getUserNo());
                    broadcastMessage(chaterInfo, chaterInfo2, payload.getContent());
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
                        join(session, chaterInfo, true);
                    } else {
                        join(session, chaterInfo, false);
                    }
                    break;
                default:
                    abort(session, chaterInfo, "abnormal");
            }
        }
    }

    private void join(Session session, ChaterInfo chaterInfo, boolean rejoin) {
        if (session.isOpen()) {
            chaters.put(chaterInfo, session);
            chatersPersistence.put(chaterInfo);
            inConvoUsersPersistence.put(chaterInfo.getUsername(), chaterInfo.getHttpSessionId());
            Set<String> roomChaters = Collections.singleton(ChatersPersistence.makeValue(chaterInfo));
            JoinPayload payload = new JoinPayload();
            payload.setUsername(chaterInfo.getUsername());
            payload.setChaters(roomChaters);
            payload.setRejoin(rejoin);
            ChatMessage message = new ChatMessage(payload);
            send(session, message);
            randomChaterPersistence.set(chaterInfo.getUserNo());
            randomChatCoupler.request(chaterInfo);
        }
    }

    @Override
    protected void abort(Session session, ChaterInfo chaterInfo, String cause) {
        randomChatCoupler.withdraw(chaterInfo);
        randomChaterPersistence.remove(chaterInfo.getUserNo());
        super.abort(session, chaterInfo, cause);
    }

    protected void close(Session session, CloseReason reason) {
        ChaterInfo chaterInfo = getChaterInfo(session);
        leave(session, chaterInfo);
    }

    private void leave(Session session, ChaterInfo chaterInfo) {
        randomChatCoupler.withdraw(chaterInfo);
        if (chaters.remove(chaterInfo, session)) {
            chatersPersistence.remove(chaterInfo);
            signedInUsersPersistence.tryAbandon(chaterInfo.getUsername(), chaterInfo.getHttpSessionId());
            inConvoUsersPersistence.remove(chaterInfo.getUsername());
            ChaterInfo chaterInfo2 = getPartner(chaterInfo.getUserNo());
            if (chaterInfo2 != null) {
                randomChaterPersistence.unset(chaterInfo.getUserNo(), chaterInfo2.getUserNo());
                randomHistoryPersistence.set(chaterInfo.getUserNo(), chaterInfo2.getUserNo());
                broadcastUserLeft(chaterInfo, chaterInfo2.getUserNo());
            }
            randomChaterPersistence.remove(chaterInfo.getUserNo());
        }
    }

    protected void broadcastUserJoined(ChaterInfo chaterInfo, ChaterInfo chaterInfo2) {
        UserJoinedPayload payload = new UserJoinedPayload();
        payload.setUserNo(chaterInfo2.getUserNo());
        payload.setUsername(chaterInfo2.getUsername());
        ChatMessage message = new ChatMessage(payload);
        message.setReceiver(chaterInfo.getUserNo());
        broadcast(message);
        broadcastUserJoined(chaterInfo, chaterInfo2.getUserNo());
    }

    private void broadcastUserJoined(ChaterInfo chaterInfo, long userNo) {
        UserJoinedPayload payload = new UserJoinedPayload();
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        payload.setPrevUsername(chaterInfo.getPrevUsername());
        ChatMessage message = new ChatMessage(payload);
        message.setReceiver(userNo);
        randomConvosPersistence.put(message);
    }

    private void broadcastUserLeft(ChaterInfo chaterInfo, long userNo) {
        UserLeftPayload payload = new UserLeftPayload();
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        ChatMessage message = new ChatMessage(payload);
        message.setReceiver(userNo);
        randomConvosPersistence.put(message);
    }

    private void broadcastMessage(ChaterInfo chaterInfo, ChaterInfo chaterInfo2, String content) {
        if (chaterInfo2 != null) {
            BroadcastPayload payload = new BroadcastPayload();
            payload.setUserNo(chaterInfo.getUserNo());
            payload.setUsername(chaterInfo.getUsername());
            payload.setContent(content);
            ChatMessage message = new ChatMessage(payload);
            message.setReceiver(chaterInfo2.getUserNo());
            randomConvosPersistence.put(message);
        }
    }

    public void broadcast(ChatMessage message) {
        for (Map.Entry<ChaterInfo, Session> entry : chaters.entrySet()) {
            ChaterInfo chaterInfo = entry.getKey();
            Session session = entry.getValue();
            if (message.getReceiver() == chaterInfo.getUserNo()) {
                send(session, message);
            }
        }
    }

    public ChaterInfo getPartner(long userNo) {
        return randomChaterPersistence.get(userNo);
    }

    public void setPartner(@NonNull ChaterInfo chaterInfo1, @NonNull ChaterInfo chaterInfo2) {
        randomChaterPersistence.set(chaterInfo1, chaterInfo2);
    }

    public boolean hasPartner(long userNo) {
        return randomChaterPersistence.exists(userNo);
    }

    public boolean isPastPartner(long userNo1, long userNo2) {
        return randomHistoryPersistence.exists(userNo1, userNo2);
    }

    public ChaterInfo randomChater() {
        return chatersPersistence.randomChater();
    }

}
