<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="lobby grid-y grid-frame">
    <div class="header cell cell-block-container">
        <div class="grid-x grid-padding-x">
            <div class="cell auto align-self-middle">
                <h2><i class="fi-results-demographics"></i> Text Chat Club</h2>
            </div>
            <div class="cell shrink align-self-middle text-right">
                <button type="button" class="button signout" title="Sign out">Sign out</button>
            </div>
        </div>
    </div>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y show-for-medium">
                <%@ include file="includes/footer-sidebar-user.jsp" %>
                <div class="side-menu"></div>
            </div>
            <div class="cell auto cell-block-y">
                <div class="grid-container">
                    <div class="grid-x grid-margin-x grid-margin-y small-up-1 medium-up-2 large-up-3 t15">
                        <div class="cell card room" data-room-id="0">
                            <div class="card-section">
                                <a href="/chat/KUpsx-Uw2dweCWaNreqtfw"><h4>Room 1</h4></a>
                                <p>This row of cards is embedded in an X-Y Block Grid.</p>
                            </div>
                        </div>
                        <div class="cell card room" data-room-id="1">
                            <div class="card-section">
                                <a href="/chat/yqOkZ13kfTArOCYb8Em88w"><h4>Room 2</h4></a>
                                <p>It has an easy to override visual style, and is appropriately subdued.</p>
                            </div>
                        </div>
                        <div class="cell card room" data-room-id="2">
                            <div class="card-section">
                                <a href="/chat/Mzc-3ShpShu5LUU22Hkn1A"><h4>Room 3</h4></a>
                                <p>It has an easy to override visual style, and is appropriately subdued.</p>
                            </div>
                        </div>
                        <div class="cell card room" data-room-id="0">
                            <div class="card-section">
                                <a href="/chat/LtjfLstiYBE0UC7kqIZCBw"><h4>Room 4</h4></a>
                                <p>This row of cards is embedded in an X-Y Block Grid.</p>
                            </div>
                        </div>
                        <div class="cell card room" data-room-id="1">
                            <div class="card-section">
                                <a href="/chat/mSsNxsNBkNh2pUgdrzmwQQ"><h4>Room 5</h4></a>
                                <p>It has an easy to override visual style, and is appropriately subdued.</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="/assets/js/lobby.js"></script>
<script>
    const currentUser = "${user.username}";
</script>