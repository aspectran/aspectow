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

import club.textchat.server.model.payload.AbnormalAccessPayload;
import club.textchat.server.model.payload.BroadcastJoinedUsersPayload;
import club.textchat.server.model.payload.BroadcastUserJoinedPayload;
import club.textchat.server.model.payload.BroadcastUserLeavedPayload;
import club.textchat.server.model.payload.BroadcastMessagePayload;
import club.textchat.server.model.payload.SendMessagePayload;
import club.textchat.server.model.payload.WelcomeUserPayload;
import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.ParameterKey;
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
    private static final ParameterKey abnormalAccess;
    private static final ParameterKey broadcastJoinedUsers;
    private static final ParameterKey broadcastUserJoined;
    private static final ParameterKey broadcastUserLeaved;
    private static final ParameterKey broadcastMessage;
    private static final ParameterKey sendMessage;

    private static final ParameterKey[] parameterKeys;

    static {
        heartBeat = new ParameterKey("heartBeat", ValueType.STRING);
        welcomeUser = new ParameterKey("welcomeUser", WelcomeUserPayload.class);
        abnormalAccess = new ParameterKey("abnormalAccess", AbnormalAccessPayload.class);
        broadcastJoinedUsers = new ParameterKey("broadcastJoinedUsers", BroadcastJoinedUsersPayload.class);
        broadcastUserJoined = new ParameterKey("broadcastUserJoined", BroadcastUserJoinedPayload.class);
        broadcastUserLeaved = new ParameterKey("broadcastUserLeaved", BroadcastUserLeavedPayload.class);
        broadcastMessage = new ParameterKey("broadcastMessage", BroadcastMessagePayload.class);
        sendMessage = new ParameterKey("sendMessage", SendMessagePayload.class);

        parameterKeys = new ParameterKey[] {
                heartBeat,
                welcomeUser,
                abnormalAccess,
                broadcastJoinedUsers,
                broadcastUserJoined,
                broadcastUserLeaved,
                broadcastMessage,
                sendMessage
        };
    }

    public ChatMessage() {
        super(parameterKeys);
    }

    public ChatMessage(WelcomeUserPayload welcomeUserPayload) {
        this();
        putValue(welcomeUser, welcomeUserPayload);
    }

    public ChatMessage(AbnormalAccessPayload abnormalAccessPayload) {
        this();
        putValue(abnormalAccess, abnormalAccessPayload);
    }

    public ChatMessage(BroadcastJoinedUsersPayload broadcastJoinedUsersPayload) {
        this();
        putValue(broadcastJoinedUsers, broadcastJoinedUsersPayload);
    }

    public ChatMessage(BroadcastUserJoinedPayload broadcastUserJoinedPayload) {
        this();
        putValue(broadcastUserJoined, broadcastUserJoinedPayload);
    }

    public ChatMessage(BroadcastUserLeavedPayload broadcastUserLeavedPayload) {
        this();
        putValue(broadcastUserLeaved, broadcastUserLeavedPayload);
    }

    public ChatMessage(BroadcastMessagePayload broadcastMessagePayload) {
        this();
        putValue(broadcastMessage, broadcastMessagePayload);
    }

    public boolean heartBeatPing() {
        String heartBeatMsg = getString(heartBeat);
        return HEARTBEAT_PING_MSG.equals(heartBeatMsg);
    }

    public void heartBeatPong() {
        putValue(heartBeat, HEARTBEAT_PONG_MSG);
    }

    public SendMessagePayload getSendMessagePayload() {
        return getParameters(sendMessage);
    }

}
