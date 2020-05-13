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

import club.textchat.persistence.ChatersPersistence;
import club.textchat.persistence.ConvosPersistence;
import club.textchat.persistence.UsersInConvoPersistence;
import club.textchat.persistence.UsersInLobbyPersistence;
import club.textchat.server.codec.ChatMessageDecoder;
import club.textchat.server.codec.ChatMessageEncoder;
import club.textchat.server.message.ChatMessage;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import com.aspectran.core.util.security.InvalidPBTokenException;
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;

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
 * WebSocket endpoint for the chat server.
 *
 * <p>Created: 29/09/2019</p>
 */
@Component
@ServerEndpoint(
        value = "/chat/{token}",
        encoders = ChatMessageEncoder.class,
        decoders = ChatMessageDecoder.class,
        configurator = ChatServerConfigurator.class
)
public class ChatServer extends ChatHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatHandler.class);

    @Autowired
    public ChatServer(UsersInLobbyPersistence usersInLobbyPersistence,
                      UsersInConvoPersistence usersInConvoPersistence,
                      ChatersPersistence chatersPersistence,
                      ConvosPersistence convosPersistence) {
        super(usersInLobbyPersistence, usersInConvoPersistence, chatersPersistence, convosPersistence);
    }

    @OnOpen
    public void onOpen(@PathParam("token") String encryptedToken,
                       Session session, EndpointConfig config) throws IOException {
        AdmissionToken admissionToken;
        try {
            admissionToken = TimeLimitedPBTokenIssuer.getPayload(encryptedToken, AdmissionToken.class);
        } catch (InvalidPBTokenException e) {
            logger.warn(e);
            String reason = "Access denied due to invalid admission token";
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            throw new IOException(reason);
        }

        ChaterInfo chaterInfo = (ChaterInfo)config.getUserProperties().get(ChaterInfo.CHATER_INFO_PROP);
        if (chaterInfo == null || chaterInfo.getUserNo() != admissionToken.getUserNo() ||
                !chaterInfo.getUsername().equals(admissionToken.getUsername())) {
            String reason = "User authentication failed";
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            throw new IOException(reason);
        }

        chaterInfo.setRoomId(admissionToken.getRoomId());
        if (logger.isDebugEnabled()) {
            logger.debug("Created chater " + chaterInfo);
        }
    }

    @OnMessage
    public void onMessage(Session session, ChatMessage chatMessage) {
        handle(session, chatMessage);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        close(session, reason);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error(session, error);
    }

}
