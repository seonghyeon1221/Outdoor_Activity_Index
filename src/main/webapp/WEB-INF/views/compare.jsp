<%-- 지역 비교: 최대 3곳의 카드를 나란히, 가장 좋은 곳 표시 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.outdoorindex.service.IndexPolicy" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>지역 비교 | 야외활동 지수</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"><jsp:param name="nav" value="compare" /></jsp:include>
<main class="wrap">
    <h1>지역 비교</h1>
    <p class="sub">최대 3곳을 나란히 비교해요. 시·도만 고르면 시·도 평균으로 비교합니다.</p>

    <%-- 입력 폼 3줄 (비교 후에도 입력값을 다시 채워 둠) --%>
    <form class="box" action="${ctx}/compare">
        <c:forEach var="i" begin="1" end="3">
            <c:set var="sel" value="${param['sido' += i]}" />
            <p class="cmp-row">
                <b>${i}</b>
                <select name="sido${i}">
                    <option value="">시·도 선택</option>
                    <c:forEach var="s" items="${sidoList}"><option ${s eq sel ? 'selected' : ''}>${s}</option></c:forEach>
                </select>
                <input name="station${i}" maxlength="20" value="<c:out value="${param['station' += i]}"/>" placeholder="시·군·구 / 측정소 (선택)">
            </p>
        </c:forEach>
        <button class="btn">비교하기</button>
    </form>

    <%-- 지역별 요약 카드 (실패한 곳은 오류 문구, 가장 좋은 곳에 표시) --%>
    <div class="cards">
        <c:forEach var="it" items="${items}">
            <c:set var="cd" value="${it.card}" />
            <article class="box mini ${empty cd.result ? '' : cd.result.level.css}">
                <c:if test="${it.best}"><span class="best">가장 좋음</span></c:if>
                <h3><c:out value="${empty cd ? it.sido : cd.place()}"/></h3>
                <c:choose>
                    <c:when test="${empty cd}"><p class="error"><c:out value="${it.error}"/></p></c:when>
                    <c:otherwise>
                        <p class="big">${empty cd.result ? '-' : cd.result.level.label}</p>
                        <p class="sub">${empty cd.result.reasons ? '감점 요인 없음' : String.join(' · ', cd.result.reasons)}</p>
                        <p class="row"><span>PM10</span><b>${empty cd.air.pm10 ? '-' : cd.air.pm10}</b><i class="grade ${cd.air.pm10Grade().css}">${cd.air.pm10Grade().label}</i></p>
                        <p class="row"><span>PM2.5</span><b>${empty cd.air.pm25 ? '-' : cd.air.pm25}</b><i class="grade ${cd.air.pm25Grade().css}">${cd.air.pm25Grade().label}</i></p>
                        <p class="row"><span>날씨</span><b>${empty cd.weather ? '정보 없음' : cd.weather.sky += ' · ' += IndexPolicy.num(cd.weather.temp) += '℃'}</b></p>
                        <c:url var="u" value="/search"><c:param name="sido" value="${cd.sido}"/><c:param name="station" value="${cd.station()}"/></c:url>
                        <a class="link" href="${u}">자세히 보기 →</a>
                    </c:otherwise>
                </c:choose>
            </article>
        </c:forEach>
    </div>
</main>
</body>
</html>
