<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div id="lobby-public-room-create-complete" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3><aspectran:message code="modal.public_room_create_complete.title"/></h3>
    <div class="grid-x grid-margin-y">
        <div class="cell content text-center">
            <p><aspectran:message code="modal.public_room_create_complete.guide"/></p>
        </div>
        <div class="cell text-center">
            <button type="button" class="button alert" title="Close" data-close><aspectran:message code="common.button.close"/></button>
            <button type="button" class="success button go-created-public-room" data-close aria-label="Go to the chat room you created">
                <aspectran:message code="modal.public_room_create_complete.join_chatroom_now"/></button>
        </div>
    </div>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>