<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div id="common-wait-popup" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h2><img src="<aspectran:token type='property' expression='cdn.assets.url'/>/images/textchat.svg" alt=""/> <aspectran:message code="common.please_wait"/></h2>
    <div class="grid-x grid-margin-y">
        <div class="cell show-for-small-only t30">
        </div>
        <div class="cell t20">
            <img class="banner" src="<aspectran:token type='property' expression='cdn.assets.url'/>/images/textchat.svg" width="50%" alt="Text Chat Club"/>
        </div>
        <div class="cell content text-center t30">
            <p></p>
        </div>
        <div class="cell text-center">
            <a class="secondary button cancel"><aspectran:message code="common.button.cancel" /></a>
        </div>
    </div>
</div>