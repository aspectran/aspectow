package club.textchat.user;

import club.textchat.persistence.InConvoUsersPersistence;
import club.textchat.persistence.SignedInUsersPersistence;
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

import java.util.HashMap;

@Component
@Bean("userManager")
@AvoidAdvice
public class UserManager extends InstantActivitySupport implements InitializableBean {

    /**
     * The key used to store the UserInfo
     */
    public static final String USER_INFO_SESSION_KEY = "user";

    public static final String PREV_USERNAME = "-prev-uname-";

    public static final String EXPIRED_TIME = "-expired-tm-";

    private final SignedInUsersPersistence signedInUsersPersistence;

    private final InConvoUsersPersistence inConvoUsersPersistence;

    @Autowired
    public UserManager(SignedInUsersPersistence signedInUsersPersistence,
                       InConvoUsersPersistence inConvoUsersPersistence) {
        this.signedInUsersPersistence = signedInUsersPersistence;
        this.inConvoUsersPersistence = inConvoUsersPersistence;
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

    public void checkSignedIn() {
        Translet translet = getCurrentActivity().getTranslet();
        if (translet == null) {
            throw new IllegalStateException("No such translet in " + getCurrentActivity());
        }
        try {
            getUserInfo();
        } catch (LoginRequiredException e) {
            translet.redirect("/", new HashMap<String, String>() {{
                put("referrer", translet.getRequestName());
            }});
        }
    }

    public void checkAlreadySignedIn() {
        Translet translet = getCurrentActivity().getTranslet();
        if (translet == null) {
            throw new IllegalStateException("No such translet in " + getCurrentActivity());
        }
        try {
            getUserInfo();
            translet.redirect("/rooms");
        } catch (LoginRequiredException e) {
            // ignore
        }
    }

    public boolean isInUseUsername(String username) {
        String httpSessionId = signedInUsersPersistence.get(username);
        String httpSessionId2 = inConvoUsersPersistence.get(username);
        return (httpSessionId != null && !httpSessionId.equals(getSessionId())) ||
                (httpSessionId2 != null && !httpSessionId2.equals(getSessionId()));
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
        sessionListenerRegistration.register(new UserInfoUnboundListener(signedInUsersPersistence));
    }

    public static class UserInfoUnboundListener implements SessionListener {

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
            if (USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                signedInUsersPersistence.put(userInfo.getUsername(), sessionId);
            }
        }

        private void abandonUsername(String name, Object value, String sessionId) {
            if (USER_INFO_SESSION_KEY.equals(name)) {
                UserInfo userInfo = (UserInfo)value;
                signedInUsersPersistence.abandon(userInfo.getUsername(), sessionId);
            }
        }

        private void abandonUsername(Session session) {
            UserInfo userInfo = session.getAttribute(USER_INFO_SESSION_KEY);
            if (userInfo != null) {
                signedInUsersPersistence.abandon(userInfo.getUsername(), session.getId());
            }
        }

    }

}
