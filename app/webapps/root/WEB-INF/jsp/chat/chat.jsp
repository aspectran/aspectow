<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="wrap grid-y grid-frame">
    <div class="header grid-container cell header cell-block-container">
        <div class="left">
            <h2>Text Chat Club</h2>
            <span id="totalPeople" class="badge primary hidden"></span>
        </div>
        <div class="right">
            <button type="button" class="button leave" onclick="leaveRoom();">Leave</button>
        </div>
    </div>
    <div class="shadow-wrap grid-container cell auto cell-block-container">
        <div class="grid-x grid-padding-y full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <div id="contacts"></div>
            </div>
            <div class="cell auto cell-block-y">
                <form id="sign-in" method="post" onsubmit="return false;">
                    <h3>Only one chat room!</h3>
                    <h4>Chat with anyone you want, about anything you want, free.</h4>
                    <h4>Please enter your username.</h4>
                    <div class="input-group">
                        <input class="input-group-field" type="text" id="username" maxlength="30" placeholder="Username" autocomplete="off" autofocus/>
                        <div class="input-group-button">
                            <button type="submit" class="button">Join</button>
                        </div>
                    </div>
                    <div id="inline-badge"></div>
                </form>
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
<script src="https://www.google.com/recaptcha/api.js?render=explicit&onload=loadCaptcha"></script>
<script>
    let clientId;
    let recaptchaResponse;
    function loadCaptcha() {
        grecaptcha.ready(function() {
            clientId = grecaptcha.render('inline-badge', {
                'sitekey': '6Ldt0r0UAAAAAP4ejDGFZLB0S-zDzWL3ZkB49FvN',
                'badge': 'inline',
                'size': 'invisible'
            });
        });
    }
    function executeCaptcha() {
        grecaptcha.ready(function() {
            grecaptcha.execute(clientId, {
                action: 'sign_in'
            }).then(function(token) {
                recaptchaResponse = token;
                startChat();
            });
        });
    }
</script>
<script>
    let socket;
    let currentUser;
    let pendedMessages;
    let heartbeatTimer;

    $(function() {
        $("form#sign-in").submit(function() {
            let username = $("#username").val().trim();
            if (!username) {
                return false;
            }
            $("#username").val(username);
            executeCaptcha();
            return false;
        });
        $("form#chat-controls").submit(function() {
            sendMessage();
            return false;
        });
    });

    function startChat() {
        if (!recaptchaResponse) {
            return;
        }
        currentUser = $("#username").val().trim();
        $("#username").val("");
        if (currentUser) {
            $("#sign-in").hide();
            $("#conversations").show();
            $("#chat-controls").show();
            $("button.leave").show();
            $("#message").focus();
            openSocket();
        }
    }

    function openSocket() {
        if (socket) {
            socket.close();
            socket = null;
        }
        let url = new URL('/chat?' + recaptchaResponse, location.href);
        url.protocol = url.protocol.replace('https:', 'wss:');
        url.protocol = url.protocol.replace('http:', 'ws:');
        socket = new WebSocket(url.href);
        socket.onopen = function (event) {
            let chatMessage = {
                sendTextMessage: {
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
            location.reload();
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
                    case "welcomeUser":
                        pendedMessages = [];
                        printRecentConversations(payload.recentConversations);
                        printWelcomeMessage(payload.username);
                        while (pendedMessages && pendedMessages.length > 0) {
                            handleMessage(pendedMessages.pop());
                        }
                        pendedMessages = null;
                        break;
                    case "duplicatedUser":
                        socket.close();
                        alert("Your username is already in use. Please enter a different username.");
                        location.reload();
                        break;
                    case "broadcastTextMessage":
                        printMessage(payload.username, payload.content);
                        break;
                    case "broadcastConnectedUser":
                        printJoinMessage(payload.username);
                        break;
                    case "broadcastDisconnectedUser":
                        printLeaveMessage(payload.username);
                        break;
                    case "broadcastAvailableUsers":
                        cleanAvailableUsers();
                        for (let i = 0; i < payload.usernames.length; i++) {
                            addAvailableUsers(payload.usernames[i]);
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
                sendTextMessage: {
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
        socket.close();
    }

    function addAvailableUsers(username) {
        let contact = $("<div/>").addClass("contact");
        let status = $("<div/>").addClass("status");
        let name = $("<span/>").addClass("name").text(username);
        contact.append(status).append(name).appendTo($("#contacts"));
        updateTotalPeople();
    }

    function cleanAvailableUsers() {
        $("#contacts").empty();
        clearTotalPeople();
    }

    function updateTotalPeople() {
        $("#totalPeople").text($("#contacts .contact").length).removeClass("hidden");
    }

    function clearTotalPeople() {
        $("#totalPeople").text("").addClass("hidden");
    }

    function printWelcomeMessage(username, animatable) {
        let text = "Welcome <strong>" + username + "</strong>";
        printEvent(text, animatable);
    }

    function printJoinMessage(username, animatable) {
        let text = "<strong>" + username + "</strong> joined the chat";
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
                        case "broadcastTextMessage":
                            printMessage(payload.username, payload.content, false);
                            break;
                        case "broadcastConnectedUser":
                            printJoinMessage(payload.username, false);
                            break;
                        case "broadcastDisconnectedUser":
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