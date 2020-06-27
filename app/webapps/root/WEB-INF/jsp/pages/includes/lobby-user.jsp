<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="lobby user my-col-${user.color} grid-x hide-for-medium t15">
    <div class="cell shrink">
        <div class="avatar"><i class="iconfont fi-mountains"></i></div>
    </div>
    <div class="cell auto">
        <div class="nameplate">
            <h5 class="username">${user.username}</h5>
            <p class="description">${user.description}</p>
            <c:if test="${not empty user.country}">
                <img class="flag" src="https://raw.githubusercontent.com/topframe/country-flags/master/svg/${fn:toLowerCase(user.country)}.svg" title="${user.country}"/>
            </c:if>
        </div>
    </div>
</div>