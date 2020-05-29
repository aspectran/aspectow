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

import club.textchat.recaptcha.ReCaptchaVerifier;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.StringUtils;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

@Component
public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    private final UserManager userManager;

    private final ChaterManager chaterManager;

    @Autowired
    public UserAction(UserManager userManager,
                      ChaterManager chaterManager) {
        this.userManager = userManager;
        this.chaterManager = chaterManager;
    }

    @RequestToPost("/guest/signin")
    @Transform(FormatType.JSON)
    public String signIn(Translet translet,
                         @Required String username,
                         String favoriteColor,
                         @Required String recaptchaResponse,
                         String timeZone) {
        username = UsernameUtils.normalize(username);

        boolean success = false;
        try {
            success = ReCaptchaVerifier.verifySuccess(recaptchaResponse);
        } catch (IOException e) {
            logger.warn("reCAPTCHA verification failed", e);
        }
        if (!success) {
            return "-1";
        }

        if (chaterManager.isInUseUsername(username)) {
            return "-2";
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        if (!StringUtils.isEmpty(favoriteColor)) {
            userInfo.setColor(favoriteColor);
        }

        Locale locale = translet.getRequestAdapter().getLocale();
        if (locale != null) {
            userInfo.setCountry(locale.getCountry());
            userInfo.setLanguage(locale.getLanguage());
        }

        userInfo.setTimeZone(timeZone);

        String remoteAddr = translet.getRequestAdapter().getHeader("X-FORWARDED-FOR");
        if (!StringUtils.isEmpty(remoteAddr)) {
            userInfo.setIpAddr(remoteAddr);
        } else {
            userInfo.setIpAddr(((HttpServletRequest)translet.getRequestAdaptee()).getRemoteAddr());
        }

        if (!chaterManager.createChater(userInfo)) {
            return "-9";
        }

        userManager.saveUserInfo(userInfo);
        return "0";
    }

    @Request("/signout")
    @Redirect("/")
    public void signOut() {
        userManager.removeUserInfo();
    }

}
