<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div id="connection-lost" class="reveal popup error" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Connection lost</h3>
    <p class="lead">You have lost connection with the server.</p>
    <div class="button-group align-right">
        <a class="success button" href="/rooms">Home</a>
        <a class="warning button" href="">Reload this page</a>
    </div>
</div>
<div id="chatroom-rejoined" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Alert</h3>
    <p class="lead">You leave this chat room because you reconnected through a different route.</p>
    <p>Note: Duplicate participation in the same chat room is prohibited.</p>
    <div class="button-group align-right">
        <a class="success button" href="/rooms">OK</a>
    </div>
</div>