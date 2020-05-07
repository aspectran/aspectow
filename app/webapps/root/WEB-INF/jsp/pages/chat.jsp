<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="grid-y grid-frame">
    <div class="header cell cell-block-container">
        <div class="grid-x grid-padding-x">
            <div class="cell auto align-self-middle">
                <button type="button" class="button people" title="People">
                    <i class="fi-results-demographics"></i>
                    <span id="totalPeople">0</span></button>
                <h2>Chat</h2>
            </div>
            <div class="cell shrink align-self-middle text-right">
                <button type="button" class="button leave" title="Leave the chat room"><i class="fi-power"></i></button>
            </div>
        </div>
    </div>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                <div id="contacts"></div>
            </div>
            <div class="cell auto cell-block-y">
                <div id="conversations" class="grid-container full-height"></div>
            </div>
        </div>
    </div>
    <div class="footer shadow cell">
        <div class="grid-x grid-padding-x grid-padding-y full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                <%@ include file="includes/footer-sidebar-user.jsp" %>
            </div>
            <div class="bottom cell auto cell-block-y">
                <form id="send-message">
                    <div class="input-group">
                        <input class="input-group-field" type="text" id="message" autocomplete="off" placeholder="Type a message..."/>
                        <div class="input-group-button">
                            <button type="submit" class="button">Send</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<div id="connection-lost" class="reveal" data-reveal>
    <h1>Connection lost</h1>
    <p class="lead">You have lost connection with the server.</p>
    <div class="button-group align-right">
        <a class="success button" href="/">Home</a>
        <a class="warning button" href="">Reload this page</a>
    </div>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>
<div id="chatroom-rejoined" class="reveal" data-reveal>
    <h1>Alert</h1>
    <p class="lead">You leave this chat room because you reconnected through a different route.</p>
    <p>Note: Duplicate participation in the same chat room is prohibited.</p>
    <div class="button-group align-right">
        <a class="success button" href="/lobby">Ok</a>
    </div>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>
<script src="/assets/js/chat.js"></script>
<script>
    const currentUser = "${page.username}";
    const admissionToken = "${page.admissionToken}";
</script>