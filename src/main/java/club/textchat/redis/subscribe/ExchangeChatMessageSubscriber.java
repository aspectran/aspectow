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
package club.textchat.redis.subscribe;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.server.ChatHandler;
import club.textchat.server.ExchangeChatHandler;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.PBEncryptionUtils;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

import java.io.IOException;

import static club.textchat.server.ExchangeChatHandler.CHAT_REQUEST;
import static club.textchat.server.ExchangeChatHandler.CHAT_REQUEST_ACCEPTED;
import static club.textchat.server.ExchangeChatHandler.CHAT_REQUEST_CANCELED;
import static club.textchat.server.ExchangeChatHandler.CHAT_REQUEST_DECLINED;

/**
 * <p>Created: 2020/05/10</p>
 */
@Component
@Bean
public class ExchangeChatMessageSubscriber extends RedisPubSubAdapter<String, String>
        implements InitializableBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeChatMessageSubscriber.class);

    private final StatefulRedisPubSubConnection<String, String> connection;

    private final ChatHandler chatHandler;

    private final ChannelManager channelManager;

    @Autowired
    public ExchangeChatMessageSubscriber(RedisConnectionPool connectionPool,
                                         ExchangeChatHandler chatHandler,
                                         ChannelManager channelManager) {
        this.connection = connectionPool.getPubSubConnection();
        this.chatHandler = chatHandler;
        this.channelManager = channelManager;
    }

    @Override
    public void message(String channel, String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(channel + ": " + message);
        }
        ChatMessage chatMessage;
        try {
            chatMessage = new ChatMessage(message);
        } catch (IOException e) {
            logger.warn(e);
            return;
        }
        BroadcastPayload broadcastPayload = chatMessage.getBroadcastPayload();
        UserJoinedPayload userJoinedPayload = chatMessage.getUserJoinedPayload();
        UserLeftPayload userLeftPayload = chatMessage.getUserLeftPayload();
        if (broadcastPayload != null) {
            String roomId = broadcastPayload.getRoomId();
            String exchangeRoomId = ExchangeChatHandler.swapExchangeRoomId(roomId);
            String content = broadcastPayload.getContent();
            if (content.startsWith(CHAT_REQUEST)) {
                int targetUserNo = ExchangeChatHandler.parseTargetUserNo(content);
                if (targetUserNo > 0) {
                    chatHandler.send(chatMessage, exchangeRoomId, targetUserNo);
                }
            } else if (content.startsWith(CHAT_REQUEST_DECLINED)) {
                int targetUserNo = ExchangeChatHandler.parseTargetUserNo(content);
                if (targetUserNo > 0) {
                    broadcastPayload.setContent(CHAT_REQUEST_DECLINED + broadcastPayload.getUserNo());
                    chatHandler.send(chatMessage, exchangeRoomId, targetUserNo);
                }
            } else if (content.startsWith(CHAT_REQUEST_CANCELED)) {
                int targetUserNo = ExchangeChatHandler.parseTargetUserNo(content);
                if (targetUserNo > 0) {
                    broadcastPayload.setContent(CHAT_REQUEST_CANCELED + broadcastPayload.getUserNo());
                    chatHandler.send(chatMessage, exchangeRoomId, targetUserNo);
                }
            } else if (content.startsWith(CHAT_REQUEST_ACCEPTED)) {
                int targetUserNo = ExchangeChatHandler.parseTargetUserNo(content);
                if (targetUserNo > 0) {
                    String encryptedPrivateRoomId = PBEncryptionUtils.encrypt(ExchangeChatHandler.nextPrivateRoomId());
                    broadcastPayload.setContent(CHAT_REQUEST_ACCEPTED + targetUserNo + ":" + encryptedPrivateRoomId);
                    chatHandler.send(chatMessage, roomId, broadcastPayload.getUserNo());
                    broadcastPayload.setContent(CHAT_REQUEST_ACCEPTED + broadcastPayload.getUserNo() + ":" + encryptedPrivateRoomId);
                    chatHandler.send(chatMessage, exchangeRoomId, targetUserNo);
                }
            } else {
                chatHandler.send(chatMessage, roomId, broadcastPayload.getUserNo());
                chatHandler.send(chatMessage, exchangeRoomId);
            }
        } else if (userJoinedPayload != null) {
            String exchangeRoomId = ExchangeChatHandler.swapExchangeRoomId(userJoinedPayload.getRoomId());
            chatHandler.send(chatMessage, exchangeRoomId);
        } else if (userLeftPayload != null) {
            String exchangeRoomId = ExchangeChatHandler.swapExchangeRoomId(userLeftPayload.getRoomId());
            chatHandler.send(chatMessage, exchangeRoomId);
        }
    }

    @Override
    public void initialize() throws Exception {
        connection.addListener(this);
        RedisPubSubCommands<String, String> sync = connection.sync();
        sync.subscribe(channelManager.getExchangeChatChannel());
    }

    @Override
    public void destroy() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

}
