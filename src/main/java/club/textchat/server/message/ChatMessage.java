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
package club.textchat.server.message;

import club.textchat.server.message.payload.AbortPayload;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.ChatersPayload;
import club.textchat.server.message.payload.JoinPayload;
import club.textchat.server.message.payload.MessagePayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.AponReader;
import com.aspectran.core.util.apon.ParameterKey;
import com.aspectran.core.util.apon.ValueType;

import java.io.IOException;

/**
 * The Chat Message.
 *
 * <p>Created: 2019/10/09</p>
 */
public class ChatMessage extends AbstractParameters {

    private static final String HEARTBEAT_PING_MSG = "-ping-";
    private static final String HEARTBEAT_PONG_MSG = "-pong-";

    private static final ParameterKey receiver;
    private static final ParameterKey join;
    private static final ParameterKey chaters;
    private static final ParameterKey userJoined;
    private static final ParameterKey userLeft;
    private static final ParameterKey message;
    private static final ParameterKey broadcast;
    private static final ParameterKey heartBeat;
    private static final ParameterKey abort;

    private static final ParameterKey[] parameterKeys;

    static {
        receiver = new ParameterKey("receiver", ValueType.INT);
        join = new ParameterKey("join", JoinPayload.class);
        chaters = new ParameterKey("chaters", ChatersPayload.class);
        userJoined = new ParameterKey("userJoined", UserJoinedPayload.class);
        userLeft = new ParameterKey("userLeft", UserLeftPayload.class);
        message = new ParameterKey("message", MessagePayload.class);
        broadcast = new ParameterKey("broadcast", BroadcastPayload.class);
        heartBeat = new ParameterKey("heartBeat", ValueType.STRING);
        abort = new ParameterKey("abort", AbortPayload.class);

        parameterKeys = new ParameterKey[] {
                receiver,
                join,
                chaters,
                userJoined,
                userLeft,
                message,
                broadcast,
                heartBeat,
                abort
        };
    }

    public ChatMessage() {
        super(parameterKeys);
    }

    public ChatMessage(String text) throws IOException {
        this();
        AponReader.parse(text, this);
    }

    public ChatMessage(JoinPayload joinPayload) {
        this();
        putValue(join, joinPayload);
    }

    public ChatMessage(AbortPayload abortPayload) {
        this();
        putValue(abort, abortPayload);
    }

    public ChatMessage(ChatersPayload chatersPayload) {
        this();
        putValue(chaters, chatersPayload);
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

    public int getReceiver() {
        return getInt(receiver);
    }

    public void setReceiver(int userNo) {
        putValue(ChatMessage.receiver, userNo);
    }

    public boolean heartBeatPing() {
        String heartBeatMsg = getString(heartBeat);
        return HEARTBEAT_PING_MSG.equals(heartBeatMsg);
    }

    public void heartBeatPong() {
        putValue(heartBeat, HEARTBEAT_PONG_MSG);
    }

    public UserJoinedPayload getUserJoinedPayload() {
        return getParameters(userJoined);
    }

    public UserLeftPayload getUserLeftPayload() {
        return getParameters(userLeft);
    }

    public MessagePayload getMessagePayload() {
        return getParameters(message);
    }

    public BroadcastPayload getBroadcastPayload() {
        return getParameters(broadcast);
    }

}
