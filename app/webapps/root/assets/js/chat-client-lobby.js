let lobbyChatEnabled = false;

$(function () {
    if (!Modernizr.websockets) {
        $("#message").blur();
        $("#message, #form-send-message button").prop("disabled", true);
        location.href = "/error/browser-not-supported";
        return;
    }
    if (!userInfo.userNo) {
        $("#message").blur();
        $("#message, #form-send-message button").prop("disabled", true);
        return;
    }
    lobbyChatEnabled = true;
    $("#form-send-message button.quiet").on("click", function () {
        lobbyChatEnabled = !lobbyChatEnabled;
        if (lobbyChatEnabled) {
            $(this).removeClass("pause");
            $("#message, #form-send-message button.send").prop("disabled", false);
            $("#convo").show();
            readyToType();
        } else {
            $(this).addClass("pause");
            $("#message, #form-send-message button.send").prop("disabled", true);
            $("#convo").hide();
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
    let sender = $("<code class='sender'/>").text(payload.username);
    let content = $("<p class='content'/>")
        .text(payload.content.substring(4))
        .prepend(sender);
    let message = $("<div/>")
        .addClass("message")
        .data("user-no", payload.userNo)
        .data("username", payload.username)
        .append(content);
    if (payload.color) {
        message.addClass("my-col-" + payload.color);
    }
    convo.append(message);
    scrollToBottom(convo, false);
    setTimeout(function () {
        message.remove();
    }, 10000);
}

function handleSystemMessage(message) {
    if (!message) {
        return;
    }
    if (message.startsWith("newPublicRoom:")) {
        let roomInfo = deserialize(message.substring(14));
        let room = $(".new-room-template").clone().removeClass("new-room-template");
        room.find("a").attr("href", "/rooms/" + roomInfo.roomId);
        room.find("h5").text(roomInfo.roomName);
        room.find(".new").show();
        room.prependTo($(".rooms.public")).fadeIn();
        printEvent("<code>" + roomInfo.roomName + "</code> chatroom has been created.");
    }
}

function printJoinMessage(payload, restored) {
}

function printUserJoinedMessage(payload, restored) {
    printEvent("<code>" + payload.username + "</code> has entered.");
}

function printUserLeftMessage(payload, restored) {
}

function printEvent(text, timeout) {
    let convo = $("#convo");
    let content = $("<p class='content'/>").html(text);
    let message = $("<div/>").addClass("message").append(content);
    message.appendTo(convo);
    scrollToBottom(convo, false);
    setTimeout(function () {
        message.remove();
    }, timeout||3500);
}

function leaveRoom(force) {
    closeSocket();
    if (force) {
        $("#common-service-not-available").foundation('close');
        location.href = "/signout";
    } else {
        gotoHomepage();
    }
}

function gotoHomepage() {
    $("#common-service-not-available").foundation('open');
}