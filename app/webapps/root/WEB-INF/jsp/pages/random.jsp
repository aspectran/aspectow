<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-common.css?v20" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-random.css?v19" />
<script src="/assets/js/chat-client-default.js?v21"></script>
<script src="/assets/js/chat-client-random.js?v19"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/random/",
        admissionToken: "${page.token}",
        autoConnectEnabled: false,
        pingPerHeartbeats: 15,
        homepage: "/"
    }
    const chatClientMessages = {
        "welcome": "<aspectran:message code='chat.common.welcome'/>",
        "wait": "<aspectran:message code='chat.random.wait'/>",
        "looking": "<aspectran:message code='chat.random.looking'/>",
        "searchAnother": "<aspectran:message code='chat.random.button.search_another'/>",
        "cancel": "<aspectran:message code='common.button.cancel'/>",
        "userJoined": "<aspectran:message code='chat.random.user_joined'/>",
        "userLeft": "<aspectran:message code='chat.random.user_left'/>",
        "systemError": "<aspectran:message code='chat.common.system_error'/>",
        "serviceNotAvailable": "<aspectran:message code='chat.common.service_not_available'/>"
    }
</script>
<div class="grid-y grid-frame random">
    <%@ include file="includes/chat-header.jsp" %>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <ul id="contacts"></ul>
            </div>
            <div class="cell auto cell-block-y">
                <div id="convo" class="grid-container full-height"></div>
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
                        <input id="message" class="input-group-field" type="text" maxlength="150" autocomplete="off" placeholder="<aspectran:message code='chat.common.enter_message'/>"/>
                        <input id="for-automata-clear" type="text"/>
                        <div class="input-group-button">
                            <button type="submit" class="button send" title="<aspectran:message code='chat.common.send_message'/>">
                                <i class="icon-paper-plane"></i></button>
                            <button type="button" class="button next" title="<aspectran:message code='chat.random.button.search_another'/>">
                                <i class="fi-shuffle"></i> <aspectran:message code='chat.random.next'/> <i class="fi-torso"></i></button>
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