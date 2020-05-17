<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div id="rooms-room-create" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Creating a chat room</h3>
    <form id="room-create">
        <div class="grid-x grid-padding-x">
            <fieldset class="cell">
                <legend>Details of the new chat room</legend>
                <label>Chatroom name:
                    <input type="text" name="room_nm" maxlength="30"/>
                    <p class="form-error already-in-use">
                        A chat room with the same name already exists.<br/>
                        Please enter a different chat room name.
                    </p>
                </label>
                <label>Language:
                    <select name="lang_cd">
                        <option value="en">English</option>
                        <option value="ko">한국어</option>
                    </select>
                </label>
            </fieldset>
        </div>
        <div class="grid-x grid-padding-x medium-up-2">
            <div class="cell medium-order-2 text-right">
                <button type="button" class="alert button" data-close aria-label="Cancel creating a chat room">Cancel</button>
                <button type="submit" class="success button">OK</button>
            </div>
            <div class="cell medium-order-1">
                <div id="captcha-container"></div>
            </div>
        </div>
    </form>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>