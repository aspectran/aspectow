<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="user my-col-${user.color} grid-x">
    <div class="cell shrink">
        <div class="avatar"><i class="iconfont fi-mountains"></i></div>
    </div>
    <div class="cell auto">
        <div class="nameplate">
            <h5 class="username">${user.username}</h5>
            <p class="description">${user.description}</p>
            <c:if test="${not empty user.country}">
                <img class="flag" src="<aspectran:token type='property' expression='cdn.assets.url'/>/flags/${fn:toLowerCase(user.country)}.svg" title="${user.country}"/>
            </c:if>
        </div>
    </div>
</div>