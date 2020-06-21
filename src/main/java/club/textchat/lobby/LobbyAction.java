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
package club.textchat.lobby;

import club.textchat.chat.ChatAction;
import club.textchat.room.PublicRoomManager;
import club.textchat.room.RoomInfo;
import club.textchat.user.LoginRequiredException;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LobbyAction {

    private static final String LOBBY_CHATROOM_ID = "0";

    private final UserManager userManager;

    private final PublicRoomManager publicRoomManager;

    @Autowired
    public LobbyAction(UserManager userManager,
                       PublicRoomManager publicRoomManager) {
        this.userManager = userManager;
        this.publicRoomManager = publicRoomManager;
    }

    @Request("/")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> lobby() {
        List<RoomInfo> rooms = publicRoomManager.getRoomList();

        UserInfo userInfo = null;
        try {
            userInfo = userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            // ignore
        }

        String token = null;
        if (userInfo != null) {
            token = ChatAction.createAdmissionToken(LOBBY_CHATROOM_ID, userInfo);
        }

        Map<String, Object> map = new HashMap<>();
        if (token != null) {
            map.put("token", token);
        }
        map.put("rooms", rooms);
        map.put("roomId", LOBBY_CHATROOM_ID);
        map.put("include", "pages/lobby");
        return map;
    }

    @Request("/lobby/rooms")
    @Transform(FormatType.JSON)
    public List<RoomInfo> getPopularPublicChatRooms() throws LoginRequiredException {
        userManager.checkSignedIn();
        return publicRoomManager.getRoomList();
    }

}
