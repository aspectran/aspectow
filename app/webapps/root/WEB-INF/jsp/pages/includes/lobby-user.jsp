<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="user my-col-${user.color} grid-x hide-for-medium t10">
    <div class="cell shrink">
        <div class="avatar"><i class="iconfont fi-mountains"></i></div>
    </div>
    <div class="cell auto">
        <div class="nameplate">
            <div class="icon country">${user.country}</div>
            <h5 class="username">${user.username}</h5>
        </div>
    </div>
</div>