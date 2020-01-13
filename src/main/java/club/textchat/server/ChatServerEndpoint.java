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
package club.textchat.server;

import club.textchat.common.recaptcha.ReCaptchaVerifier;
import club.textchat.server.codec.ChatMessageDecoder;
import club.textchat.server.codec.ChatMessageEncoder;
import club.textchat.server.model.ChatMessage;
import club.textchat.server.model.payload.BroadcastAvailableUsersPayload;
import club.textchat.server.model.payload.BroadcastConnectedUserPayload;
import club.textchat.server.model.payload.BroadcastDisconnectedUserPayload;
import club.textchat.server.model.payload.BroadcastTextMessagePayload;
import club.textchat.server.model.payload.DuplicatedUserPayload;
import club.textchat.server.model.payload.SendTextMessagePayload;
import club.textchat.server.model.payload.WelcomeUserPayload;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.logging.Log;
import com.aspectran.core.util.logging.LogFactory;
import com.aspectran.web.socket.jsr356.ActivityContextAwareEndpoint;
import com.aspectran.web.socket.jsr356.AspectranConfigurator;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for the chat server.
 *
 * <p>Created: 29/09/2019</p>
 */
@Component
@ServerEndpoint(
        value = "/chat",
        encoders = ChatMessageEncoder.class,
        decoders = ChatMessageDecoder.class,
        configurator = AspectranConfigurator.class
)
public class ChatServerEndpoint extends ActivityContextAwareEndpoint {

    private static final Log log = LogFactory.getLog(ChatServerEndpoint.class);

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String recaptchaResponse = session.getQueryString();
        boolean success = ReCaptchaVerifier.verifySuccess(recaptchaResponse);
        if (!success) {
            String reason = "reCAPTCHA verification failed";
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            throw new IOException(reason);
        }
    }

    @OnMessage
    public void onMessage(Session session, ChatMessage chatMessage) {
        SendTextMessagePayload payload = chatMessage.getSendTextMessagePayload();
        if (payload != null) {
            String nickname = getNickname(session);
            switch (payload.getType()) {
                case CHAT:
                    if (nickname != null) {
                        broadcastTextMessage(nickname, payload.getContent());
                    }
                    break;
                case JOIN:
                    if (nickname == null) {
                        nickname = payload.getNickname();
                        if (sessions.containsKey(nickname)) {
                            duplicatedUser(session, nickname);
                            return;
                        }
                        setNickname(session, nickname);
                        sessions.put(nickname, session);
                        welcomeUser(session, nickname);
                        broadcastUserConnected(session, nickname);
                        broadcastAvailableUsers();
                    }
                    break;
                case LEAVE:
                    if (nickname != null) {
                        leaveUser(nickname);
                    }
                    break;
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        String nickname = getNickname(session);
        if (nickname != null) {
            leaveUser(nickname);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Error in websocket session: " + session.getId(), error);
        try {
            String nickname = getNickname(session);
            if (nickname != null) {
                leaveUser(nickname);
            }
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, null));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getNickname(Session session) {
        if (session.getUserProperties().get("nickname") != null) {
            return session.getUserProperties().get("nickname").toString();
        } else {
            return null;
        }
    }

    private void setNickname(Session session, String nickname) {
        session.getUserProperties().put("nickname", nickname);
    }

    private void welcomeUser(Session session, String nickname) {
        WelcomeUserPayload payload = new WelcomeUserPayload();
        payload.setNickname(nickname);
        session.getAsyncRemote().sendObject(new ChatMessage(payload));
    }

    private void duplicatedUser(Session session, String nickname) {
        DuplicatedUserPayload payload = new DuplicatedUserPayload();
        payload.setNickname(nickname);
        session.getAsyncRemote().sendObject(new ChatMessage(payload));
    }

    private void leaveUser(String nickname) {
        sessions.remove(nickname);
        broadcastUserDisconnected(nickname);
        broadcastAvailableUsers();
    }

    private void broadcastUserConnected(Session session, String nickname) {
        BroadcastConnectedUserPayload payload = new BroadcastConnectedUserPayload();
        payload.setNickname(nickname);
        broadcast(session, new ChatMessage(payload));
    }

    private void broadcastUserDisconnected(String nickname) {
        BroadcastDisconnectedUserPayload payload = new BroadcastDisconnectedUserPayload();
        payload.setNickname(nickname);
        broadcast(new ChatMessage(payload));
    }

    private void broadcastTextMessage(String nickname, String text) {
        BroadcastTextMessagePayload payload = new BroadcastTextMessagePayload();
        payload.setContent(text);
        payload.setNickname(nickname);
        broadcast(new ChatMessage(payload));
    }

    private void broadcastAvailableUsers() {
        BroadcastAvailableUsersPayload payload = new BroadcastAvailableUsersPayload();
        payload.setNicknames(sessions.keySet());
        broadcast(new ChatMessage(payload));
    }

    private void broadcast(ChatMessage message) {
        synchronized (sessions) {
            for (Session session : sessions.values()) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendObject(message);
                }
            }
        }
    }

    private void broadcast(Session ignoredSession, ChatMessage message) {
        synchronized (sessions) {
            for (Session session : sessions.values()) {
                if (session.isOpen() && !session.getId().equals(ignoredSession.getId())) {
                    session.getAsyncRemote().sendObject(message);
                }
            }
        }
    }

}
