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
import com.aspectran.aspectow.console.common.db.model.Permission;
import com.aspectran.aspectow.console.common.db.model.Role;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.net.IpAddressUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.http.HttpHeaders;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import com.aspectran.web.support.util.WebUtils;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handles authentication requests.
 */
@Component("/auth")
@Profile("[console.ui, console.custom-ui]")
public class LoginActivity {

    private final UserService userService;
    private final IPCountryResolver ipCountryResolver;

    @Autowired
    public LoginActivity(UserService userService, Optional<IPCountryResolver> ipCountryResolver) {
        this.userService = userService;
        this.ipCountryResolver = ipCountryResolver.orElse(null);
    }

    @Request("/login")
    @Dispatch("auth/login")
    @Action("page")
    public Map<String, String> loginPage() {
        return Map.of(
                "title", "Login",
                "layout", "popup"
        );
    }

    @RequestToPost("/login")
    public RestResponse login(Translet translet, String username, String password, String referrer) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return new FailureResponse().setError("required", "Username and password are required.");
        }

        String remoteAddr = WebUtils.getRemoteAddr(translet);
        String countryCode = null;
        if (ipCountryResolver != null && remoteAddr != null) {
            countryCode = ipCountryResolver.resolveCountryCode(remoteAddr, translet.getRequestAdapter().getLocale());
        }
        String userAgent = translet.getRequestAdapter().getHeader(HttpHeaders.USER_AGENT);

        User user = userService.getUserByUsername(username);
        if (user != null && "LOCKED".equals(user.getStatus())) {
            userService.recordLogin(username, remoteAddr, countryCode, userAgent, false);
            return new FailureResponse().setError("locked", "Account is LOCKED. Please contact administrator.");
        }

        if (user != null && userService.checkPassword(user, password)) {
            if (!IpAddressUtils.isAllowedIp(remoteAddr, user.getAllowedIps())) {
                userService.recordLogin(username, remoteAddr, countryCode, userAgent, false);
                userService.recordAuditLog(username, "LOGIN_FAILED_UNALLOWED_IP", "User: " + username,
                        "Login attempt from unallowed IP address: " + remoteAddr, remoteAddr);
                return new FailureResponse().setError("ip_denied", "Access denied. Your IP address (" +
                        remoteAddr + ") is not allowed for this account.");
            }
            if (userService.isPasswordChangeRequired(user, password)) {
                return new FailureResponse().setError("setup_required",
                        "Administrator password setup is required.");
            }
            if (!"NORMAL".equals(user.getStatus())) {
                userService.recordLogin(username, remoteAddr, countryCode, userAgent, false);
                return new FailureResponse().setError("locked", "Account is " + user.getStatus());
            }

            doLogin(translet, user);

            userService.recordLogin(username, remoteAddr, countryCode, userAgent, true);
            String redirectUrl = resolveRedirectUrl(translet, referrer);
            return new SuccessResponse("OK").setData(Map.of("redirectUrl", redirectUrl)).ok();
        } else {
            userService.recordLogin(username, remoteAddr, countryCode, userAgent, false);
            if (user != null) {
                User updatedUser = userService.getUserByUsername(username);
                if (updatedUser != null && "LOCKED".equals(updatedUser.getStatus())) {
                    return new FailureResponse().setError("locked",
                            "Account has been LOCKED due to 5 consecutive failed login attempts.");
                }
            }
            return new FailureResponse().setError("invalid", "Invalid username or password.");
        }
    }

    public void doLogin(@NonNull Translet translet, @NonNull User user) {
        SessionAdapter sessionAdapter = translet.getSessionAdapter();
        sessionAdapter.invalidate();

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setLoginIp(WebUtils.getRemoteAddr(translet));

        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                roles.add(role.getRoleName());
                if (role.getPermissions() != null) {
                    for (Permission perm : role.getPermissions()) {
                        permissions.add(perm.getPermCode());
                    }
                }
            }
        }
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        sessionAdapter.setAttribute(UserInfo.USERINFO_KEY, userInfo);
    }

    @RequestToPost("/setup-password")
    public RestResponse setupPassword(String username, String currentPassword, String newPassword) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(currentPassword) || StringUtils.isEmpty(newPassword)) {
            return new FailureResponse().setError("required", "All fields are required.");
        }
        User user = userService.getUserByUsername(username);
        if (user != null && userService.checkPassword(user, currentPassword)) {
            if (userService.isPasswordChangeRequired(user, currentPassword)) {
                user.setPassword(newPassword);
                userService.updateUser(user, null);
                return new SuccessResponse("Password updated successfully.").ok();
            }
        }
        return new FailureResponse().setError("invalid", "Invalid request.");
    }

    @Request("/logout")
    @Redirect("/")
    public void logout(@NonNull Translet translet) {
        SessionAdapter sessionAdapter = translet.getSessionAdapter();
        sessionAdapter.removeAttribute(UserInfo.USERINFO_KEY);
        sessionAdapter.invalidate();
    }

    /**
     * Resolves a safe redirect URL based on the referrer and application context path.
     * @param translet the current translet
     * @param referrer the requested redirect target URL
     * @return the resolved safe redirect URL
     */
    public static @NonNull String resolveRedirectUrl(@NonNull Translet translet, String referrer) {
        String contextPath = translet.getContextPath();
        String defaultUrl = StringUtils.hasText(contextPath) ? contextPath + "/" : "/";

        if (StringUtils.hasText(referrer)) {
            String cleanUrl = referrer.trim();
            if (isSafeLocalUrl(cleanUrl)) {
                String pathWithoutContext = cleanUrl;
                if (StringUtils.hasText(contextPath) && pathWithoutContext.startsWith(contextPath)) {
                    pathWithoutContext = pathWithoutContext.substring(contextPath.length());
                }
                if (!pathWithoutContext.startsWith("/auth/") && !pathWithoutContext.equals("/auth")) {
                    if (StringUtils.hasText(contextPath) && !cleanUrl.startsWith(contextPath + "/") && !cleanUrl.equals(contextPath)) {
                        return contextPath + cleanUrl;
                    }
                    return cleanUrl;
                }
            }
        }
        return defaultUrl;
    }

    /**
     * Determines whether the given URL is a safe local relative URL to prevent open redirect vulnerabilities.
     * @param url the URL to validate
     * @return true if the URL is safe to redirect to; false otherwise
     */
    public static boolean isSafeLocalUrl(String url) {
        if (url == null || !url.startsWith("/") || url.startsWith("//") || url.startsWith("/\\") || url.startsWith("\\")) {
            return false;
        }
        int colonIdx = url.indexOf(':');
        int queryIdx = url.indexOf('?');
        if (colonIdx != -1 && (queryIdx == -1 || colonIdx < queryIdx)) {
            return false;
        }
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c < 32 || c == 127) {
                return false;
            }
        }
        return !url.contains("%00") && !url.contains("%0d") && !url.contains("%0D")
                && !url.contains("%0a") && !url.contains("%0A");
    }

}
