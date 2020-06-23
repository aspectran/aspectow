<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-core.css?21" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-stranger.css?1" />
<script src="/assets/js/chat-client-core.js?v21"></script>
<script src="/assets/js/chat-client-stranger.js?v1"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/stranger/",
        admissionToken: "${page.token}",
        autoConnectEnabled: true,
        pingPerHeartbeats: 15,
        homepage: "/"
    }
    const chatClientMessages = {
        "userJoined": "<aspectran:message code='chat.stranger.user_joined'/>",
        "userLeft": "<aspectran:message code='chat.common.user_left'/>",
        "systemError": "<aspectran:message code='chat.common.system_error'/>",
        "serviceNotAvailable": "<aspectran:message code='chat.common.service_not_available'/>"
    }
</script>
<div class="grid-y grid-frame">
    <%@ include file="includes/chat-header.jsp" %>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <ul id="contacts"></ul>
            </div>
            <div class="convo-container cell auto cell-block-y">
                <div class="grid-container full-height">
                    <div class="choose-info grid-x grid-margin-x show-for-small-only">
                        <div class="cell">
                            <i class="iconfont fi-arrow-up"></i>
                            <p><aspectran:message code='chat.stranger.choose_info_small'/></p>
                        </div>
                    </div>
                    <div class="choose-info grid-x grid-margin-x show-for-medium">
                        <div class="cell">
                            <i class="iconfont fi-arrow-left"></i>
                            <p><aspectran:message code='chat.stranger.choose_info_large'/></p>
                        </div>
                    </div>
                    <div class="chat-requests grid-x grid-margin-x t15">
                        <div class="confirm-request template cell grid-x medium-up-1 large-up-2">
                            <div class="cell">
                                <p class="title"><aspectran:message code='chat.stranger.confirm_request'/></p>
                            </div>
                            <div class="cell align-self-bottom text-right">
                                <button type="button" class="button small alert cancel"><aspectran:message code="common.button.cancel"/></button>
                                <button type="button" class="button small success ok"><aspectran:message code="common.button.ok"/></button>
                            </div>
                        </div>
                        <div class="request active template cell grid-x medium-up-1 large-up-2">
                            <div class="cell">
                                <p class="title"><aspectran:message code='chat.stranger.request'/></p>
                                <p><i class="icon-heart"></i> <aspectran:message code='chat.stranger.time_left'/>: <span class="remains">35</span><aspectran:message code='chat.stranger.time_left_secs'/></p>
                                <p class="done"><aspectran:message code='chat.stranger.request_done'/></p>
                            </div>
                            <div class="cell align-self-bottom text-right">
                                <button type="button" class="button small alert cancel"><aspectran:message code="common.button.cancel"/></button>
                            </div>
                        </div>
                        <div class="request-received active template cell grid-x medium-up-1 large-up-2">
                            <div class="cell">
                                <p class="title"><aspectran:message code='chat.stranger.request_received'/></p>
                                <p><aspectran:message code='chat.stranger.request_received_1'/></p>
                                <p><i class="icon-heart"></i> <aspectran:message code='chat.stranger.time_left'/>: <span class="remains">30</span><aspectran:message code='chat.stranger.time_left_secs'/></p>
                                <p class="done"><aspectran:message code='chat.stranger.request_done'/></p>
                            </div>
                            <div class="cell align-self-bottom text-right">
                                <button type="button" class="button small alert decline"><aspectran:message code='common.button.decline'/></button>
                                <button type="button" class="button small success accept"><aspectran:message code='common.button.accept'/></button>
                            </div>
                        </div>
                        <div class="canceled-request template cell grid-x">
                            <div class="cell auto">
                                <p class="title"><aspectran:message code='chat.stranger.canceled_request'/></p>
                            </div>
                        </div>
                        <div class="request-canceled template cell grid-x">
                            <div class="cell auto">
                                <p class="title"><aspectran:message code='chat.stranger.request_canceled'/></p>
                            </div>
                        </div>
                        <div class="declined-request template cell grid-x">
                            <div class="cell auto">
                                <p class="title"><aspectran:message code='chat.stranger.declined_request'/></p>
                            </div>
                        </div>
                        <div class="request-declined template cell grid-x">
                            <div class="cell auto">
                                <p class="title"><aspectran:message code='chat.stranger.request_declined'/></p>
                            </div>
                        </div>
                        <div class="request-timeout template cell grid-x">
                            <div class="cell auto">
                                <p class="title"><aspectran:message code='chat.stranger.request_timeout'/></p>
                            </div>
                        </div>
                        <div class="exceeded-requests template cell grid-x">
                            <div class="cell auto">
                                <p class="title"><aspectran:message code='chat.stranger.exceeded_requests'/></p>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="convo"></div>
            </div>
        </div>
    </div>
    <div class="footer shadow cell">
        <div class="grid-x grid-padding-x grid-padding-y">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <%@ include file="includes/sidebar-user.jsp" %>
            </div>
            <div class="message-box cell auto cell-block-y">
                <form id="form-send-message">
                    <div class="input-group">
                        <input id="message" class="input-group-field" type="text" maxlength="100" autocomplete="off" placeholder="<aspectran:message code='chat.common.enter_message'/>"/>
                        <input id="for-automata-clear" type="text"/>
                        <div class="input-group-button">
                            <button type="submit" class="button send" title="<aspectran:message code='chat.common.send_message'/>"><i class="icon-paper-plane"></i></button>
                            <button type="button" class="button quiet" title="<aspectran:message code='chat.lobby.neither_read_send'/>"><i class="fi-pause"></i></button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<%@ include file="includes/chat-duplicate-join.jsp" %>
<%@ include file="includes/common-wait-popup.jsp" %>
<%@ include file="includes/common-notice-popup.jsp" %>
<%@ include file="includes/common-connection-lost.jsp" %>
<c:if test="${empty user}">
    <%@ include file="includes/common-sign-in.jsp" %>
</c:if>