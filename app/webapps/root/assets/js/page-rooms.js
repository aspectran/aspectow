let recentlyCreatedRoom;

$(function () {
    $(".room-create").on("click", function () {
        $("form#room-create").each(function () {
            this.reset();
        });
        $("form#room-create select[name=lang_cd] option").each(function () {
            if ($(this).val() === userInfo.language) {
                $(this).attr("selected", true);
            }
        });
        $('#rooms-room-create').foundation('open');
    });
    $("form#room-create").submit(function () {
        executeCaptcha("room_create", doCreateRoom);
        return false;
    });
    $("button.go-created-room").on("click", function () {
        if (recentlyCreatedRoom) {
            location.href = "/rooms/" + recentlyCreatedRoom;
        }
    });
});

function doCreateRoom() {
    if (!recaptchaResponse) {
        return;
    }
    $("form#room-create .form-error").hide();
    let roomName = $("form#room-create input[name=room_nm]").val().trim();
    let langCode = $("form#room-create select[name=lang_cd]").val().trim();
    if (!roomName) {
        $("form#room-create .form-error.room-name-required").show();
        $("form#room-create input[name=room_nm]").focus();
        return;
    }
    $.ajax({
        url: '/room/create',
        type: 'post',
        dataType: 'json',
        data: {
            room_nm: roomName,
            lang_cd: langCode,
            recaptchaResponse: recaptchaResponse
        },
        success: function (result) {
            recentlyCreatedRoom = null;
            switch (result) {
                case "-1":
                    alert("reCAPTCHA verification failed");
                    break;
                case "-2":
                    $("form#room-create .form-error.already-in-use").show();
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