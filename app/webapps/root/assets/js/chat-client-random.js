$(function() {
    $("form#send-message").off().submit(function() {
        $("#for-automata-clear").focus();
        if (getTotalPeople() > 1) {
            sendMessage();
        }
        readyToType();
        return false;
    });
    $(".message-box button.next").on("click", function() {
        startLooking();
    });
    $("#convo").on("click", ".message.event .content button.next", function() {
        startLooking();
    }).on("click", ".message.event .content button.cancel", function() {
        stopLooking(true);
    });
    startLooking();
});

let startTimer;
let canceled;
function startLooking() {
    if (startTimer) {
        canceled = true;
        clearTimeout(startTimer);
    }
    canceled = false;
    startTimer = setTimeout(function () {
        $.ajax("/rooms/random/token")
            .done(function(token) {
                if (token) {
                    if (!canceled) {
                        hideSidebar();
                        openSocket(token);
                    }
                } else {
                    gotoHomepage();
                }
            })
            .fail(function() {
                reloadPage();
            });
    }, 1000);
    hideSidebar();
    clearChaters();
    clearConvo();
    drawLookingBar(true);
}

function stopLooking(convoClear) {
    if (startTimer) {
        clearTimeout(startTimer);
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

function drawLookingBar(wait) {
    let sign;
    let title;
    if (wait) {
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
    if (wait) {
        setTimeout(function() {
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
    readyToType();
    setTimeout(function() {
        hideSidebar();
    }, 500);
}

function printUserLeftMessage(payload, restored) {
    let text = "<strong>" + payload.username + "</strong> has left this chat.";
    printEvent(text, restored);
    stopLooking();
}