package club.textchat.server;

import com.aspectran.core.lang.NonNull;
import com.aspectran.core.lang.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>Created: 2020/05/07</p>
 */
public class TalkerInfo implements Serializable {

    private static final long serialVersionUID = 6164395040844520357L;

    public static final String TALKER_INFO_PROP = "talkerInfo";

    private String roomId;

    private String username;

    private String prevUsername;

    private String httpSessionId;

    @NonNull
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Nullable
    public String getPrevUsername() {
        return (username != null && username.equals(prevUsername) ? null : prevUsername);
    }

    public void setPrevUsername(String prevUsername) {
        this.prevUsername = prevUsername;
    }

    @NonNull
    public String getHttpSessionId() {
        return httpSessionId;
    }

    public void setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TalkerInfo)) {
            return false;
        }
        TalkerInfo talkerInfo = (TalkerInfo)o;
        return (Objects.equals(roomId, talkerInfo.getRoomId()) &&
                Objects.equals(username, talkerInfo.getUsername()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 7;
        result = prime * result + (roomId != null ? roomId.hashCode() : 0);
        result = prime * result + (username != null ? username.hashCode() : 0);
        return result;
    }

}
