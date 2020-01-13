package club.textchat.recaptcha;

import com.aspectran.core.util.apon.JsonToApon;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Verifies reCAPTCHA responses.
 *
 * <p>Created: 2019/10/15</p>
 */
public class ReCaptchaVerifier {

    private static final String SITEVERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private static final String SECRET_KEY = "6Ldt0r0UAAAAAFFsdRWamrAKViusrrJIXiAGK7bU";

    /**
     * Verify re captcha status.
     *
     * @param recaptchaResponse the user response token provided by reCAPTCHA, verifying the user on your site
     * @param remoteIp the user's IP address
     * @return Google Re-Captcha API result
     */
    public static ReCaptchaVerifyResponse verify(String recaptchaResponse, String remoteIp) throws IOException {
        String params = "secret=" + SECRET_KEY + "&response=" + recaptchaResponse;
        if (remoteIp != null) {
            params += "&remoteip=" + remoteIp;
        }

        byte[] postData = params.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        URL obj = new URL(SITEVERIFY_URL);
        HttpsURLConnection conn = (HttpsURLConnection)obj.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);
        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            dos.write(postData);
        }
        String responseBody = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                .lines()
                .parallel()
                .collect(Collectors.joining("\n"));

        return JsonToApon.from(responseBody, ReCaptchaVerifyResponse.class);
    }

    /**
     * Verify re captcha status.
     *
     * @param recaptchaResponse the user response token provided by reCAPTCHA, verifying the user on your site
     * @param remoteIp the user's IP address
     * @return boolean value for success
     */
    public static boolean verifySuccess(String recaptchaResponse, String remoteIp) throws IOException {
        ReCaptchaVerifyResponse response = verify(recaptchaResponse, remoteIp);
        return response.isSuccess();
    }

    /**
     * Verify re captcha status.
     *
     * @param recaptchaResponse the user response token provided by reCAPTCHA, verifying the user on your site
     * @return boolean value for success
     */
    public static boolean verifySuccess(String recaptchaResponse) throws IOException {
        return verifySuccess(recaptchaResponse, null);
    }

}
