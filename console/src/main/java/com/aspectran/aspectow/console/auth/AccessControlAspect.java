/*
 * Copyright (c) 2026-present The Aspectran Project
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

import com.aspectran.core.activity.HintParameters;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.web.support.http.MediaType;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.util.WebUtils;
import org.jspecify.annotations.NonNull;

/**
 * Aspect that checks if a user has sufficient roles/permissions to access the requested resource.
 *
 * <p>Created: 2026/06/25</p>
 */
@Component
@Aspect(
        id = "AccessControlAspect",
        order = 2
)
@Joinpoint(
        pointcut = {
                "+: /**",
                "-: /auth/**",
                "-: /"
        }
)
public class AccessControlAspect {

    @Before
    public void checkPermission(@NonNull Translet translet) {
        SessionAdapter sessionAdapter = translet.getSessionAdapter();
        UserInfo userInfo = sessionAdapter.getAttribute(UserInfo.USERINFO_KEY);
        if (userInfo == null) {
            return;
        }

        String requestName = translet.getRequestName();
        boolean hasAccess = true;

        if (requestName.startsWith("/user")) {
            hasAccess = (userInfo.hasPermission("USER_MANAGE") || userInfo.hasRole("SUPER_ADMIN"));
        } else if (requestName.startsWith("/cluster") ||
                requestName.startsWith("/scheduler") ||
                requestName.startsWith("/commands") ||
                requestName.startsWith("/vault")) {
            hasAccess = (userInfo.hasRole("SUPER_ADMIN") || userInfo.hasRole("ADMIN"));
        } else if (requestName.startsWith("/appmon")) {
            hasAccess = (userInfo.hasPermission("MONITOR_VIEW") ||
                    userInfo.hasRole("SUPER_ADMIN") ||
                    userInfo.hasRole("ADMIN"));
        } else if (requestName.startsWith("/framework")) {
            hasAccess = (userInfo.hasRole("SUPER_ADMIN") ||
                    userInfo.hasRole("ADMIN") ||
                    userInfo.hasRole("VIEWER"));
        }

        if (!hasAccess) {
            accessDenied(translet);
        }
    }

    private void accessDenied(@NonNull Translet translet) {
        HintParameters hint = translet.peekHint("layout");
        if (hint != null && "popup".equals(hint.getString("layout"))) {
            translet.transform(new FailureResponse().forbidden());
            return;
        }
        if (WebUtils.isAcceptContentTypes(translet, MediaType.TEXT_HTML)) {
            translet.redirect("/");
        } else {
            translet.transform(new FailureResponse().forbidden());
        }
    }

}
