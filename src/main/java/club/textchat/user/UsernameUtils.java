package club.textchat.user;

/**
 * <p>Created: 2020/05/11</p>
 */
public class UsernameUtils {

    private static final int MAX_USERNAME_LEN = 30;

    private static final String USERNAME_NORMALIZATION_PATTERN = "[\\s,'`\"&/<>\\\\]";

    private static final String CONDENSATION_PATTERN = "[\\s`~!@#$%^&*()_|+\\-=?;:'\",.<>\\{\\}\\[\\]\\\\\\/]";

    public static String normalize(String username) {
        username = username.replaceAll(USERNAME_NORMALIZATION_PATTERN, " ").trim();
        if (username.length() > MAX_USERNAME_LEN) {
            username = username.substring(0, MAX_USERNAME_LEN);
        }
        return username;
    }

    public static String condense(String username) {
        return username.replaceAll(CONDENSATION_PATTERN,"");
    }

}
