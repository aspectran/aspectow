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
    readyToType();
});