<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div id="common-service-not-available" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h2><aspectran:message code="modal.service_not_available.title"/></h2>
    <div class="grid-x grid-margin-y">
        <div class="cell text-center">
            <i class="banner icon-warning"></i>
        </div>
        <div class="cell content">
            <p class="lead"><aspectran:message code="modal.service_not_available.guide_1"/></p>
            <p><aspectran:message code="modal.service_not_available.guide_2"/></p>
            <ul>
                <li><aspectran:message code="modal.service_not_available.guide_3"/></li>
                <li><aspectran:message code="modal.service_not_available.guide_4"/></li>
                <li><aspectran:message code="modal.service_not_available.guide_5"/></li>
            </ul>
        </div>
        <div class="cell text-center">
            <a class="button alert" href="/"><aspectran:message code="common.button.retry"/></a>
        </div>
    </div>
</div>