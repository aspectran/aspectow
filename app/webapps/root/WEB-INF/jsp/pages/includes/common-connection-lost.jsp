<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div id="common-connection-lost" class="reveal popup error" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h2><aspectran:message code="modal.connection_lost.title"/></h2>
    <div class="grid-x grid-margin-y">
        <div class="cell text-center">
            <i class="banner fi-unlink"></i>
        </div>
        <div class="cell text-center">
            <p><aspectran:message code="modal.connection_lost.guide"/></p>
        </div>
        <div class="cell text-center">
            <a class="button alert" href=""><aspectran:message code="modal.connection_lost.button.refresh"/></a>
        </div>
    </div>
</div>