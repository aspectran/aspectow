<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:if test="${not empty user}">
<div id="index-already-signed-in" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Please note</h3>
    <div class="grid-x">
        <div class="cell content">
            <p class="lead">You have already signed in.</p>
            <p>If you wish to sign in again, please click <a class="button secondary" href="/signout">Sign out</a> below.<br/>
                Otherwise, press <a class="button alert" href="/rooms">Home</a> to return to the previous page.</p>
        </div>
        <div class="cell buttons">
            <a class="button alert" href="/lobby">Home</a>
            <a class="button secondary" href="/signout">Sign out</a>
        </div>
    </div>
</div>
</c:if>