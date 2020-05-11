package club.textchat.user;

import club.textchat.recaptcha.ReCaptchaVerifier;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.TransformType;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

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
    @Transform(TransformType.JSON)
    public Map<String, Integer> signin(@Required String username,
                               @Required String recaptchaResponse) {
        username = UsernameUtils.nomalize(username);

        boolean success = false;
        try {
            success = ReCaptchaVerifier.verifySuccess(recaptchaResponse);
        } catch (IOException e) {
            logger.warn("reCAPTCHA verification failed", e);
        }

        if (!success) {
            return Collections.singletonMap("result", -1);
        }

        if (userManager.isAnotherUser(username)) {
            return Collections.singletonMap("result", -2);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        userManager.saveUserInfo(userInfo);

        return Collections.singletonMap("result", 0);
    }

    @Request("/signout")
    @Redirect("/")
    public void signout() {
        userManager.removeUserInfo();
    }

}
