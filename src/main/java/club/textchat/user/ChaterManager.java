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
import club.textchat.redis.persistence.SignedInUsersPersistence;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

@Component
@AvoidAdvice
public class ChaterManager extends InstantActivitySupport implements InitializableBean {

    private static final Logger logger = LoggerFactory.getLogger(ChaterManager.class);

    private final SimpleSqlSession sqlSession;

    private final UserManager userManager;

    private final SignedInUsersPersistence signedInUsersPersistence;

    private final InConvoUsersPersistence inConvoUsersPersistence;

    @Autowired
    public ChaterManager(SimpleSqlSession sqlSession,
                         UserManager userManager,
                         SignedInUsersPersistence signedInUsersPersistence,
                         InConvoUsersPersistence inConvoUsersPersistence) {
        this.sqlSession = sqlSession;
        this.userManager = userManager;
        this.signedInUsersPersistence = signedInUsersPersistence;
        this.inConvoUsersPersistence = inConvoUsersPersistence;
    }

    public boolean createChater(UserInfo userInfo) {
        sqlSession.insert("users.insertUser", userInfo);
        return (userInfo.getUserNo() > 0);
    }

    public void discardUsername(UserInfo userInfo) {
        if (userInfo.getUserNo() < 0) {
            sqlSession.update("users.discardUsername", userInfo.getUserNo());
            if (logger.isDebugEnabled()) {
                logger.debug("Discarded username " + userInfo);
            }
        }
    }

    public boolean isInUseUsername(String username) {
        String httpSessionId = signedInUsersPersistence.get(username);
        String httpSessionId2 = inConvoUsersPersistence.get(username);
        return (httpSessionId != null && !httpSessionId.equals(userManager.getSessionId())) ||
                (httpSessionId2 != null && !httpSessionId2.equals(userManager.getSessionId()));
    }

    @Override
    public void initialize() throws Exception {
        SessionListenerRegistration sessionListenerRegistration = getBeanRegistry().getBean(SessionListenerRegistration.class);
        if (sessionListenerRegistration == null) {
            throw new IllegalStateException("Bean for SessionListenerRegistration must be defined");
        }
        sessionListenerRegistration.register(new UserInfoUnboundListener(signedInUsersPersistence));
    }

    public class UserInfoUnboundListener implements SessionListener {

        private final SignedInUsersPersistence signedInUsersPersistence;

        public UserInfoUnboundListener(SignedInUsersPersistence signedInUsersPersistence) {
            this.signedInUsersPersistence = signedInUsersPersistence;
        }

        public void sessionDestroyed(Session session) {
            abandonUsername(session);
        }

        public void attributeAdded(Session session, String name, Object value) {
            acquireUsername(name, value, session.getId());
        }

        @Override
        public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
            if (oldValue != null && oldValue != newValue) {
                abandonUsername(name, oldValue, session.getId());
            }
            acquireUsername(name, newValue, session.getId());
        }

        @Override
        public void attributeRemoved(Session session, String name, Object oldValue) {
            abandonUsername(name, oldValue, session.getId());
        }

        private void acquireUsername(String name, Object value, String sessionId) {
            if (UserManager.USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                signedInUsersPersistence.put(userInfo.getUsername(), sessionId);
            }
        }

        private void abandonUsername(String name, Object value, String sessionId) {
            if (UserManager.USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                signedInUsersPersistence.abandon(userInfo.getUsername(), sessionId);
                discardUsername(userInfo);
            }
        }

        private void abandonUsername(Session session) {
            UserInfo userInfo = session.getAttribute(UserManager.USER_INFO_SESSION_KEY);
            if (userInfo != null) {
                signedInUsersPersistence.abandon(userInfo.getUsername(), session.getId());
                discardUsername(userInfo);
            }
        }

    }

}
