<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=0">
<meta name="google" content="notranslate">
<title><c:if test="${not empty page.title}">${page.title} - </c:if>Text Chat Club</title>
<meta name="description" content="Chat with anyone. Meet new people at random. No registration. No logs.">
<meta name="keywords" content="chat, online chat, random chat, stranger chat, chat with anyone, talk to strangers, chat with strangers">
<link rel="stylesheet" type="text/css" href="/assets/css/aspectran.css?v1.2">
<link rel="stylesheet" type="text/css" href="/assets/css/page-common.css?v22">
<script src="/assets/js/modernizr-custom.js?v2"></script>
<script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
<script src="/assets/js/foundation.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.3/moment.min.js"></script>
<script src="https://www.google.com/recaptcha/api.js?render=explicit"></script>
<script src="/assets/js/recaptcha.js?v1"></script>
<script src="/assets/js/utils.js?v1.1"></script>
<script src="/assets/js/page-common.js?v17"></script>
<script>
    /* Set a custom property that contains the height value of the viewport */
    document.documentElement.style.setProperty("--vh", (window.innerHeight * 0.01) + "px");
    let currentWindowSize = {
        width: window.outerWidth,
        height: window.outerHeight,
        innerWidth: window.innerWidth,
        innerHeight: window.innerHeight
    };
    window.addEventListener("resize", function() {
        if (window.outerWidth !== currentWindowSize.width || window.outerHeight !== currentWindowSize.height) {
            currentWindowSize.width = window.outerWidth;
            currentWindowSize.height = window.outerHeight;
            currentWindowSize.innerWidth = window.innerWidth;
            currentWindowSize.innerHeight = window.innerHeight;
            document.documentElement.style.setProperty("--vh", (window.innerHeight * 0.01) + "px");
        }
    });
</script>
<script data-ad-client="ca-pub-8543949924656308" async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"></script>
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