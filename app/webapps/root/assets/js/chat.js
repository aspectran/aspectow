let socket;
let heartbeatTimer;
let pendedMessages;
let aborted;

$(function() {
    if (!currentUser || !admissionToken) {
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
    $(".header button.people").click(function() {
        $(".sidebar").toggle()
        $(".sidebar").toggleClass("show-for-medium");
        if ($(".sidebar").hasClass("show-for-medium") || !$(".sidebar").is(":visible")) {
            $("#message").focus();
        }
    })
    $("button.leave").click(function() {
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
                username: currentUser
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
            location.href = "/lobby";
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
                    printMessage(payload.username, payload.content);
                    break;
                case "userJoined":
                    addUser(payload.username);
                    printJoinMessage(payload.username, payload.prevUsername);
                    break;
                case "userLeft":
                    removeUser(payload.username);
                    printLeaveMessage(payload.username);
                    break;
                case "join":
                    console.log(payload);
                    pendedMessages = [];
                    printRecentConversations(payload.recentConversations);
                    printWelcomeMessage(payload.username);
                    while (pendedMessages && pendedMessages.length > 0) {
                        handleMessage(pendedMessages.pop());
                    }
                    pendedMessages = null;
                    break;
                case "joinedUsers":
                    clearUsers();
                    for (let i = 0; i < payload.usernames.length; i++) {
                        addUser(payload.usernames[i]);
                    }
                    thatsMe(currentUser);
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
        let chatMessage = {
            message: {
                type: 'CHAT',
                username: currentUser,
                content: text
            }
        };
        $("#message").val('');
        socket.send(serialize(chatMessage));
        printMessage(currentUser, text);
        $("#message").focus();
    }
}

function leaveRoom() {
    socket.onclose = null;
    socket.close();
    socket = null;
    location.href = "/lobby";
}

function addUser(username) {
    let contact = $("<li/>").addClass("contact").data("username", username);
    let status = $("<div/>").addClass("status badge");
    let badge = $("<i class='badge fi-record'/>");
    let name = $("<div/>").addClass("name").text(username);
    contact.append(status.append(badge)).append(name).appendTo($("#contacts"));
    updateTotalPeople();
}

function removeUser(username) {
    findUser(username).remove();
    updateTotalPeople();
}

function thatsMe(username) {
    findUser(username).addClass("me");
    updateTotalPeople();
}

function findUser(username) {
    return $("#contacts .contact")
        .filter(function() {
            return $(this).data("username") === username;
        });
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

function printWelcomeMessage(username, animatable) {
    let text = "Welcome <strong>" + username + "</strong>";
    printEvent(text, animatable);
}

function printJoinMessage(username, prevUsername, animatable) {
    let text = "<strong>" + username + "</strong> joined the chat";
    if (prevUsername) {
        text += " (Previous username: " + prevUsername + ")"
    }
    printEvent(text, animatable);
}

function printLeaveMessage(username, animatable) {
    let text = "<strong>" + username + "</strong> left the chat";
    printEvent(text, animatable);
}

function printMessage(username, text, animatable) {
    let sentByCurrentUer = (currentUser === username);
    let message = $("<div/>").addClass(sentByCurrentUer === true ? "message sent" : "message received");
    message.data("sender", username);

    let sender = $("<span/>").addClass("sender");
    sender.text(sentByCurrentUer === true ? "You" : username);
    sender.appendTo(message);

    let content = $("<span/>").addClass("content").text(text);
    content.appendTo(message);

    let lastMessage = $("#conversations .message").last();
    if (lastMessage.length && lastMessage.data("sender") === username) {
        message.addClass("same-sender");
    }

    $("#conversations").append(message);
    if (animatable !== false) {
        $("#conversations").animate({scrollTop: $("#conversations").prop("scrollHeight")});
    }
}

function printEvent(text, animatable) {
    let div = $("<div/>").addClass("message event");
    $("<p/>").addClass("content").html(text).appendTo(div);
    $("#conversations").append(div);
    if (animatable !== false) {
        $("#conversations").animate({scrollTop: $("#conversations").prop("scrollHeight")});
    }
}

function printError(text, animatable) {
    let div = $("<div/>").addClass("message event error");
    $("<p/>").addClass("content").html(text).appendTo(div);
    $("#conversations").append(div);
    if (animatable !== false) {
        $("#conversations").animate({scrollTop: $("#conversations").prop("scrollHeight")});
    }
}

function printRecentConversations(chatMessages) {
    for (let i in chatMessages) {
        let chatMessage = chatMessages[i];
        Object.getOwnPropertyNames(chatMessage).forEach(function(val, idx, array) {
            let payload = chatMessage[val];
            if (payload) {
                switch (val) {
                    case "broadcast":
                        printMessage(payload.username, payload.content, false);
                        break;
                    case "userJoined":
                        printJoinMessage(payload.username, payload.prevUsername, false);
                        break;
                    case "userLeft":
                        printLeaveMessage(payload.username, false);
                        break;
                }
            }
        });
    }
    $("#conversations").animate({scrollTop: $("#conversations").prop("scrollHeight")});
}

function serialize(json) {
    return JSON.stringify(json);
}

function deserialize(str) {
    return JSON.parse(str);
}