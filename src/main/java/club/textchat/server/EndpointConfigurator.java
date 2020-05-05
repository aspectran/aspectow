package club.textchat.server;

import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.web.socket.jsr356.AspectranConfigurator;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class EndpointConfigurator extends AspectranConfigurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {
        super.modifyHandshake(config, request, response);
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        if (httpSession != null) {
            String httpSessionId = httpSession.getId();
            UserInfo userInfo = (UserInfo)httpSession.getAttribute(UserManager.USER_INFO_SESSION_KEY);
            config.getUserProperties().put("httpSessionId", httpSessionId);
            config.getUserProperties().put("username", userInfo.getUsername());
            config.getUserProperties().put("prevUsername", userInfo.getPrevUsername());
        }
    }

}
