<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
<meta name="google" content="notranslate"/>
<title><c:if test="${not empty page.title}">${page.title} - </c:if>Text Chat Club</title>
<meta name="description" content="Chat with anyone. Meet new people at random. No registration. No logs."/>
<meta name="keywords" content="chat, online chat, random chat, stranger chat, chat with anyone, talk to strangers, chat with strangers"/>
<link rel="mask-icon" href="/assets/favicons/textchat.svg" color="#FFFFFF"/>
<link rel="apple-touch-icon" sizes="57x57" href="/assets/favicons/apple-icon-57x57.png"/>
<link rel="apple-touch-icon" sizes="60x60" href="/assets/favicons/apple-icon-60x60.png"/>
<link rel="apple-touch-icon" sizes="72x72" href="/assets/favicons/apple-icon-72x72.png"/>
<link rel="apple-touch-icon" sizes="76x76" href="/assets/favicons/apple-icon-76x76.png"/>
<link rel="apple-touch-icon" sizes="114x114" href="/assets/favicons/apple-icon-114x114.png"/>
<link rel="apple-touch-icon" sizes="120x120" href="/assets/favicons/apple-icon-120x120.png"/>
<link rel="apple-touch-icon" sizes="144x144" href="/assets/favicons/apple-icon-144x144.png"/>
<link rel="apple-touch-icon" sizes="152x152" href="/assets/favicons/apple-icon-152x152.png"/>
<link rel="apple-touch-icon" sizes="180x180" href="/assets/favicons/apple-icon-180x180.png"/>
<link rel="icon" type="image/png" sizes="192x192" href="/assets/favicons/android-icon-192x192.png"/>
<link rel="icon" type="image/png" sizes="32x32" href="/assets/favicons/favicon-32x32.png"/>
<link rel="icon" type="image/png" sizes="96x96" href="/assets/favicons/favicon-96x96.png"/>
<link rel="icon" type="image/png" sizes="16x16" href="/assets/favicons/favicon-16x16.png"/>
<link rel="manifest" href="/assets/favicons/manifest.json"/>
<meta name="msapplication-TileColor" content="#FFFFFF"/>
<meta name="msapplication-TileImage" content="/assets/favicons/ms-icon-144x144.png"/>
<link rel="stylesheet" type="text/css" href="/assets/css/aspectran.css?v1.3"/>
<link rel="stylesheet" type="text/css" href="/assets/css/page-common.css?v23"/>
<script src="/assets/js/modernizr-custom.js?v2"></script>
<script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
<script src="/assets/js/foundation.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.3/moment.min.js"></script>
<script src="https://www.google.com/recaptcha/api.js?render=explicit"></script>
<script src="/assets/js/recaptcha.js?v1"></script>
<script src="/assets/js/utils.js?v1.1"></script>
<script src="/assets/js/page-common.js?v19"></script>
<script>
    /* Set a custom property that contains the height value of the viewport */
    document.documentElement.style.setProperty("--vh", (window.innerHeight * 0.01) + "px");
    let currentWindowSize = {
        height: window.outerHeight,
        innerHeight: window.innerHeight
    };
    let windowResizeTimer;
    window.addEventListener("resize", function() {
        if (windowResizeTimer) {
            clearTimeout(windowResizeTimer);
        }
        windowResizeTimer = setTimeout(function () {
            if (window.outerHeight !== currentWindowSize.height) {
                currentWindowSize.height = window.outerHeight;
                currentWindowSize.innerHeight = window.innerHeight;
                document.documentElement.style.setProperty("--vh", (window.innerHeight * 0.01) + "px");
                setTimeout(function () {
                    document.documentElement.style.setProperty("--vh", (window.innerHeight * 0.01) + "px");
                }, 1000);
            }
        }, 150);
    });
</script>
<script async src="https://www.googletagmanager.com/gtag/js?id=AW-798244126"></script>
<script>
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments);}
    gtag('js', new Date());
    gtag('config', 'UA-150079188-1');
    gtag('config', 'AW-798244126');
<c:if test="${not empty user}">
    gtag('event', 'conversion', {'send_to': 'AW-798244126/VV_4CKaBwNEBEJ760PwC'});
</c:if>
</script>
<script>
    const userInfo = {
        userNo: Number("${user.userNo}"),
        username: "${user.username}",
        country: "${user.country}",
        language: "${user.language}"
    }
</script>