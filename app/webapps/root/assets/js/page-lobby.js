let recentlyCreatedRoomId;

$(function () {
    $(".service a.guide").on("click", function () {
       $(this).parent().find("p.guide").toggleClass("show-for-large");
    });
    $(".public-room-create").on("click", function () {
        if (!checkSignedIn()) {
            return false;
        }
        $("#lobby-public-room-create").foundation('open');
        $("#form-public-room-create .form-error").hide();
        $("#form-public-room-create").each(function () {
            this.reset();
        });
        $("#form-public-room-create select[name=lang_cd] option").filter(function () {
            return $(this).val() === userInfo.language;
        }).each(function () {
            $("#form-public-room-create select[name=lang_cd]").val(userInfo.language);
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
        $(this).data("old-text", $(this).text()).text(modalMessages.copied).addClass("alert");
    });
    $("a.start[href]").on("click", function (event) {
        event.preventDefault();
        closeSocket();
        location.href = $(this).attr("href");
    });
    $(".service.random a.start").off().on("click", function (event) {
        event.preventDefault();
        closeSocket();
        let lang = $(".service-options select[name=chat_lang]").val();
        if (lang) {
            location.href = $(this).attr("href") + "?lang=" + lang;
        } else {
            location.href = $(this).attr("href");
        }
    });
    $(".service-options select[name=chat_lang] option").filter(function () {
        return $(this).val() === userInfo.language;
    }).each(function () {
        $(".service-options select[name=chat_lang]").val(userInfo.language);
    });
    $(".refresh-rooms").on("click", function () {
        if (checkSignedIn()) {
            refreshRooms();
        }
    });
    $(".rooms-options select[name=room_lang]").change(function () {
        refreshRooms();
        $(this).blur();
    });
    refreshRooms(userInfo.language);
    setInterval(function () {
        refreshRooms();
    }, 1000 * 60 * 5);
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
                    location.reload();
                    break;
                case "-2":
                    $("#form-public-room-create .form-error.already-in-use").show();
                    $("#form-public-room-create input[name=room_nm]").select().focus();
                    break;
                default:
                    if (!result) {
                        alert("Unexpected error occurred.");
                        location.reload();
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
                    location.reload();
                    break;
                default:
                    if (!result) {
                        alert("Unexpected error occurred.");
                        location.reload();
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
function refreshRooms(roomLang) {
    if (refreshRoomsTimer) {
        clearTimeout(refreshRoomsTimer);
        refreshRoomsTimer = null;
    }
    if (roomLang) {
        $(".rooms-options select[name=room_lang] option").filter(function () {
            return $(this).val() === roomLang;
        }).each(function () {
            $(".rooms-options select[name=room_lang]").val(roomLang);
        });
    } else {
        roomLang = $(".rooms-options select[name=room_lang]").val();
    }
    refreshRoomsTimer = setTimeout(function () {
        $.ajax({
            url: '/lobby/rooms',
            data: {
                lang_cd: roomLang
            },
            type: 'get',
            dataType: 'json',
            success: function (list) {
                if (list) {
                    $(".rooms .room:visible").remove();
                    for (let i in list) {
                        let roomInfo = list[i];
                        let room = $(".rooms .room.template").clone().removeClass("template");
                        room.find("a").attr("href", "/rooms/" + roomInfo.roomId);
                        room.find("h5").text(roomInfo.roomName);
                        room.find(".curr-users span").text(roomInfo.currentUsers);
                        if (!roomLang && roomInfo.language) {
                            room.find(".lang").data("lang-cd", roomInfo.language).text(roomInfo.languageName);
                        }
                        if (roomInfo.currentUsers > 0) {
                            room.addClass("active");
                        }
                        if (roomInfo.pastDays < 2) {
                            room.find(".new").show();
                        }
                        room.appendTo($(".rooms")).hide().fadeIn();
                    }
                }
            },
            error: function (request, status, error) {
                location.reload();
            }
        });
    }, 400);
}