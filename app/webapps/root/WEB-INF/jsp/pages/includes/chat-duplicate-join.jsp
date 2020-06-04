<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div id="chat-duplicate-join" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3><aspectran:message code="modal.chat_duplicate_join.title"/></h3>
    <div class="grid-x grid-padding-x grid-margin-y">
        <div class="cell show-for-small-only t30">
        </div>
        <div class="cell t20">
            <img class="banner animate denial" src="/assets/images/textchat.svg" width="50%" alt="Text Chat Club"/>
        </div>
        <div class="cell">
            <p><aspectran:message code="modal.chat_duplicate_join.guide_1"/><br/>
                <aspectran:message code="modal.chat_duplicate_join.guide_2"/></p>
        </div>
        <div class="cell text-center">
            <h4><aspectran:message code="modal.chat_duplicate_join.please_close"/></h4>
        </div>
    </div>
</div>