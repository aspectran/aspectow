let socket;
let heartbeatTimer;
let pendedMessages;
let aborted;

$(function() {
    $("form#send-message").submit(function() {
        let wrap = $("#contacts-wrap");
        if (!wrap.hasClass("show-for-medium")) {
            wrap.addClass("show-for-medium");
        }
        $("#for-automata-clear").focus();
        sendMessage();
        return false;
    });
    $(".header button.people").on("click", function() {
        let sidebar = $(".sidebar");
        sidebar.toggle().toggleClass("show-for-medium");
        if (sidebar.hasClass("show-for-medium") || !sidebar.is(":visible")) {
            $("#message").focus();
        }
    })
    $("button.leave").on("click", function() {
        leaveRoom();
    });
    $("#message").focus();

    openSocket();
});

function openSocket() {
    if (!chatServerType || !currentUserNo || !currentUsername || !admissionToken) {
        location.href = "/rooms";
    }
    if (socket) {
        socket.onclose = null;
        socket.close();
        socket = null;
    }
    let url = new URL('/chat/' + chatServerType + '/' + admissionToken, location.href);
    url.protocol = url.protocol.replace('https:', 'wss:');
    url.protocol = url.protocol.replace('http:', 'ws:');
    socket = new WebSocket(url.href);
    socket.onopen = function(event) {
        let chatMessage = {
            message: {
                type: 'JOIN',
                userNo: currentUserNo,
                username: currentUsername
            }
        };
        socket.send(serialize(chatMessage));
        heartbeatPing();
    };
    socket.onmessage = function(event) {
        if (typeof event.data === "string") {
            let chatMessage = deserialize(event.data);
            handleMessage(chatMessage);
        }
    };
    socket.onclose = function(event) {
        if (aborted) {
            location.href = "/rooms";
        } else {
            $.ajax('/ping')
                .done(function() {
                    location.reload();
                })
                .fail(function() {
                    $('#connection-lost').foundation('open');
                });
        }
    };
    socket.onerror = function(event) {
        console.error("WebSocket error observed:", event);
        printError('Could not connect to server. Please refresh this page.');
    };
}

function heartbeatPing() {
    if (heartbeatTimer) {
        clearTimeout(heartbeatTimer);
    }
    heartbeatTimer = setTimeout(function() {
        if (socket) {
            let chatMessage = {
                heartBeat: "-ping-"
            };
            socket.send(serialize(chatMessage));
            // heartbeatTimer = null;
            heartbeatPing();
        }
    }, 57000);
}

function handleMessage(chatMessage) {
    if (pendedMessages) {
        pendedMessages.push(chatMessage);
        return;
    }
    Object.getOwnPropertyNames(chatMessage).forEach(function(val, idx, array) {
        let payload = chatMessage[val];
        if (payload) {
            switch (val) {
                case "heartBeat":
                    if (payload === "-pong-") {
                        heartbeatPing();
                    }
                    break;
                case "broadcast":
                    printMessage(payload);
                    break;
                case "userJoined":
                    addChater(payload.userNo, payload.username);
                    printJoinMessage(payload);
                    break;
                case "userLeft":
                    removeChater(payload.userNo);
                    printLeftMessage(payload);
                    break;
                case "join":
                    pendedMessages = [];
                    setChaters(payload.chaters);
                    printRecentConvo(payload.recentConvo);
                    printWelcomeMessage(payload);
                    while (pendedMessages && pendedMessages.length > 0) {
                        handleMessage(pendedMessages.pop());
                    }
                    pendedMessages = null;
                    break;
                case "abort":
                    aborted = true;
                    switch (payload.cause) {
                        case "exists":
                            alert("Username already in use.");
                            leaveRoom();
                            break;
                        case "rejoin":
                            $('#chatroom-rejoined').foundation('open');
                            break;
                        default:
                            alert("Abnormal access detected.");
                            leaveRoom();
                    }
                    break;
            }
        }
    });
}

function sendMessage() {
    let $msg = $("#message");
    let text = $msg.val().trim();
    if (text) {
        let message = {
            type: 'CHAT',
            userNo: currentUserNo,
            username: currentUsername,
            content: text
        }
        let chatMessage = {
            message: message
        };
        $msg.val('');
        socket.send(serialize(chatMessage));
        printMessage(message, text);
        $msg.focus();
    }
}

