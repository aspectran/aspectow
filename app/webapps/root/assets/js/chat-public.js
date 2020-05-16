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
    $("#convo").on("click", ".message.event.group .more", function() {
        $(this).parent().toggleClass("all-visible");
    });
    readyToType();
    if (autoConnect !== false) {
        openSocket();
    }
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

function closeSocket() {
    if (socket) {
        socket.onclose = null;
        socket.close();
        socket = null;
    }
}

function leaveRoom() {
    closeSocket();
    location.href = "/rooms";
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
                    printUserJoinedMessage(payload);
                    break;
                case "userLeft":
                    removeChater(payload.userNo);
                    printUserLeftMessage(payload);
                    break;
                case "join":
                    pendedMessages = [];
                    setChaters(payload.chaters);
                    printRecentConvo(payload.recentConvo);
                    printJoinMessage(payload);
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

function readyToType(select) {
    if (select) {
        $("#message").focus().select();
    } else {
        $("#message").focus();
    }
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

function printJoinMessage(payload, restored) {
    let text = "Welcome <strong>" + payload.username + "</strong>";
    printEvent(text, restored);
}

function printUserJoinedMessage(payload, restored, container) {
    printUserEvent(payload, "user-joined", restored, container);
}

function printUserLeftMessage(payload, restored, container) {
    printUserEvent(payload, "user-left", restored, container);
}

function printUserEvent(payload, event, restored, container) {
    let convo = $("#convo");
    if (!restored) {
        let last = convo.find(".message.event").last();
        if (last.length > 0) {
            let userNo = last.data("user-no");
            if (payload.userNo === userNo) {
                container = last;
            }
        }
    }
    let content = $("<p class='content'/>").addClass(event).data("event", event);
    content.append("<strong>" + payload.username + "</strong> ");
    switch (event) {
        case "user-joined":
            content.append("joined this chat");
            break;
        case "user-left":
            content.append("has left this chat");
            break;
        default:
            console.error("Unknown user event: " + event);
            return;
    }
    if (payload.datetime) {
        let datetime = moment.utc(payload.datetime).local().format("L LT");
        content.append("<span class='datetime'>" + datetime + "</span>");
    }
    if (container) {
        let contents = container.find(".content");
        if (contents.length >= 30) {
            contents.first().remove();
        }
        container.addClass("group").append(content);
        contents = container.find(".content").addClass("omitted");
        let first = contents.first();
        let last = contents.last();
        if (first.data("event") !== last.data("event")) {
            first.removeClass("omitted");
        }
        last.removeClass("omitted");
        if (contents.length > 2) {
            let more = container.find(".more");
            if (more.length > 0) {
                more.attr("title", contents.length)
            } else {
                $("<i class='more fi-indent-more'></i>").attr("title", contents.length).insertAfter(first);
            }
        }
    } else {
        $("<div class='message event'/>")
            .data("user-no", payload.userNo)
            .append(content)
            .appendTo(convo);
    }
    if (!restored) {
        convo.animate({scrollTop: convo.prop("scrollHeight")});
    }
}

function printMessage(payload, restored) {
    let convo = $("#convo");
    let myself = (currentUserNo === payload.userNo);
    let sender = $("<span class='username'/>")
        .text(myself ? "You" : payload.username);
    let content = $("<p class='content'/>").text(payload.content);
    if (payload.datetime) {
        let datetime = moment.utc(payload.datetime).local();
        let hours = moment.duration(moment().diff(datetime)).asHours();
        content.append("<span class='datetime'>" +
            datetime.format(hours < 24 ? "LTS" : "L LT") + "</span>");
    }
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
    if (!restored) {
        convo.animate({scrollTop: convo.prop("scrollHeight")});
    }
}

function printEvent(text, restored, container, event) {
    let convo = $("#convo");
    let content = $("<p class='content'/>").html(text);
    if (event) {
        content.addClass(event).data("event", event);
    }
    if (container) {
        container.addClass("group").append(content);
    } else {
        $("<div class='message event'/>")
            .append(content)
            .appendTo(convo);
    }
    if (!restored) {
        convo.animate({scrollTop: convo.prop("scrollHeight")});
    }
}

function printError(text, restored) {
    let convo = $("#convo");
    let div = $("<div/>").addClass("message event error");
    $("<p/>").addClass("content").html(text).appendTo(div);
    convo.append(div);
    if (!restored) {
        convo.animate({scrollTop: convo.prop("scrollHeight")});
    }
}

function printRecentConvo(chatMessages) {
    let convo = $("#convo");
    let prevUserNo = null;
    let container = null;
    for (let i in chatMessages) {
        let chatMessage = chatMessages[i];
        Object.getOwnPropertyNames(chatMessage).forEach(function(val, idx, array) {
            let payload = chatMessage[val];
            if (payload) {
                switch (val) {
                    case "broadcast":
                        printMessage(payload, true);
                        prevUserNo = null;
                        break;
                    case "userJoined":
                        if (prevUserNo === payload.userNo) {
                            if (!container) {
                                container = convo.find(".message.event").last();
                            }
                        } else {
                            container = null;
                        }
                        printUserJoinedMessage(payload, true, container);
                        prevUserNo = payload.userNo;
                        break;
                    case "userLeft":
                        if (prevUserNo === payload.userNo) {
                            if (!container) {
                                container = convo.find(".message.event").last();
                            }
                        } else {
                            container = null;
                        }
                        printUserLeftMessage(payload, true, container);
                        prevUserNo = payload.userNo;
                        break;
                }
            }
        });
    }
    convo.animate({scrollTop: convo.prop("scrollHeight")});
}

function serialize(json) {
    return JSON.stringify(json);
}

function deserialize(str) {
    return JSON.parse(str);
}