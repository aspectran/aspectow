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
package club.textchat.server.message;

import club.textchat.server.message.payload.AbortPayload;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.JoinPayload;
import club.textchat.server.message.payload.JoinedUsersPayload;
import club.textchat.server.message.payload.MessagePayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.ParameterKey;
import com.aspectran.core.util.apon.ValueType;

/**
 * The Chat Message.
 *
 * <p>Created: 2019/10/09</p>
 */
public class ChatMessage extends AbstractParameters {

    private static final String HEARTBEAT_PING_MSG = "--ping--";
    private static final String HEARTBEAT_PONG_MSG = "--pong--";

    private static final ParameterKey heartBeat;
    private static final ParameterKey join;
    private static final ParameterKey joinedUsers;
    private static final ParameterKey userJoined;
    private static final ParameterKey userLeft;
    private static final ParameterKey message;
    private static final ParameterKey broadcast;
    private static final ParameterKey abort;

    private static final ParameterKey[] parameterKeys;

    static {
        heartBeat = new ParameterKey("heartBeat", ValueType.STRING);
        join = new ParameterKey("join", JoinPayload.class);
        joinedUsers = new ParameterKey("joinedUsers", JoinedUsersPayload.class);
        userJoined = new ParameterKey("userJoined", UserJoinedPayload.class);
        userLeft = new ParameterKey("userLeft", UserLeftPayload.class);
        message = new ParameterKey("message", MessagePayload.class);
        broadcast = new ParameterKey("broadcast", BroadcastPayload.class);
        abort = new ParameterKey("abort", AbortPayload.class);

        parameterKeys = new ParameterKey[] {
                heartBeat,
                join,
                joinedUsers,
                userJoined,
                userLeft,
                message,
                broadcast,
                abort
        };
    }

    public ChatMessage() {
        super(parameterKeys);
    }

    public ChatMessage(JoinPayload joinPayload) {
        this();
        putValue(join, joinPayload);
    }

    public ChatMessage(AbortPayload abortPayload) {
        this();
        putValue(abort, abortPayload);
    }

    public ChatMessage(JoinedUsersPayload joinedUsersPayload) {
        this();
        putValue(joinedUsers, joinedUsersPayload);
    }

    public ChatMessage(UserJoinedPayload userJoinedPayload) {
        this();
        putValue(userJoined, userJoinedPayload);
    }

    public ChatMessage(UserLeftPayload userLeftPayload) {
        this();
        putValue(userLeft, userLeftPayload);
    }

    public ChatMessage(BroadcastPayload broadcastPayload) {
        this();
        putValue(broadcast, broadcastPayload);
    }

    public boolean heartBeatPing() {
        String heartBeatMsg = getString(heartBeat);
        return HEARTBEAT_PING_MSG.equals(heartBeatMsg);
    }

    public void heartBeatPong() {
        putValue(heartBeat, HEARTBEAT_PONG_MSG);
    }

    public MessagePayload getMessagePayload() {
        return getParameters(message);
    }

}
