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
    $("#username").val(getCookie("username")).select();
});

function signIn() {
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
                        setCookie("username", username, 7);
                        location.href = "/rooms";
                        break;
                    case -1:
                        alert("reCAPTCHA verification failed");
                        break;
                    case -2:
                        $(".form-error.already-in-use").show();
                        $("#username").select().focus();
                        break;
                    default:
                        console.error(data);
                        alert("Unexpected error occurred.");
                }
            },
            error: function (request, status, error) {
                alert("An error has occurred making the request: " + error);
            }
        });
    }
}