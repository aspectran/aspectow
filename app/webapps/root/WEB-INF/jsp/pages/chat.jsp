<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="grid-y grid-frame">
    <div class="header cell cell-block-container">
        <div class="grid-x grid-padding-x">
            <div class="cell auto align-self-middle">
                <button type="button" class="button people" title="People">
                    <i class="iconfont fi-results-demographics"></i>
                    <span id="totalPeople">0</span></button>
                <h2>Room ${page.roomName}</h2>
            </div>
            <div class="cell shrink align-self-middle text-right">
                <button type="button" class="button leave" title="Leave the chat room"><i class="iconfont fi-power"></i></button>
            </div>
        </div>
    </div>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                <ul id="contacts"></ul>
            </div>
            <div class="cell auto cell-block-y">
                <div id="conversations" class="grid-container full-height"></div>
            </div>
        </div>
    </div>
    <div class="footer shadow cell">
        <div class="grid-x grid-padding-x grid-padding-y">
            <div class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                <%@ include file="includes/footer-sidebar-user.jsp" %>
            </div>
            <div class="bottom cell auto cell-block-y">
                <form id="send-message">
                    <div class="input-group">
                        <input id="message" class="input-group-field" type="text" autocomplete="off" placeholder="Type a message..."/>
                        <input id="for-automata-clear" type="text" style="width:0;height:0;padding:0;margin:0;border:none;"/>
                        <div class="input-group-button">
                            <button type="submit" class="button send">Send</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<div id="connection-lost" class="reveal" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h1>Connection lost</h1>
    <p class="lead">You have lost connection with the server.</p>
    <div class="button-group align-right">
        <a class="success button" href="/rooms">Home</a>
        <a class="warning button" href="">Reload this page</a>
    </div>
</div>
<div id="chatroom-rejoined" class="reveal" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h1>Alert</h1>
    <p class="lead">You leave this chat room because you reconnected through a different route.</p>
    <p>Note: Duplicate participation in the same chat room is prohibited.</p>
    <div class="button-group align-right">
        <a class="success button" href="/rooms">Ok</a>
    </div>
</div>
<script src="/assets/js/chat.js?v0.5"></script>
<script>
    const currentUser = "${page.username}";
    const admissionToken = "${page.token}";
</script>