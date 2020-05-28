<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div id="common-sign-in" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <c:choose>
        <c:when test="${not empty page.roomName}">
            <h3><i class="fi-results-demographics"></i> ${page.roomName}</h3>
        </c:when>
        <c:when test="${page.roomId eq '-1'}">
            <h3><i class="fi-results-demographics"></i> Random Chat</h3>
        </c:when>
        <c:otherwise>
            <h3>Sign in into TextChatClub</h3>
        </c:otherwise>
    </c:choose>
    <form id="form-sign-in" method="post">
        <div class="grid-x">
            <div class="cell content">
                <c:if test="${page.roomId eq '0'}">
                    <p>TextChatClub is a place to chat with your friends, meet new people and discover conversations.</p>
                </c:if>
                <c:if test="${page.roomId eq '-1'}">
                    <p>Our random chat service lets you text chat with randomly selected people.
                        To chat with strangers on the Internet is a great way to find new friends.
                        Please note that you must be 18+ to start random chats with strangers. </p>
                </c:if>
                <label class="title">Please enter your name.</label>
                <div class="input-group">
                    <input class="input-group-field" type="text" name="username" maxlength="30" placeholder="Your name" autocomplete="off" autofocus/>
                    <div class="input-group-button">
                        <button type="submit" class="button success" title="Sign in">Sign in</button>
                    </div>
                </div>
                <p class="form-error already-in-use">
                    Your name is already in use or a similar name already exists.<br/>
                    Please enter a different name.
                </p>
            </div>
            <div class="cell">
                <label class="title">Choose your favorite color.</label>
                <div>
                    <span class="my-col-block my-col-1">1</span>
                    <span class="my-col-block my-col-2">2</span>
                    <span class="my-col-block my-col-3">3</span>
                    <span class="my-col-block my-col-4">4</span>
                    <span class="my-col-block my-col-5 selected">5</span>
                    <span class="my-col-block my-col-6">6</span>
                    <span class="my-col-block my-col-7">7</span>
                </div>
            </div>
        </div>
        <div class="grid-x t10">
            <div class="cell small-12">
                <label class="title">Remember me</label>
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
                <button type="button" class="button small alert cancel" title="Cancel">Cancel</button>
            </div>
        </div>
    </form>
    <div class="grid-x t10">
        <div class="cell">
            <div id="captcha-container-sign-in"></div>
        </div>
    </div>
</div>