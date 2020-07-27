package club.textchat.common;

import com.aspectran.core.activity.Translet;
import com.aspectran.core.activity.response.ResponseTemplate;
import com.aspectran.core.adapter.RequestAdapter;
import com.aspectran.core.component.bean.annotation.Aspect;
import com.aspectran.core.component.bean.annotation.Before;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Joinpoint;
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
                "+: /",
                "+: /info",
                "+: /random",
                "+: /exchange",
                "+: /strangers",
                "+: /strangers/*",
                "+: /rooms/*"
        }
)
public class ETagInterceptorAspect {

    private final ETagInterceptor eTagInterceptor;

    public ETagInterceptorAspect() {
        this.eTagInterceptor = new ETagInterceptor(new ETagTokenFactory() {
            @Override
            public byte[] getToken(Translet translet) {
                return "12".getBytes();
            }
        });
    }

    @Before
    public void intercept(Translet translet) {
        eTagInterceptor.intercept(translet);
    }

}
