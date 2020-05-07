package club.textchat.chat;

import club.textchat.persistence.UsernamesPersistence;
import club.textchat.server.AdmissionToken;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.util.apon.Parameters;
import com.aspectran.core.util.apon.VariableParameters;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;
import com.aspectran.daemon.AbstractDaemon;

import java.util.HashMap;
import java.util.Map;

@Component
@Bean("chatAction")
public class ChatAction {

    private static final Logger logger = LoggerFactory.getLogger(ChatAction.class);

    private final UserManager userManager;

    private final UsernamesPersistence usernamesPersistence;

    @Autowired
    public ChatAction(UserManager userManager,
                      UsernamesPersistence usernamesPersistence) {
        this.userManager = userManager;
        this.usernamesPersistence = usernamesPersistence;
    }

    @Request("/chat/${roomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> joinChat(@Required String roomId) {
        UserInfo userInfo = userManager.getUserInfo();
        String username = userInfo.getUsername();

        Parameters params = new VariableParameters();
        params.putValue("username", username);
        params.putValue("roomId", roomId);

        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setUsername(username);
        admissionToken.setRoomId(roomId);

        String encryptedAdmissionToken = TimeLimitedPBTokenIssuer.getToken(admissionToken);

        Map<String, String> map = new HashMap<>();
        map.put("include", "pages/chat");
        map.put("username", username);
        map.put("admissionToken", encryptedAdmissionToken);
        return map;
    }

}