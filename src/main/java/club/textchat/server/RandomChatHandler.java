/*
 * Copyright (c) 2020 The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package club.textchat.server;

import club.textchat.chat.ChatAction;
import club.textchat.redis.persistence.ChatersPersistence;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.RandomChaterPersistence;
import club.textchat.redis.persistence.RandomConvoPersistence;
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

    private final RandomConvoPersistence randomConvoPersistence;

    private final RandomChatCoupler randomChatCoupler;

    @Autowired
    public RandomChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                             InConvoUsersPersistence inConvoUsersPersistence,
                             ChatersPersistence chatersPersistence,
                             RandomChaterPersistence randomChaterPersistence,
                             RandomHistoryPersistence randomHistoryPersistence,
                             RandomConvoPersistence randomConvoPersistence) {
        super(signedInUsersPersistence, inConvoUsersPersistence, chatersPersistence);
        this.chatersPersistence = chatersPersistence;
        this.randomChaterPersistence = randomChaterPersistence;
        this.randomHistoryPersistence = randomHistoryPersistence;
        this.randomConvoPersistence = randomConvoPersistence;
        this.randomChatCoupler = new RandomChatCoupler(this);
    }

    protected void handle(Session session, ChatMessage chatMessage) {
        if (heartBeat(session, chatMessage)) {
            return;
        }
        MessagePayload payload = chatMessage.getMessagePayload();
        if (payload != null) {
            ChaterInfo chaterInfo = getChaterInfo(session);
            if (!ChatAction.RANDOM_CHATROOM_ID.equals(chaterInfo.getRoomId())) {
                sendAbort(session, chaterInfo, "no-chater");
                return;
            }
            switch (payload.getType()) {
                case CHAT:
                    ChaterInfo chaterInfo2 = getPartner(payload.getUserNo());
                    broadcastMessage(chaterInfo, chaterInfo2, payload.getContent());
                    break;
                case JOIN:
                    String username = chaterInfo.getUsername();
                    String username2 = payload.getUsername();
                    if (!username.equals(username2)) {
                        sendAbort(session, chaterInfo, "abnormal");
                        return;
                    }
                    if (existsChater(chaterInfo)) {
                        if (!checkSameUser(chaterInfo)) {
                            sendAbort(session, chaterInfo, "exists");
                            return;
                        }
                        Session session2 = chaters.get(chaterInfo);
                        if (session2 != null) {
                            sendAbort(session2, chaterInfo, "rejoin");
                        }
                        join(session, chaterInfo, true);
                    } else {
                        join(session, chaterInfo, false);
                    }
                    break;
                default:
                    sendAbort(session, chaterInfo, "abnormal");
            }
        }
    }

    protected void close(Session session, CloseReason reason) {
        ChaterInfo chaterInfo = getChaterInfo(session);
        leave(session, chaterInfo);
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

    @Override
    protected void sendAbort(Session session, ChaterInfo chaterInfo, String cause) {
        randomChatCoupler.withdraw(chaterInfo);
        randomChaterPersistence.remove(chaterInfo.getUserNo());
        super.sendAbort(session, chaterInfo, cause);
    }

    private void broadcastUserJoined(ChaterInfo chaterInfo, ChaterInfo chaterInfo2) {
        UserJoinedPayload payload = new UserJoinedPayload();
        payload.setUserNo(chaterInfo2.getUserNo());
        payload.setUsername(chaterInfo2.getUsername());
        ChatMessage message = new ChatMessage(payload);
        message.setReceiver(chaterInfo.getUserNo());
        broadcast(message, chaterInfo.getUserNo());
        broadcastUserJoined(chaterInfo, chaterInfo2.getUserNo());
    }

    private void broadcastUserJoined(ChaterInfo chaterInfo, int userNo) {
        UserJoinedPayload payload = new UserJoinedPayload();
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        payload.setPrevUsername(chaterInfo.getPrevUsername());
        ChatMessage message = new ChatMessage(payload);
        message.setReceiver(userNo);
        randomConvoPersistence.put(message);
    }

    private void broadcastUserLeft(ChaterInfo chaterInfo, int userNo) {
        UserLeftPayload payload = new UserLeftPayload();
        payload.setUserNo(chaterInfo.getUserNo());
        payload.setUsername(chaterInfo.getUsername());
        ChatMessage message = new ChatMessage(payload);
        message.setReceiver(userNo);
        randomConvoPersistence.put(message);
    }

    private void broadcastMessage(ChaterInfo chaterInfo, ChaterInfo chaterInfo2, String content) {
        if (chaterInfo2 != null) {
            BroadcastPayload payload = new BroadcastPayload();
            payload.setUserNo(chaterInfo.getUserNo());
            payload.setUsername(chaterInfo.getUsername());
            payload.setContent(content);
            ChatMessage message = new ChatMessage(payload);
            message.setReceiver(chaterInfo2.getUserNo());
            randomConvoPersistence.put(message);
        }
    }

    public ChaterInfo getPartner(int userNo) {
        return randomChaterPersistence.get(userNo);
    }

    public void setPartner(@NonNull ChaterInfo chaterInfo1, @NonNull ChaterInfo chaterInfo2) {
        randomChaterPersistence.set(chaterInfo1, chaterInfo2);
        broadcastUserJoined(chaterInfo1, chaterInfo2);
    }

    public boolean hasPartner(int userNo) {
        return randomChaterPersistence.exists(userNo);
    }

    public boolean isPastPartner(int userNo1, int userNo2) {
        return randomHistoryPersistence.exists(userNo1, userNo2);
    }

    public ChaterInfo randomChater() {
        return chatersPersistence.randomChater(ChatAction.RANDOM_CHATROOM_ID);
    }

}
