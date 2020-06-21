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
        "userJoined": "<aspectran:message code='chat.common.user_joined'/>",
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
                    <div class="selection-info grid-x grid-margin-x t10">
                        <div class="cell show-for-small-only">
                            <i class="iconfont fi-arrow-up"></i>
                            <p>위 버튼을 클릭하면 낯선 사람을 선택할 수 있습니다.</p>
                        </div>
                        <div class="cell show-for-medium">
                            <i class="iconfont fi-arrow-left"></i>
                            <p>낯선 사람을 선택하세요.</p>
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