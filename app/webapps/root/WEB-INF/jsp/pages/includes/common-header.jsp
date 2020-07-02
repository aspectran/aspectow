<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="header cell cell-block-container">
    <div class="grid-x">
        <div class="cell auto align-self-middle">
            <a class="button people" href="/" title="Text Chat Club"><i class="iconfont fi-results-demographics"></i></a>
            <h1 title="<aspectran:message code="site.title"/>"><aspectran:message code="site.title"/></h1>
        </div>
        <div class="cell shrink align-self-middle text-right">
            <c:if test="${translet.requestName eq '/'}">
                <a class="button about" href="/info" title="<aspectran:message code="common.about_us"/>"><i class="iconfont fi-info"></i></a>
            </c:if>
            <c:if test="${translet.requestName ne '/'}">
                <a type="button" class="button back" href="/" title="<aspectran:message code="common.button.back_prev"/>"><i class="iconfont fi-arrow-left"></i></a>
            </c:if>
        </div>
    </div>
</div>