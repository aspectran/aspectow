window.onresize = function() {
    document.body.height = window.innerHeight;
};
window.onresize();
$(function() {
    $(document).foundation();

    $("button.signout").click(function() {
        location.href = "/signout"
    });

    /* Creating custom :external selector */
    $.expr[':'].external = function(obj) {
        return !obj.href.match(/^javascript:/)
            && !obj.href.match(/^mailto:/)
            && (obj.hostname !== location.hostname);
    };
    /* Add 'external' CSS class to all external links */
    $('a:external').addClass('external');
    /* turn target into target=_blank for elements w external class */
    $(".external").attr('target','_blank');
});
