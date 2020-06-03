<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div class="header cell cell-block-container">
    <div class="grid-x">
        <div class="cell auto align-self-middle">
            <a class="button people" href="/" title="Text Chat Club"><i class="iconfont fi-results-demographics"></i></a>
            <h2>Text Chat Club</h2>
        </div>
        <div class="cell shrink align-self-middle text-right">
            <c:if test="${translet.requestName eq '/'}">
                <a class="button about" href="/about" title="About Text Chat Club"><i class="iconfont fi-info"></i></a>
            </c:if>
            <c:if test="${translet.requestName ne '/'}">
                <button type="button" class="button back" onclick="history.back();" title="Back to previous page"><i class="iconfont fi-arrow-left"></i></button>
            </c:if>
        </div>
    </div>
</div>