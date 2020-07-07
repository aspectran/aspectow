<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div id="lobby-public-room-create" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h2><img src="<aspectran:token type='property' expression='cdn.assets.url'/>/images/textchat.svg" alt=""/> <aspectran:message code="modal.public_room_create.title"/></h2>
    <form id="form-public-room-create">
    <div class="grid-x grid-margin-y">
        <fieldset class="cell">
            <ul>
                <li><aspectran:message code="modal.public_room_create.guide_1"/></li>
                <li><aspectran:message code="modal.public_room_create.guide_2"/></li>
            </ul>
            <label><aspectran:message code="modal.public_room_create.chatroom_name"/>
                <input type="text" name="room_nm" maxlength="40" autocomplete="off"/>
            </label>
            <p class="form-error already-in-use">
                <aspectran:message code="modal.public_room_create.error.already_in_use"/>
            </p>
            <p class="form-error room-name-required">
                <aspectran:message code="modal.public_room_create.error.enter_chatroom_name"/>
            </p>
            <label><aspectran:message code="modal.public_room_create.language"/>
                <select name="lang_cd">
                    <aspectran:token type="property" expression="languages" var="languages"/>
                    <c:forEach items="${languages}" var="entry">
                        <option value="${entry.key}">${entry.value}</option>
                    </c:forEach>
                </select>
            </label>
        </fieldset>
    </div>
    <div class="grid-x grid-margin-y medium-up-2">
        <div class="cell medium-order-2 text-right">
            <button type="button" class="alert button" data-close aria-label="Cancel creating a chat room"><aspectran:message code="common.button.cancel"/></button>
            <button type="submit" class="success button"><aspectran:message code="common.button.ok"/></button>
        </div>
        <div class="cell medium-order-1">
            <div id="captcha-container-public-room-create"></div>
        </div>
    </div>
    </form>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>