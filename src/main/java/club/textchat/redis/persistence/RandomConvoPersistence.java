package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.redis.persistence.AbstractPersistence;
import club.textchat.redis.subscribe.RandomMessageSubscriber;
import club.textchat.server.message.ChatMessage;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;

/**
 * <p>Created: 2020/05/03</p>
 */
@Component
@Bean
public class RandomConvoPersistence extends AbstractPersistence {

    @Autowired
    public RandomConvoPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void put(ChatMessage message) {
        publish(RandomMessageSubscriber.CHANNEL, message.toString());
    }

}
