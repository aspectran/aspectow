let socket;
let heartbeatTimer;
let heartbeatCount = 0;
let pendedMessages;
let frequentlySentCount = 0;
let chatAborted;
let justStayHere;

$(function () {
    if (!chatClientSettings) {
        return;
    }
    $("button.signout").off().click(function () {
        $("button.signout").prop("disabled", true);
        closeSocket();
        setTimeout(function () {
            location.href = "/signout";
        }, 900);
    });
    $(".header button.people").on("click", function () {
        toggleSidebar();
    });
    $("button.leave").on("click", function () {
        $("button.leave").prop("disabled", true);
        closeSocket();
        setTimeout(function () {
            leaveRoom();
        }, 500);
    });
    $("#convo").on("click", ".message.event.group .more", function () {
        $(this).parent().toggleClass("all-visible");
    });
    $("#message").on("focusin", function () {
        hideSidebar();
    });
    $("#form-send-message").submit(function () {
        if (!$("#message").val()) {
            return false;
        }
        if (frequentlySentCount > 1) {
            return false;
        }
        $("#for-automata-clear").focus();
        if (userInfo.userNo) {
            sendMessage();
        }
        frequentlySentCount++;
        $("#form-send-message button.send").addClass("busy");
        setTimeout(function () {
            $("#form-send-message button.send").removeClass("busy");
        }, 500);
        setTimeout(function () {
            frequentlySentCount--;
        }, 1000);
        return false;
    });
    readyToType();
    if (chatClientSettings.admissionToken && chatClientSettings.autoConnectEnabled !== false) {
        setTimeout(function () {
            openSocket(chatClientSettings.admissionToken);
        }, 300);
    }
});

function openSocket(token) {
    if (!token || token.length > 100) {
        gotoHomepage();
        return;
    }
    closeSocket();
    let url = new URL(chatClientSettings.serverEndpoint + token, location.href);
    url.protocol = url.protocol.replace('https:', 'wss:');
    url.protocol = url.protocol.replace('http:', 'ws:');
    socket = new WebSocket(url.href);
    socket.onopen = function (event) {
        let chatMessage = {
            message: {
                type: 'JOIN',
                userNo: userInfo.userNo,
                username: userInfo.username
            }
        };
        socket.send(serialize(chatMessage));
        heartbeatPing();
    };
    socket.onmessage = function (event) {
        if (typeof event.data === "string") {
            let chatMessage = deserialize(event.data);
            handleMessage(chatMessage);
        }
    };
    socket.onclose = function (event) {
        if (chatAborted ) {
            closeSocket();
            if (!justStayHere) {
                gotoHomepage();
            }
        } else {
            closeSocket();
            checkConnection(100);
        }
    };
    socket.onerror = function (event) {
        console.error("WebSocket error observed:", event);
        closeSocket();
        checkConnection(100);
    };
}

function heartbeatPing() {
    if (heartbeatTimer) {
        clearTimeout(heartbeatTimer);
    }
    heartbeatTimer = setTimeout(function () {
        if (socket) {
            let chatMessage = {
                heartBeat: "-ping-"
            };
            socket.send(serialize(chatMessage));
            heartbeatPing();
            heartbeatCount++;
            if (heartbeatCount % 15 === 0) {
                $.ajax({
                    url: '/ping',
                    type: 'get',
                    dataType: 'text',
                    success: function (result) {
                        if (result !== "pong") {
                            leaveRoom();
                        }
                    },
                    error: function () {
                        leaveRoom();
                    }
                });
            }
        }
    }, 57000);
}

function checkConnection(delay) {
    setTimeout(function () {
        $.ajax({
            url: '/ping',
            type: 'get',
            dataType: 'text',
            timeout: 30000,
            success: function (result) {
                if (result === "pong" && !chatAborted) {
                    reloadPage();
                } else {
                    gotoHomepage();
                }
            },
            error: function () {
                let retries = $("#common-connection-lost").data("retries")||0;
                $("#common-connection-lost").data("retries", retries + 1);
                if (retries === 0) {
                    $("#common-connection-lost").foundation('open');
                } else if (retries > 25) {
                    console.log("Abandon reconnection");
                    return;
                }
                console.log(retries + " retries");
                checkConnection(2000 * retries);
            }
        });
    }, delay);
}

function closeSocket() {
    if (socket) {
        socket.onclose = null;
        socket.close();
        socket = null;
    }
}

function leaveRoom(force) {
    closeSocket();
    gotoHomepage();
}

