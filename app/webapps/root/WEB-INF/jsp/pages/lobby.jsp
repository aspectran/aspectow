<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-common.css?v21" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-lobby.css?v19" />
<link rel="stylesheet" type="text/css" href="/assets/css/page-lobby.css?v19" />
<script src="/assets/js/chat-client-default.js?v21"></script>
<script src="/assets/js/chat-client-lobby.js?v24"></script>
<script src="/assets/js/page-lobby.js?v20"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/lobby/",
        admissionToken: "${page.token}",
        autoConnectEnabled: true,
        pingPerHeartbeats: 15,
        homepage: "/"
    }
    const chatClientMessages = {
        "userJoined": "<aspectran:message code='chat.lobby.user_joined'/>",
        "roomCreated": "<aspectran:message code='chat.lobby.room_created'/>",
        "systemError": "<aspectran:message code='chat.common.system_error'/>",
        "serviceNotAvailable": "<aspectran:message code='chat.common.service_not_available'/>"
    }
</script>
<div class="grid-y grid-frame">
    <%@ include file="includes/chat-header.jsp" %>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <c:if test="${empty user}">
                    <div class="text-center t30">
                        <img src="/assets/images/textchat-heartbeat.svg" width="70%" alt="Text Chat Club"/>
                    </div>
                    <div class="text-center t10">
                        <a href="?locale=en">English</a>
                    </div>
                    <div class="text-center t5">
                        <a href="?locale=ko">한국어</a>
                    </div>
                </c:if>
                <ul id="contacts"></ul>
            </div>
            <div class="convo-container cell auto cell-block-y">
                <div class="grid-container full-height">
                    <c:if test="${not empty user}">
                        <%@ include file="includes/lobby-user.jsp" %>
                    </c:if>
                    <div class="grid-x grid-margin-x grid-margin-y grid-padding-x grid-padding-y t15">
                        <div class="cell service random">
                            <a class="start" href="/random"><h4><aspectran:message code="lobby.random_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="lobby.random_chat.teaser"/></p>
                            <p class="guide show-for-large"><aspectran:message code="lobby.random_chat.guide"/></p>
                            <p class="dummy hide-for-large"></p>
                            <a class="button small start" href="/random"><aspectran:message code="lobby.random_chat.button.start"/></a>
                        </div>
                        <div class="cell service private create">
                            <a class="private-room-create"><h4><aspectran:message code="lobby.private_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="lobby.private_chat.teaser"/></p>
                            <p class="guide show-for-large"><aspectran:message code="lobby.private_chat.guide"/></p>
                            <p class="dummy hide-for-large"></p>
                            <a class="button small start private-room-create" title="Create a private chat room"><aspectran:message code="lobby.private_chat.button.create"/></a>
                        </div>
                        <div class="cell service public create">
                            <a class="public-room-create"><h4><aspectran:message code="lobby.public_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="lobby.public_chat.teaser"/></p>
                            <p class="guide show-for-large"><aspectran:message code="lobby.public_chat.guide"/></p>
                            <p class="dummy hide-for-large"></p>
                            <a class="button small start public-room-create" title="Create an open chat room"><aspectran:message code="lobby.public_chat.button.create"/></a>
                        </div>
                    </div>
                    <div class="grid-x grid-margin-y">
                        <div class="cell">
                            <h6 class="bar"><aspectran:message code="lobby.rooms.title"/>
                                <a class="refresh-rooms float-right"><i class="fi-refresh"> <aspectran:message code="lobby.rooms.button.refresh"/></i></a></h6>
                        </div>
                    </div>
                    <div class="rooms public grid-x grid-margin-x grid-margin-y b15">
                    <c:forEach items="${page.rooms}" var="roomInfo">
                        <div class="cell small-12 medium-6 large-4 card room<c:if test="${roomInfo.currentUsers gt 0}"> active</c:if>">
                            <div class="card-section">
                                <a class="start" href="/rooms/${roomInfo.roomId}"><h5>${roomInfo.roomName}</h5></a>
                                <i class="curr-users fi-torsos-all"> <span>${roomInfo.currentUsers}</span></i>
                                <c:if test="${roomInfo.pastDays le 1}"><i class="new fi-burst-new"></i></c:if>
                                <a class="button small start" href="/rooms/${roomInfo.roomId}"><aspectran:message code="lobby.rooms.button.enter_chatroom"/></a>
                            </div>
                        </div>
                    </c:forEach>
                        <div class="new-room-template cell small-12 medium-6 large-4 card room" style="display: none">
                            <div class="card-section">
                                <a class="start"><h5></h5></a>
                                <i class="curr-users fi-torsos-all"> <span>0</span></i>
                                <i class="new fi-burst-new" style="display: none"></i>
                                <a class="button small start"><aspectran:message code="lobby.rooms.button.enter_chatroom"/></a>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="convo">
                </div>
            </div>
        </div>
    </div>
<c:if test="${not empty user}">
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
</c:if>
</div>
<%@ include file="includes/lobby-public-room-create.jsp" %>
<%@ include file="includes/lobby-public-room-create-complete.jsp" %>
<%@ include file="includes/lobby-private-room-create.jsp" %>
<%@ include file="includes/lobby-private-room-create-complete.jsp" %>
<%@ include file="includes/chat-duplicate-join.jsp" %>
<%@ include file="includes/common-wait-popup.jsp" %>
<%@ include file="includes/common-connection-lost.jsp" %>
<c:if test="${empty user}">
    <%@ include file="includes/common-sign-in.jsp" %>
</c:if>