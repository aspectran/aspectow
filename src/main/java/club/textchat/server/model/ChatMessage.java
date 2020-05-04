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
package club.textchat.server.model;

import club.textchat.server.model.payload.BroadcastAvailableUsersPayload;
import club.textchat.server.model.payload.BroadcastConnectedUserPayload;
import club.textchat.server.model.payload.BroadcastDisconnectedUserPayload;
import club.textchat.server.model.payload.BroadcastTextMessagePayload;
import club.textchat.server.model.payload.DuplicatedUserPayload;
import club.textchat.server.model.payload.SendTextMessagePayload;
import club.textchat.server.model.payload.WelcomeUserPayload;
import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.ParameterKey;
import com.aspectran.core.util.apon.ParameterValue;
import com.aspectran.core.util.apon.ValueType;

/**
 * The Chat Message.
 *
 * <p>Created: 2019/10/09</p>
 */
public class ChatMessage extends AbstractParameters {

    private static final String HEARTBEAT_PING_MSG = "--heartbeat-ping--";
    private static final String HEARTBEAT_PONG_MSG = "--heartbeat-pong--";

    private static final ParameterKey heartBeat;
    private static final ParameterKey welcomeUser;
    private static final ParameterKey duplicatedUser;
    private static final ParameterKey broadcastAvailableUsers;
    private static final ParameterKey broadcastConnectedUser;
    private static final ParameterKey broadcastDisconnectedUser;
    private static final ParameterKey broadcastTextMessage;
    private static final ParameterKey sendTextMessage;

    private static final ParameterKey[] parameterKeys;

    static {
        heartBeat = new ParameterKey("heartBeat", ValueType.STRING);
        welcomeUser = new ParameterKey("welcomeUser", WelcomeUserPayload.class);
        duplicatedUser = new ParameterKey("duplicatedUser", DuplicatedUserPayload.class);
        broadcastAvailableUsers = new ParameterKey("broadcastAvailableUsers", BroadcastAvailableUsersPayload.class);
        broadcastConnectedUser = new ParameterKey("broadcastConnectedUser", BroadcastConnectedUserPayload.class);
        broadcastDisconnectedUser = new ParameterKey("broadcastDisconnectedUser", BroadcastDisconnectedUserPayload.class);
        broadcastTextMessage = new ParameterKey("broadcastTextMessage", BroadcastTextMessagePayload.class);
        sendTextMessage = new ParameterKey("sendTextMessage", SendTextMessagePayload.class);

        parameterKeys = new ParameterKey[] {
                heartBeat,
                welcomeUser,
                duplicatedUser,
                broadcastAvailableUsers,
                broadcastConnectedUser,
                broadcastDisconnectedUser,
                broadcastTextMessage,
                sendTextMessage
        };
    }

    public ChatMessage() {
        super(parameterKeys);
    }

    public ChatMessage(WelcomeUserPayload welcomeUserPayload) {
        this();
        putValue(welcomeUser, welcomeUserPayload);
    }

    public ChatMessage(DuplicatedUserPayload duplicatedUserPayload) {
        this();
        putValue(duplicatedUser, duplicatedUserPayload);
    }

    public ChatMessage(BroadcastAvailableUsersPayload broadcastAvailableUsersPayload) {
        this();
        putValue(broadcastAvailableUsers, broadcastAvailableUsersPayload);
    }

    public ChatMessage(BroadcastConnectedUserPayload broadcastConnectedUserPayload) {
        this();
        putValue(broadcastConnectedUser, broadcastConnectedUserPayload);
    }

    public ChatMessage(BroadcastDisconnectedUserPayload broadcastDisconnectedUserPayload) {
        this();
        putValue(broadcastDisconnectedUser, broadcastDisconnectedUserPayload);
    }

    public ChatMessage(BroadcastTextMessagePayload broadcastTextMessagePayload) {
        this();
        putValue(broadcastTextMessage, broadcastTextMessagePayload);
    }

    public boolean heartBeatPing() {
        String heartBeatMsg = getString(heartBeat);
        return HEARTBEAT_PING_MSG.equals(heartBeatMsg);
    }

    public void heartBeatPong() {
        putValue(heartBeat, HEARTBEAT_PONG_MSG);
    }

    public SendTextMessagePayload getSendTextMessagePayload() {
        return getParameters(sendTextMessage);
    }

}
