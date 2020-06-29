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
import club.textchat.util.CountryCodeLookup;
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
import java.util.Enumeration;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    private static final Pattern SPACE_CONDENSATION_PATTERN = Pattern.compile("\\s{2,}");

    private final UserManager userManager;

    private final ChaterManager chaterManager;

    @Autowired
    public UserAction(UserManager userManager,
                      ChaterManager chaterManager) {
        this.userManager = userManager;
        this.chaterManager = chaterManager;
    }

    @RequestToPost("/signin")
    @Transform(FormatType.JSON)
    public String signIn(Translet translet,
                         @Required String recaptchaResponse,
                         @Required String username,
                         String description,
                         String favoriteColor,
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

        if (chaterManager.isInConvoUser()) {
            return "-3";
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        if (description != null) {
            description = SPACE_CONDENSATION_PATTERN.matcher(description).replaceAll(" ").trim();
            if (!description.isEmpty()) {
                if (description.length() > 256) {
                    description = description.substring(0, 256);
                }
                userInfo.setDescription(description);
            }
        }
        if (!StringUtils.isEmpty(favoriteColor)) {
            userInfo.setColor(favoriteColor);
        }

        String remoteAddr = translet.getRequestAdapter().getHeader("X-FORWARDED-FOR");
        if (!StringUtils.isEmpty(remoteAddr)) {
            if (remoteAddr.contains(",")) {
                remoteAddr = StringUtils.tokenize(remoteAddr, ",", true)[0];
            }
            userInfo.setIpAddr(remoteAddr);
        } else {
            userInfo.setIpAddr(((HttpServletRequest)translet.getRequestAdaptee()).getRemoteAddr());
        }

        String countryCode = CountryCodeLookup.getInstance().getCountryCodeByIP(userInfo.getIpAddr());
        userInfo.setCountry(countryCode);

        HttpServletRequest request = translet.getRequestAdapter().getAdaptee();
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = locales.nextElement();
            if (userInfo.getCountry() == null && !locale.getCountry().isEmpty()) {
                userInfo.setCountry(locale.getCountry());
            }
            if (userInfo.getLanguage() == null && !locale.getLanguage().isEmpty()) {
                userInfo.setLanguage(locale.getLanguage());
            }
        }

        if (StringUtils.isEmpty(userInfo.getLanguage())) {
            Locale locale = translet.getRequestAdapter().getLocale();
            if (locale != null) {
                userInfo.setLanguage(locale.getLanguage());
            }
        }

        userInfo.setTimeZone(timeZone);

        userManager.saveUserInfo(userInfo);
        return "0";
    }

    @Request("/signout")
    @Redirect("/")
    public void signOut() {
        userManager.removeUserInfo();
    }

}
