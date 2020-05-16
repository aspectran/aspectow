$(function() {
    $("form#send-message").off().submit(function() {
        if (!$("#contacts-wrap").hasClass("show-for-medium")) {
            $("#contacts-wrap").addClass("show-for-medium");
        }
        $("#for-automata-clear").focus();
        if (getTotalPeople() > 1) {
            sendMessage();
        }
        readyToType();
        return false;
    });
    $(".message-box button.next").on("click", function() {
        clearChaters();
        clearConvo();
        startLooking();
    });
    $("#convo").on("click", ".message.event .content button.next", function() {
        startLooking();
    }).on("click", ".message.event .content button.cancel", function() {
        stopLooking();
    });

    startLooking();
});

let startTimer;
function startLooking() {
    if (startTimer) {
        clearTimeout(startTimer);
    }
    startTimer = setTimeout(function () {
        openSocket();
    }, 2000);
    clearChaters();
    clearConvo();
    drawLookingBar(true);
}

function stopLooking() {
    if (startTimer) {
        clearTimeout(startTimer);
    }
    clearChaters();
    clearConvo();
    closeSocket();
    drawSearchBar();
}

function drawSearchBar() {
    let text = "<i class='iconfont fi-shuffle sign'></i>" +
        "<button type='button' class='success button next'>Search for another stranger</button>";
    printEvent(text, false);
}

function drawLookingBar(wait) {
    let title;
    if (wait) {
        title = "<h3 class='wait'>Please wait a moment.</h3>";
    } else {
        title = "<h3>Looking for stranger...</h3>";
    }
    let text = "<i class='iconfont fi-shuffle sign'></i>" +
        title +
        "<div class='progress-bar'><div class='cylon_eye'></div></div>" +
        "<button type='button' class='success button cancel'>Cancel</button>";
    printEvent(text, true);
}

function printJoinMessage(payload, animatable, container) {
    clearConvo();
    drawLookingBar();
}

function printUserJoinedMessage(payload, animatable, container) {
    clearConvo();
    let text = "<i class='fi-microphone'></i> Chat started. Feel free to say hello to <strong>" + payload.username + "</strong>.";
    printEvent(text, animatable, container);
    readyToType();
}

function printUserLeftMessage(payload, animatable, container) {
    let text = "<strong>" + payload.username + "</strong> has left this chat";
    printEvent(text, animatable, container);
    drawLookingBar();
}