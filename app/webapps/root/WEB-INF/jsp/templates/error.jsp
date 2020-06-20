<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html class="no-js" lang="en">
<head>
<%@ include file="includes/head.jsp" %>
<script>
    $(function () {
        $(".reveal").foundation('open');
    })
</script>
</head>
<body>
    <c:if test="${not empty errorCode}">
        <jsp:include page="/WEB-INF/jsp/templates/includes/${errorCode}.jsp"/>
    </c:if>
    <c:if test="${empty errorCode}">
        <jsp:include page="/WEB-INF/jsp/templates/includes/error-report.jsp"/>
    </c:if>
</body>
</html>