package club.textchat.user;

import com.aspectran.core.util.ToStringBuilder;

import java.io.Serializable;

public class UserInfo implements Serializable {

    private static final long serialVersionUID = -2434622566554519523L;

    private long userNo;

    private String username;

    private String prevUsername;

    private String country;

    private String locale;

    private String timeZone;

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

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public String toString() {
        ToStringBuilder tsb = new ToStringBuilder();
        tsb.append("userNo", userNo);
        tsb.append("username", username);
        tsb.append("prevUsername", prevUsername);
        tsb.append("country", country);
        tsb.append("locale", locale);
        tsb.append("timeZone", timeZone);
        return tsb.toString();
    }

}
