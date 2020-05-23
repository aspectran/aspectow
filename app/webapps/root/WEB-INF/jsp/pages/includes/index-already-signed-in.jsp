<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div id="index-already-signed-in" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3>You have already signed in.</h3>
    <div class="grid-x grid-margin-y">
        <div class="cell text-center">
            <i class="banner fi-mountains"></i>
        </div>
        <div class="cell content">
            <p>If you wish to sign in again, please click <a class="button secondary" href="/signout">Sign out</a> below.<br/>
                Otherwise, press <a class="button alert" href="/rooms">Home</a> to return to the previous page.</p>
        </div>
        <div class="cell text-center">
            <a class="button alert" href="/lobby">Home</a>
            <a class="button secondary" href="/signout">Sign out</a>
        </div>
    </div>
</div>