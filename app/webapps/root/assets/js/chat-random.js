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
    $("button.next").on("click", function() {
        clearChaters();
        clearConvo();
        openSocket();
    });
});

function printWelcomeMessage(payload, animatable, container) {
    let text = "<i class='fi-flag'></i> Waiting for stranger ...";
    printEvent(text, animatable, container);
}

function printJoinedMessage(payload, animatable, container) {
    let text = "<i class='fi-microphone'></i> Chat started. Feel free to say hello to <strong>" + payload.username + "</strong>.";
    printEvent(text, animatable, container);
}