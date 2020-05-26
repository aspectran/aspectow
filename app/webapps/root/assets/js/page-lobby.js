let recentlyCreatedRoom;

$(function () {
    $(".room-create").on("click", function () {
        if (!checkSignedIn()) {
            return false;
        }
        $("#form-room-create").each(function () {
            this.reset();
        });
        $("#form-room-create select[name=lang_cd] option").each(function () {
            if ($(this).val() === userInfo.language) {
                $(this).attr("selected", true);
            }
        });
        loadCaptcha("room_create", "captcha-container-room-create");
        $('#lobby-room-create').foundation('open');
        $("#form-room-create input[name=room_nm]").focus();
    });
    $("#form-room-create").submit(function () {
        executeCaptcha("room_create", doCreateRoom);
        return false;
    });
    $("button.go-created-room").on("click", function () {
        if (recentlyCreatedRoom) {
            closeSocket();
            location.href = "/rooms/" + recentlyCreatedRoom;
        }
    });
    $(".rooms a.start[href]").on("click", function (event) {
        event.stopPropagation();
        closeSocket();
        location.href = $(this).attr("href");
    });
    $(".refresh-rooms").on("click", function () {
        if (checkSignedIn()) {
            refreshRooms();
        }
    });

});

function doCreateRoom() {
    if (!recaptchaResponse) {
        return;
    }
    $("#form-room-create .form-error").hide();
    let roomName = $("#form-room-create input[name=room_nm]").val().trim();
    let langCode = $("#form-room-create select[name=lang_cd]").val().trim();
    if (!roomName) {
        $("#form-room-create .form-error.room-name-required").show();
        $("#form-room-create input[name=room_nm]").focus();
        return;
    }
    $.ajax({
        url: '/rooms',
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
                    $("#form-room-create .form-error.already-in-use").show();
                    $("#form-room-create input[name=room_nm]").select().focus();
                    break;
                default:
                    if (!result) {
                        alert("Unexpected error occurred.");
                        return;
                    }
                    $("#form-room-create input[name=room_nm]").val("");
                    recentlyCreatedRoom = result;
                    $('#lobby-room-create').foundation('close');
                    $('#lobby-room-create-complete').foundation('open');
            }
        },
        error: function (request, status, error) {
            alert("An error has occurred making the request: " + error);
        }
    });
}

let refreshRoomsTimer;
function refreshRooms() {
    if (refreshRoomsTimer) {
        clearTimeout(refreshRoomsTimer);
        refreshRoomsTimer = null;
    }
    refreshRoomsTimer = setTimeout(function () {
        $.ajax({
            url: '/rooms',
            type: 'get',
            dataType: 'json',
            success: function (list) {
                if (list) {
                    $(".rooms.public .room:visible").remove();
                    for (let i in list) {
                        let roomInfo = list[i];
                        console.log(roomInfo);
                        let room = $(".new-room-template").clone().removeClass("new-room-template");
                        room.find("a").attr("href", "/rooms/" + roomInfo.encryptedRoomId);
                        room.find("h5").text(roomInfo.roomName);
                        if (roomInfo.pastDays < 2) {
                            room.find(".new").show();
                        }
                        room.appendTo($(".rooms.public")).fadeIn();
                    }
                }
            },
            error: function (request, status, error) {
                alert("An error has occurred making the request: " + error);
            }
        });
    }, 400);
}