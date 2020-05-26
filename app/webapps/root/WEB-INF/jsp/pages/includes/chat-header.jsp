<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div class="header cell cell-block-container">
    <div class="grid-x">
        <div class="cell auto align-self-middle">
            <button type="button" class="button people" title="People in chatroom">
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
            <c:when test="${(page.roomId eq '-1' or page.roomId gt '0') && not empty user}">
                <button type="button" class="button leave" title="Leave this chat room"><i class="iconfont fi-power"></i></button>
            </c:when>
            <c:otherwise>
                <button type="button" class="button signin" title="Sign in"><i class="iconfont fi-arrow-right"></i> Sign in</button>
                <button type="button" class="button signout" title="Sign out">Sign out <i class="iconfont icon-circle-with-cross"></i></button>
            </c:otherwise>
        </c:choose>
        </div>
    </div>
</div>