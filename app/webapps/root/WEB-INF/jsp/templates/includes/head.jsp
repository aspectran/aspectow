<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
<meta name="google" content="notranslate"/>
<title><c:if test='${not empty page.title}'>${page.title}</c:if><c:if test='${empty page.title}'><aspectran:message code='page.default.title'/></c:if> - <aspectran:message code='site.title'/></title>
<meta name="description" content="<aspectran:message code="site.description"/>"/>
<meta name="keywords" content="<aspectran:message code="site.keywords"/>"/>
<link rel="mask-icon" href="https://textchat.club/assets/images/textchat.svg" color="#FFFFFF"/>
<link rel="apple-touch-icon" sizes="57x57" href="https://textchat.club/assets/favicons/apple-icon-57x57.png"/>
<link rel="apple-touch-icon" sizes="60x60" href="https://textchat.club/assets/favicons/apple-icon-60x60.png"/>
<link rel="apple-touch-icon" sizes="72x72" href="https://textchat.club/assets/favicons/apple-icon-72x72.png"/>
<link rel="apple-touch-icon" sizes="76x76" href="https://textchat.club/assets/favicons/apple-icon-76x76.png"/>
<link rel="apple-touch-icon" sizes="114x114" href="https://textchat.club/assets/favicons/apple-icon-114x114.png"/>
<link rel="apple-touch-icon" sizes="120x120" href="https://textchat.club/assets/favicons/apple-icon-120x120.png"/>
<link rel="apple-touch-icon" sizes="144x144" href="https://textchat.club/assets/favicons/apple-icon-144x144.png"/>
<link rel="apple-touch-icon" sizes="152x152" href="https://textchat.club/assets/favicons/apple-icon-152x152.png"/>
<link rel="apple-touch-icon" sizes="180x180" href="https://textchat.club/assets/favicons/apple-icon-180x180.png"/>
<link rel="icon" type="image/png" sizes="192x192" href="https://textchat.club/assets/favicons/android-icon-192x192.png"/>
<link rel="icon" type="image/png" sizes="32x32" href="https://textchat.club/assets/favicons/favicon-32x32.png"/>
<link rel="icon" type="image/png" sizes="96x96" href="https://textchat.club/assets/favicons/favicon-96x96.png"/>
<link rel="icon" type="image/png" sizes="16x16" href="https://textchat.club/assets/favicons/favicon-16x16.png"/>
<link rel="manifest" href="https://textchat.club/assets/favicons/manifest.json"/>
<meta name="msapplication-TileColor" content="#FFFFFF"/>
<meta name="msapplication-TileImage" content="https://textchat.club/assets/favicons/ms-icon-144x144.png"/>
<meta property="og:type" content="website">
<meta property="og:title" content="<c:if test='${not empty page.title}'>${page.title}</c:if><c:if test='${empty page.title}'><aspectran:message code='page.default.title'/></c:if> - <aspectran:message code='site.title'/>">
<meta property="og:description" content="<aspectran:message code="site.description"/>">
<meta property="og:image" content="https://textchat.club/assets/favicons/android-icon-192x192.png">
<meta property="og:url" content="https://textchat.club">
<link rel="stylesheet" type="text/css" href="/assets/css/aspectran.css?v1.5"/>
<link rel="stylesheet" type="text/css" href="/assets/css/page-common.css?v34"/>
<script src="/assets/js/modernizr-custom.js?v2"></script>
<script src="/assets/js/jquery-3.5.1.min.js"></script>
<script src="/assets/js/foundation.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.3/moment.min.js"></script>
<script src="https://www.google.com/recaptcha/api.js?render=explicit"></script>
<script src="/assets/js/recaptcha.js?v1"></script>
<script src="/assets/js/utils.js?v5"></script>
<script src="/assets/js/page-common.js?v27"></script>
<script>
    /* Set a custom property that contains the height value of the viewport */
    document.documentElement.style.setProperty("--vh", (window.innerHeight * 0.01) + "px");
    let oldWindowHeight = {
        outer: window.outerHeight,
        inner: window.innerHeight
    };
    function resizeWindowHeight(timeout) {
        return setTimeout(function () {
            if (window.outerHeight !== oldWindowHeight.outer || window.innerHeight !== oldWindowHeight.inner) {
                oldWindowHeight.outer = window.outerHeight;
                oldWindowHeight.inner = window.innerHeight;
                document.documentElement.style.setProperty("--vh", (window.innerHeight * 0.01) + "px");
            }
        }, timeout);
    }
    let windowResizeTimers = [];
    window.addEventListener("resize", function() {
        for (let i in windowResizeTimers) {
            if (windowResizeTimers[i]) {
                clearTimeout(windowResizeTimers[i]);
            }
        }
        for (let i = 0; i < 31; i++) {
            windowResizeTimers[i] = resizeWindowHeight(i > 0 ? i * 100 : 10);
        }
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
        language: "${user.language}"||"<aspectran:message code='site.lang'/>"
    }
    const modalMessages = {}
</script>