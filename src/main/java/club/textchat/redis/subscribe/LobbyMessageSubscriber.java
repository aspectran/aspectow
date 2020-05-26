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
import club.textchat.server.LobbyChatHandler;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

import java.io.IOException;

/**
 * <p>Created: 2020/05/10</p>
 */
@Component
@Bean
public class LobbyMessageSubscriber extends RedisPubSubAdapter<String, String>
        implements InitializableBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(LobbyMessageSubscriber.class);

    private final StatefulRedisPubSubConnection<String, String> connection;

    private final ChatHandler chatHandler;

    private final ChannelManager channelManager;

    @Autowired
    public LobbyMessageSubscriber(RedisConnectionPool connectionPool,
                                  LobbyChatHandler chatHandler,
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
        if (broadcastPayload != null) {
            chatHandler.broadcast(chatMessage);
            return;
        }
        UserJoinedPayload userJoinedPayload = chatMessage.getUserJoinedPayload();
        if (userJoinedPayload != null) {
            chatHandler.broadcast(chatMessage, (targetRoomId, targetUserNo) ->
                    (targetRoomId.equals(userJoinedPayload.getRoomId()) &&
                            targetUserNo != userJoinedPayload.getUserNo()));
            return;
        }
        UserLeftPayload userLeftPayload = chatMessage.getUserLeftPayload();
        if (userLeftPayload != null) {
            chatHandler.broadcast(chatMessage); // talker already left
        }
    }

    @Override
    public void initialize() throws Exception {
        connection.addListener(this);
        RedisPubSubCommands<String, String> sync = connection.sync();
        sync.subscribe(channelManager.getLobbyChatChannel());
    }

    @Override
    public void destroy() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

}
