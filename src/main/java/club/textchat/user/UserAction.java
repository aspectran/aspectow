package club.textchat.user;

import club.textchat.common.mybatis.SimpleSqlSession;
import club.textchat.recaptcha.ReCaptchaVerifier;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Component
@Bean("userAction")
public class UserAction {

    private static final Logger logger = LoggerFactory.getLogger(UserAction.class);

    private final UserManager userManager;

    private final SimpleSqlSession sqlSession;

    @Autowired
    public UserAction(UserManager userManager,
                      SimpleSqlSession sqlSession) {
        this.userManager = userManager;
        this.sqlSession = sqlSession;
    }

    @RequestToPost("/guest/signin")
    @Transform(FormatType.JSON)
    public Map<String, Integer> signin(Translet translet,
                                       @Required String username,
                                       @Required String recaptchaResponse,
                                       String timeZone) {
        username = UsernameUtils.normalize(username);

        boolean success = false;
        try {
            success = ReCaptchaVerifier.verifySuccess(recaptchaResponse);
        } catch (IOException e) {
            logger.warn("reCAPTCHA verification failed", e);
        }

        if (!success) {
            return Collections.singletonMap("result", -1);
        }

        if (userManager.isInUseUsername(username)) {
            return Collections.singletonMap("result", -2);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);

        Locale locale = translet.getRequestAdapter().getLocale();
        if (locale != null) {
            userInfo.setLocale(locale.toString());
            userInfo.setCountry(locale.getCountry());
        }
        userInfo.setTimeZone(timeZone);

        sqlSession.insert("users.insertLoginHist", userInfo);
        if (userInfo.getUserNo() <= 0) {
            return Collections.singletonMap("result", -3);
        }
        userInfo.setUserNo(-userInfo.getUserNo());

        userManager.saveUserInfo(userInfo);

        return Collections.singletonMap("result", 0);
    }

    @Request("/signout")
    @Redirect("/")
    public void signout() {
        userManager.removeUserInfo();
    }

}
