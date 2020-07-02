let tokenIssuanceTimer;
let tokenIssuanceCanceled;

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
    $("button.leave").off().on("click", function () {
        $("button.leave").prop("disabled", true);
        if (tokenIssuanceTimer) {
            clearTimeout(tokenIssuanceTimer);
        }
        closeSocket();
        setTimeout(function () {
            leaveRoom();
        }, 500);
    });
    $(".message-box button.send").prop("disabled", true).addClass("pause");
    $(".message-box button.next").on("click", function () {
        $(".message-box button.send").prop("disabled", true).addClass("pause");
        closeSocket();
        startLooking();
    });
    $("#convo").on("click", ".message.custom button.next", function () {
        $(".message-box button.next").click();
    }).on("click", ".message.custom button.cancel", function () {
        stopLooking(true);
    });
    startLooking();
});

function startLooking() {
    if (tokenIssuanceTimer) {
        tokenIssuanceCanceled = true;
        clearTimeout(tokenIssuanceTimer);
    }
    tokenIssuanceCanceled = false;
    tokenIssuanceTimer = setTimeout(function () {
        $.ajax({
            url: "/random/request",
            method: 'GET',
            dataType: 'json',
            success: function (response) {
                if (response) {
                    if (response.usersByCountry) {
                        drawUsersByCountry(response.usersByCountry, 1);
                    }
                    if (!tokenIssuanceCanceled) {
                        switch (response.error) {
                            case -1:
                                reloadPage();
                                break;
                            case 0:
                                hideSidebar();
                                openSocket(response.token);
                        }
                    }
                } else {
                    serviceNotAvailable();
                }
            },
            error: function () {
                serviceNotAvailable();
            }
        });
    }, 1000);
    hideSidebar();
    clearChaters();
    removeConvoMessages();
    drawLookingBox(true);
}

function stopLooking(convoClear) {
    if (tokenIssuanceTimer) {
        clearTimeout(tokenIssuanceTimer);
    }
    hideSidebar();
    closeSocket();
    clearChaters();
    if (convoClear) {
        removeConvoMessages();
    }
    drawSearchBox();
}

function drawSearchBox() {
    let html = "<div class='text-center'>" +
        "<i class='iconfont fi-shuffle banner'></i>" +
        "<button type='button' class='success button next'>" + chatClientMessages.searchAnother + "</button>" +
        "</div>";
    printCustomMessage(html);
}

function drawLookingBox(intermission) {
    let banner;
    let title;
    if (intermission) {
        banner = "<i class='iconfont fi-shuffle banner'></i>";
        title = "<h3 class='wait'>" + chatClientMessages.wait + "</h3>";
    } else {
        banner = "<i class='iconfont fi-shuffle banner active'></i>";
        title = "<h3>" + chatClientMessages.looking + "</h3>";
    }
    let html = "<div class='text-center'>" + banner + title +
        "<div class='progress-bar'><div class='cylon_eye'></div></div>" +
        "<button type='button' class='success button cancel'>" + chatClientMessages.cancel + "</button>" +
        "</div>";
    printCustomMessage(html);
    if (intermission) {
        setTimeout(function () {
            $("#convo .message.custom .banner").addClass("animate");
        }, 200);
    }
}

function printJoinMessage(chater, restored) {
    removeConvoMessages();
    drawLookingBox();
}

function printUserJoinedMessage(payload, restored) {
    removeConvoMessages();
    let chater = deserialize(payload.chater);
    let html = chatClientMessages.userJoined.replace("[username]", "<strong>" + chater.username + "</strong>");
    printEventMessage(html, restored);
    if (chater.description) {
        let title = chatClientMessages.selfIntroductionTitle.replace("[username]", "<strong>" + chater.username + "</strong>");
        let selfIntro = $("<div class='self-introduction'/>");
        $("<p class='self-introduction-title'/>").html(title).appendTo(selfIntro);
        $("<p class='description'/>").text(chater.description).appendTo(selfIntro);
        if (chater.color) {
            selfIntro.addClass("my-col-" + chater.color);
        }
        printCustomMessage(selfIntro);
    }
    $(".message-box button.send").prop("disabled", false).removeClass("pause");
    readyToType();
    setTimeout(function () {
        hideSidebar();
    }, 500);
}

function printUserLeftMessage(payload, restored) {
    let chater = deserialize(payload.chater);
    let html = chatClientMessages.userLeft.replace("[username]", "<strong>" + chater.username + "</strong>");
    printEventMessage(html, restored);
    $(".message-box button.send").prop("disabled", true).addClass("pause");
    stopLooking();
}

function serviceNotAvailable() {
    closeSocket();
    clearChaters();
    removeConvoMessages();
    openNoticePopup(chatClientMessages.systemError,
        chatClientMessages.serviceNotAvailable,
        function () {
            gotoHome();
    });
}