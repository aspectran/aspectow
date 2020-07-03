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

import club.textchat.redis.persistence.ChatersPersistence;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.LobbyChatPersistence;
import club.textchat.redis.persistence.SignedInUsersPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.JoinPayload;
import club.textchat.server.message.payload.MessagePayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.StringUtils;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.Set;

/**
 * <p>Created: 2020/05/14</p>
 */
@Component
@Bean
public class LobbyChatHandler extends AbstractChatHandler {

    public static final String BROADCAST_MESSAGE_PREFIX = "broadcast:";

    private final LobbyChatPersistence lobbyChatPersistence;

    public LobbyChatHandler(SignedInUsersPersistence signedInUsersPersistence,
                            InConvoUsersPersistence inConvoUsersPersistence,
                            ChatersPersistence chatersPersistence,
                            LobbyChatPersistence lobbyChatPersistence) {
        super(signedInUsersPersistence, inConvoUsersPersistence, chatersPersistence);
        this.lobbyChatPersistence = lobbyChatPersistence;
    }

    protected void handle(Session session, ChatMessage chatMessage) {
        if (heartBeat(session, chatMessage)) {
            return;
        }
        MessagePayload payload = chatMessage.getMessagePayload();
        if (payload != null) {
            ChaterInfo chaterInfo = getChaterInfo(session);
            switch (payload.getType()) {
                case POST:
                    String content = payload.getContent();
                    if (!StringUtils.isEmpty(content)) {
                        broadcastMessage(chaterInfo, BROADCAST_MESSAGE_PREFIX + payload.getContent());
                    }
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
                        if (chaters.containsKey(chaterInfo)) {
                            sendAbort(session, chaterInfo, "rejoin");
                            return;
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
                    sendAbort(session, chaterInfo, "abnormal");
            }
        }
    }

    protected void close(Session session, CloseReason reason) {
        ChaterInfo chaterInfo = getChaterInfo(session);
        leave(session, chaterInfo);
    }

    private boolean join(Session session, ChaterInfo chaterInfo, boolean rejoin) {
        boolean replaced = false;
        if (session.isOpen()) {
            if (chaters.put(chaterInfo, session) != null) {
                replaced = true;
            }
            inConvoUsersPersistence.put(chaterInfo.getHttpSessionId(), chaterInfo.getRoomId());
            chatersPersistence.put(chaterInfo);
            Set<String> roomChaters = chatersPersistence.getChaters(chaterInfo.getRoomId());
            JoinPayload payload = new JoinPayload();
            payload.setChater(chaterInfo);
            payload.setChaters(roomChaters);
            payload.setRejoin(rejoin);
            send(session, new ChatMessage(payload));
        }
        return replaced;
    }

    private void leave(Session session, ChaterInfo chaterInfo) {
        if (chaters.remove(chaterInfo, session)) {
            chatersPersistence.remove(chaterInfo);
            signedInUsersPersistence.tryAbandon(chaterInfo.getUsername(), chaterInfo.getHttpSessionId());
            inConvoUsersPersistence.remove(chaterInfo.getHttpSessionId());
            broadcastUserLeft(chaterInfo);
        }
    }

    private void broadcastUserJoined(ChaterInfo chaterInfo) {
        UserJoinedPayload payload = new UserJoinedPayload();
        payload.setChater(chaterInfo);
        payload.setDatetime(getCurrentDatetime(chaterInfo));
        ChatMessage message = new ChatMessage(payload);
        lobbyChatPersistence.publish(message);
    }

    private void broadcastUserLeft(ChaterInfo chaterInfo) {
        UserLeftPayload payload = new UserLeftPayload();
        payload.setChater(chaterInfo);
        payload.setDatetime(getCurrentDatetime(chaterInfo));
        ChatMessage message = new ChatMessage(payload);
        lobbyChatPersistence.publish(message);
    }

    private void broadcastMessage(ChaterInfo chaterInfo, String content) {
        BroadcastPayload payload = new BroadcastPayload();
        payload.setChater(chaterInfo);
        payload.setContent(content);
        payload.setDatetime(getCurrentDatetime(chaterInfo));
        ChatMessage message = new ChatMessage(payload);
        lobbyChatPersistence.publish(message);
    }

}
