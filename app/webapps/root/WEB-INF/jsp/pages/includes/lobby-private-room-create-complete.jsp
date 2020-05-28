<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div id="lobby-private-room-create-complete" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Chat room creation is complete</h3>
    <div class="grid-x">
        <div class="cell">
            <p>The URL of your private chat room is:</p>
        </div>
        <div class="cell card">
            <div class="card-section">
                <p class="private-chatroom-url">http://textchat.club/private/</p>
            </div>
        </div>
        <div class="cell text-right">
            <button type="button" class="button small copy-to-clipboard">Copy to clipboard</button>
        </div>
        <div class="cell text-center t10">
            <button type="button" class="button alert" title="Close" data-close>Close</button>
            <button type="button" class="success button go-created-private-room" data-close aria-label="Go to the chat room you created">Enter the chat room now <i class="iconfont fi-arrow-right"></i></button>
        </div>
    </div>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>