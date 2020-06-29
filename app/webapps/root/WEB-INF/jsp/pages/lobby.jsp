<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<aspectran:token type="property" expression="languages" var="languages"/>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-core.css?v26"/>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-lobby.css?v22"/>
<link rel="stylesheet" type="text/css" href="/assets/css/page-lobby.css?v29"/>
<script src="/assets/js/chat-client-core.js?v27"></script>
<script src="/assets/js/chat-client-lobby.js?v27"></script>
<script src="/assets/js/page-lobby.js?v28"></script>
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
                    <div class="text-center t30 b20">
                        <img src="/assets/images/textchat-heartbeat.svg" width="70%" alt="<aspectran:message code='site.title'/>"/>
                    </div>
                    <div class="text-center">
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
                    <div class="services grid-x grid-margin-x grid-margin-y grid-padding-x grid-padding-y t15">
                        <div class="cell service exchange">
                            <a class="title start" href="/exchange"><h4><aspectran:message code="service.exchange_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="service.exchange_chat.feature"/></p>
                            <p class="guide show-for-large"><aspectran:message code="service.exchange_chat.guide"/></p>
                            <p class="dummy"></p>
                            <a class="guide hide-for-large" title="<aspectran:message code="service.exchange_chat.button.guide"/>"><i class="fi-info"></i></a>
                            <div class="service-options">
                                <a class="button small wide start" href="/exchange"><aspectran:message code="service.exchange_chat.button.start"/></a>
                            </div>
                        </div>
                        <div class="cell large-6 service random">
                            <a class="title start" href="/random"><h4><aspectran:message code="service.random_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="service.random_chat.feature"/></p>
                            <p class="guide show-for-large"><aspectran:message code="service.random_chat.guide"/></p>
                            <p class="dummy"></p>
                            <a class="guide hide-for-large" title="<aspectran:message code="service.random_chat.button.guide"/>"><i class="fi-info"></i></a>
                            <div class="service-options">
                                <select class="convo-lang" name="convo_lang">
                                    <option value=""><aspectran:message code="service.random_chat.any_language"/></option>
                                    <c:forEach items="${languages}" var="entry">
                                        <option value="${entry.key}">${entry.value}</option>
                                    </c:forEach>
                                </select>
                                <a class="button small start" href="/random"><aspectran:message code="service.random_chat.button.start"/></a>
                            </div>
                        </div>
                        <div class="cell large-6 service strangers">
                            <a class="title start" href="/strangers"><h4><aspectran:message code="service.stranger_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="service.stranger_chat.feature"/></p>
                            <p class="guide show-for-large"><aspectran:message code="service.stranger_chat.guide"/></p>
                            <p class="dummy"></p>
                            <a class="guide hide-for-large" title="<aspectran:message code="service.stranger_chat.button.guide"/>"><i class="fi-info"></i></a>
                            <div class="service-options">
                                <a class="button small start" href="/strangers"><aspectran:message code="service.stranger_chat.button.start"/></a>
                            </div>
                        </div>
                        <div class="cell large-6 service private create">
                            <a class="title private-room-create"><h4><aspectran:message code="service.private_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="service.private_chat.feature"/></p>
                            <p class="guide show-for-large"><aspectran:message code="service.private_chat.guide"/></p>
                            <p class="dummy"></p>
                            <a class="guide hide-for-large" title="<aspectran:message code="service.private_chat.button.guide"/>"><i class="fi-info"></i></a>
                            <div class="service-options">
                                <a class="button small wide start private-room-create <aspectran:message code="site.lang"/>"><aspectran:message code="service.private_chat.button.create"/></a>
                            </div>
                        </div>
                        <div class="cell large-6 service public create">
                            <a class="title public-room-create"><h4><aspectran:message code="service.public_chat"/></h4></a>
                            <p class="feature"><aspectran:message code="service.public_chat.feature"/></p>
                            <p class="guide show-for-large"><aspectran:message code="service.public_chat.guide"/></p>
                            <p class="dummy"></p>
                            <a class="guide hide-for-large" title="<aspectran:message code="service.public_chat.button.guide"/>"><i class="fi-info"></i></a>
                            <div class="service-options">
                                <a class="button small start public-room-create <aspectran:message code="site.lang"/>"><aspectran:message code="service.public_chat.button.create"/></a>
                            </div>
                        </div>
                    </div>
                    <div class="rooms-options grid-x small-up-2 t15 b10">
                        <div class="cell align-self-middle">
                            <h6><aspectran:message code="lobby.rooms.title"/>
                            <a class="refresh-rooms"><i class="fi-refresh"> <aspectran:message code="lobby.rooms.button.refresh"/></i></a></h6>
                        </div>
                        <div class="cell align-self-bottom text-right">
                            <select class="room-lang" name="room_lang">
                                <c:forEach items="${languages}" var="entry">
                                    <option value="${entry.key}">${entry.value}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="rooms grid-x grid-margin-x grid-margin-y medium-up-2 large-up-3 b15">
                        <div class="cell room template">
                            <a class="title start"><h5></h5></a>
                            <div class="room-info">
                                <i class="curr-users fi-torsos-all"> <span>0</span></i>
                                <span class="lang" data-lang-cd=""></span>
                            </div>
                            <i class="new fi-burst-new"></i>
                            <a class="button small start"><aspectran:message code="lobby.rooms.button.enter_chatroom"/></a>
                        </div>
                    </div>
                </div>
                <div id="convo"></div>
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