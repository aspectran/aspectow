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
package club.textchat.server;

import club.textchat.server.codec.ChatMessageDecoder;
import club.textchat.server.codec.ChatMessageEncoder;
import club.textchat.server.message.ChatMessage;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * WebSocket endpoint for the public chat server.
 *
 * <p>Created: 29/09/2019</p>
 */
@Component
@ServerEndpoint(
        value = "/chat/default/{token}",
        encoders = ChatMessageEncoder.class,
        decoders = ChatMessageDecoder.class,
        configurator = ChatServerConfigurator.class
)
public class DefaultChatServer {

    private final DefaultChatHandler chatHandler;

    @Autowired
    public DefaultChatServer(DefaultChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @OnOpen
    public void onOpen(@PathParam("token") String encryptedToken,
                       Session session, EndpointConfig config) throws IOException {
        chatHandler.open(encryptedToken, session, config);
    }

    @OnMessage
    public void onMessage(Session session, ChatMessage chatMessage) {
        chatHandler.handle(session, chatMessage);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        chatHandler.close(session, reason);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        chatHandler.error(session, error);
    }

}
