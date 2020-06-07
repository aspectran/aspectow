$(function () {
    if (!Modernizr.websockets) {
        gotoHome();
        return;
    }
    if (!checkSignedIn()) {
        $("#message").blur();
        $("#message, #form-send-message button").prop("disabled", true);
    }
    readyToType();
});