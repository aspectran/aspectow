<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<script>
    $.extend(modalMessages, {
        "copied": "<aspectran:message code='modal.private_room_create_complete.copy_to_clipboard.copied'/>"
    });
</script>
<div id="lobby-private-room-create-complete" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h2><aspectran:message code="modal.private_room_create_complete.title"/></h2>
    <div class="grid-x">
        <div class="cell">
            <p><aspectran:message code="modal.private_room_create_complete.share_this_link"/></p>
        </div>
        <div class="cell card">
            <div class="card-section">
                <p class="private-chatroom-url">https://textchat.club/private/</p>
            </div>
        </div>
        <div class="cell text-right">
            <button type="button" class="button small copy-to-clipboard"><aspectran:message code="modal.private_room_create_complete.copy_to_clipboard"/></button>
        </div>
        <div class="cell text-center t10">
            <button type="button" class="button alert" title="Close" data-close><aspectran:message code="common.button.close"/></button>
            <button type="button" class="success button go-created-private-room" data-close aria-label="Go to the chat room you created">
                <aspectran:message code="modal.private_room_create_complete.join_chatroom_now"/></button>
        </div>
    </div>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>