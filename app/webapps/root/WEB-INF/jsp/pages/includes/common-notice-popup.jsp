<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div id="common-notice-popup" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h2><img src="<aspectran:token type='property' expression='cdn.assets.url'/>/images/textchat.svg" alt=""/> Oops!</h2>
    <div class="grid-x grid-margin-y">
        <div class="cell text-center">
            <i class="banner fi-megaphone"></i>
        </div>
        <div class="cell content text-center">
            <p>Please note</p>
        </div>
        <div class="cell buttons text-center">
            <a class="secondary button ok"><aspectran:message code="common.button.ok"/></a>
        </div>
    </div>
    <button type="button" class="close-button ok" aria-label="Close modal">
        <span aria-hidden="true">&times;</span>
    </button>
</div>