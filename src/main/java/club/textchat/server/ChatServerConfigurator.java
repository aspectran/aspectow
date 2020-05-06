package club.textchat.server;

import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.web.socket.jsr356.AspectranConfigurator;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ChatServerConfigurator extends AspectranConfigurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {
        super.modifyHandshake(config, request, response);
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        if (httpSession != null) {
            UserInfo userInfo = (UserInfo)httpSession.getAttribute(UserManager.USER_INFO_SESSION_KEY);
            if (userInfo != null) {
                config.getUserProperties().put(ChatService.USERNAME_PROP, userInfo.getUsername());
                config.getUserProperties().put(ChatService.PREV_USERNAME_PROP, userInfo.getPrevUsername());
                config.getUserProperties().put(ChatService.HTTP_SESSION_ID, httpSession.getId());
            }
        }
    }

}
