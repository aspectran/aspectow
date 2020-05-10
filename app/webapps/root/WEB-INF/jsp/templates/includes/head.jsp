<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=0"/>
<meta name="google" content="notranslate">
<title>Text Chat Club<c:if test="${not empty page.title}"> - ${page.title}</c:if></title>
<meta name="description" content="${empty page.description ? "Chat with anyone you want, about anything you want, free." : page.description}" />
<link rel="stylesheet" type="text/css" href="/assets/css/aspectran.css?20200505" />
<link rel="stylesheet" type="text/css" href="/assets/css/textchat.css?20200505" />
<link href="https://fonts.googleapis.com/css?family=Raleway:500,500i,700" rel="stylesheet">
<script src="/assets/js/modernizr-custom.js"></script>
<script src="/assets/js/jquery.min.js"></script>
<script src="/assets/js/foundation.min.js"></script>
<script src="/assets/js/textchat.js"></script>
<!-- Global site tag (gtag.js) - Google Analytics -->
<script async src="https://www.googletagmanager.com/gtag/js?id=UA-150079188-1"></script>
<script>
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments);}
    gtag('js', new Date());
    gtag('config', 'UA-150079188-1');
</script>