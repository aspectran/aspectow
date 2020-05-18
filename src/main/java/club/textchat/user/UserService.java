package club.textchat.user;

import club.textchat.recaptcha.ReCaptchaVerifier;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Redirect;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

@Component
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserManager userManager;

    private final ChaterManager chaterManager;

    @Autowired
    public UserService(UserManager userManager,
                       ChaterManager chaterManager) {
        this.userManager = userManager;
        this.chaterManager = chaterManager;
    }

    @RequestToPost("/guest/signin")
    @Transform(FormatType.JSON)
    public String signin(Translet translet,
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
            return "-1";
        }

        if (chaterManager.isInUseUsername(username)) {
            return "-2";
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        userInfo.setIpAddr(((HttpServletRequest)translet.getRequestAdaptee()).getRemoteAddr());

        Locale locale = translet.getRequestAdapter().getLocale();
        if (locale != null) {
            userInfo.setCountry(locale.getCountry());
            userInfo.setLanguage(locale.getLanguage());
        }
        userInfo.setTimeZone(timeZone);

        if (!chaterManager.createGuestChater(userInfo)) {
            return "-9";
        }

        userManager.saveUserInfo(userInfo);
        return "0";
    }

    @Request("/signout")
    @Redirect("/")
    public void signout() {
        userManager.removeUserInfo();
    }

}
