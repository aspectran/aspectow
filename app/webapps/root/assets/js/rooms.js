let recentlyCreatedRoom;

$(function() {
    $("form#room-create").submit(function() {
        executeCaptcha("room_create", createRoom);
        return false;
    });
    $("button.go-created-room").on("click", function() {
        if (recentlyCreatedRoom) {
            location.href = "/rooms/" + recentlyCreatedRoom;
        }
    });
});

function createRoom() {
    if (!recaptchaResponse) {
        return;
    }
    $.ajax({
        url: '/room/create',
        type: 'post',
        dataType: 'json',
        data: {
            room_nm: $("form#room-create input[name=room_nm]").val().trim(),
            lang_cd: $("form#room-create select[name=lang_cd]").val().trim(),
            recaptchaResponse: recaptchaResponse
        },
        success: function(result) {
            recentlyCreatedRoom = null;
            switch (result) {
                case "-1":
                    alert("reCAPTCHA verification failed");
                    break;
                case "-2":
                    $(".form-error.already-in-use").show();
                    $("form#room-create input[name=room_nm]").select().focus();
                    break;
                default:
                    if (!result) {
                        alert("Unexpected error occurred.");
                        return;
                    }
                    $("form#room-create input[name=room_nm]").val("");
                    recentlyCreatedRoom = result;
                    $('#rooms-room-create').foundation('close');
                    $('#rooms-room-create-complete').foundation('open');
            }
        },
        error: function (request, status, error) {
            alert("An error has occurred making the request: " + error);
        }
    });
}