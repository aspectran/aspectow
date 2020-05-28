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

import club.textchat.recaptcha.ReCaptchaVerifier;
import club.textchat.user.LoginRequiredException;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Component
public class PrivateRoomAction {

    private static final Logger logger = LoggerFactory.getLogger(PrivateRoomAction.class);

    private final UserManager userManager;

    private final PrivateRoomManager privateRoomManager;

    @Autowired
    public PrivateRoomAction(UserManager userManager,
                             PrivateRoomManager privateRoomManager) {
        this.userManager = userManager;
        this.privateRoomManager = privateRoomManager;
    }

    @RequestToPost("/private")
    @Transform(FormatType.JSON)
    public String createPrivateChatroom(@Required String recaptchaResponse,
                                        @Required @Qualifier("room_nm") String roomName) throws Exception {
        try {
            ReCaptchaVerifier.verifySuccess(recaptchaResponse);
        } catch (IOException e) {
            logger.warn("reCAPTCHA verification failed", e);
            return "-1";
        }

        UserInfo userInfo = userManager.getUserInfo();

        RoomInfo roomInfo = new RoomInfo();
        roomInfo.setRoomName(roomName);
        roomInfo.setUserNo(userInfo.getUserNo());

        return privateRoomManager.createRoom(roomInfo);
    }

}
