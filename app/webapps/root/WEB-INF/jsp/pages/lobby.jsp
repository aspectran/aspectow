<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-common.css?v0.13" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-lobby.css?v0.2" />
<link rel="stylesheet" type="text/css" href="/assets/css/page-lobby.css?v12" />
<script src="/assets/js/chat-client-default.js?v0.11"></script>
<script src="/assets/js/chat-client-lobby.js?v0.2"></script>
<script src="https://www.google.com/recaptcha/api.js?render=explicit&onload=loadCaptcha"></script>
<script src="/assets/js/recaptcha.js?v1"></script>
<script src="/assets/js/page-lobby.js?v0.1"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/lobby/",
        autoConnectEnabled: true,
        admissionToken: "${page.token}",
        homepage: "/"
    }
</script>
<div class="grid-y grid-frame">
    <div class="header cell cell-block-container">
        <div class="grid-x">
            <div class="cell auto align-self-middle">
                <button type="button" class="button people" title="People">
                    <i class="iconfont fi-results-demographics"></i>
                    <span id="totalPeople">0</span></button>
                <h1>Text Chat Club</h1>
            </div>
            <div class="cell shrink align-self-middle text-right">
                <button type="button" class="button signout" title="Sign out">Sign out</button>
            </div>
        </div>
    </div>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <ul id="contacts"></ul>
            </div>
            <div class="convo-container cell auto cell-block-y">
                <div class="grid-container full-height">
                    <%@ include file="includes/lobby-user.jsp" %>
                    <div class="rooms grid-x grid-margin-x grid-margin-y t10">
                        <div class="cell medium-12 large-8 card random guide small-order-1 large-order-2">
                            <div class="card-section">
                                <h5>Welcome to Text Chat Club.</h5>
                                You must be 18+ to start random chat with strangers.<br/>
                                How To Chat With Strangers Safely?<br/>
                                Don't share any personal information or contacts, don't send any money to strangers.
                            </div>
                        </div>
                        <div class="cell medium-12 large-4 card room random small-order-2 large-order-1">
                            <div class="card-section">
                                <a href="/rooms/random"><h4>Random Chat</h4></a>
                                <p>Best place to talk to strangers</p>
                                <a class="button small start" href="/rooms/random">Start random chat</a>
                            </div>
                        </div>
                        <div class="cell medium-12 large-8 card create guide small-order-4 show-for-medium">
                            <div class="card-section">
                                <h5>Would you like to create a new chat room?</h5>
                                You can talk freely in chat rooms of people with similar interests.<br/>
                                If no one is in the chat room, it is automatically discarded after a certain period of time.<br/>
                                It is also a good idea to join the chat room already created below.
                            </div>
                        </div>
                        <div class="cell medium-12 large-4 card room create small-order-3">
                            <div class="card-section">
                                <a data-open="lobby-room-create"><h4>New Chatroom</h4></a>
                                <p>Meeting people with similar interests</p>
                                <a class="button small start room-create">Create chatroom</a>
                            </div>
                        </div>
                    </div>
                    <div class="rooms public grid-x grid-margin-x grid-margin-y t10">
                    <c:forEach items="${page.rooms}" var="roomInfo">
                        <div class="cell small-12 medium-6 large-4 card room<c:if test="${roomInfo.currentUsers gt 0}"> active</c:if>">
                            <div class="card-section">
                                <a href="/rooms/${roomInfo.encryptedRoomId}"><h5>${roomInfo.roomName}</h5></a>
                                <i class="curr-users fi-torsos-all"> ${roomInfo.currentUsers}</i>
                                <c:if test="${roomInfo.pastDays le 1}"><i class="new fi-burst-new"></i></c:if>
                                <a class="button small start" href="/rooms/${roomInfo.encryptedRoomId}">Start chat</a>
                            </div>
                        </div>
                    </c:forEach>
                        <div class="new-room-template cell small-12 medium-6 large-4 card room" style="display: none">
                            <div class="card-section">
                                <a><h5></h5></a>
                                <i class="curr-users fi-torsos-all"> 0</i>
                                <i class="new fi-burst-new"></i>
                                <a class="button small start">Start chat</a>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="convo">
                </div>
            </div>
        </div>
    </div>
    <div class="footer shadow cell">
        <div class="grid-x grid-padding-x grid-padding-y">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <%@ include file="includes/sidebar-user.jsp" %>
            </div>
            <div class="message-box cell auto cell-block-y">
                <form id="send-message">
                    <div class="input-group">
                        <input id="message" class="input-group-field" type="text" autocomplete="off" placeholder="Enter your message"/>
                        <input id="for-automata-clear" type="text"/>
                        <div class="input-group-button">
                            <button type="submit" class="button send" title="Send message"><i class="icon-paper-plane"></i></button>
                            <button type="button" class="button quiet" title="Neither read nor send messages"><i class="fi-pause"></i></button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<%@ include file="includes/lobby-not-available.jsp" %>
<%@ include file="includes/lobby-room-create.jsp" %>
<%@ include file="includes/lobby-room-create-complete.jsp" %>
<%@ include file="includes/common-connection-lost.jsp" %>
<%@ include file="includes/chat-duplicate-join.jsp" %>

