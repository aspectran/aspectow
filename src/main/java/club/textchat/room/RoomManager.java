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
package club.textchat.room;

import club.textchat.common.mybatis.SimpleSqlSession;
import club.textchat.redis.persistence.LobbyConvoPersistence;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.BroadcastPayload;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.PBEncryptionUtils;
import com.aspectran.core.util.json.JsonWriter;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.util.List;

/**
 * <p>Created: 2020/05/18</p>
 */
@Component
public class RoomManager {

    private static final String NEW_ROOM_MESSAGE_PREFIX = "newRoom:";

    private final SqlSession sqlSession;

    private final LobbyConvoPersistence lobbyConvoPersistence;

    @Autowired
    public RoomManager(SimpleSqlSession sqlSession, LobbyConvoPersistence lobbyConvoPersistence) {
        this.sqlSession = sqlSession;
        this.lobbyConvoPersistence = lobbyConvoPersistence;
    }

    public String createRoom(RoomInfo roomInfo) throws IOException {
        Integer count = sqlSession.selectOne("rooms.getRoomCountByName", roomInfo.getRoomName());
        if (count != null && count > 0) {
            return null;
        }

        sqlSession.insert("rooms.insertRoom", roomInfo);

        String encryptedRoomId = PBEncryptionUtils.encrypt(Integer.toString(roomInfo.getRoomId()));
        roomInfo.setRoomId(0);
        roomInfo.setEncryptedRoomId(encryptedRoomId);

        String json = new JsonWriter().prettyPrint(false).nullWritable(false).write(roomInfo).toString();
        lobbyConvoPersistence.publish(NEW_ROOM_MESSAGE_PREFIX + json);
        lobbyConvoPersistence.say("\"" + roomInfo.getRoomName() + "\" chatroom was created.");

        return encryptedRoomId;
    }

    public List<RoomInfo> getRoomList() {
        List<RoomInfo> list = sqlSession.selectList("rooms.getRoomList");
        for (RoomInfo roomInfo : list) {
            roomInfo.setEncryptedRoomId(PBEncryptionUtils.encrypt(Integer.toString(roomInfo.getRoomId())));
        }
        return list;
    }

    public void checkIn(String roomId) {
        sqlSession.update("rooms.updateRoomForCheckIn", roomId);
    }

    public void checkOut(String roomId) {
        sqlSession.update("rooms.updateRoomForCheckOut", roomId);
    }

}
