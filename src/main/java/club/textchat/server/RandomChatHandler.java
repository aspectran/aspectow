package club.textchat.server;

import club.textchat.redis.persistence.ChatersPersistence;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.RandomConvosPersistence;
import club.textchat.redis.persistence.SignedInUsersPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.JoinPayload;
import club.textchat.server.message.payload.MessagePayload;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

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

    @Autowired
    public RandomChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                             InConvoUsersPersistence inConvoUsersPersistence,
                             ChatersPersistence chatersPersistence,
                             RandomConvosPersistence convosPersistence) {
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

    protected void join(Session session, ChaterInfo chaterInfo, boolean rejoin) {
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
        }
    }

    @Override
    public void broadcast(ChatMessage message, String roomId, final long targetUserNo) {
        for (Map.Entry<ChaterInfo, Session> entry : chaters.entrySet()) {
            ChaterInfo chaterInfo = entry.getKey();
            Session session = entry.getValue();
            if (targetUserNo == chaterInfo.getUserNo()) {
                send(session, message);
            }
        }
    }

}
