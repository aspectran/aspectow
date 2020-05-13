let socket;
let heartbeatTimer;
let pendedMessages;
let aborted;

$(function() {
    if (!currentUserNo || !currentUsername || !admissionToken) {
        location.href = "/";
    }

    $("form#send-message").submit(function() {
        if (!$("#contacts-wrap").hasClass("show-for-medium")) {
            $("#contacts-wrap").addClass("show-for-medium");
        }
        $("#for-automata-clear").focus();
        sendMessage();
        return false;
    });
    $(".header button.people").on("click", function() {
        $(".sidebar").toggle().toggleClass("show-for-medium");
        if ($(".sidebar").hasClass("show-for-medium") || !$(".sidebar").is(":visible")) {
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
    if (socket) {
        socket.close();
        socket = null;
    }
    let url = new URL('/chat/' + admissionToken, location.href);
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
            heartbeatTimer = null;
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
                    if (payload === "--pong--") {
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
                    printLeaveMessage(payload);
                    break;
                case "join":
                    pendedMessages = [];
                    printRecentConvos(payload.recentConvos);
                    printWelcomeMessage(payload);
                    while (pendedMessages && pendedMessages.length > 0) {
                        handleMessage(pendedMessages.pop());
                    }
                    pendedMessages = null;
                    break;
                case "chaters":
                    clearUsers();
                    setChaters(payload);
                    thatsMe(currentUserNo);
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
    let text = $("#message").val().trim();
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
        $("#message").val('');
        socket.send(serialize(chatMessage));
        printMessage(message, text);
        $("#message").focus();
    }
}

function leaveRoom() {
    socket.onclose = null;
    socket.close();
    socket = null;
    location.href = "/rooms";
}

function setChaters(payload) {
    if (payload.chaters) {
        for (let i in payload.chaters) {
            let str = payload.chaters[i];
            let index = str.indexOf(':');
            if (index > -1) {
                let userNo = Number(str.substring(0, index));
                let username = str.substring(index + 1);
                addChater(userNo, username);
            }
        }
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

function thatsMe(userNo) {
    findUser(userNo).addClass("me");
    updateTotalPeople();
}

function clearUsers() {
    $("#contacts").empty();
    clearTotalPeople();
}

function updateTotalPeople() {
    $("#totalPeople").text($("#contacts .contact").length);
}

function clearTotalPeople() {
    $("#totalPeople").text("");
}

function printWelcomeMessage(payload, animatable) {
    let text = "Welcome <strong>" + payload.username + "</strong>";
    printEvent(text, animatable);
}

function printJoinMessage(payload, animatable) {
    let text = "<strong>" + payload.username + "</strong> joined the chat";
    if (payload.prevUsername) {
        text += " (Previous username: " + payload.prevUsername + ")"
    }
    printEvent(text, animatable);
}

function printLeaveMessage(payload, animatable) {
    let text = "<strong>" + payload.username + "</strong> left the chat";
    printEvent(text, animatable);
}

function printMessage(payload, animatable) {
    let myself = (currentUserNo === payload.userNo);
    let sender = $("<span class='username'/>")
        .text(myself ? "You" : payload.username);
    let content = $("<span class='content'/>").text(payload.content);
    let lastMessage = $("#convos .message").last();
    if (lastMessage.length && lastMessage.data("user-no") === payload.userNo) {
        lastMessage.append(content);
    } else {
        let message = $("<div/>")
            .addClass(myself ? "message sent" : "message received")
            .data("user-no", payload.userNo)
            .data("username", payload.username)
            .append(sender).append(content);
        $("#convos").append(message);
    }
    if (animatable !== false) {
        $("#convos").animate({scrollTop: $("#convos").prop("scrollHeight")});
    }
}

function printEvent(text, animatable) {
    let div = $("<div/>").addClass("message event");
    $("<p/>").addClass("content").html(text).appendTo(div);
    $("#convos").append(div);
    if (animatable !== false) {
        $("#convos").animate({scrollTop: $("#convos").prop("scrollHeight")});
    }
}

function printError(text, animatable) {
    let div = $("<div/>").addClass("message event error");
    $("<p/>").addClass("content").html(text).appendTo(div);
    $("#convos").append(div);
    if (animatable !== false) {
        $("#convos").animate({scrollTop: $("#convos").prop("scrollHeight")});
    }
}

function printRecentConvos(chatMessages) {
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
                        printLeaveMessage(payload, false);
                        break;
                }
            }
        });
    }
    $("#convos").animate({scrollTop: $("#convos").prop("scrollHeight")});
}

function serialize(json) {
    return JSON.stringify(json);
}

function deserialize(str) {
    return JSON.parse(str);
}