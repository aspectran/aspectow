<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="robby grid-y grid-frame">
    <div class="header cell cell-block-container">
        <div class="grid-x grid-padding-x">
            <div class="cell auto align-self-middle">
                <h2><i class="fi-results-demographics"></i> Text Chat Club</h2>
            </div>
            <div class="cell shrink align-self-middle text-right">
            </div>
        </div>
    </div>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x grid-padding-x grid-padding-y full-height">
            <div id="contacts-wrap" class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                <div id="contacts"></div>
            </div>
            <div class="cell auto cell-block-y">
                <form id="sign-in" method="post">
                    <h3>Only five chat rooms!</h3>
                    <h4>Chat with anyone you want, about anything you want, free.</h4>
                    <h4>Please enter your username.</h4>
                    <div class="input-group">
                        <input class="input-group-field" type="text" id="username" maxlength="30" placeholder="Username" autocomplete="off" autofocus/>
                        <div class="input-group-button">
                            <button type="submit" class="button">Join</button>
                        </div>
                    </div>
                    <p class="form-error already-in-use">
                        Your username is already in use. Please enter a different username.
                    </p>
                    <div id="inline-badge"></div>
                </form>
            </div>
        </div>
    </div>
</div>
<script src="https://www.google.com/recaptcha/api.js?render=explicit&onload=loadCaptcha"></script>
<script src="/assets/js/recaptcha.js"></script>
<script src="/assets/js/index.js"></script>
<script>
    const currentUser = "${user.username}";
</script>