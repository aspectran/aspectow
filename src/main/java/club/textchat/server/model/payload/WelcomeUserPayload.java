/*
 * Copyright (c) 2008-2020 The Aspectran Project
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
package club.textchat.server.model.payload;

import club.textchat.server.model.ChatMessage;
import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.ParameterKey;
import com.aspectran.core.util.apon.ValueType;

import java.util.List;

/**
 * Represents the payload of a WebSocket frame to welcome a user.
 *
 * <p>Created: 2019/10/09</p>
 */
public class WelcomeUserPayload extends AbstractParameters {

    private static final ParameterKey username;
    private static final ParameterKey recentConversations;
    private static final ParameterKey rejoin;

    private static final ParameterKey[] parameterKeys;

    static {
        username = new ParameterKey("username", ValueType.STRING);
        recentConversations = new ParameterKey("recentConversations", ChatMessage.class, true);
        rejoin = new ParameterKey("rejoin", ValueType.BOOLEAN);

        parameterKeys = new ParameterKey[] {
                username,
                recentConversations,
                rejoin
        };
    }

    public WelcomeUserPayload() {
        super(parameterKeys);
    }

    public void setUsername(String username) {
        putValue(WelcomeUserPayload.username, username);
    }

    public void setRecentConversations(List<ChatMessage> messages) {
        if (messages != null) {
            for (ChatMessage message : messages) {
                putValue(recentConversations, message);
            }
        }
    }

    public void setRejoin(boolean rejoin) {
        putValue(WelcomeUserPayload.rejoin, rejoin);
    }

}
