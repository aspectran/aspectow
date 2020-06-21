let broadcastEnabled = false;

$(function () {
    if (!Modernizr.websockets || detectIE()) {
        gotoHome();
        return;
    }
    if (!checkSignedIn()) {
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

    $("#contacts").on("click", ".contact", function () {
        if (!$(this).hasClass("me")) {
            $(".selection-info").hide();
            hideSidebar();
        }
    });
});

function printMessage(payload, restored) {
    if (!broadcastEnabled) {
        return;
    }
    if (!payload.content.startsWith("say:")) {
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

function printJoinMessage(payload, restored) {
}

function printUserJoinedMessage(payload, restored) {
    printEvent(chatClientMessages.userJoined.replace("[username]", "<strong>" + payload.username + "</strong>"));
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