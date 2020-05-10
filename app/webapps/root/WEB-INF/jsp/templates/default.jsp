<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html class="no-js" lang="en">
<head>
    <%@ include file="includes/head.jsp" %>
</head>
<body>
<c:if test="${not empty page.include}">
    <jsp:include page="/WEB-INF/jsp/${page.include}.jsp"/>
</c:if>
</body>
</html>