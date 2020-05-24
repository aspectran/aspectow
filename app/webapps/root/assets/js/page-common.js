$(function () {
    $(document).foundation();

    $("button.signout").click(function () {
        location.href = "/signout";
    });

    /* Creating custom :external selector */
    $.expr[':'].external = function (obj) {
        return !obj.href.match(/^javascript:/)
            && !obj.href.match(/^mailto:/)
            && (obj.hostname !== location.hostname);
    };
    /* Add 'external' CSS class to all external links */
    $('a:external').addClass('external');
    /* turn target into target=_blank for elements w external class */
    $(".external").attr('target','_blank');
});

function openNoticePopup(title, message, action) {
    let p = $("<p/>").text(message);
    let popup = $("#common-notice-popup");
    popup.find("h3").text(title);
    popup.find(".content").html("").append(p);
    popup.find(".button.ok").off().on("click", function () {
        if (action) {
            action();
        }
        popup.foundation('close');
    });
    popup.foundation('open');
}

let openWaitPopupTimer;
function openWaitPopup(message, action, timeout) {
    if (openWaitPopupTimer) {
        clearTimeout(openWaitPopupTimer);
        openWaitPopupTimer = null;
    }
    let p = $("<p/>").text(message);
    let popup = $("#common-wait-popup");
    popup.find(".content").html("").append(p);
    popup.find(".button.cancel").hide().off().on("click", function () {
        if (action) {
            action();
        }
        popup.foundation('close');
    });
    popup.foundation('open');
    if (timeout > 0) {
        openWaitPopupTimer = setTimeout(function () {
            popup.find(".button.cancel").show();
        }, timeout);
    } else {
        popup.find(".button.cancel").show();
    }
}

function closeWaitPopup() {
    $("#common-wait-popup").foundation('close');
}