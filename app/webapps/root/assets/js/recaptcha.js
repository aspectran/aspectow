let clientId;
let recaptchaResponse;

function loadCaptcha() {
    grecaptcha.ready(function() {
        clientId = grecaptcha.render('inline-badge', {
            'sitekey': '6Ldt0r0UAAAAAP4ejDGFZLB0S-zDzWL3ZkB49FvN',
            'badge': 'inline',
            'size': 'invisible'
        });
    });
}

function executeCaptcha() {
    grecaptcha.ready(function() {
        grecaptcha.execute(clientId, {
            action: 'sign_in'
        }).then(function(token) {
            recaptchaResponse = token;
            signIn();
        });
    });
}