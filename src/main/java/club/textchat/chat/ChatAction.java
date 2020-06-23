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
import club.textchat.user.LoginRequiredException;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
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

    private final UserManager userManager;

    private final PublicRoomManager publicRoomManager;

    private final PrivateRoomManager privateRoomManager;

    @Autowired
    public ChatAction(UserManager userManager,
                      PublicRoomManager publicRoomManager,
                      PrivateRoomManager privateRoomManager) {
        this.userManager = userManager;
        this.publicRoomManager = publicRoomManager;
        this.privateRoomManager = privateRoomManager;
    }

    @Request("/random")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> randomChat() {
        Map<String, String> map = new HashMap<>();
        map.put("roomId", RANDOM_CHATROOM_ID);
        map.put("include", "pages/random");
        return map;
    }

    @Request("/random/token")
    @Transform(FormatType.JSON)
    public String randomChatToken() {
        UserInfo userInfo;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            return "-1";
        }
        return createAdmissionToken(RANDOM_CHATROOM_ID, userInfo);
    }

    @Request("/strangers")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> strangerChat() {
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
            token = createAdmissionToken(roomId, userInfo);
        }

        Map<String, String> map = new HashMap<>();
        map.put("roomId", roomInfo.getEncryptedRoomId());
        map.put("roomName", roomInfo.getRoomName());
        if (token != null) {
            map.put("token", token);
        }
        map.put("title", roomInfo.getRoomName());
        map.put("include", "pages/private");
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
