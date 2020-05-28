<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div id="lobby-private-room-create" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Create a private chat room</h3>
    <form id="form-private-room-create">
    <div class="grid-x grid-margin-y">
        <fieldset class="cell">
            <ul>
                <li>Private chat rooms are always available for free.</li>
                <li>This private chat has no registration steps - your friends can join instantly.</li>
                <li>After creating a private chat room, send the URL only to those who want to chat.</li>
                <li>Note that the conversation is not saved, so when you refresh the screen, the conversation disappears.</li>
            </ul>
            <label>Chatroom name:
                <input type="text" name="room_nm" maxlength="40" autocomplete="off"/>
            </label>
            <p class="form-error room-name-required">
                Please enter your chat room name.
            </p>
        </fieldset>
    </div>
    <div class="grid-x grid-margin-y medium-up-2">
        <div class="cell medium-order-2 text-right">
            <button type="button" class="alert button" data-close aria-label="Cancel creating a chat room">Cancel</button>
            <button type="submit" class="success button">OK</button>
        </div>
        <div class="cell medium-order-1">
            <div id="captcha-container-private-room-create"></div>
        </div>
    </div>
    </form>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>