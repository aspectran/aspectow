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
import club.textchat.room.RoomInfo;
import club.textchat.server.AdmissionToken;
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
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;
import org.apache.ibatis.session.SqlSession;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChatAction {

    public static final String RANDOM_CHATROOM_ID = "-1";

    private final UserManager userManager;

    private final SqlSession sqlSession;

    @Autowired
    public ChatAction(UserManager userManager,
                      SimpleSqlSession sqlSession) {
        this.userManager = userManager;
        this.sqlSession = sqlSession;
    }

    @Request("/rooms/random")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startRandomChat() {
        Map<String, String> map = new HashMap<>();
        map.put("title", "Random Chat");
        map.put("include", "pages/chat-random");
        return map;
    }

    @Request("/rooms/${roomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startPublicChat(@Required String roomId) {
        try {
            roomId = PBEncryptionUtils.decrypt(roomId);
        } catch (Exception e) {
            throw new InvalidChatRoomException(roomId, "invalid-room-id");
        }

        UserInfo userInfo = userManager.getUserInfo();
        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setUserNo(userInfo.getUserNo());
        admissionToken.setUsername(userInfo.getUsername());
        admissionToken.setRoomId(roomId);

        Map<String, String> map = new HashMap<>();
        RoomInfo roomInfo = sqlSession.selectOne("rooms.getRoomInfo", roomId);
        map.put("roomName", roomInfo.getRoomName());
        map.put("token", TimeLimitedPBTokenIssuer.getToken(admissionToken));
        map.put("title", roomInfo.getRoomName());
        map.put("include", "pages/chat-default");
        return map;
    }

    @Request("/rooms/random/token")
    @Transform(FormatType.JSON)
    public String randomChatToken() {
        UserInfo userInfo = userManager.getUserInfo();
        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setUserNo(userInfo.getUserNo());
        admissionToken.setUsername(userInfo.getUsername());
        admissionToken.setRoomId(RANDOM_CHATROOM_ID);
        return TimeLimitedPBTokenIssuer.getToken(admissionToken);
    }

}