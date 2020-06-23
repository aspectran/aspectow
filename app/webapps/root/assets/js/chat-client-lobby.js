let broadcastEnabled = false;

$(function () {
    if (!Modernizr.websockets || detectIE()) {
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
    broadcastEnabled = true;
    $("#form-send-message button.quiet").on("click", function () {
        broadcastEnabled = !broadcastEnabled;
        if (broadcastEnabled) {
            $(this).removeClass("pause");
            $("#message, #form-send-message button.send").prop("disabled", false);
            $("#convo").show();
        } else {
            $(this).addClass("pause");
            $("#message, #form-send-message button.send").prop("disabled", true);
            $("#convo").hide();
        }
    });
});

function printMessage(payload, restored) {
    if (payload.content.startsWith("broadcast:")) {
        if (broadcastEnabled) {
            printBroadcastMessage(payload);
        }
    } else {
        handleSystemMessage(payload.content);
    }
}

function printBroadcastMessage(payload) {
    let convo = $("#convo");
    if (convo.find(".message").length >= 5) {
        convo.find(".message").first().remove();
    }
    let sender = $("<code class='sender'/>").text(payload.username);
    let content = $("<p class='content'/>")
        .text(payload.content.substring(10))
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
        printEvent(chatClientMessages.roomCreated.replace("[roomName]", "<code>" + roomInfo.roomName + "</code>"));
    }
}

function printJoinMessage(chater, restored) {
}

function printUserJoinedMessage(payload, restored) {
    let chater = deserialize(payload.chater);
    printEvent(chatClientMessages.userJoined.replace("[username]", "<strong>" + chater.username + "</strong>"));
}

function printUserLeftMessage(payload, restored) {
}

function printEvent(text, timeout) {
    if (!broadcastEnabled) {
        return;
    }
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
        location.href = "/signout";
    } else {
        gotoHome();
    }
}

function gotoHome() {
    leaveRoom(true);
}