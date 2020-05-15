<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<link rel="stylesheet" type="text/css" href="/assets/css/rooms.css?v11" />
<script src="/assets/js/rooms.js?v0.1"></script>
<div class="lobby grid-y grid-frame">
    <div class="header cell cell-block-container">
        <div class="grid-x">
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
                    <div class="rooms grid-x grid-margin-x grid-margin-y t15">
                        <div class="cell medium-12 large-4 card room random">
                            <div class="card-section">
                                <a href="/rooms/random"><h4>Random Chat</h4></a>
                                <p>Best place to talk to strangers</p>
                                <a class="button small start" href="/rooms/random">Start chat</a>
                            </div>
                        </div>
                        <div class="cell medium-12 large-8 no-room">
                            <div class="card-section">
                                <p>Welcome to Text Chat Club.</p>
                                You must be 18+ to start random chat with strangers.<br/>
                                How To Chat With Strangers Safely?<br/>
                                Don't share any personal information or contacts, don't send any money to strangers.
                            </div>
                        </div>
                        <div class="cell small-12 medium-6 large-4 card room">
                            <div class="card-section">
                                <a href="/rooms/KUpsx-Uw2dweCWaNreqtfw"><h4>Room 1</h4></a>
                                <p>This row of cards is embedded in an X-Y Block Grid.</p>
                                <a class="button small start" href="/rooms/KUpsx-Uw2dweCWaNreqtfw">Start chat</a>
                            </div>
                        </div>
                        <div class="cell small-12 medium-6 large-4 card room">
                            <div class="card-section">
                                <a href="/rooms/yqOkZ13kfTArOCYb8Em88w"><h4>Room 2</h4></a>
                                <p>It has an easy to override visual style, and is appropriately subdued.</p>
                                <a class="button small start" href="/rooms/yqOkZ13kfTArOCYb8Em88w">Start chat</a>
                            </div>
                        </div>
                        <div class="cell small-12 medium-6 large-4 card room" data-room-id="3">
                            <div class="card-section">
                                <a href="/rooms/Mzc-3ShpShu5LUU22Hkn1A"><h4>Room 3</h4></a>
                                <p>It has an easy to override visual style, and is appropriately subdued.</p>
                                <a class="button small start" href="/rooms/Mzc-3ShpShu5LUU22Hkn1A">Start chat</a>
                            </div>
                        </div>
                        <div class="cell small-12 medium-6 large-4 card room" data-room-id="4">
                            <div class="card-section">
                                <a href="/rooms/LtjfLstiYBE0UC7kqIZCBw"><h4>Room 4</h4></a>
                                <p>This row of cards is embedded in an X-Y Block Grid.</p>
                                <a class="button small start" href="/rooms/LtjfLstiYBE0UC7kqIZCBw">Start chat</a>
                            </div>
                        </div>
                        <div class="cell small-12 medium-6 large-4 card room" data-room-id="5">
                            <div class="card-section">
                                <a href="/rooms/mSsNxsNBkNh2pUgdrzmwQQ"><h4>Room 5</h4></a>
                                <p>It has an easy to override visual style, and is appropriately subdued.</p>
                                <a class="button small start" href="/rooms/mSsNxsNBkNh2pUgdrzmwQQ">Start chat</a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>