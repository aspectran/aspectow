package club.textchat.user;

import com.aspectran.core.activity.InstantActivitySupport;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.lang.NonNull;

import java.util.HashMap;

@Component
@Bean("userManager")
@AvoidAdvice
public class UserManager extends InstantActivitySupport {

    /**
     * The key used to store the UserInfo
     */
    public static final String USER_INFO_SESSION_KEY = "user";

    public static final String PREV_USERNAME = "-prev-uname-";

    public static final String EXPIRED_TIME = "-expired-tm-";

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

}
