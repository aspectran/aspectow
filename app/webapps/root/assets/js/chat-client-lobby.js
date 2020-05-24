let lobbyChatEnabled = true;
$(function () {
    $("#form-send-message button.quiet").on("click", function () {
        $(this).toggleClass("pause");
        $("#convo").toggle();
        $("#message, #form-send-message button.send").prop("disabled", lobbyChatEnabled);
        lobbyChatEnabled = !lobbyChatEnabled;
        if (lobbyChatEnabled) {
            readyToType();
        }
    });
});

function printMessage(payload, restored) {
    if (!lobbyChatEnabled) {
        return;
    }
    if (!payload.content.startsWith("say:")) {
        handleSystemMessage(payload.content);
        return;
    }
    let convo = $("#convo");
    if (convo.find(".message").length >= 5) {
        convo.find(".message").first().remove();
    }
    let sender = $("<span class='username'/>").text(payload.username);
    let content = $("<p class='content'/>")
        .text(payload.content.substring(4))
        .append(sender);
    let message = $("<div/>")
        .addClass("message")
        .data("user-no", payload.userNo)
        .data("username", payload.username)
        .append(content);
    convo.append(message);
    scrollToBottom(convo, false);
    setTimeout(function () {
        message.remove();
    }, 7000);
}

function handleSystemMessage(message) {
    if (!message) {
        return;
    }
    if (message.startsWith("newRoom:")) {
        let roomInfo = deserialize(message.substring(8));
        let room = $(".new-room-template").clone().removeClass("new-room-template");
        room.find("a").attr("href", "/rooms/" + roomInfo.encryptedRoomId);
        room.find("h5").text(roomInfo.roomName);
        room.prependTo($(".rooms.public")).fadeIn();
    }
}

function printJoinMessage(payload, restored) {
}

function printUserJoinedMessage(payload, restored) {
}

function printUserLeftMessage(payload, restored) {
}

function leaveRoom(force) {
    closeSocket();
    if (force) {
        $("#lobby-not-available").foundation('close');
        location.href = "/signout";
    } else {
        gotoHomepage();
    }
}

function gotoHomepage() {
    $("#lobby-not-available").foundation('open');
}