function handleMessage(chatMessage) {
    if (pendedMessages) {
        pendedMessages.push(chatMessage);
        return;
    }
    Object.getOwnPropertyNames(chatMessage).forEach(function (val, idx, array) {
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
                    if (payload.recentConvo) {
                        printRecentConvo(payload.recentConvo);
                    }
                    printJoinMessage(payload);
                    while (pendedMessages && pendedMessages.length > 0) {
                        handleMessage(pendedMessages.pop());
                    }
                    pendedMessages = null;
                    break;
                case "abort":
                    chatAborted = true;
                    justStayHere = false;
                    switch (payload.cause) {
                        case "exists":
                            alert("Username already in use. Please sign in again.");
                            leaveRoom(true);
                            break;
                        case "rejoin":
                            justStayHere = true;
                            clearChaters();
                            clearConvo();
                            closeSocket();
                            $("#chat-duplicate-join").foundation('open');
                            break;
                        default:
                            justStayHere = true;
                            serviceNotAvailable();
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
            userNo: userInfo.userNo,
            username: userInfo.username,
            content: text
        }
        let chatMessage = {
            message: message
        };
        $msg.val('');
        socket.send(serialize(chatMessage));
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
    }
}

function addChater(userNo, username) {
    let contact = $("<li class='contact'/>")
        .data("user-no", userNo)
        .data("username", username);
    let status = $("<div/>").addClass("status");
    let badge = $("<i class='iconfont fi-record'/>");
    let name = $("<div class='name'/>").text(username);
    contact.append(status.append(badge)).append(name).appendTo($("#contacts"));
    if (userInfo.userNo === userNo) {
        contact.addClass("me");
    }
    updateTotalPeople();
}

function removeChater(userNo) {
    findUser(userNo).remove();
    updateTotalPeople();
}

function findUser(userNo) {
    return $("#contacts .contact")
        .filter(function () {
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

function printUserJoinedMessage(payload, restored) {
    printUserEvent(payload, "user-joined", restored);
}

function printUserLeftMessage(payload, restored) {
    printUserEvent(payload, "user-left", restored);
}

function printUserEvent(payload, event, restored) {
    let convo = $("#convo");
    let last = convo.find(".message").last();
    let container = null;
    if (last.length) {
        let userNo = last.data("user-no");
        if (last.hasClass("event") && payload.userNo === userNo) {
            container = last;
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
        scrollToBottom(convo);
    }
}

function printMessage(payload, restored) {
    let myself = (userInfo.userNo === payload.userNo);
    let sender = $("<span class='username'/>")
        .text(myself ? "You" : payload.username);
    let content = $("<p class='content'/>").text(payload.content);
    if (payload.datetime) {
        let datetime = moment.utc(payload.datetime).local();
        let hours = moment.duration(moment().diff(datetime)).asHours();
        content.append("<span class='datetime'>" +
            datetime.format(hours < 24 ? "LTS" : "L LT") + "</span>");
    }
    let convo = $("#convo");
    let last = convo.find(".message").last();
    if (last.length && !last.hasClass("event") && last.data("user-no") === payload.userNo) {
        if (restored) {
            last.addClass("restored");
        }
        last.append(content);
    } else {
        let message = $("<div/>")
            .addClass(myself ? "message sent" : "message received")
            .data("user-no", payload.userNo)
            .data("username", payload.username)
            .append(sender).append(content);
        if (restored) {
            message.addClass("restored");
        } else if (payload.color) {
            message.addClass("my-col-" + payload.color);
        }
        convo.append(message);
    }
    if (!restored) {
        scrollToBottom(convo);
    }
}

function printEvent(text, restored) {
    let convo = $("#convo");
    let content = $("<p class='content'/>").html(text);
    $("<div class='message event'/>")
        .append(content)
        .appendTo(convo);
    if (!restored) {
        scrollToBottom(convo);
    }
}

function printError(text, restored) {
    let convo = $("#convo");
    let div = $("<div/>").addClass("message event error");
    $("<p/>").addClass("content").html(text).appendTo(div);
    convo.append(div);
    if (!restored) {
        scrollToBottom(convo);
    }
}

function printRecentConvo(chatMessages) {
    for (let i in chatMessages) {
        let chatMessage = chatMessages[i];
        Object.getOwnPropertyNames(chatMessage).forEach(function (val, idx, array) {
            let payload = chatMessage[val];
            if (payload) {
                switch (val) {
                    case "broadcast":
                        printMessage(payload, true);
                        break;
                    case "userJoined":
                        printUserJoinedMessage(payload, true);
                        break;
                    case "userLeft":
                        printUserLeftMessage(payload, true);
                        break;
                }
            }
        });
    }
    scrollToBottom($("#convo"), false);
}

function toggleSidebar() {
    $(".sidebar").toggleClass("hide-for-small-only").toggleClass("show-for-small-only");
}

function hideSidebar(force) {
    let sidebar = $(".sidebar");
    if (sidebar.is(":visible") && !sidebar.hasClass("hide-for-small-only")) {
        toggleSidebar();
    }
}

function scrollToBottom(container, animate) {
    if (animate) {
        container.animate({scrollTop: container.prop("scrollHeight")});
    } else {
        container.scrollTop(container.prop("scrollHeight"));
    }
}

function reloadPage() {
    location.reload();
}

function gotoHomepage() {
    if (chatClientSettings.homepage) {
        location.href = chatClientSettings.homepage;
    }
}

function serviceNotAvailable() {
    openNoticePopup("Please note",
        "Sorry. Our service is unavailable due to a system error.",
        function () {
            gotoHomepage();
        });
}

function serialize(json) {
    return JSON.stringify(json);
}

function deserialize(str) {
    return JSON.parse(str);
}