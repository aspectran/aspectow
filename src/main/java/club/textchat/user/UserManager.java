package club.textchat.user;

import club.textchat.persistence.TalkersPersistence;
import club.textchat.persistence.UsernamesPersistence;
import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.session.Session;
import com.aspectran.core.component.session.SessionListener;
import com.aspectran.core.component.session.SessionListenerRegistration;
import com.aspectran.core.lang.NonNull;

import javax.servlet.annotation.WebListener;
import java.util.HashMap;

@Component
@Bean("userManager")
@AvoidAdvice
public class UserManager extends InstantActivitySupport implements InitializableBean {

    /**
     * The key used to store the UserInfo
     */
    public static final String USER_INFO_SESSION_KEY = "user";

    public static final String PREV_USERNAME = "--prev-username--";

    public static final String EXPIRED_TIME = "--expired-time--";

    private final UsernamesPersistence usernamesPersistence;

    private final TalkersPersistence talkersPersistence;

    @Autowired
    public UserManager(UsernamesPersistence usernamesPersistence, TalkersPersistence talkersPersistence) {
        this.usernamesPersistence = usernamesPersistence;
        this.talkersPersistence = talkersPersistence;
    }

    public void saveUserInfo(UserInfo userInfo) {
        SessionAdapter sessionAdapter = getSessionAdapter();
        String prevUsername = sessionAdapter.getAttribute(PREV_USERNAME);
        if (prevUsername != null && !prevUsername.equals(userInfo.getUsername())) {
            userInfo.setPrevUsername(prevUsername);
        }
        sessionAdapter.setAttribute(USER_INFO_SESSION_KEY, userInfo);
        sessionAdapter.setAttribute(PREV_USERNAME, userInfo.getUsername());
    }

    public void removeUserInfo() {
        SessionAdapter sessionAdapter = getSessionAdapter();
        UserInfo userInfo = sessionAdapter.getAttribute(USER_INFO_SESSION_KEY);
        if (userInfo != null) {
            sessionAdapter.removeAttribute(USER_INFO_SESSION_KEY);
            sessionAdapter.setAttribute(PREV_USERNAME, userInfo.getUsername());
            sessionAdapter.setAttribute(EXPIRED_TIME, System.currentTimeMillis());
        }
    }

    public void checkUserAuthenticated() {
        Translet translet = getCurrentActivity().getTranslet();
        if (translet == null) {
            throw new UnsupportedOperationException("No such translet in " +
                    getCurrentActivity());
        }
        try {
            getUserInfo();
        } catch (LoginRequiredException e) {
            translet.redirect("/", new HashMap<String, String>() {{
                put("referrer", translet.getRequestName());
            }});
        }
    }

    public boolean isAnotherUser(String username) {
        String httpSessionId = usernamesPersistence.get(username);
        if (httpSessionId != null) {
            return !httpSessionId.equals(getSessionId());
        }
        return talkersPersistence.isTalker(username);
    }

    @NonNull
    public UserInfo getUserInfo() {
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

    @Override
    public void initialize() throws Exception {
        SessionListenerRegistration sessionListenerRegistration = getBeanRegistry().getBean(SessionListenerRegistration.class);
        if (sessionListenerRegistration == null) {
            throw new IllegalStateException("Bean for SessionListenerRegistration must be defined");
        }
        sessionListenerRegistration.register(new UserInfoUnboundListener(usernamesPersistence));
    }

    @WebListener
    public static class UserInfoUnboundListener implements SessionListener {

        private final UsernamesPersistence usernamesPersistence;

        public UserInfoUnboundListener(UsernamesPersistence usernamesPersistence) {
            this.usernamesPersistence = usernamesPersistence;
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
            if (USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                usernamesPersistence.acquire(userInfo.getUsername(), sessionId);
            }
        }

        private void abandonUsername(String name, Object value, String sessionId) {
            if (USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                usernamesPersistence.abandon(userInfo.getUsername(), sessionId);
            }
        }

        private void abandonUsername(Session session) {
            UserInfo userInfo = session.getAttribute(USER_INFO_SESSION_KEY);
            if (userInfo != null) {
                usernamesPersistence.abandon(userInfo.getUsername(), session.getId());
            }
        }

    }

}
