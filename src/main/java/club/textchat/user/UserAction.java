package club.textchat.user;

import club.textchat.persistence.UsernamesPersistence;
import club.textchat.recaptcha.ReCaptchaVerifier;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import javax.websocket.CloseReason;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Bean("userAction")
public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    private final UserManager userManager;

    private final UsernamesPersistence usernamesPersistence;

    @Autowired
    public UserAction(UserManager userManager,
                      UsernamesPersistence usernamesPersistence) {
        this.userManager = userManager;
        this.usernamesPersistence = usernamesPersistence;
    }

    @RequestToPost("/guest/signin")
    public RestResponse signin(@Required String username,
                               @Required String recaptchaResponse) {
        boolean success = false;
        try {
            success = ReCaptchaVerifier.verifySuccess(recaptchaResponse);
        } catch (IOException e) {
            logger.warn("reCAPTCHA verification failed", e);
        }
        if (!success) {
            return new DefaultRestResponse()
                    .setData("result", -1)
                    .ok();
        }

        if (userManager.isAnotherUser(username)) {
            return new DefaultRestResponse()
                    .setData("result", -2)
                    .ok();
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        userManager.saveUserInfo(userInfo);

        return new DefaultRestResponse()
                .setData("result", 0)
                .ok();
    }

    @Request("/signout")
    @Redirect("/")
    public void signout() {
        userManager.removeUserInfo();
    }

}
