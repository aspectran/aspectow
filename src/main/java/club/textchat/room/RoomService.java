package club.textchat.room;

import club.textchat.common.mybatis.SimpleSqlSession;
import club.textchat.recaptcha.ReCaptchaVerifier;
import club.textchat.user.UserService;
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
import com.aspectran.core.util.PBEncryptionUtils;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final SqlSession sqlSession;

    @Autowired
    public RoomService(SimpleSqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Request("/rooms")
    @Dispatch("templates/default")
    @Action("page")
    public Map<String, Object> rooms() {
        List<RoomInfo> rooms = sqlSession.selectList("rooms.getRoomList");
        for (RoomInfo roomInfo : rooms) {
            roomInfo.setEncryptedRoomId(PBEncryptionUtils.encrypt(Integer.toString(roomInfo.getRoomId())));
        }

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

        Integer count = sqlSession.selectOne("rooms.getRoomCountByName", roomName);
        if (count != null && count > 0) {
            return "-2";
        }

        RoomInfo roomInfo = new RoomInfo();
        roomInfo.setRoomName(roomName);
        roomInfo.setLanguage(language);

        sqlSession.insert("rooms.insertRoom", roomInfo);
        if (roomInfo.getRoomId() <= 0) {
            return "-9";
        }
        sqlSession.insert("rooms.insertRoomHist", roomInfo);

        return PBEncryptionUtils.encrypt(Integer.toString(roomInfo.getRoomId()));
    }

}