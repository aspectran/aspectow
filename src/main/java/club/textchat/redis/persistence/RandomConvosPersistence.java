package club.textchat.redis.persistence;

import club.textchat.redis.RedisConnectionPool;
import club.textchat.redis.pubsub.RandomMessageSubscriber;
import club.textchat.server.message.ChatMessage;

/**
 * <p>Created: 2020/05/03</p>
 */
public class RandomConvosPersistence extends AbstractPersistence {

    public RandomConvosPersistence(RedisConnectionPool connectionPool) {
        super(connectionPool);
    }

    public void put(ChatMessage message) {
        publish(RandomMessageSubscriber.CHANNEL, message.toString());
    }

}
