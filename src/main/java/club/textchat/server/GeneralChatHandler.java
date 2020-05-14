package club.textchat.server;

import club.textchat.redis.persistence.ChatersPersistence;
import club.textchat.redis.persistence.GeneralConvosPersistence;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.SignedInUsersPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.JoinPayload;
import club.textchat.server.message.payload.MessagePayload;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

import javax.websocket.Session;
import java.util.Map;
import java.util.Set;

/**
 * <p>Created: 2020/05/14</p>
 */
@Component
@Bean
public class GeneralChatHandler extends AbstractChatHandler {

    public GeneralChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                             InConvoUsersPersistence inConvoUsersPersistence,
                             ChatersPersistence chatersPersistence,
                             GeneralConvosPersistence convosPersistence) {
        super(signedInUsersPersistence, inConvoUsersPersistence, chatersPersistence, convosPersistence);
    }

    protected void handle(Session session, ChatMessage chatMessage) {
        if (heartBeat(session, chatMessage)) {
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
                    break;
                default:
                    abort(session, chaterInfo, "abnormal");
            }
        }
    }

    protected boolean join(Session session, ChaterInfo chaterInfo, boolean rejoin) {
        boolean replaced = false;
        if (session.isOpen()) {
            if (chaters.put(chaterInfo, session) != null) {
                replaced = true;
            }
            chatersPersistence.put(chaterInfo);
            inConvoUsersPersistence.put(chaterInfo.getUsername(), chaterInfo.getHttpSessionId());
            Set<String> roomChaters = chatersPersistence.getChaters(chaterInfo.getRoomId());
            JoinPayload payload = new JoinPayload();
            payload.setUsername(chaterInfo.getUsername());
            payload.setChaters(roomChaters);
            payload.setRecentConvo(convosPersistence.getRecentConvo(chaterInfo.getRoomId()));
            payload.setRejoin(rejoin);
            ChatMessage message = new ChatMessage(payload);
            send(session, message);
        }
        return replaced;
    }

    @Override
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

}
