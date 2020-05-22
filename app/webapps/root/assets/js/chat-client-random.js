let tokenIssuanceTimer;
let tokenIssuanceCanceled;

$(function () {
    $(".message-box button.send").prop("disabled", true).addClass("pause");
    $(".message-box button.next").on("click", function () {
        $(".message-box button.send").prop("disabled", true).addClass("pause");
        closeSocket();
        startLooking();
    });
    $("#convo").on("click", ".message.event .content button.next", function () {
        $(".message-box button.next").click();
    }).on("click", ".message.event .content button.cancel", function () {
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
            url: "/rooms/random/token",
            method: 'GET',
            dataType: 'json',
            success: function (token) {
                if (token) {
                    if (!tokenIssuanceCanceled) {
                        hideSidebar();
                        openSocket(token);
                    }
                } else {
                    serviceNotAvailable();
                }
            },
            error: function (xhr) {
                serviceNotAvailable();
            }
        });
    }, 1000);
    hideSidebar();
    clearChaters();
    clearConvo();
    drawLookingBar(true);
}

function stopLooking(convoClear) {
    if (tokenIssuanceTimer) {
        clearTimeout(tokenIssuanceTimer);
    }
    hideSidebar();
    closeSocket();
    clearChaters();
    if (convoClear) {
        clearConvo();
    }
    drawSearchBar();
}

function drawSearchBar() {
    let text = "<i class='iconfont fi-shuffle sign'></i>" +
        "<button type='button' class='success button next'>Search for another stranger</button>";
    printEvent(text);
}

function drawLookingBar(intermission) {
    let sign;
    let title;
    if (intermission) {
        sign = "<i class='iconfont fi-shuffle sign'></i>";
        title = "<h3 class='wait'>Please wait a moment.</h3>";
    } else {
        sign = "<i class='iconfont fi-shuffle sign active'></i>";
        title = "<h3>Looking for stranger...</h3>";
    }
    let text = sign + title +
        "<div class='progress-bar'><div class='cylon_eye'></div></div>" +
        "<button type='button' class='success button cancel'>Cancel</button>";
    printEvent(text);
    if (intermission) {
        setTimeout(function () {
            $("#convo .message.event .content .sign").addClass("animate");
        }, 200);
    }
}

function printJoinMessage(payload, restored) {
    clearConvo();
    drawLookingBar();
}

function printUserJoinedMessage(payload, restored) {
    clearConvo();
    let text = "<i class='fi-microphone'></i> Chat started. Feel free to say hello to <strong>" +
        payload.username + "</strong>.";
    printEvent(text, restored);
    $(".message-box button.send").prop("disabled", false).removeClass("pause");
    readyToType();
    setTimeout(function () {
        hideSidebar();
    }, 500);
}

function printUserLeftMessage(payload, restored) {
    let text = "<strong>" + payload.username + "</strong> has left this chat.";
    printEvent(text, restored);
    $(".message-box button.send").prop("disabled", true).addClass("pause");
    stopLooking();
}

function serviceNotAvailable() {
    closeSocket();
    clearChaters();
    clearConvo();
    openNoticePopup("Please note",
        "Sorry. Our random chat service is not available at this time.",
        function () {
            gotoHomepage();
    });
}