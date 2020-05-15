$(function() {
    $("form#send-message").off().submit(function() {
        if (!$("#contacts-wrap").hasClass("show-for-medium")) {
            $("#contacts-wrap").addClass("show-for-medium");
        }
        $("#for-automata-clear").focus();
        if (getTotalPeople() > 1) {
            sendMessage();
        }
        return false;
    });
    $(".message-box button.next").on("click", function() {
        clearChaters();
        clearConvo();
        openSocket();
    });
    $("#convo")
        .on("click", ".message.event .content button.next", function() {
            clearChaters();
            clearConvo();
            openSocket();
        }).on("click", ".message.event .content button.cancel", function() {
            clearChaters();
            clearConvo();
            closeSocket();
            drawSearchBar();
        });

    drawLookingBar();
    setTimeout(function () {
        openSocket();
    }, 2000);
});

function drawSearchBar() {
    let text = "<i class='iconfont fi-shuffle sign next'></i>" +
        "<button type='button' class='success button next'>Search for another stranger</button>";
    printEvent(text, false);
}

function drawLookingBar() {
    let text = "<i class='iconfont fi-shuffle sign cancel'></i>" +
        "<h3>Looking for stranger...</h3>" +
        "<div class='progress-bar'><div class='cylon_eye'></div></div>" +
        "<button type='button' class='success button cancel'>Cancel</button>";
    printEvent(text, true);
}

function printWelcomeMessage(payload, animatable, container) {
    clearConvo();
    drawLookingBar();
}

function printJoinedMessage(payload, animatable, container) {
    clearConvo();
    let text = "<i class='fi-microphone'></i> Chat started. Feel free to say hello to <strong>" + payload.username + "</strong>.";
    printEvent(text, animatable, container);
    $("#message").focus();
}

function printLeftMessage(payload, animatable, container) {
    let text = "<strong>" + payload.username + "</strong> has left this chat";
    printEvent(text, animatable, container);
    drawLookingBar();
}