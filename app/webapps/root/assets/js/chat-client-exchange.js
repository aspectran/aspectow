let tokenIssuanceTimer;
let tokenIssuanceCanceled;

$(function () {
    $(".language-settings .button.ok").on("click", function () {
        $(".choose-info").hide();
        $(".language-settings").addClass("rounded-top-corners");
        $(".language-settings .guide").show();
        $(".language-settings .form-error").hide();
        let nativeLang = $(".language-settings select[name=native_lang]").val();
        let convoLang = $(".language-settings select[name=convo_lang]").val();
        if (!nativeLang || !convoLang) {
            $(".language-settings .form-error.exchange-languages-required").fadeIn();
            return;
        }
        if (nativeLang === convoLang) {
            $(".language-settings .form-error.same-exchange-languages").fadeIn();
            localStorage.removeItem("convoLang");
            return;
        }
        localStorage.setItem("nativeLang", nativeLang);
        localStorage.setItem("convoLang", convoLang);
        let params = {
            native_lang: nativeLang,
            convo_lang: convoLang
        }
        closeSocket();
        startExchangeChat(params)
    });
    let storedNativeLang = localStorage.getItem("nativeLang")||userInfo.language;
    let storedConvoLang = localStorage.getItem("convoLang");
    $(".language-settings select[name=native_lang] option").filter(function () {
        return $(this).val() === storedNativeLang;
    }).each(function () {
        $(".language-settings select[name=native_lang]").val(storedNativeLang);
    });
    $(".language-settings select[name=convo_lang] option").filter(function () {
        return $(this).val() === storedConvoLang;
    }).each(function () {
        $(".language-settings select[name=convo_lang]").val(storedConvoLang);
    });
    if (storedNativeLang && storedConvoLang) {
        $(".language-settings .button.ok").click();
    }
});

function startExchangeChat(params) {
    if (tokenIssuanceTimer) {
        tokenIssuanceCanceled = true;
        clearTimeout(tokenIssuanceTimer);
    }
    tokenIssuanceCanceled = false;
    tokenIssuanceTimer = setTimeout(function () {
        $.ajax({
            url: "/exchange/token",
            data: params,
            method: 'GET',
            dataType: 'json',
            success: function (token) {
                if (token) {
                    if (!tokenIssuanceCanceled) {
                        if (token === "-1") {
                            reloadPage();
                            return;
                        }
                        hideSidebar();
                        openSocket(token, params);
                        $(".choose-info").fadeIn();
                        $(".language-settings").removeClass("rounded-top-corners");
                        $(".language-settings .guide").hide();
                    }
                } else {
                    serviceNotAvailable();
                }
            },
            error: function () {
                serviceNotAvailable();
            }
        });
    }, 600);
    hideSidebar();
    clearChaters();
    clearConvo();
}