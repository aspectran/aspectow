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
package club.textchat.user;

import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.lang.NonNull;

@Component
@Bean("userManager")
@AvoidAdvice
public class UserManager extends InstantActivitySupport {

    /**
     * The key used to store the UserInfo
     */
    public static final String USER_INFO_SESSION_KEY = "user";

    public static final String PREV_USERNAME = "-prev-uname-";

    public static final String EXPIRED_TIME = "-expired-tm-";

    public void saveUserInfo(UserInfo userInfo) {
        SessionAdapter sessionAdapter = getSessionAdapter();
        String prevUsername = sessionAdapter.getAttribute(PREV_USERNAME);
        if (prevUsername != null && !prevUsername.equals(userInfo.getUsername())) {
            userInfo.setPrevUsername(prevUsername);
        }
        sessionAdapter.setAttribute(USER_INFO_SESSION_KEY, userInfo);
        sessionAdapter.setAttribute(PREV_USERNAME, userInfo.getUsername());
    }

    public UserInfo removeUserInfo() {
        SessionAdapter sessionAdapter = getSessionAdapter();
        UserInfo userInfo = sessionAdapter.getAttribute(USER_INFO_SESSION_KEY);
        if (userInfo != null) {
            sessionAdapter.removeAttribute(USER_INFO_SESSION_KEY);
            sessionAdapter.setAttribute(PREV_USERNAME, userInfo.getUsername());
            sessionAdapter.setAttribute(EXPIRED_TIME, System.currentTimeMillis());
        }
        return userInfo;
    }

    public void checkSignedIn() throws LoginRequiredException {
        getUserInfo();
    }

    public UserInfo getUserInfoOnPage() throws LoginRequiredException {
        try {
            return getUserInfo();
        } catch (LoginRequiredException e) {
            Translet translet = getCurrentActivity().getTranslet();
            if (translet == null) {
                throw e;
            }
            translet.redirect("/");
            return null;
        }
    }

    @NonNull
    public UserInfo getUserInfo() throws LoginRequiredException {
        try {
            UserInfo userInfo = getSessionAdapter().getAttribute(USER_INFO_SESSION_KEY);
            if (userInfo == null) {
                throw new LoginRequiredException();
            }
            return userInfo;
        } catch (ClassCastException e) {
            // Exception that can occur if the UserInfo class changes during development.
            getSessionAdapter().invalidate();
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public String getSessionId() {
        return getSessionAdapter().getId();
    }

    private SessionAdapter getSessionAdapter() {
        SessionAdapter sessionAdapter = getCurrentActivity().getSessionAdapter();
        if (sessionAdapter == null) {
            throw new UnsupportedOperationException("No such SessionAdapter in " +
                    getCurrentActivity());
        }
        return sessionAdapter;
    }

}
