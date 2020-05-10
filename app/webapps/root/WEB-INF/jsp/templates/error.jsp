<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html class="no-js" lang="en">
<head>
    <%@ include file="includes/head.jsp" %>
    <script>
        $(function() {
            $('.reveal').foundation('open');
        })
    </script>
</head>
<body>
    <div class="grid-y grid-frame">
        <div class="header cell cell-block-container">
            <div class="grid-x grid-padding-x">
                <div class="cell auto align-self-middle">
                    <h2><i class="fi-results-demographics"></i> Text Chat Club</h2>
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
    <div class="reveal" aria-labelledby="ooops" data-reveal data-close-on-click="false" data-close-on-esc="false">
        <h1 id="ooops">Ooops!</h1>
        <p class="lead">An unexpected error has occurred.</p>
        <p>${page.cause.message}</p>
        <div class="button-group align-right">
            <a class="alert button" href="/lobby">Ok</a>
        </div>
    </div>
</body>
</html>