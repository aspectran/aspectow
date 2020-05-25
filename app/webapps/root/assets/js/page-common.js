$(function () {
    $(document).foundation();

    if (userInfo.userNo) {
        $(".header .signin").hide();
        $(".header .signout").show();
    } else {
        $(".header .signin").show();
        $(".header .signout").hide();
    }
    $(".button.signin").on("click", function () {
        openSignInPopup();
    });
    $(".button.signout").on("click", function () {
        location.href = "/signout";
    });
    $("#form-sign-in").submit(function () {
        let username = $("#form-sign-in input[name=username]").val().trim();
        if (!username) {
            $("#form-sign-in input[name=username]").focus();
            return false;
        }
        $("#form-sign-in input[name=username]").val(username);
        executeCaptcha("sign_in", startSignIn);
        return false;
    });
    $("#form-sign-in input[name=remember-me]").on("change", function () {
        if (!this.checked) {
            setCookie("username", "", 10);
        }
    });
    $("#common-sign-in .button.cancel").on("click", function () {
        if (location.pathname !== "/") {
            location.href = "/";
        } else {
            $("#common-sign-in").foundation('close');
        }
    });
});

function checkSignedIn() {
    if (!userInfo.userNo) {
        openSignInPopup();
        return false;
    }
    return true;
}

function openSignInPopup() {
    $("#common-sign-in").foundation('open');
    loadCaptcha("sign_in", "captcha-container-sign-in");
    let username = getCookie("username");
    $("#form-sign-in input[name=username]").val(username).focus();
    if (username) {
        $("#form-sign-in input[name=remember-me]").prop("checked", true);
    }
}

let startSignInTimer;
function startSignIn() {
    if (startSignInTimer) {
        clearTimeout(startSignInTimer);
        startSignInTimer = null;
    }
    $("#common-sign-in").foundation('close');
    let username = $("#form-sign-in input[name=username]").val().trim();
    if (username) {
        if ($("#form-sign-in input[name=remember-me]").prop("checked")) {
            setCookie("username", username, 7);
        } else {
            setCookie("username", "", 0);
        }
        openWaitPopup("Signing in...", function () {
            location.reload();
        }, 10000);
        startSignInTimer = setTimeout(function () {
            doSignIn(username);
        }, 600);
    }
}

function doSignIn(username) {
    if (!recaptchaResponse) {
        return;
    }
    $.ajax({
        url: '/guest/signin',
        type: 'post',
        dataType: 'json',
        data: {
            username: username,
            recaptchaResponse: recaptchaResponse,
            timeZone: getTimeZone()
        },
        success: function (result) {
            switch (result) {
                case "0":
                    location.reload();
                    break;
                case "-1":
                    closeWaitPopup();
                    alert("reCAPTCHA verification failed");
                    break;
                case "-2":
                    closeWaitPopup();
                    $(".form-error.already-in-use").show();
                    $("#username").select().focus();
                    break;
                default:
                    closeWaitPopup();
                    console.error(result);
                    alert("Unexpected error occurred.");
            }
        },
        error: function (request, status, error) {
            closeWaitPopup();
            alert("An error has occurred making the request: " + error);
        }
    });
}

function openNoticePopup(title, message, action) {
    let p = $("<p/>").text(message);
    let popup = $("#common-notice-popup");
    popup.find("h3").text(title);
    popup.find(".content").html("").append(p);
    popup.find(".button.ok").off().on("click", function () {
        if (action) {
            action();
        }
        popup.foundation('close');
    });
    popup.foundation('open');
}

let openWaitPopupTimer;
function openWaitPopup(message, action, timeout) {
    if (openWaitPopupTimer) {
        clearTimeout(openWaitPopupTimer);
        openWaitPopupTimer = null;
    }
    let p = $("<p/>").text(message);
    let popup = $("#common-wait-popup");
    popup.find(".content").html("").append(p);
    popup.find(".button.cancel").hide().off().on("click", function () {
        if (action) {
            action();
        }
        popup.foundation('close');
    });
    popup.foundation('open');
    if (timeout > 0) {
        openWaitPopupTimer = setTimeout(function () {
            popup.find(".button.cancel").show();
        }, timeout);
    } else {
        popup.find(".button.cancel").show();
    }
}

function closeWaitPopup() {
    $("#common-wait-popup").foundation('close');
}