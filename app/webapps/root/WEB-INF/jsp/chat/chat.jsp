<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="wrap grid-container">
    <div class="grid-x grid-padding-x">
        <div class="header cell">
            <h2>Chat <span id="totalPeople"></span></h2>
            <a class="leave" onclick="leaveRoom();">Leave</a>
        </div>
        <div class="sidebar cell medium-4 large-3 hide-for-small-only">
            <div id="contacts"></div>
        </div>
        <div class="main cell small-12 medium-8 large-9">
            <form id="sign-in" method="post" onsubmit="return false;">
                <h3>Only one chat room!</h3>
                <h4>If no one chats for a minute, the chat room will close.<br/>
                    So shall we start chatting?</h4>
                <h4>Please enter your username.</h4>
                <div class="input-group">
                    <input class="input-group-field" type="text" id="username" maxlength="30" placeholder="Username" autocomplete="off" autofocus/>
                    <div class="input-group-button">
                        <button type="submit" class="button" onclick="executeCaptcha()">Join</button>
                    </div>
                </div>
                <div id="inline-badge"></div>
            </form>
            <div id="conversations"></div>
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
<script src="https://www.google.com/recaptcha/api.js?render=explicit&onload=loadCaptcha"></script>
<script>
    var clientId;
    var recaptchaResponse;
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
    var socket;
    var currentUser;
    var pendedMessages;

    $(function() {
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
            $("a.leave").show();
            $("#message").focus();
            openSocket();
        }
    }

    function openSocket() {
        if (socket) {
            socket.close();
        }
        var url = new URL('/chat?' + recaptchaResponse, location.href);
        url.protocol = url.protocol.replace('https:', 'wss:');
        url.protocol = url.protocol.replace('http:', 'ws:');
        socket = new WebSocket(url.href);
        socket.onopen = function (event) {
            var chatMessage = {
                sendTextMessage: {
                    type: 'JOIN',
                    username: currentUser
                }
            };
            socket.send(serialize(chatMessage));
        };

        socket.onmessage = function (event) {
            if (typeof event.data === "string") {
                var chatMessage = deserialize(event.data);
                handleMessage(chatMessage);
            }
        };

        socket.onclose = function (event) {
            location.reload();
        };

        socket.onerror = function (event) {
            console.error("WebSocket error observed:", event);
            printError('Could not connect to WebSocket server. Please refresh this page to try again!');
        };
    }

    function handleMessage(chatMessage) {
        if (pendedMessages) {
            pendedMessages.push(chatMessage);
            return;
        }
        Object.getOwnPropertyNames(chatMessage).forEach(function(val, idx, array) {
            var payload = chatMessage[val];
            if (payload) {
                switch (val) {
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
                        for (var i = 0; i < payload.usernames.length; i++) {
                            addAvailableUsers(payload.usernames[i]);
                        }
                        break;
                }
            }
        });
    }

    function sendMessage() {
        var text = $("#message").val().trim();
        if (text) {
            var chatMessage = {
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
        var contact = $("<div/>").addClass("contact");
        var status = $("<div/>").addClass("status");
        var name = $("<span/>").addClass("name").text(username);
        contact.append(status).append(name).appendTo($("#contacts"));
        updateTotalPeople();
    }

    function cleanAvailableUsers() {
        $("#contacts").empty();
        clearTotalPeople();
    }

    function updateTotalPeople() {
        $("#totalPeople").text("(" + $("#contacts .contact").length + ")");
    }

    function clearTotalPeople() {
        $("#totalPeople").text("");
    }

    function printWelcomeMessage(username, animatable) {
        var text = "Welcome <strong>" + username + "</strong>";
        printEvent(text, animatable);
    }

    function printJoinMessage(username, animatable) {
        var text = "<strong>" + username + "</strong> joined the chat";
        printEvent(text, animatable);
    }

    function printLeaveMessage(username, animatable) {
        var text = "<strong>" + username + "</strong> left the chat";
        printEvent(text, animatable);
    }
    
    function printMessage(username, text, animatable) {
        var sentByCurrentUer = (currentUser === username);
        var message = $("<div/>").addClass(sentByCurrentUer === true ? "message sent" : "message received");
        message.data("sender", username);

        var sender = $("<span/>").addClass("sender");
        sender.text(sentByCurrentUer === true ? "You" : username);
        sender.appendTo(message);

        var content = $("<span/>").addClass("content").text(text);
        content.appendTo(message);

        var lastMessage = $("#conversations .message").last();
        if (lastMessage.length && lastMessage.data("sender") === username) {
            message.addClass("same-sender-previous-message");
        }

        $("#conversations").append(message);
        if (animatable !== false) {
            $("#conversations").animate({scrollTop: $("#conversations").prop("scrollHeight")});
        }
    }

    function printEvent(text, animatable) {
        var div = $("<div/>").addClass("message event");
        $("<p/>").addClass("content").html(text).appendTo(div);
        $("#conversations").append(div);
        if (animatable !== false) {
            $("#conversations").animate({scrollTop: $("#conversations").prop("scrollHeight")});
        }
    }

    function printError(text, animatable) {
        var div = $("<div/>").addClass("message event error");
        $("<p/>").addClass("content").html(text).appendTo(div);
        $("#conversations").append(div);
        if (animatable !== false) {
            $("#conversations").animate({scrollTop: $("#conversations").prop("scrollHeight")});
        }
    }

    function printRecentConversations(chatMessages) {
        for (var i in chatMessages) {
            var chatMessage = chatMessages[i];
            Object.getOwnPropertyNames(chatMessage).forEach(function(val, idx, array) {
                var payload = chatMessage[val];
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