package club.textchat.common;

import club.textchat.user.LoginRequiredException;
import club.textchat.user.UserManager;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.activity.response.ResponseTemplate;
import com.aspectran.core.adapter.RequestAdapter;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.context.rule.type.MethodType;
import com.aspectran.core.lang.Nullable;
import com.aspectran.core.util.DigestUtils;
import com.aspectran.core.util.StringUtils;
import com.aspectran.web.support.etag.ETagInterceptor;
import com.aspectran.web.support.etag.ETagTokenFactory;
import com.aspectran.web.support.http.HttpHeaders;
import com.aspectran.web.support.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Created: 2020/07/26</p>
 */
@Component
@Aspect(
        id = "etagInterceptorAspect"
)
@Joinpoint(
        methods = {
                MethodType.GET
        },
        pointcut = {
                "+: /info",
                "+: /random",
                "+: /exchange"
        }
)
@Profile("dev")
public class ETagInterceptorAspect {

    private final UserManager userManager;

    private final ETagInterceptor eTagInterceptor;

    private final String version = Long.toString(System.currentTimeMillis());

    public ETagInterceptorAspect(UserManager userManager) {
        this.userManager = userManager;
        this.eTagInterceptor = new ETagInterceptor(translet -> generateETagToken().getBytes());
        this.eTagInterceptor.setWriteWeakETag(true);
    }

    @Before
    public void intercept(Translet translet) {
        eTagInterceptor.intercept(translet);
    }

    private String generateETagToken() {
        try {
            return version + userManager.getUserInfo();
        } catch (LoginRequiredException e) {
            return version;
        }
    }

}
