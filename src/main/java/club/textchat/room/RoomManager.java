package club.textchat.room;

import club.textchat.common.mybatis.SimpleSqlSession;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.PBEncryptionUtils;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

/**
 * <p>Created: 2020/05/18</p>
 */
@Component
public class RoomManager {

    private final SqlSession sqlSession;

    @Autowired
    public RoomManager(SimpleSqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    public String createRoom(RoomInfo roomInfo) {
        Integer count = sqlSession.selectOne("rooms.getRoomCountByName", roomInfo.getRoomName());
        if (count != null && count > 0) {
            return null;
        }

        sqlSession.insert("rooms.insertRoom", roomInfo);
        sqlSession.insert("rooms.insertRoomHist", roomInfo);

        return PBEncryptionUtils.encrypt(Integer.toString(roomInfo.getRoomId()));
    }

    public List<RoomInfo> getRoomList() {
        List<RoomInfo> list = sqlSession.selectList("rooms.getRoomList");
        for (RoomInfo roomInfo : list) {
            roomInfo.setEncryptedRoomId(PBEncryptionUtils.encrypt(Integer.toString(roomInfo.getRoomId())));
        }
        return list;
    }

}
