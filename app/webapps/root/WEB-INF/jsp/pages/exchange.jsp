<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<aspectran:token type="property" expression="languages" var="languages"/>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-core.css?25" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-stranger.css?6" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-exchange.css?6" />
<script src="/assets/js/chat-client-core.js?v25"></script>
<script src="/assets/js/chat-client-stranger.js?v6"></script>
<script src="/assets/js/chat-client-exchange.js?v6"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/exchange/",
        admissionToken: "",
        autoConnectEnabled: false,
        pingPerHeartbeats: 15,
        homepage: "/"
    }
    const chatClientMessages = {
        "userJoined": "<aspectran:message code='chat.exchange.user_joined'/>",
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
                    <div class="language-settings grid-x">
                        <div class="cell grid-x grid-padding-x">
                            <div class="cell guide">
                                <p><aspectran:message code='chat.exchange.guide'/></p>
                            </div>
                            <div class="cell small-12 medium-6 large-4">
                                <label><aspectran:message code="chat.exchange.native_language"/>
                                    <select name="native_lang">
                                        <option value=""></option>
                                        <c:forEach items="${languages}" var="entry">
                                            <option value="${entry.key}">${entry.value}</option>
                                        </c:forEach>
                                    </select>
                                </label>
                            </div>
                            <div class="cell small-12 medium-6 large-4">
                                <label><aspectran:message code="chat.exchange.practice_language"/>
                                    <select name="convo_lang">
                                        <option value=""></option>
                                        <c:forEach items="${languages}" var="entry">
                                            <option value="${entry.key}">${entry.value}</option>
                                        </c:forEach>
                                    </select>
                                </label>
                            </div>
                            <div class="cell small-12 medium-4 large-4 small-order-2 large-order-1 align-self-bottom text-right">
                                <button type="button" class="button small ok"><aspectran:message code="common.button.ok"/></button>
                            </div>
                            <div class="cell medium-8 large-12 small-order-1 large-order-2">
                                <p class="form-error exchange-languages-required"><aspectran:message code="chat.exchange.error.exchange_languages_required"/></p>
                                <p class="form-error same-exchange-languages"><aspectran:message code="chat.exchange.error.same_exchange_languages"/></p>
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