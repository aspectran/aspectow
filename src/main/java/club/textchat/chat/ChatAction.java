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
package club.textchat.chat;

import club.textchat.room.PrivateRoomManager;
import club.textchat.room.PublicRoomManager;
import club.textchat.room.RoomInfo;
import club.textchat.server.AdmissionToken;
import club.textchat.server.ExchangeChatHandler;
import club.textchat.server.PrivateChatHandler;
import club.textchat.user.ChaterManager;
import club.textchat.user.LoginRequiredException;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.PBEncryptionUtils;
import com.aspectran.core.util.StringUtils;
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChatAction {

    public static final String RANDOM_CHATROOM_ID = "-1";

    public static final String STRANGER_CHATROOM_ID = "-2";

    public static final String EXCHANGE_CHATROOM_ID = "-3";

    private final UserManager userManager;

    private final PublicRoomManager publicRoomManager;

    private final PrivateRoomManager privateRoomManager;

    private final ChaterManager chaterManager;

    @Autowired
    public ChatAction(UserManager userManager,
                      PublicRoomManager publicRoomManager,
                      PrivateRoomManager privateRoomManager,
                      ChaterManager chaterManager) {
        this.userManager = userManager;
        this.publicRoomManager = publicRoomManager;
        this.privateRoomManager = privateRoomManager;
        this.chaterManager = chaterManager;
    }

    @Request("/random")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> randomChat(Translet translet) {
        Map<String, String> map = new HashMap<>();
        map.put("roomId", RANDOM_CHATROOM_ID);
        map.put("title", translet.getMessage("service.random_chat"));
        map.put("include", "pages/random");
        return map;
    }

    @Request("/random/request")
    @Transform(FormatType.JSON)
    public Map<String, Object> requestRandomChat() {
        int error = 0;

        UserInfo userInfo = null;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            error = -1;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("error", error);
        if (error == 0) {
            map.put("token", createAdmissionToken(RANDOM_CHATROOM_ID, userInfo));
            map.put("usersByCountry", chaterManager.getUsersByCountry());
        }
        return map;
    }

    @Request("/strangers")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> strangerChat(Translet translet) {
        UserInfo userInfo = null;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            // ignore
        }

        String token = null;
        if (userInfo != null) {
            token = ChatAction.createAdmissionToken(STRANGER_CHATROOM_ID, userInfo);
        }

        Map<String, Object> map = new HashMap<>();
        if (token != null) {
            map.put("token", token);
        }
        map.put("roomId", STRANGER_CHATROOM_ID);
        map.put("title", translet.getMessage("service.stranger_chat"));
        map.put("include", "pages/strangers");
        return map;
    }

    @Request("/strangers/${encryptedRoomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startStrangerChat(Translet translet, String encryptedRoomId) {
        if (StringUtils.isEmpty(encryptedRoomId)) {
            translet.redirect("/");
            return null;
        }

        String roomId;
        try {
            roomId = PBEncryptionUtils.decrypt(encryptedRoomId);
        } catch (Exception e) {
            throw new InvalidChatRoomException(encryptedRoomId, "room-not-found");
        }

        UserInfo userInfo;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            translet.redirect("/");
            return null;
        }

        String roomName = translet.getMessage("service.stranger_chat");

        Map<String, String> map = new HashMap<>();
        map.put("roomId", roomId);
        map.put("roomName", roomName);
        map.put("token", createAdmissionToken(roomId, userInfo));
        map.put("title", roomName);
        map.put("include", "pages/private");
        map.put("homepage", "/strangers");
        return map;
    }

    @Request("/exchange")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> exchangeChat(Translet translet) {
        Map<String, Object> map = new HashMap<>();
        map.put("roomId", EXCHANGE_CHATROOM_ID);
        map.put("title", translet.getMessage("service.exchange_chat"));
        map.put("include", "pages/exchange");
        return map;
    }

    @Request("/exchange/request")
    @Transform(FormatType.JSON)
    public Map<String, Object> requestExchangeChat(@Qualifier("native_lang") String nativeLang,
                                                   @Qualifier("convo_lang") String convoLang) {
        int error = 0;

        UserInfo userInfo = null;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            error = -1;
        }

        if (error == 0) {
            if (StringUtils.isEmpty(nativeLang) || StringUtils.isEmpty(convoLang)) {
                error = -2;
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("error", error);
        if (error == 0) {
            String roomId = ExchangeChatHandler.makeExchangeRoomId(nativeLang, convoLang);
            map.put("token", createAdmissionToken(roomId, userInfo));
            map.put("usersByCountry", chaterManager.getUsersByCountry());
        }
        return map;
    }

    @Request("/exchange/${encryptedRoomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startExchangeChat(Translet translet, String encryptedRoomId) {
        if (StringUtils.isEmpty(encryptedRoomId)) {
            translet.redirect("/");
            return null;
        }

        String roomId;
        try {
            roomId = PBEncryptionUtils.decrypt(encryptedRoomId);
        } catch (Exception e) {
            throw new InvalidChatRoomException(encryptedRoomId, "room-not-found");
        }

        UserInfo userInfo;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            translet.redirect("/");
            return null;
        }

        String roomName = translet.getMessage("service.exchange_chat");

        Map<String, String> map = new HashMap<>();
        map.put("roomId", EXCHANGE_CHATROOM_ID);
        map.put("roomName", roomName);
        map.put("token", createAdmissionToken(roomId, userInfo));
        map.put("title", roomName);
        map.put("include", "pages/private");
        map.put("homepage", "/exchange");
        return map;
    }

    @Request("/private/${encryptedRoomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startPrivateChat(Translet translet, String encryptedRoomId) {
        if (StringUtils.isEmpty(encryptedRoomId)) {
            translet.redirect("/");
            return null;
        }

        String roomId;
        try {
            roomId = PBEncryptionUtils.decrypt(encryptedRoomId);
        } catch (Exception e) {
            throw new InvalidChatRoomException(encryptedRoomId, "room-not-found");
        }

        RoomInfo roomInfo = privateRoomManager.getRoomInfo(roomId);
        if (roomInfo == null) {
            throw new InvalidChatRoomException(encryptedRoomId, "room-not-found");
        }

        UserInfo userInfo = null;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            // ignore
        }

        String token = null;
        if (userInfo != null) {
            String privateRoomId = PrivateChatHandler.makePrivateRoomId(roomId);
            token = createAdmissionToken(privateRoomId, userInfo);
        }

        Map<String, String> map = new HashMap<>();
        map.put("roomId", Integer.toString(roomInfo.getRoomId()));
        map.put("roomName", roomInfo.getRoomName());
        if (token != null) {
            map.put("token", token);
        }
        map.put("title", roomInfo.getRoomName());
        map.put("include", "pages/private");
        return map;
    }

    @Request("/rooms/${roomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startPublicChat(Translet translet, String roomId) {
        if (StringUtils.isEmpty(roomId)) {
            translet.redirect("/");
            return null;
        }

        RoomInfo roomInfo = publicRoomManager.getRoomInfo(roomId);
        if (roomInfo == null) {
            throw new InvalidChatRoomException(roomId, "room-not-found");
        }

        UserInfo userInfo = null;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            // ignore
        }

        String token = null;
        if (userInfo != null) {
            token = createAdmissionToken(roomId, userInfo);
        }

        Map<String, String> map = new HashMap<>();
        map.put("roomId", Integer.toString(roomInfo.getRoomId()));
        map.put("roomName", roomInfo.getRoomName());
        if (token != null) {
            map.put("token", token);
        }
        map.put("title", roomInfo.getRoomName());
        map.put("include", "pages/public");
        return map;
    }

    public static String createAdmissionToken(String roomId, UserInfo userInfo) {
        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setRoomId(roomId);
        admissionToken.setUserNo(userInfo.getUserNo());
        admissionToken.setUsername(userInfo.getUsername());
        return TimeLimitedPBTokenIssuer.getToken(admissionToken);
    }

}
