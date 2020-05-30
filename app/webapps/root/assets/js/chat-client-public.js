$(function () {
    if (!Modernizr.websockets) {
        gotoHomepage();
        return;
    }
    if (!checkSignedIn()) {
        $("#message").blur();
        $("#message, #form-send-message button").prop("disabled", true);
    }
    readyToType();
});