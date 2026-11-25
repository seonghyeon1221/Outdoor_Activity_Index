<%-- 검색 화면: 시·도(필수) + 측정소/구(선택) --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>야외활동 지수</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"><jsp:param name="nav" value="search" /></jsp:include>
<main class="wrap narrow">
    <h1 class="hero">오늘, 밖에 나가도 괜찮을까?</h1>
    <p class="lead">미세먼지와 날씨를 함께 보고 야외활동 지수를 좋음 · 주의 · 나쁨으로 알려드려요.</p>

    <%-- 검색 폼: GET /search?sido=..&station=.. 으로 컨트롤러에 전달 --%>
    <form class="box" action="${ctx}/search">
        <label>시·도
            <select name="sido">
                <option value="">시·도를 선택하세요</option>
                <c:forEach var="s" items="${sidoList}">
                    <option ${s eq sido ? 'selected' : ''}>${s}</option>
                </c:forEach>
            </select>
        </label>
        <label>시·군·구 / 측정소 <small>선택</small>
            <%-- c:out 으로 출력해 입력값의 HTML 이 실행되지 않게 함 --%>
            <input name="station" maxlength="20" value="<c:out value='${station}'/>" placeholder="예: 중구 (비우면 시·도 평균)">
        </label>
        <button class="btn">야외활동 지수 확인하기</button>
        <c:if test="${not empty message}"><p class="error">${message}</p></c:if>
    </form>

    <%-- 즐겨찾기 바로가기 (없으면 예시 하나) --%>
    <p class="chips">
        <span>${empty favorites ? '예시' : '즐겨찾기'}</span>
        <c:forEach var="f" items="${favorites}">
            <c:url var="u" value="/search"><c:param name="sido" value="${f.sido}"/><c:param name="station" value="${f.station}"/></c:url>
            <a class="chip" href="${u}"><c:out value="${f.name()}"/></a>
        </c:forEach>
        <c:if test="${empty favorites}">
            <c:url var="u" value="/search"><c:param name="sido" value="서울"/><c:param name="station" value="중구"/></c:url>
            <a class="chip" href="${u}">서울 중구</a>
        </c:if>
    </p>
</main>
</body>
</html>
