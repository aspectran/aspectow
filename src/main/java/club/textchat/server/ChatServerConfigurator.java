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

import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.web.socket.jsr356.AspectranConfigurator;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

public class ChatServerConfigurator extends AspectranConfigurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {
        super.modifyHandshake(config, request, response);
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        if (httpSession != null) {
            UserInfo userInfo = (UserInfo)httpSession.getAttribute(UserManager.USER_INFO_SESSION_KEY);
            if (userInfo != null) {
                ChaterInfo chaterInfo = new ChaterInfo(userInfo);
                chaterInfo.setHttpSessionId(httpSession.getId());
                Map<String, List<String>> parameterMap = request.getParameterMap();
                String nativeLang = getParameter(parameterMap, "native_lang");
                if (nativeLang != null) {
                    chaterInfo.setNativeLang(nativeLang);
                }
                String convoLang = getParameter(parameterMap, "convo_lang");
                if (convoLang != null) {
                    chaterInfo.setConvoLang(convoLang);
                }
                config.getUserProperties().put(ChaterInfo.CHATER_INFO_PROP, chaterInfo);
            }
        }
    }

    private String getParameter(Map<String, List<String>> parameterMap, String name) {
        List<String> params = parameterMap.get(name);
        if (params != null && !params.isEmpty()) {
            return params.get(0);
        } else {
            return null;
        }
    }

}
