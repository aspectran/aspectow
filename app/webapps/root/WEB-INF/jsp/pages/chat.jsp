<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="wrap grid-y grid-frame">
    <div class="header grid-container grid-padding-x cell header cell-block-container">
        <div class="left">
            <button type="button" class="button" title="People">
                <i class="fi-results-demographics"></i>
                <span id="totalPeople"></span></button>
            <h2>Chat</h2>
        </div>
        <div class="right">
            <button type="button" class="button leave" title="Leave the chat room"><i class="fi-power"></i></button>
        </div>
    </div>
    <div class="shadow-wrap grid-container cell auto cell-block-container">
        <div class="grid-x grid-padding-y full-height">
            <div id="contacts-wrap" class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <div id="contacts"></div>
            </div>
            <div class="cell auto cell-block-y">
                <div id="conversations" class="full-height"></div>
            </div>
        </div>
    </div>
    <div class="footer shadow-wrap grid-container cell">
        <div class="grid-x grid-padding-y full-height">
            <div class="sidebar cell small-12 medium-4 large-3 cell-block-y hide-for-small-only">
            </div>
            <div class="cell auto cell-block-y">
                <form id="chat-controls">
                    <div class="input-group">
                        <input class="input-group-field" type="text" id="message" autocomplete="off" placeholder="Type a message..."/>
                        <div class="input-group-button">
                            <button type="submit" class="button">Send</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<div id="connection-lost" class="reveal" data-reveal>
    <h1>Connection lost</h1>
    <p class="lead">You have lost connection with the server.</p>
    <div class="button-group align-right">
        <a class="success button" href="/">Home</a>
        <a class="warning button" href="">Reload this page</a>
    </div>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
</div>
<script>
    const currentUser = "${user.username}";
    let socket;
    let heartbeatTimer;
    let pendedMessages;
    let aborted;

    $(function() {
        if (!currentUser) {
            location.href = "/";
        }

        $("form#chat-controls").submit(function() {
            if (!$("#contacts-wrap").hasClass("hide-for-small-only")) {
                $("#contacts-wrap").addClass("hide-for-small-only");
            }
            sendMessage();
            return false;
        });
        $(".header .left button").click(function() {
            $(".sidebar").toggle()
            $("#contacts-wrap").toggleClass("hide-for-small-only");
            if ($("#contacts-wrap").hasClass("hide-for-small-only") || !$("#contacts-wrap").is(":visible")) {
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
        let url = new URL('/chat', location.href);
        url.protocol = url.protocol.replace('https:', 'wss:');
        url.protocol = url.protocol.replace('http:', 'ws:');
        socket = new WebSocket(url.href);
        socket.onopen = function(event) {
            let chatMessage = {
                sendMessage: {
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
                location.href = "/";
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
            printError('Could not connect to WebSocket server. Please refresh this page to try again!');
        };
    }
    
    function heartbeatPing() {
        if (heartbeatTimer) {
            clearTimeout(heartbeatTimer);
        }
        this.heartbeatTimer = setTimeout(function() {
            if (socket) {
                let chatMessage = {
                    heartBeat: "--heartbeat-ping--"
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
                        if (payload === "--heartbeat-pong--") {
                            heartbeatPing();
                        }
                        break;
                    case "broadcastMessage":
                        printMessage(payload.username, payload.content);
                        break;
                    case "broadcastUserJoined":
                        addUser(payload.username);
                        printJoinMessage(payload.username, payload.prevUsername);
                        break;
                    case "broadcastUserLeft":
                        removeUser(payload.username);
                        printLeaveMessage(payload.username);
                        break;
                    case "broadcastJoinedUsers":
                        clearUsers();
                        for (let i = 0; i < payload.usernames.length; i++) {
                            addUser(payload.usernames[i]);
                        }
                        thatsMe(currentUser);
                        break;
                    case "welcomeUser":
                        pendedMessages = [];
                        printRecentConversations(payload.recentConversations);
                        if (!payload.rejoin) {
                            printWelcomeMessage(payload.username);
                        }
                        while (pendedMessages && pendedMessages.length > 0) {
                            handleMessage(pendedMessages.pop());
                        }
                        pendedMessages = null;
                        break;
                    case "abnormalAccess":
                        aborted = true;
                        switch (payload.cause) {
                            case "exists":
                                alert("Username already in use.");
                                break;
                            case "rejoin":
                                break;
                            default:
                                alert("Abnormal access detected.");
                        }
                        leaveRoom();
                        break;
                }
            }
        });
    }

    function sendMessage() {
        let text = $("#message").val().trim();
        if (text) {
            let chatMessage = {
                sendMessage: {
                    type: 'CHAT',
                    username: currentUser,
                    content: text
                }
            };
            socket.send(serialize(chatMessage));
            printMessage(currentUser, text);
            $("#message").val('').focus();
        }
    }

    function leaveRoom() {
        socket.onclose = null;
        socket.close();
        socket = null;
        location.href = "/";
    }

    function addUser(username) {
        let contact = $("<div/>").addClass("contact").data("username", username);
        let status = $("<div/>").addClass("status");
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
            message.addClass("same-sender-previous-message");
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
                        case "broadcastMessage":
                            printMessage(payload.username, payload.content, false);
                            break;
                        case "broadcastUserJoined":
                            printJoinMessage(payload.username, payload.prevUsername, false);
                            break;
                        case "broadcastUserLeft":
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
</script>