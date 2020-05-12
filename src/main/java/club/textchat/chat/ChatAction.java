package club.textchat.chat;

import club.textchat.server.AdmissionToken;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.util.PBEncryptionUtils;
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;

import java.util.HashMap;
import java.util.Map;

@Component
@Bean("chatAction")
public class ChatAction {

    private final UserManager userManager;

    @Autowired
    public ChatAction(UserManager userManager) {
        this.userManager = userManager;
    }

    @Request("/rooms/${roomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> joinChat(@Required @Qualifier("roomId") String encryptedRoomId) {
        String roomId = PBEncryptionUtils.decrypt(encryptedRoomId);

        UserInfo userInfo = userManager.getUserInfo();
        String username = userInfo.getUsername();

        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setUsername(username);
        admissionToken.setRoomId(roomId);

        String encryptedAdmissionToken = TimeLimitedPBTokenIssuer.getToken(admissionToken);

        Map<String, String> map = new HashMap<>();
        map.put("include", "pages/chat");
        map.put("username", username);
        map.put("roomName", roomId);
        map.put("token", encryptedAdmissionToken);
        return map;
    }

}