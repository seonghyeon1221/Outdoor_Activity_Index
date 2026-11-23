<%-- 검색 화면 (담당: D)
     - 시·도(필수) + 측정소/구 이름(선택) 입력
     - 컨트롤러가 넘겨주는 값: sidoList, sido, station, errorMessage --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp"/>

<section class="hero">
    <h1>오늘 밖에 나가도 될까?</h1>
    <p>지역을 고르면 <strong>미세먼지</strong>와 <strong>날씨</strong>를 합쳐<br>
       야외활동 지수를 <span class="pill good">좋음</span> <span class="pill caution">주의</span> <span class="pill bad">나쁨</span> 으로 알려드려요.</p>
</section>

<section class="panel search-panel">
    <form class="search-form" action="${pageContext.request.contextPath}/search" method="get">
        <div class="field">
            <label for="sido">시·도 <span class="req">필수</span></label>
            <select id="sido" name="sido">
                <option value="">시·도 선택</option>
                <c:forEach var="s" items="${sidoList}">
                    <option value="${s}" <c:if test="${s eq sido}">selected</c:if>>${s}</option>
                </c:forEach>
            </select>
        </div>
        <div class="field">
            <label for="station">측정소 / 구 이름 <span class="opt">선택</span></label>
            <input type="text" id="station" name="station" maxlength="20"
                   value="<c:out value='${station}'/>" placeholder="예: 중구 (비우면 시·도 평균)">
        </div>
        <button type="submit" class="btn-primary">검색하기</button>
    </form>

    <c:if test="${not empty errorMessage}">
        <p class="form-error" role="alert"><c:out value="${errorMessage}"/></p>
    </c:if>

    <div class="quick">
        <span class="quick-label">빠른 검색</span>
        <c:url var="q1" value="/search"><c:param name="sido" value="서울"/><c:param name="station" value="중구"/></c:url>
        <c:url var="q2" value="/search"><c:param name="sido" value="서울"/><c:param name="station" value="강남구"/></c:url>
        <c:url var="q3" value="/search"><c:param name="sido" value="부산"/></c:url>
        <c:url var="q4" value="/search"><c:param name="sido" value="제주"/></c:url>
        <a class="chip" href="${q1}">서울 중구</a>
        <a class="chip" href="${q2}">서울 강남구</a>
        <a class="chip" href="${q3}">부산</a>
        <a class="chip" href="${q4}">제주</a>
    </div>
</section>

<section class="how">
    <div class="how-step"><b>1</b>에어코리아에서 시·도의 <em>측정소</em>와 미세먼지를 받아요</div>
    <div class="how-step"><b>2</b>고른 측정소의 위치로 기상청 <em>날씨</em>를 가져와요</div>
    <div class="how-step"><b>3</b>두 정보를 합쳐 <em>야외활동 지수</em>를 판정해요</div>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>
