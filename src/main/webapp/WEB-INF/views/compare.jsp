<%-- 여러 지역 비교 화면 (담당: D)
     - 컨트롤러가 넘겨주는 값: inputs(입력 3칸), items(비교 결과), sidoList, favorites --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>지역 비교 | 야외활동 지수</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=3">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="nav" value="compare" />
</jsp:include>

<main class="main-container">

    <section class="page-head">
        <h1>지역 비교</h1>
        <p>최대 3곳을 나란히 비교해요. 시·도만 고르면 시·도 평균으로 비교합니다.</p>
    </section>

    <form class="box" action="${pageContext.request.contextPath}/compare" method="get">
        <c:forEach var="in" items="${inputs}" varStatus="st">
            <div class="compare-row">
                <span class="idx">${st.count}</span>
                <select name="sido${st.count}" aria-label="${st.count}번째 시·도">
                    <option value="">시·도 선택</option>
                    <c:forEach var="s" items="${sidoList}">
                        <option value="${s}" ${s eq in.sido ? 'selected' : ''}>${s}</option>
                    </c:forEach>
                </select>
                <input type="text" name="station${st.count}" maxlength="20"
                       aria-label="${st.count}번째 시·군·구"
                       value="<c:out value='${in.station}'/>"
                       placeholder="시·군·구 / 측정소 (선택)">
            </div>
        </c:forEach>
        <div class="compare-foot">
            <span>
                <c:if test="${not empty favorites}">
                    <c:url var="favCmp" value="/compare">
                        <c:forEach var="f" items="${favorites}" end="2" varStatus="fs">
                            <c:param name="sido${fs.count}" value="${f.sido}"/>
                            <c:param name="station${fs.count}" value="${f.station}"/>
                        </c:forEach>
                    </c:url>
                    <a class="text-link" href="${favCmp}">즐겨찾기 상위 3곳으로 채우기</a>
                </c:if>
            </span>
            <button type="submit" class="btn">비교하기</button>
        </div>
    </form>

    <c:if test="${not empty items}">
        <section class="compare-grid">
            <c:forEach var="it" items="${items}">
                <c:set var="cd" value="${it.card}"/>
                <article class="mini-card ${not empty cd.index ? cd.index.level.cssClass : ''}">
                    <c:if test="${it.best}"><span class="best-tag">가장 좋음</span></c:if>
                    <h3><c:out value="${it.label}"/></h3>
                    <c:choose>
                        <c:when test="${empty cd}">
                            <p class="error-message" style="margin-top:12px"><c:out value="${it.errorMessage}"/></p>
                        </c:when>
                        <c:otherwise>
                            <c:choose>
                                <c:when test="${not empty cd.index}">
                                    <p class="verdict-value">${cd.index.level.label}</p>
                                    <p class="msg">
                                        <c:choose>
                                            <c:when test="${not empty cd.index.reasons}">
                                                <c:forEach var="r" items="${cd.index.reasons}" varStatus="rs"><c:out value="${r}"/><c:if test="${not rs.last}"> · </c:if></c:forEach>
                                            </c:when>
                                            <c:otherwise>감점 요인 없음</c:otherwise>
                                        </c:choose>
                                    </p>
                                </c:when>
                                <c:otherwise>
                                    <p class="verdict-value" style="color:#94a3b8">-</p>
                                    <p class="msg">판정 정보 없음</p>
                                </c:otherwise>
                            </c:choose>
                            <dl class="mini-stats">
                                <dt>PM10</dt>
                                <dd><c:choose>
                                    <c:when test="${cd.airAvailable and not empty cd.air.pm10}"><span class="num">${cd.air.pm10}</span> <span class="grade ${cd.air.pm10Grade.cssClass}">${cd.air.pm10Grade.label}</span></c:when>
                                    <c:otherwise><span class="env-na">정보 없음</span></c:otherwise>
                                </c:choose></dd>
                                <dt>PM2.5</dt>
                                <dd><c:choose>
                                    <c:when test="${cd.airAvailable and not empty cd.air.pm25}"><span class="num">${cd.air.pm25}</span> <span class="grade ${cd.air.pm25Grade.cssClass}">${cd.air.pm25Grade.label}</span></c:when>
                                    <c:otherwise><span class="env-na">정보 없음</span></c:otherwise>
                                </c:choose></dd>
                                <dt>날씨</dt>
                                <dd><c:choose>
                                    <c:when test="${cd.weatherAvailable}">${cd.weather.precipitation.precipitating ? cd.weather.precipitation.label : cd.weather.sky.label}
                                        · <span class="num"><fmt:formatNumber value="${cd.weather.temperature}" maxFractionDigits="1"/>℃</span></c:when>
                                    <c:otherwise><span class="env-na"><c:out value="${cd.weatherError.message}"/></span></c:otherwise>
                                </c:choose></dd>
                                <c:if test="${cd.weatherAvailable}">
                                    <dt>바람</dt>
                                    <dd class="num"><fmt:formatNumber value="${cd.weather.windSpeed}" maxFractionDigits="1"/>m/s</dd>
                                </c:if>
                            </dl>
                            <c:if test="${cd.airAvailable and cd.air.filledFromAverage}"><p class="hint" style="margin:0 0 10px">측정소 <c:out value="${empty cd.air.statusNote ? '측정값 없음' : cd.air.statusNote}"/> → 시·도 평균값 사용</p></c:if>
                            <c:if test="${cd.savedDataUsed}"><p class="hint" style="margin:0 0 10px">저장된 데이터</p></c:if>
                            <c:url var="detail" value="/search"><c:param name="sido" value="${it.sido}"/><c:param name="station" value="${it.station}"/></c:url>
                            <a class="text-link" href="${detail}">자세히 보기 →</a>
                        </c:otherwise>
                    </c:choose>
                </article>
            </c:forEach>
        </section>
    </c:if>

</main>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

</body>
</html>
