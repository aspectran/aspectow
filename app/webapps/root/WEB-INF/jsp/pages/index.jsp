<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<script src="https://www.google.com/recaptcha/api.js?render=explicit&onload=loadCaptcha"></script>
<script src="/assets/js/recaptcha.js?v1"></script>
<script src="/assets/js/page-index.js?v0.1"></script>
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
            <div class="sidebar cell medium-4 large-3 cell-block-y show-for-large">
            </div>
            <div class="cell auto cell-block-y">
                <div class="grid-container grid-x grid-margin-y">
                    <div class="cell large-8">
                        <form id="form-sign-in" method="post">
                            <h3>Welcome to Text Chat Club.</h3>
                            <p>Chat with anyone. Meet new people at random. Completely anonymously.<br/>No registration. No logs.</p>
                            <p>Please enter your username.</p>
                            <div class="input-group">
                                <input class="input-group-field" type="text" id="username" maxlength="30" placeholder="Username" autocomplete="off" autofocus/>
                                <div class="input-group-button">
                                    <button type="submit" class="button" title="Sign in">Sign in</button>
                                </div>
                            </div>
                            <p class="form-error already-in-use">
                                Your username is already in use or a similar name already exists.<br/>
                                Please enter a different username.
                            </p>
                            <div id="captcha-container"></div>
                        </form>
                    </div>
                    <div class="cell">
                        <h3>TextChatClub launches Beta Service on May 24th!</h3>
                        <p>This website provides a chat service that anyone can talk to freely.</p>
                        <p>Here are the differences from other chat sites:</p>
                        <ul>
                            <li>Text-based chat to prevent social side effects and to feel the purity of the past</li>
                            <li>Random chat to make new friends</li>
                            <li>Easy-to-use interface</li>
                        </ul>
                        <p>Thank you.</p>
                    </div>
<%--                    <div class="cell">--%>
<%--                        <h3>출시 예정</h3>--%>
<%--                        <p>우리는 이 웹사이트를 만들기 위해 열심히 노력하고 있습니다.<br/>--%>
<%--                            이 웹사이트는 누구나 자유롭게 대화할 수 있는 채팅 서비스를 제공합니다.</p>--%>
<%--                        <p>다른 채팅 사이트와 다른 점은 다음과 같습니다.</p>--%>
<%--                        <ul>--%>
<%--                            <li>사회적인 부작용을 방지하고, 과거의 순수함을 느낄 수 있는 텍스트 기반의 채팅</li>--%>
<%--                            <li>새로운 친구를 사귈 수 있는 랜덤 채팅</li>--%>
<%--                            <li>남녀노소 누구나 사용하기 쉬운 인터페이스 제공</li>--%>
<%--                        </ul>--%>
<%--                        <p>감사합니다.</p>--%>
<%--                    </div>--%>
                </div>
            </div>
        </div>
    </div>
</div>
<%@ include file="includes/index-already-signed-in.jsp" %>
<%@ include file="includes/common-wait-popup.jsp" %>