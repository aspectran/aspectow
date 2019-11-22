<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="wrap row">
    <div class="header columns">
        <h2>Chat <span id="totalPeople"></span></h2>
        <a class="leave" onclick="leaveRoom();">Leave</a>
    </div>
    <div class="sidebar columns medium-4 large-3 hide-for-small-only">
        <div id="contacts"></div>
    </div>
    <div class="main columns small-12 medium-8 large-9">
        <form id="sign-in" method="post" onsubmit="return false;">
            <h3>Only one chat room!</h3>
            <h4>If no one chats for a minute, the chat room will close.<br/>
                So shall we start chatting?<br/>
                Our chat is not recorded anywhere.</h4>
            <h4>Please enter your nickname.</h4>
            <div class="input-group">
                <input class="input-group-field" type="text" id="nickname" maxlength="30" placeholder="Nickname" autocomplete="off" autofocus/>
                <div class="input-group-button">
                    <button type="submit" class="button" onclick="executeCaptcha()">Join</button>
                </div>
            </div>
            <div id="inline-badge"></div>
        </form>
        <div id="messages"></div>
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

    $(function() {
        $("form#chat-controls]").submit(function() {
            sendMessage();
            return false;
        });
    });

    function startChat() {
        if (!recaptchaResponse) {
            return;
        }
        currentUser = $("#nickname").val().trim();
        $("#nickname").val("");
        if (currentUser) {
            $("#sign-in").hide();
            $("#messages").show();
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
                    nickname: currentUser
                }
            };
            socket.send(JSON.stringify(chatMessage));
        };

        socket.onmessage = function (event) {
            if (typeof event.data === "string") {
                var chatMessage = JSON.parse(event.data);
                Object.getOwnPropertyNames(chatMessage).forEach(function(val, idx, array) {
                    var payload = chatMessage[val];
                    if (payload) {
                        switch (val) {
                            case "welcomeUser":
                                displayConnectedUserMessage(payload.nickname);
                                break;
                            case "duplicatedUser":
                                socket.close();
                                alert("Your nickname is already in use. Please enter a different nickname.");
                                location.reload();
                                break;
                            case "broadcastTextMessage":
                                displayMessage(payload.nickname, payload.content);
                                break;
                            case "broadcastConnectedUser":
                                displayConnectedUserMessage(payload.nickname);
                                break;
                            case "broadcastDisconnectedUser":
                                displayDisconnectedUserMessage(payload.nickname);
                                break;
                            case "broadcastAvailableUsers":
                                cleanAvailableUsers();
                                for (var i = 0; i < payload.nicknames.length; i++) {
                                    addAvailableUsers(payload.nicknames[i]);
                                }
                                break;
                        }
                    }
                });
            }
        };

        socket.onclose = function (event) {
            location.reload();
        };

        socket.onerror = function (event) {
            console.error("WebSocket error observed:", event);
            displayErrorMessage('Could not connect to WebSocket server. Please refresh this page to try again!');
        };
    }

    function leaveRoom() {
        socket.close();
    }

    function sendMessage() {
        var text = $("#message").val().trim();
        if (text) {
            var chatMessage = {
                sendTextMessage: {
                    type: 'CHAT',
                    nickname: currentUser,
                    content: text
                }
            };
            socket.send(JSON.stringify(chatMessage));
            $("#message").val('').focus();
        }
    }

    function displayMessage(nickname, text) {
        var sentByCurrentUer = (currentUser === nickname);
        var message = $("<div/>").addClass(sentByCurrentUer === true ? "message sent" : "message received");
        message.data("sender", nickname);

        var sender = $("<span/>").addClass("sender");
        sender.text(sentByCurrentUer === true ? "You" : nickname);
        sender.appendTo(message);

        var content = $("<span/>").addClass("content").text(text);
        content.appendTo(message);

        var lastMessage = $("#messages .message").last();
        if (lastMessage.length && lastMessage.data("sender") === nickname) {
            message.addClass("same-sender-previous-message");
        }

        $("#messages").append(message);
        $("#messages").animate({scrollTop: $("#messages").prop("scrollHeight")});
    }

    function displayConnectedUserMessage(nickname) {
        var sentByCurrentUer = currentUser === nickname;
        var text = (sentByCurrentUer === true ? "Welcome <strong>" + nickname : nickname + "</strong> joined the chat");
        displayEventMessage(text);
    }

    function displayDisconnectedUserMessage(nickname) {
        var text = "<strong>" + nickname + "</strong> left the chat";
        displayEventMessage(text);
    }

    function addAvailableUsers(nickname) {
        var contact = $("<div/>").addClass("contact");
        var status = $("<div/>").addClass("status");
        var name = $("<span/>").addClass("name").text(nickname);
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

    function displayEventMessage(text) {
        var div = $("<div/>").addClass("message event");
        $("<p/>").addClass("content").html(text).appendTo(div);
        $("#messages").append(div);
        $("#messages").animate({scrollTop: $("#messages").prop("scrollHeight")});
    }

    function displayErrorMessage(text) {
        var div = $("<div/>").addClass("message event error");
        $("<p/>").addClass("content").html(text).appendTo(div);
        $("#messages").append(div);
        $("#messages").animate({scrollTop: $("#messages").prop("scrollHeight")});
    }
</script>