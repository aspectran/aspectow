/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.console.auth;

import com.aspectran.aspectow.appmon.common.support.IPCountryResolver;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.core.activity.HintParameters;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.adapter.WebRequestAdapter;
import com.aspectran.web.support.http.HttpHeaders;
import com.aspectran.web.support.http.MediaType;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.util.WebUtils;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Optional;

/**
 * Aspect that checks if a user is authenticated.
 *
 * <p>Created: 2020/09/25</p>
 */
@Component
@Profile("[console.ui, console.custom-ui]")
@Aspect(
        id = "userAuthAspect",
        order = 1
)
@Joinpoint(
        pointcut = {
                "+: /**",
                "-: /auth/**",
                "-: /nodes/**/ping",
                "-: /nodes/**/polling/**"
        }
)
public class UserAuthAspect {

    private final UserService userService;
    private final LoginActivity loginActivity;
    private final RememberMeService rememberMeService;
    private final IPCountryResolver ipCountryResolver;

    @Autowired
    public UserAuthAspect(UserService userService,
                          LoginActivity loginActivity,
                          Optional<RememberMeService> rememberMeService,
                          Optional<IPCountryResolver> ipCountryResolver) {
        this.userService = userService;
        this.loginActivity = loginActivity;
        this.rememberMeService = rememberMeService.orElse(null);
        this.ipCountryResolver = ipCountryResolver.orElse(null);
    }

    @Before
    public void checkAuthenticated(@NonNull Translet translet) {
        SessionAdapter sessionAdapter = translet.getSessionAdapter();
        UserInfo userInfo = sessionAdapter.getAttribute(UserInfo.USERINFO_KEY);
        if (userInfo == null) {
            if (tryAutoLogin(translet)) {
                return;
            }
            authRequired(translet);
        }
    }

    private boolean tryAutoLogin(@NonNull Translet translet) {
        if (rememberMeService != null && rememberMeService.isRememberMeEnabled()) {
            String username = rememberMeService.autoLogin(translet);
            if (username != null) {
                User user = userService.getUserByUsername(username);
                if (user != null && "NORMAL".equals(user.getStatus())) {
                    loginActivity.doLogin(translet, user);
                    String remoteAddr = WebUtils.getRemoteAddr(translet);
                    String countryCode = null;
                    if (ipCountryResolver != null && remoteAddr != null) {
                        countryCode = ipCountryResolver.resolveCountryCode(remoteAddr, translet.getRequestAdapter().getLocale());
                    }
                    String userAgent = translet.getRequestAdapter().getHeader(HttpHeaders.USER_AGENT);
                    userService.recordLogin(username, remoteAddr, countryCode, userAgent, true);
                    return true;
                }
            }
        }
        return false;
    }

    private void authRequired(@NonNull Translet translet) {
        HintParameters hint = translet.peekHint("layout");
        if (hint != null && "popup".equals(hint.getString("layout"))) {
            translet.transform(new FailureResponse().forbidden());
            return;
        }
        if (WebUtils.isAcceptContentTypes(translet, MediaType.TEXT_HTML)) {
            String target = translet.getRequestName();
            if (translet.getRequestAdapter() instanceof WebRequestAdapter webRequestAdapter) {
                String queryString = webRequestAdapter.getQueryString();
                if (StringUtils.hasText(queryString)) {
                    target += "?" + queryString;
                }
            }
            translet.redirect("/auth/login", Map.of("referrer", target));
        } else {
            translet.transform(new FailureResponse().forbidden());
        }
    }

}
