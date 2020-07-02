<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div id="error-report" class="reveal popup error" aria-labelledby="oops" data-reveal data-close-on-click="false" data-close-on-esc="false">
    <h2 id="oops">Oops!</h2>
    <div class="grid-x grid-padding-x grid-margin-y">
        <div class="cell text-center">
            <i class="banner icon-warning"></i>
        </div>
        <div class="cell card">
            <div class="card-section">
                <c:choose>
                    <c:when test="${translet.rootCauseOfRaisedException.message eq 'invalid-room-id'}">
                        <p>Invalid Chat Room ID: ${translet.rootCauseOfRaisedException.roomId}</p>
                        <script>
                            setTimeout(function() {
                                location.href = "/";
                            }, 3500);
                        </script>
                    </c:when>
                    <c:when test="${translet.rootCauseOfRaisedException.message eq 'room-not-found'}">
                        <p>Non-existent chat room: ${translet.rootCauseOfRaisedException.roomId}</p>
                        <script>
                            setTimeout(function() {
                                location.href = "/";
                            }, 3500);
                        </script>
                    </c:when>
                    <c:otherwise>
                        <p class="lead">An unexpected error has occurred.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="cell text-center">
            <a class="alert button" href="/">OK</a>
        </div>
    </div>
</div>