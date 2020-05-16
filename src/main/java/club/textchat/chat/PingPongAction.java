package club.textchat.chat;

import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Created: 2020/05/17</p>
 */
@Component
public class PingPongAction {

    private static final int MAX_INTERVAL = 2000;

    private final Map<String, Long> cache = Collections.synchronizedMap(createLRUMap(64));

    @Request("/ping")
    @Transform(FormatType.JSON)
    public String randomChatToken(Translet translet) {
        String sessionId = translet.getSessionAdapter().getId();
        long currentTime = System.currentTimeMillis();
        Long lastAccessedTime = cache.put(sessionId, currentTime);
        if (lastAccessedTime != null) {
            if (lastAccessedTime > currentTime - MAX_INTERVAL) {
                return null;
            }
        }
        return "pong";
    }

    public static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
        return new LinkedHashMap<K, V>(maxEntries * 10 / 7, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

}
