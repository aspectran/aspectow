package club.textchat.server;

import club.textchat.user.UserInfo;
import club.textchat.user.UserManager;
import com.aspectran.web.socket.jsr356.AspectranConfigurator;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

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
                TalkerInfo talkerInfo = new TalkerInfo();
                talkerInfo.setUsername(userInfo.getUsername());
                talkerInfo.setPrevUsername(userInfo.getPrevUsername());
                talkerInfo.setHttpSessionId(httpSession.getId());
                config.getUserProperties().put(TalkerInfo.TALKER_INFO_PROP, talkerInfo);
            }
        }
    }

}
