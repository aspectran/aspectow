package club.textchat.user;

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

import java.io.IOException;

@Component
@Bean("userAction")
public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    private final UserManager userManager;

    @Autowired
    public UserAction(UserManager userManager) {
        this.userManager = userManager;
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
