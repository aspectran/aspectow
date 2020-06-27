<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<script>
    $.extend(modalMessages, {
        "signingIn": "<aspectran:message code='modal.sign_in.signing_in'/>",
        "alreadySignedIn": "<aspectran:message code="modal.sign_in.already_signed_in"/>"
    });
</script>
<div id="common-sign-in" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <c:choose>
        <c:when test="${not empty page.roomName}">
            <h3><i class="fi-results-demographics"></i> ${page.roomName}</h3>
        </c:when>
        <c:when test="${page.roomId eq '-1'}">
            <h3><i class="fi-results-demographics"></i> <aspectran:message code="service.random_chat"/></h3>
        </c:when>
        <c:when test="${page.roomId eq '-2'}">
            <h3><i class="fi-results-demographics"></i> <aspectran:message code="service.stranger_chat"/></h3>
        </c:when>
        <c:when test="${page.roomId eq '-3'}">
            <h3><i class="fi-results-demographics"></i> <aspectran:message code="service.exchange_chat"/></h3>
        </c:when>
        <c:otherwise>
            <h3><aspectran:message code="modal.sign_in.title"/></h3>
        </c:otherwise>
    </c:choose>
    <form id="form-sign-in" method="post">
        <div class="grid-x">
            <div class="cell content">
                <c:if test="${page.roomId eq '0'}">
                    <ul>
                        <li><aspectran:message code="modal.sign_in.intro.from_lobby_1"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_lobby_2"/></li>
                    </ul>
                </c:if>
                <c:if test="${page.roomId eq '-1'}">
                    <ul>
                        <li><aspectran:message code="modal.sign_in.intro.from_random_chat_1"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_random_chat_2"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_random_chat_3"/></li>
                    </ul>
                </c:if>
                <c:if test="${page.roomId eq '-2'}">
                    <ul>
                        <li><aspectran:message code="modal.sign_in.intro.from_stranger_chat_1"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_stranger_chat_2"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_stranger_chat_3"/></li>
                    </ul>
                </c:if>
                <c:if test="${page.roomId eq '-3'}">
                    <ul>
                        <li><aspectran:message code="modal.sign_in.intro.from_exchange_chat_1"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_exchange_chat_2"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_exchange_chat_3"/></li>
                        <li><aspectran:message code="modal.sign_in.intro.from_exchange_chat_4"/></li>
                    </ul>
                </c:if>
                <c:if test="${page.roomId gt '0'}">
                <ul>
                    <li><aspectran:message code="modal.sign_in.intro.from_public_chat_1"/></li>
                    <li><aspectran:message code="modal.sign_in.intro.from_public_chat_2"/></li>
                </ul>
                </c:if>
                <label class="title"><aspectran:message code="modal.sign_in.enter_name"/></label>
                <div class="input-group">
                    <input class="input-group-field" type="text" name="username" maxlength="30" placeholder="<aspectran:message code="modal.sign_in.your_name"/>" autocomplete="off" autofocus/>
                    <div class="input-group-button">
                        <button type="submit" class="button success" title="Sign in"><aspectran:message code="common.button.sign_in"/></button>
                    </div>
                </div>
                <p class="form-error already-in-use">
                    <aspectran:message code="modal.sign_in.already_in_use"/>
                </p>
            </div>
            <div class="cell">
                <label class="title"><aspectran:message code="modal.sign_in.choose_color"/></label>
                <div>
                    <span class="my-col-box my-col-1">1</span>
                    <span class="my-col-box my-col-2">2</span>
                    <span class="my-col-box my-col-3">3</span>
                    <span class="my-col-box my-col-4">4</span>
                    <span class="my-col-box my-col-5 selected">5</span>
                    <span class="my-col-box my-col-6">6</span>
                    <span class="my-col-box my-col-7">7</span>
                </div>
            </div>
        </div>
        <div class="grid-x t10">
            <div class="cell small-12">
                <label class="title"><aspectran:message code="modal.sign_in.remember_me"/></label>
            </div>
        </div>
        <div class="grid-x">
            <div class="cell auto">
                <div class="switch">
                    <input class="switch-input" id="remember-me-yes-no" type="checkbox" name="remember-me">
                    <label class="switch-paddle" for="remember-me-yes-no">
                        <span class="show-for-sr">Remember me?</span>
                        <span class="switch-active" aria-hidden="true">Yes</span>
                        <span class="switch-inactive" aria-hidden="true">No</span>
                    </label>
                </div>
            </div>
            <div class="cell auto text-right">
                <button type="button" class="button alert cancel" title="Cancel"><aspectran:message code="common.button.cancel"/></button>
            </div>
        </div>
    </form>
    <div class="grid-x t10">
        <div class="cell">
            <div id="captcha-container-sign-in"></div>
        </div>
    </div>
    <button type="button" class="close-button cancel" aria-label="Close modal">
        <span aria-hidden="true">&times;</span>
    </button>
</div>