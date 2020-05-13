package club.textchat.user;

import com.aspectran.core.util.ToStringBuilder;

import java.io.Serializable;

public class UserInfo implements Serializable {

    private static final long serialVersionUID = -2434622566554519523L;

    private long userNo;

    private String username;

    private String prevUsername;

    public long getUserNo() {
        return userNo;
    }

    public void setUserNo(long userNo) {
        this.userNo = userNo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrevUsername() {
        return prevUsername;
    }

    public void setPrevUsername(String prevUsername) {
        this.prevUsername = prevUsername;
    }

    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("userNo", userNo);
        tsb.append("username", username);
        tsb.append("prevUsername", prevUsername);
        return tsb.toString();
    }

}
