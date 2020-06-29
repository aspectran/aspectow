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

import com.aspectran.core.util.ToStringBuilder;
import com.aspectran.core.util.json.JsonWriter;

import java.io.IOException;
import java.io.Serializable;

public class RoomInfo implements Serializable {

    private static final long serialVersionUID = 5289914964536222900L;

    private int roomId;

    private String roomName;

    private String language;

    private int userNo;

    private int cumulativeUsers;

    private int currentUsers;

    private int pastDays;

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getUserNo() {
        return userNo;
    }

    public void setUserNo(int userNo) {
        this.userNo = userNo;
    }

    public int getCumulativeUsers() {
        return cumulativeUsers;
    }

    public void setCumulativeUsers(int cumulativeUsers) {
        this.cumulativeUsers = cumulativeUsers;
    }

    public int getCurrentUsers() {
        return currentUsers;
    }

    public void setCurrentUsers(int currentUsers) {
        this.currentUsers = currentUsers;
    }

    public int getPastDays() {
        return pastDays;
    }

    public void setPastDays(int pastDays) {
        this.pastDays = pastDays;
    }

    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("roomId", roomId);
        tsb.append("roomName", roomName);
        tsb.append("language", language);
        tsb.append("cumulativeUsers", cumulativeUsers);
        tsb.append("currentUsers", currentUsers);
        tsb.append("pastDays", pastDays);
        return tsb.toString();
    }

    public String serialize() {
        try {
            JsonWriter writer = new JsonWriter().prettyPrint(false).nullWritable(false);
            writer.beginObject();
            writer.writeName("roomId").writeValue(getRoomId());
            writer.writeName("roomName").writeValue(getRoomName());
            writer.writeName("language").writeValue(getLanguage());
            writer.writeName("currentUsers").writeValue(getCurrentUsers());
            writer.endObject();
            return writer.toString();
        } catch (IOException e) {
            return "";
        }
    }

}
