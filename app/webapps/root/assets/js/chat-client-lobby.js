$(function () {
});

function printMessage(payload, restored) {
    console.log(payload);
}

function leaveRoom(force) {
    closeSocket();
    if (force) {
        location.href = "/signout";
    } else {
        gotoHomepage();
    }
}

function gotoHomepage() {
    $("#lobby-not-available").foundation('open');
}