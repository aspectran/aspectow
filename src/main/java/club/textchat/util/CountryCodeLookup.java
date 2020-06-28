package club.textchat.util;

import com.aspectran.core.util.apon.JsonToApon;
import com.aspectran.core.util.apon.Parameters;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * WHOIS OpenAPI
 * Email: aspectran@gmail.com
 *
 * {"whois":{"query":"185.80.140.175","queryType":"IPv4","registry":"RIPENCC","countryCode":"YE"}}
 *
 * <p>Created: 2020/06/29</p>
 */
public class CountryCodeLookup {

    private static final Logger logger = LoggerFactory.getLogger(CountryCodeLookup.class);

    private static final String API_URL = "http://whois.kisa.or.kr/openapi/ipascc.jsp?key=2020062900202295685342&answer=json&query=";

    private static final int TIMEOUT = 3000;

    private static final CountryCodeLookup instance;

    private final CloseableHttpClient httpClient;

    private final RequestConfig requestConfig;

    static {
        instance = new CountryCodeLookup();
    }

    private CountryCodeLookup() {
        this.httpClient = HttpClients.createDefault();
        this.requestConfig = RequestConfig.custom()
                .setSocketTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT)
                .setConnectionRequestTimeout(TIMEOUT)
                .build();
    }

    public String getCountryCodeByIP(String ip) {
        if (ip == null || ip.equals("127.0.0.1") ||
                ip.equals("localhost") || ip.startsWith("192.168.0.")) {
            return null;
        }
        HttpGet request = new HttpGet(API_URL + ip);
        request.setConfig(requestConfig);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new IOException("Failed with HTTP error code : " + statusCode);
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                Parameters parameters = JsonToApon.from(result);
                Parameters whois = parameters.getParameters("whois");
                return whois.getString("countryCode");
            }
        } catch (IOException e) {
            logger.error("IP address lookup failed", e);
        }
        return null;
    }

    public static CountryCodeLookup getInstance() {
        return instance;
    }

}
