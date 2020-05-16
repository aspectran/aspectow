package club.textchat.chat;

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
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.PBEncryptionUtils;
import com.aspectran.core.util.security.TimeLimitedPBTokenIssuer;

import java.util.HashMap;
import java.util.Map;

@Component
@Bean("chatAction")
public class ChatAction {

    private static final String RANDOM_CHATROOM_ID = "0";

    private final UserManager userManager;

    @Autowired
    public ChatAction(UserManager userManager) {
        this.userManager = userManager;
    }

    @Request("/rooms/${roomId}")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, String> startChat(@Required String roomId) {
        if ("random".equals(roomId)) {
            roomId = "0";
        } else {
            try {
                roomId = PBEncryptionUtils.decrypt(roomId);
            } catch (Exception e) {
                throw new InvalidChatRoomException(roomId, "invalid-room-id");
            }
        }

        UserInfo userInfo = userManager.getUserInfo();

        Map<String, String> map = new HashMap<>();
        map.put("userNo", Long.toString(userInfo.getUserNo()));
        map.put("username", userInfo.getUsername());
        map.put("roomName", roomId);

        if (RANDOM_CHATROOM_ID.equals(roomId)) {
            map.put("include", "pages/chat-random");
        } else {
            AdmissionToken admissionToken = new AdmissionToken();
            admissionToken.setUserNo(userInfo.getUserNo());
            admissionToken.setUsername(userInfo.getUsername());
            admissionToken.setRoomId(roomId);

            map.put("token", TimeLimitedPBTokenIssuer.getToken(admissionToken));
            map.put("include", "pages/chat-default");
        }

        return map;
    }

    @Request("/rooms/random/token")
    @Transform(FormatType.JSON)
    public String randomChatToken() {
        if (true) {
            throw new RuntimeException("test");
        }
        UserInfo userInfo = userManager.getUserInfo();
        AdmissionToken admissionToken = new AdmissionToken();
        admissionToken.setUserNo(userInfo.getUserNo());
        admissionToken.setUsername(userInfo.getUsername());
        admissionToken.setRoomId(RANDOM_CHATROOM_ID);
        return TimeLimitedPBTokenIssuer.getToken(admissionToken);
    }

}