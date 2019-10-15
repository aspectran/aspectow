<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!doctype html>
<html class="no-js" lang="en">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
    <meta name="google" content="notranslate">
    <title>${empty page.title ? "Text Chat Club" : page.title}</title>
    <meta name="description" content="${empty page.description ? "Online Chat Rooms For Everyone" : page.description}" />
    <link rel="stylesheet" type="text/css" href="/assets/css/base.css?20191015" />
    <link rel="stylesheet" type="text/css" href="/assets/css/textchat.css?20191015" />
    <link href="http://fonts.googleapis.com/css?family=Raleway:500,500i,700" rel="stylesheet">
    <script src="/assets/js/jquery.js"></script>
    <script>
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
            (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
        ga('create', 'UA-66807210-2', 'auto');
        ga('set', 'anonymizeIp', true);
        ga('send', 'pageview');
    </script>
</head>
<body>
<section class="wrap">
<c:if test="${not empty page.include}">
    <jsp:include page="/WEB-INF/jsp/${page.include}.jsp"/>
</c:if>
</section>
<script src="/assets/js/foundation.min.js"></script>
<script>
    $(document).foundation();
</script>
<script>
    /* Creating custom :external selector */
    $.expr[':'].external = function(obj) {
        return !obj.href.match(/^javascript\:/)
            && !obj.href.match(/^mailto\:/)
            && (obj.hostname != location.hostname);
    };
    $(function(){
        /* Add 'external' CSS class to all external links */
        $('a:external').addClass('external');
        /* turn target into target=_blank for elements w external class */
        $(".external").attr('target','_blank');
    })
</script>
</body>
</html>