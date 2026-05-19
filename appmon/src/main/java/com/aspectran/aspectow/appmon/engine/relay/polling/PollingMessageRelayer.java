/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.appmon.engine.relay.polling;

import com.aspectran.aspectow.appmon.engine.config.PollingConfig;
import com.aspectran.aspectow.appmon.engine.manager.AppMonManager;
import com.aspectran.aspectow.appmon.engine.relay.CommandOptions;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayManager;
import com.aspectran.aspectow.appmon.engine.relay.MessageRelayer;
import com.aspectran.aspectow.appmon.engine.relay.RelaySession;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Destroy;
import com.aspectran.core.component.bean.annotation.Initialize;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.core.component.bean.annotation.RequestToGet;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.utils.StringUtils;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_FOCUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_LOAD_PREVIOUS;
import static com.aspectran.aspectow.appmon.engine.relay.CommandOptions.COMMAND_REFRESH;
import static com.aspectran.aspectow.node.manager.NodeMessageProtocol.NODES_BASE_PATH;

/**
 * An {@link MessageRelayer} implementation based on HTTP long-polling.
 * Clients connect to join, then periodically pull for new messages.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
@Component(NODES_BASE_PATH + "/${thisNodeId}/appmon")
public class PollingMessageRelayer implements MessageRelayer {

    private final AppMonManager appMonManager;

    private final PollingSessionManager pollingSessionManager;

    @Autowired
    public PollingMessageRelayer(@NonNull AppMonManager appMonManager) {
        this.appMonManager = appMonManager;
        this.pollingSessionManager = new PollingSessionManager(appMonManager);
    }

    /**
     * Initializes the service by registering it with the {@link MessageRelayManager}.
     */
    @Initialize
    public void registerRelayer() throws Exception {
        pollingSessionManager.initialize();
        appMonManager.getMessageRelayManager().addRelayer(this);
    }

    /**
     * Destroys the service, cleaning up resources and unregistering from the manager.
     */
    @Destroy
    public void destroy() throws Exception {
        pollingSessionManager.destroy();
        appMonManager.getMessageRelayManager().removeRelayer(this);
    }

    /**
     * Allows a client to join and start a polling session.
     * @param translet the current translet
     * @return a map containing the app info, and initial messages
     * @throws IOException if an I/O error occurs
     */
    @RequestToPost("/polling/join")
    @Transform(FormatType.JSON)
    public Map<String, Object> join(@NonNull Translet translet) throws IOException {
        String nodeId = translet.getParameter("nodeId");
        if (appMonManager.getMessageRelayManager().isSameNode(nodeId)) {
            String appsToJoin = translet.getParameter("appsToJoin");
            String[] appIds = StringUtils.splitWithComma(appsToJoin);
            appIds = appMonManager.getVerifiedAppIds(appIds);

            PollingConfig pollingConfig = appMonManager.getPollingConfig();
            PollingRelaySession relaySession = pollingSessionManager.createSession(translet, pollingConfig, appIds);
            String timeZone = translet.getParameter("timeZone");
            if (StringUtils.hasText(timeZone)) {
                relaySession.setTimeZone(timeZone);
            }
            appMonManager.getMessageRelayManager().registerSession(relaySession.getId(), this);
            if (!appMonManager.getMessageRelayManager().subscribe(relaySession)) {
                return null;
            }
            return Map.of(
                    "appsToJoin", StringUtils.joinWithCommas(appIds),
                    "pollingInterval", relaySession.getPollingInterval(),
                    "nodeId", nodeId,
                    "established", true,
                    "alive", true
            );
        } else if (appMonManager.getMessageRelayManager().isGatewayMode()) {
            String nodeInfo = appMonManager.getMessageRelayManager().getNodeRegistry().getNode(nodeId);
            return Map.of(
                    "nodeId", nodeId,
                    "established", false,
                    "alive", (nodeInfo != null ? nodeId : StringUtils.EMPTY)
            );
        } else {
            return null;
        }
//
//        List<AppInfo> appInfoList = appMonManager.getAppInfoList(relaySession.getJoinedApps());
//        List<String> messages = appMonManager.getMessageRelayManager().getLastMessages(relaySession);
//        return Map.of(
//                "appsToJoin", StringUtils.joinWithCommas(verifiedAppIds),
//                "apps", appInfoList,
//                "pollingInterval", relaySession.getPollingInterval(),
//                "messages", messages
//        );
    }

    /**
     * Allows a client to pull new messages from the server.
     * @param translet the current translet
     * @param commands an array of commands from the client
     * @return a map containing the new token and any new messages
     * @throws IOException if an I/O error occurs
     */
    @RequestToGet("/polling/pull")
    @Transform(FormatType.JSON)
    public Map<String, Object> pull(
            @NonNull Translet translet, @Qualifier("commands[]") String[] commands) throws IOException {
        PollingRelaySession relaySession = pollingSessionManager.getSession(translet);
        if (relaySession == null || !relaySession.isValid()) {
            return null;
        }

        if (commands != null) {
            handleCommand(relaySession, commands);
        }

        String[] messages = pollingSessionManager.pull(relaySession);
        return Map.of(
                "messages", (messages != null ? messages : new String[0])
        );
    }

    private void handleCommand(PollingRelaySession relaySession, String[] commands) {
        CommandOptions commandOptions = new CommandOptions(commands);
        switch (commandOptions.getCommand()) {
//            case COMMAND_JOIN:
//                joinRemotely(relaySession, commandOptions);
//                break;
//            case COMMAND_ESTABLISHED:
//                established(relaySession, commandOptions);
//                break;
            case COMMAND_REFRESH:
            case COMMAND_LOAD_PREVIOUS:
                refreshData(relaySession, commandOptions);
                break;
            case COMMAND_FOCUS:
                focus(relaySession, commandOptions);
                break;
        }
    }

    private void joinRemotely(PollingRelaySession relaySession, @NonNull CommandOptions commandOptions) {
        if (appMonManager.getMessageRelayManager().isGatewayMode()) {
            String targetNodeId = commandOptions.getNodeId();
            pollingSessionManager.push(targetNodeId + "::joined:" + relaySession.getId());
        }
    }

    private void established(@NonNull PollingRelaySession relaySession, @NonNull CommandOptions commandOptions) {
        String targetNodeId = commandOptions.getNodeId();
        if (appMonManager.getNodeId().equals(targetNodeId)) {
            if (appMonManager.getMessageRelayManager().subscribe(relaySession)) {
                List<String> messages = appMonManager.getMessageRelayManager().getLastMessages(relaySession);
                for (String message : messages) {
                    pollingSessionManager.push(message);
                }
            }
        }
    }

    private void focus(@NonNull PollingRelaySession relaySession, @NonNull CommandOptions commandOptions) {
        String focusedAppId = commandOptions.getAppId();
        relaySession.setFocusedAppId(focusedAppId);
    }

    private void refreshData(@NonNull PollingRelaySession relaySession, @NonNull CommandOptions commandOptions) {
        List<String> newMessages = appMonManager.getMessageRelayManager().refreshData(relaySession, commandOptions);
        if (newMessages != null) {
            for (String message : newMessages) {
                pollingSessionManager.push(message);
            }
        }
    }

    /**
     * Adjusts the polling interval for a client session.
     * @param translet the current translet
     * @param speed the desired speed multiplier (1 for fast)
     * @return a map containing the new polling interval
     */
    @RequestToPost("/polling/interval")
    @Transform(FormatType.JSON)
    public Map<String, Object> pollingInterval(@NonNull Translet translet, int speed) {
        PollingRelaySession relaySession = pollingSessionManager.getSession(translet);
        if (relaySession == null) {
            return null;
        }

        if (speed == 1) {
            relaySession.setPollingInterval(1000);
        } else {
            PollingConfig pollingConfig = appMonManager.getPollingConfig();
            relaySession.setPollingInterval(pollingConfig.getPollingInterval());
        }

        return Map.of(
            "pollingInterval", relaySession.getPollingInterval()
        );
    }

    @Override
    public void relay(String message) {
        pollingSessionManager.push(message);
    }

    @Override
    public void relay(RelaySession relaySession, String message) {
        pollingSessionManager.push(message);
    }

    @Override
    public RelaySession findRelaySession(String sessionId) {
        return pollingSessionManager.getSession(sessionId);
    }

}
