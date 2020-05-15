$(function() {
    $("form#send-message").submit(function() {
        // if (!$("#contacts-wrap").hasClass("show-for-medium")) {
        //     $("#contacts-wrap").addClass("show-for-medium");
        // }
        // $("#for-automata-clear").focus();
        // sendMessage();
        return false;
    });
    $("button.next").on("click", function() {
        clearChaters();
        clearConvo();
        openSocket();
    });
});

function printWelcomeMessage(payload, animatable) {
    let text = "새로운 ";
    printEvent(text, animatable);
}