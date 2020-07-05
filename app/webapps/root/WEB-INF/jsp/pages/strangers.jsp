<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-core.css?31"/>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-stranger.css?9"/>
<script src="/assets/js/chat-client-core.js?v31"></script>
<script src="/assets/js/chat-client-stranger.js?v8"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/stranger/",
        admissionToken: "${page.token}",
        autoConnectEnabled: true,
        pingPerHeartbeats: 9,
        homepage: "/"
    };
    const chatClientMessages = {
        "userJoined": "<aspectran:message code='chat.stranger.user_joined'/>",
        "userLeft": "<aspectran:message code='chat.common.user_left'/>",
        "systemError": "<aspectran:message code='chat.common.system_error'/>",
        "serviceNotAvailable": "<aspectran:message code='chat.common.service_not_available'/>"
    };
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
                    <div class="choose-info grid-x show-for-small-only">
                        <div class="cell grid-x grid-padding-x">
                            <div class="cell">
                                <i class="iconfont fi-arrow-up"></i>
                                <p><aspectran:message code='chat.stranger.choose_info_small'/></p>
                            </div>
                        </div>
                    </div>
                    <div class="choose-info grid-x show-for-medium">
                        <div class="cell grid-x grid-padding-x">
                            <div class="cell">
                                <i class="iconfont fi-arrow-left"></i>
                                <p><aspectran:message code='chat.stranger.choose_info_large'/></p>
                            </div>
                        </div>
                    </div>
                    <%@ include file="includes/chat-requests.jsp" %>
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