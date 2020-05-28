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

import club.textchat.common.mybatis.SimpleSqlSession;
import club.textchat.room.PrivateRoomManager;
import club.textchat.room.PublicRoomManager;
import club.textchat.room.RoomInfo;
import club.textchat.server.AdmissionToken;
import club.textchat.user.LoginRequiredException;
import club.textchat.user.UserAction;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.PBEncryptionUtils;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;
import org.apache.ibatis.session.SqlSession;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChatAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    public static final String RANDOM_CHATROOM_ID = "-1";

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
    public Map<String, String> startRandomChat() {
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
        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setUserNo(userInfo.getUserNo());
        admissionToken.setUsername(userInfo.getUsername());
        admissionToken.setRoomId(RANDOM_CHATROOM_ID);
        return TimeLimitedPBTokenIssuer.getToken(admissionToken);
    }

    @Request("/rooms/${roomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startPublicChat(@Required String roomId) {
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
    public Map<String, String> startPrivateChat(@Required String encryptedRoomId) {
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

    private String createAdmissionToken(String roomId, UserInfo userInfo) {
        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setUserNo(userInfo.getUserNo());
        admissionToken.setUsername(userInfo.getUsername());
        admissionToken.setRoomId(roomId);
        return TimeLimitedPBTokenIssuer.getToken(admissionToken);
    }

}
