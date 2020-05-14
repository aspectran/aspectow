package club.textchat.user;

import java.util.regex.Pattern;

/**
 * <p>Created: 2020/05/11</p>
 */
public class UsernameUtils {

    private static final int MAX_USERNAME_LEN = 30;

    private static final Pattern USERNAME_NORMALIZATION_PATTERN = Pattern.compile("[,'`\"#&/<>\\\\]");

    private static final Pattern USERNAME_CONDENSATION_PATTERN = Pattern.compile("[\\s`~!@#$%^&*()_|+\\-=?;:'\",.<>\\{\\}\\[\\]\\\\\\/]");

    private static final Pattern SPACE_CONDENSATION_PATTERN = Pattern.compile("\\s{2,}");

    public static String normalize(String username) {
        username = USERNAME_NORMALIZATION_PATTERN.matcher(username).replaceAll("");
        username = SPACE_CONDENSATION_PATTERN.matcher(username).replaceAll(" ").trim();
        if (username.length() > MAX_USERNAME_LEN) {
            username = username.substring(0, MAX_USERNAME_LEN);
        }
        return username;
    }

    public static String condense(String username) {
        return USERNAME_CONDENSATION_PATTERN.matcher(username).replaceAll("");
    }

}
