<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:if test="${not empty user}">
<div id="index-already-signed-in" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>Wait</h3>
    <div class="grid-x grid-padding-x grid-margin-y">
        <div class="cell">
            <h5>You have already signed in.</h5>
            If you wish to sign in again, please click the Sign out button below.<br/>
            Otherwise, press the Back button to return to the previous page.
        </div>
        <div class="cell text-right">
            <a class="button alert" href="/rooms">Back</a>
            <a class="button secondary" href="/signout">Sign out</a>
        </div>
    </div>
</div>
</c:if>