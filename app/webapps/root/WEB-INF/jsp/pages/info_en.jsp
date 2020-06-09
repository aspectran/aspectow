<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="grid-y grid-frame">
    <%@ include file="includes/common-header.jsp" %>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
                <p style="position: absolute; bottom: 0; right: 20px;"><a href="?locale=ko">한국어</a></p>
            </div>
            <div class="cell auto cell-block-y">
                <div class="grid-container grid-x t15">
                    <div class="cell small-12 large-4 large-order-2 text-center large-text-right">
                        <img src="/assets/images/textchat-heartbeat.svg" width="60%" alt="Text Chat Club"/>
                    </div>
                    <div class="cell small-12 large-8 large-order-1 t15">
                        <h3>About Text Chat Club</h3>
                        <ul>
                            <li>We offer a chat service that lets you chat with new people online.</li>
                            <li>No separate sign-up process is required to use our chat service. Just enter your nickname and go.</li>
                            <li>You cannot send or receive photos or videos, you can only chat with text.</li>
                        </ul>
                    </div>
                    <div class="cell t15 large-order-3">
                        <h3>Privacy Policy</h3>
                        <h4>1. Purpose for collection of personal information</h4>
                        <p>Text Chat Club collects the minimum amount of personal information necessary to provide services.</p>
                        <h4>2. Collected items of personal information</h4>
                        <p>The personal information items collected to provide the service are as follows.<br/>
                            - Nickname, service use start time, service use end time</p>
                        <h4>3. Processing and retention period of personal information</h4>
                        <p>Text Chat Club uses personal information for the following purposes.<br/>
                            - Prevent personal identification and nickname duplication</p>
                        <p>The personal information used is stored for at least one year to prevent misuse or
                            leakage, and to determine the cause of the accident in the event of an accident.
                            Personal information older than one year is periodically discarded.</p>
                        <h4>4. Cookies and other technologies</h4>
                        <ul>
                            <li>Cookies are used to identify users who access using a web browser.</li>
                            <li>Cookies are small text data exchanged between web browsers and servers, and are stored on users' PCs or mobile devices.</li>
                            <li>Th main purpose of cookies is to identify users and may be used for the convenience of users.</li>
                            <li>You can refuse to use cookies by setting your web browser's options.</li>
                            <li>However, If you refuse to use cookies, you cannot use our service.</li>
                        </ul>
                        <h4>5. Privacy questions</h4>
                        <p>If you have any questions or concerns about Text Chat Club’s Privacy Policy,
                            please contact us at the following email address:</p>
                        <ul>
                            <li><img src="/assets/images/email-help.jpg"/></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<c:if test="${empty user}">
    <%@ include file="includes/common-sign-in.jsp" %>
</c:if>