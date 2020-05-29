<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-common.css?v20" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-lobby.css?v19" />
<link rel="stylesheet" type="text/css" href="/assets/css/page-lobby.css?v19" />
<script src="/assets/js/chat-client-default.js?v17"></script>
<script src="/assets/js/chat-client-lobby.js?v19"></script>
<script src="/assets/js/page-lobby.js?v17"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/lobby/",
        autoConnectEnabled: true,
        admissionToken: "${page.token}",
        homepage: "/"
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
                    <c:if test="${not empty user}">
                        <%@ include file="includes/lobby-user.jsp" %>
                    </c:if>
                    <div class="rooms grid-x grid-margin-x t15">
                        <div class="cell medium-12 large-4 card room random">
                            <div class="card-section">
                                <a class="start" href="/random"><h4>Random Chat</h4></a>
                                <p>Best place to talk to randomly selected people from all over the world</p>
                                <a class="button small start" href="/random">Start random chat</a>
                            </div>
                        </div>
                        <div class="cell medium-12 large-8 card random guide show-for-large">
                            <div class="card-section">
                                <h5>Welcome to Text Chat Club.</h5>
                                Our random chat service lets you text chat with randomly selected people.
                                To chat with strangers on the Internet is a great way to find new friends.
                                Please note that you must be 18+ to start random chats with strangers.
                            </div>
                        </div>
                        <div class="cell medium-12 large-4 card room private create">
                            <div class="card-section">
                                <a class="private-room-create"><h4>Private Chat</h4></a>
                                <p>Best place to talk only between you and the people you invite</p>
                                <a class="button small start private-room-create" title="Create a private chat room">Create chatroom</a>
                            </div>
                        </div>
                        <div class="cell medium-12 large-8 card private guide show-for-large">
                            <div class="card-section">
                                <h5>Would you like to create a new private chat room?</h5>
                                Private chat rooms are always available for free.<br/>
                                After creating a private chat room, send the URL only to those who want to chat.
                                Note that the conversation is not saved, so when you refresh the screen, the conversation disappears.
                            </div>
                        </div>
                        <div class="cell medium-12 large-4 card room public create">
                            <div class="card-section">
                                <a class="public-room-create"><h4>Public Chat</h4></a>
                                <p>Meeting people with similar interests in public chat rooms</p>
                                <a class="button small start public-room-create" title="Create an open chat room">Create chatroom</a>
                            </div>
                        </div>
                        <div class="cell medium-12 large-8 card public guide show-for-large">
                            <div class="card-section">
                                <h5>Would you like to create a new open chat room?</h5>
                                You can chat with people of similar interests by creating chat rooms.
                                If no one is in the chat room, it is automatically deleted after a certain period of time.<br/>
                                It is also a good idea to join the chat rooms already created below.
                            </div>
                        </div>
                    </div>
                    <div class="grid-x grid-margin-y">
                        <div class="cell">
                            <h6 class="bar">Most popular public chat rooms <a class="refresh-rooms float-right"><i class="fi-refresh"> Refresh</i></a></h6>
                        </div>
                    </div>
                    <div class="rooms public grid-x grid-margin-x grid-margin-y b15">
                    <c:forEach items="${page.rooms}" var="roomInfo">
                        <div class="cell small-12 medium-6 large-4 card room<c:if test="${roomInfo.currentUsers gt 0}"> active</c:if>">
                            <div class="card-section">
                                <a class="start" href="/rooms/${roomInfo.roomId}"><h5>${roomInfo.roomName}</h5></a>
                                <i class="curr-users fi-torsos-all"> <span>${roomInfo.currentUsers}</span></i>
                                <c:if test="${roomInfo.pastDays le 1}"><i class="new fi-burst-new"></i></c:if>
                                <a class="button small start" href="/rooms/${roomInfo.roomId}">Start chat</a>
                            </div>
                        </div>
                    </c:forEach>
                        <div class="new-room-template cell small-12 medium-6 large-4 card room" style="display: none">
                            <div class="card-section">
                                <a class="start"><h5></h5></a>
                                <i class="curr-users fi-torsos-all"> <span>0</span></i>
                                <i class="new fi-burst-new" style="display: none"></i>
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
<c:if test="${not empty user}">
    <div class="footer shadow cell">
        <div class="grid-x grid-padding-x grid-padding-y">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <%@ include file="includes/sidebar-user.jsp" %>
            </div>
            <div class="message-box cell auto cell-block-y">
                <form id="form-send-message">
                    <div class="input-group">
                        <input id="message" class="input-group-field" type="text" maxlength="100" autocomplete="off" placeholder="Enter your message"/>
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
</c:if>
</div>
<%@ include file="includes/lobby-public-room-create.jsp" %>
<%@ include file="includes/lobby-public-room-create-complete.jsp" %>
<%@ include file="includes/lobby-private-room-create.jsp" %>
<%@ include file="includes/lobby-private-room-create-complete.jsp" %>
<%@ include file="includes/chat-duplicate-join.jsp" %>
<%@ include file="includes/common-wait-popup.jsp" %>
<%@ include file="includes/common-connection-lost.jsp" %>
<%@ include file="includes/common-service-not-available.jsp" %>
<c:if test="${empty user}">
    <%@ include file="includes/common-sign-in.jsp" %>
</c:if>