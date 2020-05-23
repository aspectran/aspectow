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
import club.textchat.redis.subscribe.LobbyMessageSubscriber;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.BroadcastPayload;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

import static club.textchat.server.LobbyChatHandler.USER_MESSAGE_PREFIX;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class LobbyConvoPersistence extends AbstractPersistence {

    @Autowired
    public LobbyConvoPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void publish(ChatMessage message) {
        publish(LobbyMessageSubscriber.CHANNEL, message.toString());
    }

    public void publish(String content) {
        BroadcastPayload payload = new BroadcastPayload();
        payload.setContent(content);
        publish(new ChatMessage(payload));
    }

    public void say(String content) {
        publish(USER_MESSAGE_PREFIX + content);
    }

}
