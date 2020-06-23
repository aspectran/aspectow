let broadcastEnabled = false;
let chatRequestEstablished = false;

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
        let ele = $(this);
        if (ele.hasClass("me")) {
            return;
        }
        $(".choose-info").hide();
        hideSidebar();
        let userNo = ele.data("user-no");
        let username = ele.data("username");
        let t = newChatRequestTemplate("confirm-request", null, username);
        t.data("user-no", userNo);
        t.data("username", username);
    });

    $(".chat-requests").on("click", ".confirm-request:visible .ok", function () {
        let ele = $(this).closest(".confirm-request");
        let userNo = ele.data("user-no");
        let username = ele.data("username");
        sendChatRequestMessage("request", userNo);
        let t = newChatRequestTemplate("request", ele, username);
        t.data("user-no", userNo);
        t.data("username", username);
        ele.remove();
        chatRequestTimer(t, 35, function () {
            newChatRequestTemplate("request-timeout", t);
            t.remove();
            sendChatRequestMessage("request-canceled", userNo);
        });
    }).on("click", ".confirm-request:visible .cancel", function () {
        $(this).closest(".confirm-request").remove();
    }).on("click", ".request:visible .cancel", function () {
        let ele = $(this).closest(".request");
        let userNo = ele.data("user-no");
        let username = ele.data("username");
        let t = newChatRequestTemplate("canceled-request", ele, username);
        t.data("user-no", userNo);
        t.data("username", username);
        ele.remove();
        sendChatRequestMessage("request-canceled", userNo);
    }).on("click", ".request-received:visible .decline", function () {
        let ele = $(this).closest(".request-received");
        let userNo = ele.data("user-no");
        let username = ele.data("username");
        let t = newChatRequestTemplate("declined-request", ele, username);
        t.data("user-no", userNo);
        t.data("username", username);
        ele.remove();
        sendChatRequestMessage("request-declined", userNo);
    }).on("click", ".request-received:visible .accept", function () {
        let ele = $(this).closest(".request-received");
        let userNo = ele.data("user-no");
        sendChatRequestMessage("request-accepted", userNo);
    });
});

function newChatRequestTemplate(requestType, before, username) {
    let t = $(".chat-requests ." + requestType + ".template").clone().removeClass("template");
    if (username) {
        let title = t.find(".title").html();
        title = title.replace("[username]", "<strong>" + username + "</strong>");
        t.find(".title").html(title);
    }
    if (before) {
        before.after(t);
    } else {
        t.appendTo(".chat-requests");
    }
    return t;
}

function handleChatRequestMessage(content) {
    if (!content || chatRequestEstablished) {
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
    } else if (content.startsWith("request-accepted:")) {
        let userNo = parseTargetUserNo(content);
        let prefix = "request-accepted:" + userNo + ":";
        let roomId = content.substring(prefix.length);
        chatRequestAccepted(userNo);
        setTimeout(function () {
            location.href = "/strangers/" + roomId;
        }, 2000);
    }
}

function chatRequest(targetUserInfo) {
    let t = newChatRequestTemplate("request-received", null, targetUserInfo.username);
    t.data("user-no", targetUserInfo.userNo);
    t.data("username", targetUserInfo.username);
    chatRequestTimer(t, 30, function () {
        t.find(".decline").click();
    });
}

function chatRequestCanceled(targetUserNo) {
    $(".chat-requests .request-received").each(function () {
        let ele = $(this);
        let userNo = ele.data("user-no");
        let username = ele.data("username");
        if (userNo === targetUserNo) {
            newChatRequestTemplate("request-canceled", ele, username);
            ele.remove();
        }
    })
}

function chatRequestDeclined(targetUserNo) {
    $(".chat-requests .request").each(function () {
        let ele = $(this);
        let userNo = ele.data("user-no");
        let username = ele.data("username");
        if (userNo === targetUserNo) {
            newChatRequestTemplate("request-declined", ele, username);
            ele.remove();
        }
    })
}

function chatRequestAccepted(targetUserNo) {
    $(".chat-requests .request, .chat-requests .request-received").each(function () {
        let ele = $(this);
        let userNo = ele.data("user-no");
        if (userNo === targetUserNo) {
            chatRequestEstablished = true;
            ele.data("done", true);
            ele.find("button").prop("disabled", true);
            ele.find(".timer").hide();
            ele.find(".done").show();
        }
    })
}

function chatRequestTimer(ele, timeoutInSecs, callback) {
    setTimeout(function () {
        if (!ele.data("done")) {
            let remains = (ele.data("remains") || timeoutInSecs) - 1;
            ele.find(".remains").text(remains);
            if (remains > 0) {
                ele.data("remains", remains);
                chatRequestTimer(ele, timeoutInSecs, callback);
            } else {
                callback();
            }
        }
    }, 999);
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
    let chater = deserialize(payload.chater);
    let convo = $("#convo");
    if (convo.find(".message").length >= 5) {
        convo.find(".message").first().remove();
    }
    let sender = $("<code class='sender'/>").text(chater.username);
    let content = $("<p class='content'/>")
        .text(payload.content.substring(10))
        .prepend(sender);
    let message = $("<div/>")
        .addClass("message")
        .data("user-no", chater.userNo)
        .data("username", chater.username)
        .append(content);
    if (chater.color) {
        message.addClass("my-col-" + chater.color);
    }
    convo.append(message);
    scrollToBottom(convo, false);
    setTimeout(function () {
        message.remove();
    }, 10000);
}

function printJoinMessage(chater, restored) {
}

function printUserJoinedMessage(payload, restored) {
    let chater = deserialize(payload.chater);
    printEvent(chatClientMessages.userJoined.replace("[username]", "<strong>" + chater.username + "</strong>"));
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