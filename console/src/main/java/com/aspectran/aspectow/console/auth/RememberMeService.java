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

import com.aspectran.aspectow.console.common.db.mapper.AccountMapper;
import com.aspectran.aspectow.console.common.db.model.PersistentLogin;
import com.aspectran.aspectow.console.common.db.model.User;
import com.aspectran.aspectow.console.common.service.UserService;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.aware.EnvironmentAware;
import com.aspectran.core.context.env.Environment;
import com.aspectran.utils.DurationUtils;
import com.aspectran.utils.PBEncryptionUtils;
import com.aspectran.utils.StringUtils;
import com.aspectran.utils.net.IpAddressUtils;
import com.aspectran.web.support.http.Cookie;
import com.aspectran.web.support.http.HttpHeaders;
import com.aspectran.web.support.util.CookieGenerator;
import com.aspectran.web.support.util.WebUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service that handles Remember-Me persistent login tokens, token rotation,
 * theft detection, and IP/device binding verification.
 *
 * <p>Created: 2026-10-24</p>
 */
@Component
@Profile("[console.ui, console.custom-ui]")
public class RememberMeService implements EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(RememberMeService.class);

    public static final String REMEMBER_ME_COOKIE_NAME = "asc_remember_me";

    public static final int DEFAULT_TOKEN_VALIDITY_SECONDS = 30 * 24 * 3600; // 30 days

    private final AccountMapper accountMapper;
    private final UserService userService;
    private final CookieGenerator cookieGenerator;

    private Environment environment;

    @Autowired
    public RememberMeService(AccountMapper accountMapper, UserService userService) {
        this.accountMapper = accountMapper;
        this.userService = userService;
        this.cookieGenerator = new CookieGenerator(REMEMBER_ME_COOKIE_NAME);
        this.cookieGenerator.setCookiePath("/");
        this.cookieGenerator.setCookieHttpOnly(true);
        this.cookieGenerator.setCookieMaxAge(DEFAULT_TOKEN_VALIDITY_SECONDS);
        this.cookieGenerator.setSameSite("Lax");
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        if (this.cookieGenerator != null) {
            this.cookieGenerator.setCookieMaxAge(getTokenValiditySeconds());
        }
    }

    /**
     * Returns the token validity duration in seconds configured in the environment.
     * Supports human-friendly duration expressions (e.g. "30d", "7d", "24h", "1800s")
     * via {@link DurationUtils}, as well as numeric seconds.
     * If not configured or invalid, defaults to 30 days (2,592,000 seconds).
     * @return the token validity in seconds
     */
    public int getTokenValiditySeconds() {
        if (environment != null) {
            Object prop = environment.getProperty("aspectow.console.auth.rememberMe.tokenValiditySeconds");
            if (prop == null) {
                prop = environment.getProperty("aspectow.console.auth.rememberMe.tokenValidity");
            }
            if (prop instanceof Number num) {
                return num.intValue();
            }
            if (prop instanceof String str && StringUtils.hasText(str)) {
                try {
                    Duration duration = DurationUtils.parseDuration(str.trim(), ChronoUnit.SECONDS);
                    return Math.toIntExact(duration.toSeconds());
                } catch (Exception e) {
                    logger.warn("Failed to parse remember-me token validity duration '{}'; falling back to default ({}s)",
                            str, DEFAULT_TOKEN_VALIDITY_SECONDS);
                }
            }
        }
        return DEFAULT_TOKEN_VALIDITY_SECONDS;
    }

    /**
     * Checks if the Remember-Me feature is enabled.
     * By default, it is enabled only in the demo profile unless explicitly configured.
     * @return true if enabled, false otherwise
     */
    public boolean isRememberMeEnabled() {
        if (environment != null) {
            Object prop = environment.getProperty("aspectow.console.auth.rememberMe.enabled");
            if (prop instanceof Boolean bool) {
                return bool;
            }
            if (prop instanceof String str && StringUtils.hasText(str)) {
                return Boolean.parseBoolean(str.trim());
            }
            if (prop != null) {
                return Boolean.parseBoolean(prop.toString().trim());
            }
            return environment.acceptsProfiles("demo");
        }
        return false;
    }

    /**
     * Creates and stores a new persistent login token for Remember-Me and sets the cookie.
     * @param translet the current translet
     * @param username the username to remember
     */
    public void createRememberMeToken(@NonNull Translet translet, @NonNull String username) {
        if (!isRememberMeEnabled() || StringUtils.isEmpty(username)) {
            return;
        }

        String series = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();
        String remoteAddr = WebUtils.getRemoteAddr(translet);
        String userAgent = translet.getRequestAdapter().getHeader(HttpHeaders.USER_AGENT);

        PersistentLogin persistentLogin = new PersistentLogin();
        persistentLogin.setSeries(series);
        persistentLogin.setUsername(username);
        persistentLogin.setToken(token);
        persistentLogin.setIpAddress(remoteAddr);
        persistentLogin.setUserAgent(userAgent);
        persistentLogin.setLastUsed(LocalDateTime.now());

        accountMapper.insertPersistentLogin(persistentLogin);

        String cookieValue = PBEncryptionUtils.encrypt(series + ":" + token);
        cookieGenerator.setCookieMaxAge(getTokenValiditySeconds());
        cookieGenerator.addCookie(translet.getResponseAdapter(), cookieValue);
    }

    /**
     * Attempts automatic authentication using the persistent login cookie.
     * Performs theft detection, IP/User-Agent validation, and token rotation.
     * @param translet the current translet
     * @return the authenticated username, or null if Remember-Me authentication failed
     */
    @Nullable
    public String autoLogin(@NonNull Translet translet) {
        if (!isRememberMeEnabled()) {
            return null;
        }

        Cookie cookie = WebUtils.getCookie(translet.getRequestAdapter(), REMEMBER_ME_COOKIE_NAME);
        if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
            return null;
        }

        String series;
        String cookieToken;
        try {
            String decrypted = PBEncryptionUtils.decrypt(cookie.getValue());
            String[] parts = StringUtils.split(decrypted, ":");
            if (parts == null || parts.length != 2) {
                removeRememberMeToken(translet);
                return null;
            }
            series = parts[0];
            cookieToken = parts[1];
        } catch (Exception e) {
            removeRememberMeToken(translet);
            return null;
        }

        PersistentLogin persistentLogin = accountMapper.getPersistentLoginBySeries(series);
        if (persistentLogin == null) {
            removeRememberMeToken(translet);
            return null;
        }

        // Check token expiration
        LocalDateTime now = LocalDateTime.now();
        if (persistentLogin.getLastUsed() != null &&
                persistentLogin.getLastUsed().plusSeconds(getTokenValiditySeconds()).isBefore(now)) {
            accountMapper.deletePersistentLoginBySeries(series);
            removeRememberMeToken(translet);
            return null;
        }

        String username = persistentLogin.getUsername();
        String currentIp = WebUtils.getRemoteAddr(translet);
        String currentUserAgent = translet.getRequestAdapter().getHeader(HttpHeaders.USER_AGENT);

        // Theft Detection: token does not match stored token
        if (!persistentLogin.getToken().equals(cookieToken)) {
            accountMapper.deletePersistentLoginByUsername(username);
            userService.recordAuditLog(username, "REMEMBER_ME_THEFT_DETECTED", "Series: " + series,
                    "Remember-Me token theft detected! Invalidated all tokens for user: " + username, currentIp);
            removeRememberMeToken(translet);
            logger.warn("Remember-Me token theft detected for user '{}'. All persistent logins invalidated.", username);
            return null;
        }

        // Account status & allowed IPs check
        User user = userService.getUserByUsername(username);
        if (user == null || !"NORMAL".equals(user.getStatus())) {
            removeRememberMeToken(translet);
            return null;
        }
        if (!IpAddressUtils.isAllowedIp(currentIp, user.getAllowedIps())) {
            userService.recordAuditLog(username, "LOGIN_FAILED_UNALLOWED_IP", "User: " + username,
                    "Remember-Me attempt from unallowed IP address: " + currentIp, currentIp);
            removeRememberMeToken(translet);
            return null;
        }

        // IP and User-Agent binding verification
        if (persistentLogin.getIpAddress() != null && !persistentLogin.getIpAddress().equals(currentIp)) {
            userService.recordAuditLog(username, "REMEMBER_ME_IP_MISMATCH", "User: " + username,
                    "Remember-Me blocked due to IP mismatch. Registered: " + persistentLogin.getIpAddress() +
                            ", Attempted: " + currentIp, currentIp);
            removeRememberMeToken(translet);
            return null;
        }
        if (persistentLogin.getUserAgent() != null && !persistentLogin.getUserAgent().equals(currentUserAgent)) {
            userService.recordAuditLog(username, "REMEMBER_ME_DEVICE_MISMATCH", "User: " + username,
                    "Remember-Me blocked due to User-Agent mismatch for series: " + series, currentIp);
            removeRememberMeToken(translet);
            return null;
        }

        // Token Rotation: issue new token on successful auto-login
        String newToken = UUID.randomUUID().toString();
        persistentLogin.setToken(newToken);
        persistentLogin.setIpAddress(currentIp);
        persistentLogin.setUserAgent(currentUserAgent);
        persistentLogin.setLastUsed(now);
        accountMapper.updatePersistentLogin(persistentLogin);

        String newCookieValue = PBEncryptionUtils.encrypt(series + ":" + newToken);
        cookieGenerator.setCookieMaxAge(getTokenValiditySeconds());
        cookieGenerator.addCookie(translet.getResponseAdapter(), newCookieValue);

        return username;
    }

    /**
     * Removes the persistent login token from the database and deletes the cookie.
     * @param translet the current translet
     */
    public void removeRememberMeToken(@NonNull Translet translet) {
        Cookie cookie = WebUtils.getCookie(translet.getRequestAdapter(), REMEMBER_ME_COOKIE_NAME);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            try {
                String decrypted = PBEncryptionUtils.decrypt(cookie.getValue());
                String[] parts = StringUtils.split(decrypted, ":");
                if (parts != null && parts.length == 2) {
                    accountMapper.deletePersistentLoginBySeries(parts[0]);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        cookieGenerator.removeCookie(translet.getResponseAdapter());
    }

}
