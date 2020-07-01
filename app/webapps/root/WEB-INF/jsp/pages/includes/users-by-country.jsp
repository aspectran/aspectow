<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<script>
    const usersByCountry = ${not empty page.usersByCountry ? page.usersByCountry : "{}"};
    //const usersByCountry = { KR: 9, KW: 8, KG: 7, LY: 6, NL: 5, MO: 4, RO: 3, SC: 2 };
</script>
<div class="users-by-country">
    <div class="legends">
        <c:if test="${page.usersByCountry ne '{}'}"><span class="item none"><em></em><span></span></span></c:if>
    </div>
    <div class="series single">
        <c:if test="${page.usersByCountry ne '{}'}"><div class="item none">0</div></c:if>
    </div>
</div>
