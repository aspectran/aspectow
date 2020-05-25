$(function () {
    if (!checkSignedIn()) {
        $("#message").blur();
        $("#message, #form-send-message button").prop("disabled", true);
        return;
    }
});