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

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.EnvironmentAware;
import com.aspectran.core.context.env.Environment;
import com.aspectran.core.lang.NonNull;
import com.aspectran.core.util.StringUtils;

/**
 * <p>Created: 2020/05/26</p>
 */
@Component
@Bean
public class ChannelManager implements EnvironmentAware {

    private String publicChatChannel;

    private String privateChatChannel;

    private String randomChatChannel;

    private String lobbyChatChannel;

    @NonNull
    public String getPublicChatChannel() {
        return publicChatChannel;
    }

    @NonNull
    public String getPrivateChatChannel() {
        return privateChatChannel;
    }

    @NonNull
    public String getRandomChatChannel() {
        return randomChatChannel;
    }

    @NonNull
    public String getLobbyChatChannel() {
        return lobbyChatChannel;
    }

    @Override
    public void setEnvironment(Environment environment) {
        publicChatChannel = environment.getProperty("redis.channel.public_chat");
        if (!StringUtils.hasText(publicChatChannel)) {
            throw new IllegalArgumentException("Undefined environment property: redis.channel.public_chat");
        }
        privateChatChannel = environment.getProperty("redis.channel.private_chat");
        if (!StringUtils.hasText(privateChatChannel)) {
            throw new IllegalArgumentException("Undefined environment property: redis.channel.private_chat");
        }
        randomChatChannel = environment.getProperty("redis.channel.random_chat");
        if (!StringUtils.hasText(randomChatChannel)) {
            throw new IllegalArgumentException("Undefined environment property: redis.channel.random_chat");
        }
        lobbyChatChannel = environment.getProperty("redis.channel.lobby_chat");
        if (!StringUtils.hasText(lobbyChatChannel)) {
            throw new IllegalArgumentException("Undefined environment property: redis.channel.lobby_chat");
        }
    }

}
