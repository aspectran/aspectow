<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div id="common-sign-in" class="reveal popup" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h3><i class="fi-results-demographics"></i> Sign in into TextChatClub</h3>
    <div class="grid-x grid-padding-x grid-margin-y">
        <div class="cell content">
            <form id="form-sign-in" method="post">
                <p>Chat with anyone. Meet new people at random.<br/>
                    Completely anonymously. No registration. No logs.</p>
                <label class="title">Please enter your name.</label>
                <div class="input-group">
                    <input class="input-group-field" type="text" name="username" maxlength="30" placeholder="Your name" autocomplete="off" autofocus/>
                    <div class="input-group-button">
                        <button type="submit" class="button" title="Sign in">Sign in</button>
                    </div>
                </div>
                <p class="form-error already-in-use">
                    Your username is already in use or a similar name already exists.<br/>
                    Please enter a different username.
                </p>
                <label class="title">Remember me</label>
                <div class="switch">
                    <input class="switch-input" id="remember-me-yes-no" type="checkbox" name="remember-me">
                    <label class="switch-paddle" for="remember-me-yes-no">
                        <span class="show-for-sr">Remember me?</span>
                        <span class="switch-active" aria-hidden="true">Yes</span>
                        <span class="switch-inactive" aria-hidden="true">No</span>
                    </label>
                </div>
            </form>
        </div>
    </div>
    <div class="grid-x grid-padding-x grid-margin-y">
        <div class="cell auto small-order-2 text-right">
            <button type="button" class="button alert cancel" title="Cancel">Cancel</button>
        </div>
        <div class="cell auto small-order-1">
            <div id="captcha-container-sign-in"></div>
        </div>
    </div>
</div>