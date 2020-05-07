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
            <div id="contacts-wrap" class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <div id="contacts"></div>
            </div>
            <div class="cell auto cell-block-y">
                <form id="sign-in" method="post">
                    <h3>Only one chat room!</h3>
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
    <div class="footer shadow cell">
        <div class="grid-x grid-padding-x grid-padding-y full-height">
            <div class="sidebar cell small-12 medium-4 large-3 cell-block-y hide-for-small-only">
            </div>
            <div class="cell auto cell-block-y">
            </div>
        </div>
    </div>
</div>
<script src="https://www.google.com/recaptcha/api.js?render=explicit&onload=loadCaptcha"></script>
<script>
    let clientId;
    let recaptchaResponse;

    function loadCaptcha() {
        grecaptcha.ready(function() {
            clientId = grecaptcha.render('inline-badge', {
                'sitekey': '6Ldt0r0UAAAAAP4ejDGFZLB0S-zDzWL3ZkB49FvN',
                'badge': 'inline',
                'size': 'invisible'
            });
        });
    }

    function executeCaptcha() {
        grecaptcha.ready(function() {
            grecaptcha.execute(clientId, {
                action: 'sign_in'
            }).then(function(token) {
                recaptchaResponse = token;
                joinChat();
            });
        });
    }
</script>
<script>
    const currentUser = "${user.username}";

    $(function() {
        $("form#sign-in").submit(function() {
            let username = $("#username").val().trim();
            if (!username) {
                return false;
            }
            $("#username").val(username);
            executeCaptcha();
            return false;
        });
        $("#username").val(currentUser);
    });

    function joinChat() {
        if (!recaptchaResponse) {
            return;
        }
        let username = $("#username").val().trim();
        if (username) {
            $.ajax({
                url: '/guest/signin',
                type: 'post',
                dataType: 'json',
                data: {
                    username: username,
                    recaptchaResponse: recaptchaResponse
                },
                success: function(data) {
                    switch (data.result) {
                        case 0:
                            location.href = "/chat";
                            break;
                        case -1:
                            alert("reCAPTCHA verification failed");
                            break;
                        case -2:
                            $(".form-error.already-in-use").show();
                            $("#username").select().focus();
                            break;
                        default:
                            alert("Unexpected error occurred.");
                    }
                },
                error: function (request, status, error) {
                    alert("An error has occurred making the request: " + error);
                }
            });
        }
    }
</script>