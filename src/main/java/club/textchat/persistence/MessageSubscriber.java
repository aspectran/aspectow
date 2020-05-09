package club.textchat.persistence;

import club.textchat.server.ChatHandler;
import club.textchat.server.ChatServer;
import club.textchat.server.message.ChatMessage;
import club.textchat.server.message.payload.BroadcastPayload;
import club.textchat.server.message.payload.UserJoinedPayload;
import club.textchat.server.message.payload.UserLeftPayload;
import com.aspectran.core.component.bean.ablility.DisposableBean;
import com.aspectran.core.component.bean.ablility.InitializableBean;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

import java.io.IOException;

/**
 * <p>Created: 2020/05/10</p>
 */
@Component
@Bean("messageSubscriber")
public class MessageSubscriber extends RedisPubSubAdapter<String, String> implements InitializableBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);

    public static final String CHANNEL = "message";

    private final StatefulRedisPubSubConnection<String, String> connection;

    private final ChatHandler chatHandler;

    @Autowired
    public MessageSubscriber(RedisConnectionPool connectionPool, ChatServer chatServer) {
        this.connection = connectionPool.getPubSubConnection();
        this.chatHandler = chatServer;
    }

    @Override
    public void message(String channel, String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(channel + ": " + message);
        }
        ChatMessage chatMessage;
        try {
            chatMessage = new ChatMessage(message);
        } catch (IOException e) {
            logger.warn(e);
            return;
        }
        BroadcastPayload broadcastPayload = chatMessage.getBroadcastPayload();
        if (broadcastPayload != null) {
            chatHandler.broadcast(chatMessage, broadcastPayload.getRoomId(), broadcastPayload.getUsername());
            return;
        }
        UserJoinedPayload userJoinedPayload = chatMessage.getUserJoinedPayload();
        if (userJoinedPayload != null) {
            chatHandler.broadcast(chatMessage, userJoinedPayload.getRoomId(), userJoinedPayload.getUsername());
            return;
        }
        UserLeftPayload userLeftPayload = chatMessage.getUserLeftPayload();
        if (userLeftPayload != null) {
            chatHandler.broadcast(chatMessage, userLeftPayload.getRoomId(), null); // talker already left
        }
    }

    @Override
    public void initialize() throws Exception {
        connection.addListener(this);
        RedisPubSubCommands<String, String> sync = connection.sync();
        sync.subscribe(CHANNEL);
    }

    @Override
    public void destroy() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

}
