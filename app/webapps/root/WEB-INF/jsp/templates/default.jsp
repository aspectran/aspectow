<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html class="no-js" lang="en">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
    <meta name="google" content="notranslate">
    <title>${empty page.title ? "Text Chat Club" : page.title}</title>
    <meta name="description" content="${empty page.description ? "Online Chat Rooms For Everyone" : page.description}" />
    <link rel="stylesheet" type="text/css" href="/assets/css/base.css?20191015" />
    <link rel="stylesheet" type="text/css" href="/assets/css/textchat.css?20191016-2" />
    <link href="https://fonts.googleapis.com/css?family=Raleway:500,500i,700" rel="stylesheet">
    <script src="/assets/js/jquery.min.js"></script>
    <!-- Global site tag (gtag.js) - Google Analytics -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=UA-150079188-1"></script>
    <script>
        window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());
        gtag('config', 'UA-150079188-1');
    </script>
</head>
<body>
<c:if test="${not empty page.include}">
    <jsp:include page="/WEB-INF/jsp/${page.include}.jsp"/>
</c:if>
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
    $(function() {
        /* Add 'external' CSS class to all external links */
        $('a:external').addClass('external');
        /* turn target into target=_blank for elements w external class */
        $(".external").attr('target','_blank');
    })
</script>
</body>
</html>