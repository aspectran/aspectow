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

    $(".choose-info").fadeIn();
    $("#contacts").on("click", ".contact", function () {
        if ($(this).hasClass("me")) {
            return;
        }
        $(".choose-info").hide();
        hideSidebar();
        let userNo =  $(this).data("user-no");
        let t = newChatRequestTemplate("confirm-request");
        t.data("user-no", userNo);
    });

    $(".chat-requests").on("click", ".confirm-request:visible .ok", function () {
        let ele = $(this).closest(".confirm-request");
        let userNo = ele.data("user-no");
        sendChatRequestMessage("request", userNo);
        let t = newChatRequestTemplate("request", ele);
        t.data("user-no", userNo);
        ele.remove();
        chatRequestTimer(t, 35, function () {
            let t2 = newChatRequestTemplate("request-timeout", t);
            t2.data("user-no", userNo);
            t.remove();
            sendChatRequestMessage("request-canceled", userNo);
        });
    }).on("click", ".confirm-request:visible .cancel", function () {
        $(this).closest(".confirm-request").remove();
    }).on("click", ".request:visible .cancel", function () {
        let ele = $(this).closest(".request");
        let userNo = ele.data("user-no");
        newChatRequestTemplate("canceled-request", ele);
        ele.remove();
        sendChatRequestMessage("request-canceled", userNo);
    }).on("click", ".request-received:visible .decline", function () {
        let ele = $(this).closest(".request-received");
        let userNo = ele.data("user-no");
        newChatRequestTemplate("request-declined", ele);
        ele.remove();
        sendChatRequestMessage("request-declined", userNo);
    });
});

function newChatRequestTemplate(requestType, before) {
    let t = $("." + requestType + ".template").clone().removeClass("template");
    if (before) {
        before.after(t);
    } else {
        t.appendTo(".chat-requests");
    }
    return t;
}

function handleChatRequestMessage(content) {
    if (!content) {
        return;
    }
    if (content.startsWith("request:")) {
        let userNo = parseTargetUserNo(content);
        if (userNo === userInfo.userNo) {
            let prefix = "request:" + userNo + ":";
            let requestUserInfo = deserialize(content.substring(prefix.length));
            chatRequest(requestUserInfo);
        }
    } else if (content.startsWith("request-canceled:")) {
        let userNo = parseTargetUserNo(content);
        chatRequestCanceled(userNo);
    } else if (content.startsWith("request-declined:")) {
        let userNo = parseTargetUserNo(content);
        chatRequestDeclined(userNo);
    }
}

function chatRequest(requestUserInfo) {
    let t = newChatRequestTemplate("request-received");
    t.data("user-no", requestUserInfo.userNo);
    chatRequestTimer(t, 30, function () {
        t.find(".decline").click();
    });
}

function chatRequestCanceled(userNo) {
    $(".request-received").each(function () {
        let ele = $(this);
        if (ele.data("user-no") === userNo) {
            newChatRequestTemplate("request-canceled", ele);
            ele.remove();
        }
    })
}

function chatRequestDeclined(userNo) {
    $(".request").each(function () {
        let ele = $(this);
        if (ele.data("user-no") === userNo) {
            newChatRequestTemplate("request-declined", ele);
            ele.remove();
        }
    })
}

function chatRequestTimer(ele, timeoutInSecs, callback) {
    setTimeout(function () {
        let remains = (ele.data("remains")||timeoutInSecs) - 1;
        ele.find(".remains").text(remains);
        if (remains > 0) {
            ele.data("remains", remains);
            chatRequestTimer(ele, timeoutInSecs, callback);
        } else {
            callback();
        }
    }, 1000);
}

function parseTargetUserNo(content) {
    try {
        let prefix = content.substring(0, content.indexOf(":") + 1);
        let start = prefix.length;
        let end = content.indexOf(":", start);
        if (end === -1) {
            end = content.length;
        }
        return parseInt(content.substring(start, end));
    } catch (e) {
        return 0;
    }
}

function sendChatRequestMessage(requestType, userNo) {
    let message = {
        type: 'POST',
        userNo: userInfo.userNo,
        username: userInfo.username,
        content: requestType + ":" + userNo
    }
    let chatMessage = {
        message: message
    };
    socket.send(serialize(chatMessage));
}

function printMessage(payload, restored) {
    if (payload.content.startsWith("broadcast:")) {
        if (broadcastEnabled) {
            printBroadcastMessage(payload);
        }
    } else {
        handleChatRequestMessage(payload.content);
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

function printJoinMessage(payload, restored) {
}

function printUserJoinedMessage(payload, restored) {
    printEvent(chatClientMessages.userJoined.replace("[username]", "<strong>" + payload.username + "</strong>"));
}

function printUserLeftMessage(payload, restored) {
    chatRequestCanceled(payload.userNo);
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