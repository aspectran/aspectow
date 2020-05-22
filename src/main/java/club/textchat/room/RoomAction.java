package club.textchat.room;

import club.textchat.recaptcha.ReCaptchaVerifier;
import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Required;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RoomAction {

    private static final Logger logger = LoggerFactory.getLogger(RoomAction.class);

    private final UserManager userManager;

    private final RoomManager roomManager;

    @Autowired
    public RoomAction(UserManager userManager,
                      RoomManager roomManager) {
        this.userManager = userManager;
        this.roomManager = roomManager;
    }

    @Request("/rooms")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> rooms() {
        List<RoomInfo> rooms = roomManager.getRoomList();

        Map<String, Object> map = new HashMap<>();
        map.put("include", "pages/rooms");
        map.put("rooms", rooms);
        return map;
    }

    @RequestToPost("/room/create")
    @Transform(FormatType.JSON)
    public String createChatroom(@Required String recaptchaResponse,
                                 @Required @Qualifier("room_nm") String roomName,
                                 @Required @Qualifier("lang_cd") String language) {
        boolean success = false;
        try {
            success = ReCaptchaVerifier.verifySuccess(recaptchaResponse);
        } catch (IOException e) {
            logger.warn("reCAPTCHA verification failed", e);
        }
        if (!success) {
            return "-1";
        }

        UserInfo userInfo = userManager.getUserInfo();

        RoomInfo roomInfo = new RoomInfo();
        roomInfo.setRoomName(roomName);
        roomInfo.setLanguage(language);
        roomInfo.setUserNo(userInfo.getUserNo());

        String encryptedRoomId = roomManager.createRoom(roomInfo);
        return (encryptedRoomId != null ? encryptedRoomId : "-2");
    }

}
