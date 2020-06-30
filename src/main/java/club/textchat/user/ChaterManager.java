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

import club.textchat.common.mybatis.SimpleSqlSession;
import club.textchat.redis.persistence.InConvoUsersPersistence;
import club.textchat.redis.persistence.LobbyChatPersistence;
import club.textchat.redis.persistence.SignedInUsersPersistence;
import club.textchat.redis.persistence.UsersByCountryPersistence;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.util.json.JsonWriter;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import java.util.Map;

@Component
@AvoidAdvice
public class ChaterManager extends InstantActivitySupport implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(ChaterManager.class);

    private static final String USERS_BY_COUNTRY_MESSAGE_PREFIX = "usersByCountry:";

    private final SimpleSqlSession sqlSession;

    private final UserManager userManager;

    private final SignedInUsersPersistence signedInUsersPersistence;

    private final InConvoUsersPersistence inConvoUsersPersistence;

    private final UsersByCountryPersistence usersByCountryPersistence;

    private final LobbyChatPersistence lobbyChatPersistence;

    @Autowired
    public ChaterManager(SimpleSqlSession sqlSession,
                         UserManager userManager,
                         SignedInUsersPersistence signedInUsersPersistence,
                         InConvoUsersPersistence inConvoUsersPersistence,
                         UsersByCountryPersistence usersByCountryPersistence,
                         LobbyChatPersistence lobbyChatPersistence) {
        this.sqlSession = sqlSession;
        this.userManager = userManager;
        this.signedInUsersPersistence = signedInUsersPersistence;
        this.inConvoUsersPersistence = inConvoUsersPersistence;
        this.usersByCountryPersistence = usersByCountryPersistence;
        this.lobbyChatPersistence = lobbyChatPersistence;
    }

    public boolean isInUseUsername(String username) {
        String httpSessionId = signedInUsersPersistence.get(username);
        return (httpSessionId != null && !httpSessionId.equals(userManager.getSessionId()));
    }

    public boolean isInConvoUser() {
        String httpSessionId = userManager.getSessionId();
        if (httpSessionId == null) {
            return false;
        }
        return inConvoUsersPersistence.exists(httpSessionId);
    }

    private void acquireUsername(String sessionId, UserInfo userInfo) {
        signedInUsersPersistence.put(userInfo.getUsername(), sessionId);
        usersByCountryPersistence.increase(userInfo.getCountry());
        lobbyChatPersistence.publish(USERS_BY_COUNTRY_MESSAGE_PREFIX + getUsersByCountryJson());
        int affected = sqlSession.insert("users.insertUser", userInfo);
        if (affected > 0 && logger.isDebugEnabled()) {
            logger.debug("New user " + userInfo);
        }
    }

    private void discardUsername(String sessionId, UserInfo userInfo) {
        signedInUsersPersistence.abandon(userInfo.getUsername(), sessionId);
        usersByCountryPersistence.decrease(userInfo.getCountry());
        lobbyChatPersistence.publish(USERS_BY_COUNTRY_MESSAGE_PREFIX + getUsersByCountryJson());
        int affected = sqlSession.update("users.discardUsername", userInfo.getUserNo());
        if (affected > 0 && logger.isDebugEnabled()) {
            logger.debug("Discarded user " + userInfo);
        }
    }

    public String getUsersByCountryJson() {
        try {
            Map<String, Long> usersByCountry = usersByCountryPersistence.getCounters();
            return new JsonWriter()
                    .prettyPrint(false)
                    .nullWritable(false)
                    .write(usersByCountry)
                    .toString();
        } catch (Exception e) {
            logger.warn(e);
            return "null";
        }
    }

    @Override
    public void initialize() throws Exception {
        SessionListenerRegistration sessionListenerRegistration = getBeanRegistry().getBean(SessionListenerRegistration.class);
        if (sessionListenerRegistration == null) {
            throw new IllegalStateException("Bean for SessionListenerRegistration must be defined");
        }
        sessionListenerRegistration.register(new UserInfoSessionListener(this));
    }

    private static class UserInfoSessionListener implements SessionListener {

        private final ChaterManager chaterManager;

        public UserInfoSessionListener(ChaterManager chaterManager) {
            this.chaterManager = chaterManager;
        }

        public void sessionDestroyed(Session session) {
            discardUsername(session);
        }

        public void attributeAdded(Session session, String name, Object value) {
            acquireUsername(name, value, session.getId());
        }

        @Override
        public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
            if (oldValue != null && oldValue != newValue) {
                discardUsername(name, oldValue, session.getId());
            }
            acquireUsername(name, newValue, session.getId());
        }

        @Override
        public void attributeRemoved(Session session, String name, Object oldValue) {
            discardUsername(name, oldValue, session.getId());
        }

        private void acquireUsername(String name, Object value, String sessionId) {
            if (UserManager.USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                chaterManager.acquireUsername(sessionId, userInfo);
            }
        }

        private void discardUsername(String name, Object value, String sessionId) {
            if (UserManager.USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                chaterManager.discardUsername(sessionId, userInfo);
            }
        }

        private void discardUsername(Session session) {
            UserInfo userInfo = session.getAttribute(UserManager.USER_INFO_SESSION_KEY);
            if (userInfo != null) {
                chaterManager.discardUsername(session.getId(), userInfo);
            }
        }

    }

}