function leaveRoom() {
    socket.onclose = null;
    socket.close();
    socket = null;
    location.href = "/rooms";
}

function setChaters(chaters) {
    if (chaters) {
        for (let i in chaters) {
            let str = chaters[i];
            let index = str.indexOf(':');
            if (index > -1) {
                let userNo = Number(str.substring(0, index));
                let username = str.substring(index + 1);
                addChater(userNo, username);
            }
        }
        updateTotalPeople();
        findUser(currentUserNo).addClass("me");
    }
}

function addChater(userNo, username) {
    let contact = $("<li class='contact'/>")
        .data("user-no", userNo)
        .data("username", username);
    let status = $("<div/>").addClass("status badge");
    let badge = $("<i class='badge fi-record'/>");
    let name = $("<div class='name'/>").text(username);
    contact.append(status.append(badge)).append(name).appendTo($("#contacts"));
    updateTotalPeople();
}

function removeChater(userNo) {
    findUser(userNo).remove();
    updateTotalPeople();
}

function findUser(userNo) {
    return $("#contacts .contact")
        .filter(function() {
            return ($(this).data("user-no") === userNo);
        });
}

function clearChaters() {
    $("#contacts").empty();
    clearTotalPeople();
}

function getTotalPeople() {
    return $("#totalPeople").text();
}

function updateTotalPeople() {
    $("#totalPeople").text($("#contacts .contact").length);
}

function clearTotalPeople() {
    $("#totalPeople").text("");
}

function clearConvo() {
    $("#convo").empty();
}

function printWelcomeMessage(payload, animatable) {
    let text = "Welcome <strong>" + payload.username + "</strong>";
    printEvent(text, animatable);
}

function printJoinMessage(payload, animatable) {
    let text = "<strong>" + payload.username + "</strong> joined this chat";
    if (payload.prevUsername) {
        text += " (Previous username: " + payload.prevUsername + ")"
    }
    printEvent(text, animatable);
}

function printLeftMessage(payload, animatable) {
    let text = "<strong>" + payload.username + "</strong> has left this chat";
    printEvent(text, animatable);
}

function printMessage(payload, animatable) {
    let convo = $("#convo");
    let myself = (currentUserNo === payload.userNo);
    let sender = $("<span class='username'/>")
        .text(myself ? "You" : payload.username);
    let content = $("<span class='content'/>").text(payload.content);
    let lastMessage = $("#convo .message").last();
    if (lastMessage.length && lastMessage.data("user-no") === payload.userNo) {
        lastMessage.append(content);
    } else {
        let message = $("<div/>")
            .addClass(myself ? "message sent" : "message received")
            .data("user-no", payload.userNo)
            .data("username", payload.username)
            .append(sender).append(content);
        convo.append(message);
    }
    if (animatable !== false) {
        convo.animate({scrollTop: convo.prop("scrollHeight")});
    }
}

function printEvent(text, animatable) {
    let convo = $("#convo");
    let div = $("<div/>").addClass("message event");
    $("<p/>").addClass("content").html(text).appendTo(div);
    convo.append(div);
    if (animatable !== false) {
        convo.animate({scrollTop: convo.prop("scrollHeight")});
    }
}

function printError(text, animatable) {
    let convo = $("#convo");
    let div = $("<div/>").addClass("message event error");
    $("<p/>").addClass("content").html(text).appendTo(div);
    convo.append(div);
    if (animatable !== false) {
        convo.animate({scrollTop: convo.prop("scrollHeight")});
    }
}

function printRecentConvo(chatMessages) {
    for (let i in chatMessages) {
        let chatMessage = chatMessages[i];
        Object.getOwnPropertyNames(chatMessage).forEach(function(val, idx, array) {
            let payload = chatMessage[val];
            if (payload) {
                switch (val) {
                    case "broadcast":
                        printMessage(payload, false);
                        break;
                    case "userJoined":
                        printJoinMessage(payload, false);
                        break;
                    case "userLeft":
                        printLeftMessage(payload, false);
                        break;
                }
            }
        });
    }
    let convo = $("#convo");
    convo.animate({scrollTop: convo.prop("scrollHeight")});
}

function serialize(json) {
    return JSON.stringify(json);
}

function deserialize(str) {
    return JSON.parse(str);
}