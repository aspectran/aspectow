<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<link rel="stylesheet" type="text/css" href="/assets/css/chat-common.css?v0.12" />
<link rel="stylesheet" type="text/css" href="/assets/css/chat-random.css?v0.11" />
<script src="/assets/js/chat-client-default.js?v0.10"></script>
<script src="/assets/js/chat-client-random.js?v0.10"></script>
<script>
    const chatClientSettings = {
        serverEndpoint: "/chat/random/",
        autoConnectEnabled: false,
        userNo: Number("${page.userNo}"),
        username: "${page.username}",
        token: "${page.token}",
        homepage: "/rooms"
    }
</script>
<div class="grid-y grid-frame random">
    <div class="header cell cell-block-container">
        <div class="grid-x">
            <div class="cell auto align-self-middle">
                <button type="button" class="button people" title="People">
                    <i class="iconfont fi-results-demographics"></i>
                    <span id="totalPeople">0</span></button>
                <h2 class="text-truncate">Chat with random strangers</h2>
            </div>
            <div class="cell shrink align-self-middle text-right">
                <button type="button" class="button leave" title="Leave this chat room"><i class="iconfont fi-power"></i></button>
            </div>
        </div>
    </div>
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
                <form id="send-message">
                    <div class="input-group">
                        <input id="message" class="input-group-field" type="text" autocomplete="off" placeholder="Enter your message"/>
                        <input id="for-automata-clear" type="text"/>
                        <div class="input-group-button">
                            <button type="submit" class="button send" title="Send message"><i class="icon-paper-plane"></i></button>
                            <button type="button" class="button next" title="Search for another stranger"><i class="fi-shuffle"></i> Next <i class="fi-torso"></i></button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<%@ include file="includes/chat-reveals.jsp" %>
<%@ include file="includes/common-notice-popup.jsp" %>