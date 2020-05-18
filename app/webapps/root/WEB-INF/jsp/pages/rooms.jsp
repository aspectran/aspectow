<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<link rel="stylesheet" type="text/css" href="/assets/css/rooms.css?v11" />
<script src="https://www.google.com/recaptcha/api.js?render=explicit&onload=loadCaptcha"></script>
<script src="/assets/js/recaptcha.js?v1"></script>
<script src="/assets/js/rooms.js?v0.1"></script>
<div class="grid-y grid-frame">
    <div class="header cell cell-block-container">
        <div class="grid-x">
            <div class="cell auto align-self-middle">
                <h1><i class="fi-results-demographics"></i> Text Chat Club</h1>
            </div>
            <div class="cell shrink align-self-middle text-right">
                <button type="button" class="button signout" title="Sign out">Sign out</button>
            </div>
        </div>
    </div>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                <%@ include file="includes/sidebar-user.jsp" %>
                <div class="side-menu"></div>
            </div>
            <div class="cell auto cell-block-y">
                <div class="grid-container">
                    <div class="rooms grid-x grid-margin-x grid-margin-y t10">
                        <div class="cell medium-12 large-4 card room random">
                            <div class="card-section">
                                <a href="/rooms/random"><h4>Random Chat</h4></a>
                                <p>Best place to talk to strangers</p>
                                <a class="button small start" href="/rooms/random">Start random chat</a>
                            </div>
                        </div>
                        <div class="cell medium-12 large-8 card random guide show-for-medium">
                            <div class="card-section">
                                <h5>Welcome to Text Chat Club.</h5>
                                You must be 18+ to start random chat with strangers.<br/>
                                How To Chat With Strangers Safely?<br/>
                                Don't share any personal information or contacts, don't send any money to strangers.
                            </div>
                        </div>
                        <div class="cell medium-12 large-4 card room create">
                            <div class="card-section">
                                <a data-open="rooms-room-create"><h4>New Chatroom</h4></a>
                                <p>Meeting people with similar interests</p>
                                <a class="button small start" data-open="rooms-room-create">Create chatroom</a>
                            </div>
                        </div>
                        <div class="cell medium-12 large-8 card create guide show-for-medium">
                            <div class="card-section">
                                <h5>Would you like to create a new chat room?</h5>
                                You can talk freely in chat rooms of people with similar interests.<br/>
                                If no one is in the chat room, it is automatically discarded after a certain period of time.<br/>
                                It is also a good idea to join the chat room already created below.
                            </div>
                        </div>
                    </div>
                    <div class="rooms grid-x grid-margin-x grid-margin-y t10">
                    <c:forEach items="${page.rooms}" var="roomInfo">
                        <div class="cell small-12 medium-6 large-4 card room">
                            <div class="card-section">
                                <a href="/rooms/${roomInfo.encryptedRoomId}"><h5>${roomInfo.roomName}</h5></a>
                                <p></p>
                                <a class="button small start" href="/rooms/${roomInfo.encryptedRoomId}">Start chat</a>
                            </div>
                        </div>
                    </c:forEach>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<%@ include file="includes/rooms-room-create.jsp" %>
<%@ include file="includes/rooms-room-create-complete.jsp" %>