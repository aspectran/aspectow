$(function () {
    if ($("#index-already-signed-in").length) {
        $("#index-already-signed-in").foundation('open');
        return;
    }
    $("form#sign-in").submit(function () {
        let username = $("#username").val().trim();
        if (!username) {
            return false;
        }
        $("#username").val(username);
        executeCaptcha("sign_in", startSignIn);
        return false;
    });
    $("#username").val(getCookie("username")).select();
});

function startSignIn() {
    openWaitPopup("Please wait while we are processing your request..", function () {
        location.reload();
    }, 10000);
    setTimeout(function () {
        doSignIn();
    }, 600);
}

function doSignIn() {
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
                recaptchaResponse: recaptchaResponse,
                timeZone: getTimeZone()
            },
            success: function (result) {
                closeWaitPopup();
                switch (result) {
                    case "0":
                        setCookie("username", username, 1);
                        location.href = "/lobby";
                        break;
                    case "-1":
                        alert("reCAPTCHA verification failed");
                        break;
                    case "-2":
                        $(".form-error.already-in-use").show();
                        $("#username").select().focus();
                        break;
                    default:
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
}

function getTimeZone() {
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone;
    } catch (e) {
        return null;
    }
}