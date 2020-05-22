let lobbyChatEnabled = true;
$(function () {
    $("#send-message button.quiet").on("click", function () {
        $(this).toggleClass("pause");
        $("#convo").toggle();
        $("#message, #send-message button.send").prop("disabled", lobbyChatEnabled);
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
    let convo = $("#convo");
    if (convo.find(".message").length >= 5) {
        convo.find(".message").first().remove();
    }
    let sender = $("<span class='username'/>").text(payload.username);
    let content = $("<p class='content'/>")
        .text(payload.content)
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

function printJoinMessage(payload, restored) {
}

function printUserJoinedMessage(payload, restored) {
}

function printUserLeftMessage(payload, restored) {
}

function leaveRoom(force) {
    closeSocket();
    if (force) {
        location.href = "/signout";
    } else {
        gotoHomepage();
    }
}

function gotoHomepage() {
    $("#lobby-not-available").foundation('open');
}