package club.textchat.user;

/**
 * <p>Created: 2020/05/11</p>
 */
public class UsernameUtils {

    private static final int MAX_USERNAME_LEN = 30;

    private static final String USERNAME_NORMALIZATION_PATTERN = "[\\s,'`\"&/<>\\\\]";

    public static String nomalize(String username) {
        username = username.replaceAll(USERNAME_NORMALIZATION_PATTERN, " ").trim();
        if (username.length() > MAX_USERNAME_LEN) {
            username = username.substring(0, MAX_USERNAME_LEN);
        }
        return username;
    }

}
