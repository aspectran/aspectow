<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
<meta name="google" content="notranslate"/>
<title><c:if test='${not empty page.title}'>${page.title}</c:if><c:if test='${empty page.title}'><aspectran:message code='page.default.title'/></c:if> - <aspectran:message code='site.title'/></title>
<meta name="description" content="<aspectran:message code="site.description"/>"/>
<meta name="keywords" content="<aspectran:message code="site.keywords"/>"/>
<link rel="mask-icon" href="<aspectran:token type='property' expression='cdn.assets.url'/>/images/textchat.svg" color="#FFFFFF"/>
<link rel="apple-touch-icon" sizes="57x57" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-57x57.png"/>
<link rel="apple-touch-icon" sizes="60x60" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-60x60.png"/>
<link rel="apple-touch-icon" sizes="72x72" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-72x72.png"/>
<link rel="apple-touch-icon" sizes="76x76" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-76x76.png"/>
<link rel="apple-touch-icon" sizes="114x114" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-114x114.png"/>
<link rel="apple-touch-icon" sizes="120x120" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-120x120.png"/>
<link rel="apple-touch-icon" sizes="144x144" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-144x144.png"/>
<link rel="apple-touch-icon" sizes="152x152" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-152x152.png"/>
<link rel="apple-touch-icon" sizes="180x180" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/apple-icon-180x180.png"/>
<link rel="icon" type="image/png" sizes="192x192" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/android-icon-192x192.png"/>
<link rel="icon" type="image/png" sizes="32x32" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/favicon-32x32.png"/>
<link rel="icon" type="image/png" sizes="96x96" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/favicon-96x96.png"/>
<link rel="icon" type="image/png" sizes="16x16" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/favicon-16x16.png"/>
<link rel="manifest" href="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/manifest.json"/>
<meta name="msapplication-TileColor" content="#FFFFFF"/>
<meta name="msapplication-TileImage" content="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/ms-icon-144x144.png"/>
<meta property="og:type" content="website">
<meta property="og:title" content="<c:if test='${not empty page.title}'>${page.title}</c:if><c:if test='${empty page.title}'><aspectran:message code='page.default.title'/></c:if> - <aspectran:message code='site.title'/>">
<meta property="og:description" content="<aspectran:message code="site.description"/>">
<meta property="og:image" content="<aspectran:token type='property' expression='cdn.assets.url'/>/favicons/android-icon-192x192.png">
<meta property="og:url" content="https://textchat.club">
<link rel="stylesheet" type="text/css" href="<aspectran:token type='property' expression='cdn.assets.url'/>/css/aspectran.css"/>
<link rel="stylesheet" type="text/css" href="<aspectran:token type='property' expression='cdn.assets.url'/>/css/page-common.css"/>
<script src="<aspectran:token type='property' expression='cdn.assets.url'/>/js/modernizr-custom.js"></script>
<script src="https://cdn.jsdelivr.net/npm/jquery@3.5.1/dist/jquery.min.js"></script>
<script src="<aspectran:token type='property' expression='cdn.assets.url'/>/js/foundation.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.3/moment.min.js"></script>
<script src="https://www.google.com/recaptcha/api.js?render=explicit"></script>
<script src="<aspectran:token type='property' expression='cdn.assets.url'/>/js/recaptcha.js"></script>
<script src="<aspectran:token type='property' expression='cdn.assets.url'/>/js/utils.js"></script>
<script src="<aspectran:token type='property' expression='cdn.assets.url'/>/js/page-common.js"></script>
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
<c:if test="${not empty user and fn:length(user.description) ge 64}">
    if (localStorage.getItem("conversioned") !== "true") {
        gtag('event', 'conversion', {'send_to': 'AW-798244126/VV_4CKaBwNEBEJ760PwC'});
        localStorage.setItem("conversioned", "true");
    }
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