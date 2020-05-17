let recaptchaClientId;
let recaptchaResponse;

function loadCaptcha() {
    grecaptcha.ready(function() {
        recaptchaClientId = grecaptcha.render('captcha-container', {
            'sitekey': '6Ldt0r0UAAAAAP4ejDGFZLB0S-zDzWL3ZkB49FvN',
            'badge': 'inline',
            'size': 'invisible'
        });
    });
}

function executeCaptcha(action, callback) {
    grecaptcha.ready(function() {
        grecaptcha.execute(recaptchaClientId, {
            action: action
        }).then(function(token) {
            recaptchaResponse = token;
            callback();
        });
    });
}