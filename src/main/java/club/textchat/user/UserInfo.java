package club.textchat.user;

import com.aspectran.core.util.ToStringBuilder;

import java.io.Serializable;

public class UserInfo implements Serializable {

    private static final long serialVersionUID = -2434622566554519523L;

    private long userNo;

    private String username;

    private String prevUsername;

    private String country;

    private String language;

    private String timeZone;

    private String ipAddr;

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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("userNo", userNo);
        tsb.append("username", username);
        tsb.append("prevUsername", prevUsername);
        tsb.append("country", country);
        tsb.append("language", language);
        tsb.append("timeZone", timeZone);
        tsb.append("ipAddr", ipAddr);
        return tsb.toString();
    }

}
