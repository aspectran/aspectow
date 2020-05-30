let recentlyCreatedRoomId;

$(function () {
    $(".public-room-create").on("click", function () {
        if (!checkSignedIn()) {
            return false;
        }
        $("#lobby-public-room-create").foundation('open');
        $("#form-public-room-create").each(function () {
            this.reset();
        });
        $("#form-public-room-create select[name=lang_cd] option").each(function () {
            if ($(this).val() === userInfo.language) {
                $(this).attr("selected", true);
            }
        });
        loadCaptcha("public_room_create", "captcha-container-public-room-create");
        $("#form-public-room-create input[name=room_nm]").focus();
    });
    $("#form-public-room-create").submit(function () {
        executeCaptcha("public_room_create", doCreatePublicRoom);
        return false;
    });
    $("button.go-created-public-room").on("click", function () {
        if (recentlyCreatedRoomId) {
            closeSocket();
            location.href = "/rooms/" + recentlyCreatedRoomId;
        }
    });
    $(".private-room-create").on("click", function () {
        if (!checkSignedIn()) {
            return false;
        }
        $("#lobby-private-room-create").foundation('open');
        $("#form-private-room-create").each(function () {
            this.reset();
        });
        loadCaptcha("private_room_create", "captcha-container-private-room-create");
        $("#form-private-room-create input[name=room_nm]").focus();
    });
    $("#form-private-room-create").submit(function () {
        executeCaptcha("private_room_create", doCreatePrivateRoom);
        return false;
    });
    $("button.go-created-private-room").on("click", function () {
        if (recentlyCreatedRoomId) {
            closeSocket();
            location.href = "/private/" + recentlyCreatedRoomId;
        }
    });
    $("#lobby-private-room-create-complete").on("click", ".copy-to-clipboard", function () {
        copyToClipboard("#lobby-private-room-create-complete .private-chatroom-url");
        $(this).data("old-text", $(this).text()).text("Copied!").addClass("alert");
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
    if (userInfo.userNo) {
        setInterval(function () {
            refreshRooms();
        }, 1000 * 60 * 5);
    }
});

function doCreatePublicRoom() {
    if (!recaptchaResponse) {
        return;
    }
    $("#form-public-room-create .form-error").hide();
    let roomName = $("#form-public-room-create input[name=room_nm]").val().trim();
    let langCode = $("#form-public-room-create select[name=lang_cd]").val().trim();
    if (!roomName) {
        $("#form-public-room-create .form-error.room-name-required").show();
        $("#form-public-room-create input[name=room_nm]").focus();
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
            recentlyCreatedRoomId = null;
            switch (result) {
                case "-1":
                    alert("reCAPTCHA verification failed");
                    break;
                case "-2":
                    $("#form-public-room-create .form-error.already-in-use").show();
                    $("#form-public-room-create input[name=room_nm]").select().focus();
                    break;
                default:
                    if (!result) {
                        alert("Unexpected error occurred.");
                        return;
                    }
                    $("#form-public-room-create input[name=room_nm]").val("");
                    recentlyCreatedRoomId = result;
                    $("#lobby-public-room-create").foundation('close');
                    $("#lobby-public-room-create-complete").foundation('open');
            }
        },
        error: function (request, status, error) {
            alert("An error has occurred making the request: " + error);
        }
    });
}

function doCreatePrivateRoom() {
    if (!recaptchaResponse) {
        return;
    }
    $("#form-private-room-create .form-error").hide();
    let roomName = $("#form-private-room-create input[name=room_nm]").val().trim();
    if (!roomName) {
        $("#form-private-room-create .form-error.room-name-required").show();
        $("#form-private-room-create input[name=room_nm]").focus();
        return;
    }
    $.ajax({
        url: '/private',
        type: 'post',
        dataType: 'json',
        data: {
            room_nm: roomName,
            recaptchaResponse: recaptchaResponse
        },
        success: function (result) {
            recentlyCreatedRoomId = null;
            switch (result) {
                case "-1":
                    alert("reCAPTCHA verification failed");
                    break;
                default:
                    if (!result) {
                        alert("Unexpected error occurred.");
                        return;
                    }
                    recentlyCreatedRoomId = result;
                    let url = "https://textchat.club/private/" + result;
                    $("#form-private-room-create input[name=room_nm]").val("");
                    $("#lobby-private-room-create").foundation('close');
                    $("#lobby-private-room-create-complete").foundation('open');
                    let oldText = $("#lobby-private-room-create-complete .copy-to-clipboard").data("old-text");
                    if (oldText) {
                        $("#lobby-private-room-create-complete .copy-to-clipboard").text(oldText).removeClass("alert");
                    }
                    $("#lobby-private-room-create-complete .private-chatroom-url").text(url);
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
            url: '/lobby/rooms',
            type: 'get',
            dataType: 'json',
            success: function (list) {
                if (list) {
                    $(".rooms.public .room:visible").remove();
                    for (let i in list) {
                        let roomInfo = list[i];
                        let room = $(".new-room-template").clone().removeClass("new-room-template");
                        room.find("a").attr("href", "/rooms/" + roomInfo.roomId);
                        room.find("h5").text(roomInfo.roomName);
                        room.find(".curr-users span").text(roomInfo.currentUsers);
                        if (roomInfo.currentUsers > 0) {
                            room.addClass("active");
                        }
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