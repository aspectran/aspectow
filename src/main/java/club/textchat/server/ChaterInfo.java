package club.textchat.server;

import club.textchat.user.UserInfo;
import com.aspectran.core.lang.NonNull;
import com.aspectran.core.lang.Nullable;
import com.aspectran.core.util.ToStringBuilder;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.Objects;

/**
 * <p>Created: 2020/05/07</p>
 */
public class ChaterInfo extends UserInfo implements Serializable {

    private static final long serialVersionUID = 6164395040844520357L;

    public static final String CHATER_INFO_PROP = "chaterInfo";

    private String roomId;

    private String httpSessionId;

    private ZoneId zoneId;

    public ChaterInfo(int userNo, String username) {
        setUserNo(userNo);
        setUsername(username);
    }

    public ChaterInfo(@NonNull UserInfo userInfo) {
        setUserNo(userInfo.getUserNo());
        setUsername(userInfo.getUsername());
        setPrevUsername(userInfo.getPrevUsername());
        setZoneId(userInfo.getTimeZone());
    }

    @NonNull
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @Override
    @Nullable
    public String getPrevUsername() {
        if (getUsername() != null && getUsername().equals(super.getPrevUsername())) {
            return null;
        } else {
            return super.getPrevUsername();
        }
    }

    @NonNull
    public String getHttpSessionId() {
        return httpSessionId;
    }

    public void setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
    }

    public void setZoneId(String timeZone) {
        if (timeZone != null) {
            try {
                this.zoneId = ZoneId.of(timeZone);
            } catch (Exception e) {
                this.zoneId = null;
            }
        } else {
            this.zoneId = null;
        }
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChaterInfo)) {
            return false;
        }
        ChaterInfo chaterInfo = (ChaterInfo)o;
        return (getUserNo() == chaterInfo.getUserNo() &&
                Objects.equals(roomId, chaterInfo.getRoomId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 7;
        result = prime * result + (roomId != null ? roomId.hashCode() : 0);
        result = prime * result + Long.hashCode(getUserNo());
        return result;
    }

    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("roomId", roomId);
        tsb.append("userNo", getUserNo());
        tsb.append("username", getUsername());
        tsb.append("prevUsername", getPrevUsername());
        tsb.append("httpSessionId", httpSessionId);
        return tsb.toString();
    }

}
