<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="grid-y grid-frame">
    <%@ include file="includes/common-header.jsp" %>
    <div class="body shadow cell auto cell-block-container">
        <div class="grid-x full-height">
            <div class="sidebar cell medium-4 large-3 cell-block-y hide-for-small-only">
            </div>
            <div class="cell auto cell-block-y">
                <div class="grid-container grid-x t15">
                    <div class="cell">
                        <h3>텍스트챗클럽 소개</h3>
                        <ul>
                            <li>온라인에서 새로운 사람들과 대화할 수 있는 채팅 서비스를 제공합니다.</li>
                            <li>채팅 서비스를 이용하기 위한 별도의 가입 절차가 없으며, 대화명만 입력하면 됩니다.</li>
                            <li>사진, 동영상은 주고 받을 수 없으며, 오직 문자로만 대화할 수 있습니다.</li>
                        </ul>
                    </div>
                    <div class="cell t15">
                        <h3>개인 정보 처리 방침</h3>
                        <h4>1. 개인정보 수집 목적</h4>
                        <p>텍스트챗클럽은 서비스 제공을 위한 필요 최소한의 개인정보를 수집하고 있습니다.</p>
                        <h4>2. 개인정보 수집 항목</h4>
                        <p>서비스 제공을 위해 수집하는 개인정보 항목은 다음과 같습니다.<br/>
                            - 대화명, 서비스 이용 시작 시간, 서비스 이용 종료 시간</p>
                        <h4>3. 개인정보 사용 목적</h4>
                        <p>텍스트챗클럽은 다음의 목적을 위하여 개인정보를 사용하고 있습니다.<br/>
                            - 개인 식별 및 대화명 중복 방지</p>
                        <h4>4. 개인정보 보유 기간</h4>
                        <p>개인정보의 오·남용이나 유출사고를 예방하고 사고가 발생한 경우 사고 원인 규명을 위해 최소 1년 이상 보관됩니다.</p>
                        <h4>5. 개인정보 자동 수집 장치의 설치•운영 및 거부에 관한 사항</h4>
                        <ul>
                            <li>웹브라저를 이용하는 접속자를 식별하기 위해 쿠키(Cookie)를 사용합니다.</li>
                            <li>쿠키는 웹브라우저와 서버가 서로 주고 받는 작은 텍스트 데이터이며, 이용자들의 PC 또는 모바일 장치에 저장됩니다.</li>
                            <li>쿠키 주 사용 목적은 이용자를 식별하기 위한 것이며, 이용자의 편의를 위한 목적으로 사용되기도 합니다.</li>
                            <li>웹브라우저의 옵션 설정을 통하여 쿠키 사용을 거부할 수 있습니다.</li>
                            <li>쿠키 저장을 거부할 경우 서비스를 이용할 수 없습니다.</li>
                        </ul>
                        <h4>6. 개인정보 보호책임자</h4>
                        <p>텍스트챗클럽은 개인정보 처리에 관한 업무를 총괄해서 책임지고, 개인정보 처리와 관련한 고객의 불만처리 및 피해구제 등을 위하여 아래와 같이 개인정보 보호책임자를 지정하고 있습니다.</p>
                        <p>개인정보 보호책임자<br/>
                            책임자: 정주호(운영 및 제작자)<br/>
                            연락처: aspectran@gmail.com</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<c:if test="${empty user}">
    <%@ include file="includes/common-sign-in.jsp" %>
</c:if>