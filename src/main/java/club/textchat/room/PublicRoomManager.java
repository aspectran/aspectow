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
import club.textchat.redis.persistence.LobbyChatPersistence;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.json.JsonWriter;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Created: 2020/05/18</p>
 */
@Component
public class PublicRoomManager extends InstantActivitySupport {

    private static final String NEW_ROOM_MESSAGE_PREFIX = "newPublicRoom:";

    private final SqlSession sqlSession;

    private final LobbyChatPersistence lobbyChatPersistence;

    @Autowired
    public PublicRoomManager(SimpleSqlSession sqlSession, LobbyChatPersistence lobbyChatPersistence) {
        this.sqlSession = sqlSession;
        this.lobbyChatPersistence = lobbyChatPersistence;
    }

    public RoomInfo getRoomInfo(String roomId) {
        return sqlSession.selectOne("public.rooms.getRoomInfo", roomId);
    }

    public Integer createRoom(RoomInfo roomInfo) throws IOException {
        Integer count = sqlSession.selectOne("public.rooms.getRoomCountByName", roomInfo.getRoomName());
        if (count != null && count > 0) {
            return null;
        }

        sqlSession.insert("public.rooms.insertRoom", roomInfo);

        String json = new JsonWriter().prettyPrint(false).nullWritable(false).write(roomInfo).toString();
        lobbyChatPersistence.publish(NEW_ROOM_MESSAGE_PREFIX + json);

        return roomInfo.getRoomId();
    }

    public List<RoomInfo> getRoomList() {
        List<RoomInfo> list = sqlSession.selectList("public.rooms.getRoomList");
        Map<String, String> languages = getEnvironment().getProperty("languages");
        for (RoomInfo roomInfo : list) {
            roomInfo.setLanguageName(languages.get(roomInfo.getLanguage()));
        }
        return list;
    }

    public Map<String, String> getRoomLanguages() {
        List<String> list = sqlSession.selectList("public.rooms.getRoomLangList");
        Map<String, String> languages = getEnvironment().getProperty("languages");
        Map<String, String> result = new HashMap<>();
        for (String lang : list) {
            result.put(lang, languages.get(lang));
        }
        return result;
    }

    public void checkIn(String roomId) {
        sqlSession.update("public.rooms.updateRoomForCheckIn", roomId);
    }

    public void checkOut(String roomId) {
        sqlSession.update("public.rooms.updateRoomForCheckOut", roomId);
    }

}
