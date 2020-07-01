<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<script>
    const usersByCountry = ${not empty page.usersByCountry ? page.usersByCountry : "{}"};
    //const usersByCountry = { KR: 9, KW: 8, KG: 7, LY: 6, NL: 5, MO: 4, RO: 3, SC: 2 };
</script>
<div class="users-by-country">
    <div class="legends">
    </div>
    <div class="series single">
        <div class="item none"></div>
    </div>
</div>
