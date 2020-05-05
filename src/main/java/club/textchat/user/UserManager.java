package club.textchat.user;

import com.aspectran.core.activity.Translet;
import com.aspectran.core.adapter.SessionAdapter;
import com.aspectran.core.component.bean.annotation.AvoidAdvice;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.aware.ActivityContextAware;
import com.aspectran.core.context.ActivityContext;

import java.util.HashMap;

@Component
@Bean("userManager")
@AvoidAdvice
public class UserManager implements ActivityContextAware {

    /**
     * The key used to store the UserInfo
     */
    public static final String USER_INFO_SESSION_KEY = "user";

    public static final String PREV_USERNAME = "_prev_username_";

    public static final String EXPIRED_TIME = "_expired_time_";

    private ActivityContext activityContext;

    public void save(UserInfo userInfo) {
        String prevUsername = getSessionAdapter().getAttribute(PREV_USERNAME);
        if (prevUsername != null && !prevUsername.equals(userInfo.getUsername())) {
            userInfo.setPrevUsername(prevUsername);
        }
        getSessionAdapter().setAttribute(USER_INFO_SESSION_KEY, userInfo);
        getSessionAdapter().setAttribute(PREV_USERNAME, userInfo.getUsername());
    }

    public void remove() {
        UserInfo userInfo = getUserInfo();
        getSessionAdapter().removeAttribute(USER_INFO_SESSION_KEY);
        if (userInfo != null) {
            getSessionAdapter().setAttribute(PREV_USERNAME, userInfo.getUsername());
            getSessionAdapter().setAttribute(EXPIRED_TIME, System.currentTimeMillis());
        }
    }

    public void checkUserAuthenticated() {
        Translet translet = activityContext.getCurrentActivity().getTranslet();
        if (translet == null) {
            throw new UnsupportedOperationException("There is no translet in " +
                    activityContext.getCurrentActivity());
        }
        UserInfo userInfo = getUserInfo();
        if (userInfo == null) {
            translet.redirect("/", new HashMap<String, String>() {{
                put("referrer", translet.getRequestName());
            }});
        }
    }

    public UserInfo getUserInfo() {
        try {
            return getSessionAdapter().getAttribute(USER_INFO_SESSION_KEY);
        } catch (ClassCastException e) {
            // Exception that can occur if the UserInfo class changes during development.
            getSessionAdapter().invalidate();
            return null;
        }
    }

    public String getSessionId() {
        return getSessionAdapter().getId();
    }

    private SessionAdapter getSessionAdapter() {
        if (activityContext == null) {
            throw new IllegalArgumentException("ActivityContext is not injected");
        }
        SessionAdapter sessionAdapter = activityContext.getCurrentActivity().getSessionAdapter();
        if (sessionAdapter == null) {
            throw new UnsupportedOperationException("There is no SessionAdapter in " +
                    activityContext.getCurrentActivity());
        }
        return sessionAdapter;
    }

    @Override
    public void setActivityContext(ActivityContext activityContext) {
        this.activityContext = activityContext;
    }

}
