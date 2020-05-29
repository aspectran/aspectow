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
    <div class="grid-y grid-frame">
        <div class="header cell cell-block-container">
            <div class="grid-x">
                <div class="cell auto align-self-middle">
                    <h1><i class="fi-results-demographics"></i> Text Chat Club</h1>
                </div>
                <div class="cell shrink align-self-middle text-right">
                </div>
            </div>
        </div>
        <div class="body shadow cell auto cell-block-container">
            <div class="grid-x grid-padding-x grid-padding-y full-height">
                <div class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                </div>
                <div class="cell auto cell-block-y">
                </div>
            </div>
        </div>
    </div>
    fdsfsa
    <c:if test="${not empty errorCode}">
        <jsp:include page="/WEB-INF/jsp/templates/includes/${errorCode}.jsp"/>
    </c:if>
    <c:if test="${empty errorCode}">
        <jsp:include page="/WEB-INF/jsp/templates/includes/error-report.jsp"/>
    </c:if>
</body>
</html>