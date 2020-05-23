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

import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.ParameterKey;
import com.aspectran.core.util.apon.ValueType;

public class AdmissionToken extends AbstractParameters {

    private static final ParameterKey userNo;
    private static final ParameterKey username;
    private static final ParameterKey roomId;

    private static final ParameterKey[] parameterKeys;

    static {
        userNo = new ParameterKey("userNo", ValueType.INT);
        username = new ParameterKey("username", ValueType.STRING);
        roomId = new ParameterKey("roomId", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                userNo,
                username,
                roomId
        };
    }

    public AdmissionToken() {
        super(parameterKeys);
    }

    public int getUserNo() {
        return getInt(userNo);
    }

    public void setUserNo(int userNo) {
        putValue(AdmissionToken.userNo, userNo);
    }

    public String getUsername() {
        return getString(AdmissionToken.username);
    }

    public void setUsername(String username) {
        putValue(AdmissionToken.username, username);
    }

    public String getRoomId() {
        return getString(AdmissionToken.roomId);
    }

    public void setRoomId(String roomId) {
        putValue(AdmissionToken.roomId, roomId);
    }

}
