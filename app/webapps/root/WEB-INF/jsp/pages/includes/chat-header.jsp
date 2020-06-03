<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="header cell cell-block-container">
    <div class="grid-x">
        <div class="cell auto align-self-middle">
            <button type="button" class="button people" title="<aspectran:message code="header.button.people"/>">
                <i class="iconfont fi-results-demographics"></i>
                <span id="totalPeople"></span></button>
                <c:choose>
                    <c:when test="${not empty page.roomName}">
                        <h2 class="text-truncate">${page.roomName}</h2>
                    </c:when>
                    <c:when test="${page.roomId eq '-1'}">
                        <h2 class="text-truncate">Random Chat</h2>
                    </c:when>
                    <c:otherwise>
                        <h2>Text Chat Club</h2>
                    </c:otherwise>
                </c:choose>
        </div>
        <div class="cell shrink align-self-middle text-right">
            <c:choose>
                <c:when test="${not empty user and not empty page.roomId and page.roomId ne '0'}">
                    <button type="button" class="button leave" title="<aspectran:message code="header.button.leave"/>"><i class="iconfont fi-power"></i></button>
                </c:when>
                <c:when test="${not empty user}">
                    <a class="button about" href="/info" title="About Text Chat Club"><i class="iconfont fi-info"></i></a>
                    <button type="button" class="button signout" title="Sign out"><aspectran:message code="common.button.sign_out"/></button>
                </c:when>
                <c:otherwise>
                    <button type="button" class="button signin" title="Sign in"><aspectran:message code="common.button.sign_in"/></button>
                    <a class="button about" href="/info" title="About Text Chat Club"><i class="iconfont fi-info"></i></a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>