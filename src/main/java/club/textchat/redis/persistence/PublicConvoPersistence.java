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
package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.redis.subscribe.ChannelManager;
import club.textchat.server.message.ChatMessage;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.apon.AponReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class PublicConvoPersistence extends AbstractPersistence {

    private static final String KEY_PREFIX = "convo:";

    private static final int MAX_SAVE_MESSAGES = 50;

    private final ChannelManager channelManager;

    @Autowired
    public PublicConvoPersistence(RedisConnectionPool connectionPool,
                                  ChannelManager channelManager) {
        super(connectionPool);
        this.channelManager = channelManager;
    }

    public void put(String roomId, ChatMessage message) {
        String value = message.toString();
        rpush(makeKey(roomId), value, MAX_SAVE_MESSAGES);
        publish(channelManager.getPublicChatChannel(), value);
    }

    public List<ChatMessage> getRecentConvo(String roomId) {
        List<String> list = lrange(makeKey(roomId), MAX_SAVE_MESSAGES);
        List<ChatMessage> result = new ArrayList<>(list.size());
        for (String str : list) {
            ChatMessage message;
            try {
                message = AponReader.parse(str, ChatMessage.class);
                result.add(message);
            } catch (IOException e) {
                // ignore
            }
        }
        return result;
    }

    private String makeKey(String roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        return KEY_PREFIX + roomId;
    }

}
