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
                ChaterInfo chaterInfo = new ChaterInfo(userInfo);
                chaterInfo.setHttpSessionId(httpSession.getId());
                config.getUserProperties().put(ChaterInfo.CHATER_INFO_PROP, chaterInfo);
            }
        }
    }

}
