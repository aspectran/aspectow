<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div id="connection-lost" class="reveal popup error" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Connection lost</h3>
    <div class="grid-x grid-margin-x grid-margin-y">
        <div class="cell">
            <p>You have lost connection with the server.</p>
        </div>
        <div class="cell broken">
            <span>===//===</span>
        </div>
        <div class="cell text-right">
            <a class="success button" href="/rooms">Home</a>
            <a class="warning button" href="">Refresh this page</a>
        </div>
    </div>
</div>
<div id="duplicate-join" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Oops!</h3>
    <div class="grid-x grid-margin-x grid-margin-y">
        <div class="cell">
            <p>You leave this chat room because you reconnected through a different route.</p>
            <p>Note: Duplicate participation in the same chat room is prohibited.</p>
        </div>
        <div class="cell text-right">
            <a class="secondary button" href="/rooms">OK</a>
        </div>
    </div>
</div>