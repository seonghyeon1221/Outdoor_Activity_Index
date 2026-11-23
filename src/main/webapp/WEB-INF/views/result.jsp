<%-- 결과 카드 화면 (담당: D)
     - 컨트롤러가 넘겨주는 값: card(OutdoorCardDTO), sidoList, sido, station
     - 미세먼지/날씨 영역은 서로 독립: 한쪽이 실패해도 다른 쪽은 정상 표시 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/common/header.jsp"/>

<%-- 작은 검색바 --%>
<form class="search-bar" action="${pageContext.request.contextPath}/search" method="get">
    <select name="sido" aria-label="시·도">
        <c:forEach var="s" items="${sidoList}">
            <option value="${s}" <c:if test="${s eq card.region.sido}">selected</c:if>>${s}</option>
        </c:forEach>
    </select>
    <input type="text" name="station" maxlength="20" aria-label="측정소 / 구 이름"
           value="<c:out value='${station}'/>" placeholder="측정소 / 구 (선택)">
    <button type="submit" class="btn-primary">검색</button>
</form>

<article class="card">
    <%-- ① 카드 머리 --%>
    <header class="card-head">
        <div>
            <h2 class="place">📍 <c:out value="${card.region.displayName}"/></h2>
            <c:if test="${card.airAvailable and card.air.sidoAverage}">
                <span class="tag">미세먼지 시·도 평균</span>
            </c:if>
        </div>
        <div class="time">
            <c:if test="${card.airAvailable and not empty card.air.dataTime}">
                <div>미세먼지 <c:out value="${card.air.dataTime}"/></div>
            </c:if>
            <c:if test="${card.weatherAvailable}">
                <div>날씨 ${card.weather.baseTimeDisplay}</div>
            </c:if>
        </div>
    </header>

    <%-- ② 야외활동 지수 --%>
    <section class="index">
        <c:choose>
            <c:when test="${not empty card.index}">
                <div class="badge ${card.index.level.cssClass}">${card.index.level.label}</div>
                <div class="index-body">
                    <p class="message"><c:out value="${card.index.message}"/></p>
                    <c:if test="${not empty card.index.reasons}">
                        <ul class="reasons">
                            <c:forEach var="r" items="${card.index.reasons}">
                                <li><c:out value="${r}"/></li>
                            </c:forEach>
                        </ul>
                    </c:if>
                    <c:if test="${card.index.airMissing}">
                        <p class="note">※ 미세먼지 정보 없이 날씨만으로 판정했어요.</p>
                    </c:if>
                    <c:if test="${not card.weatherAvailable}">
                        <p class="note">※ 날씨 정보 없이 미세먼지만으로 판정했어요.</p>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="badge unknown">?</div>
                <div class="index-body">
                    <p class="message">미세먼지와 날씨 정보를 모두 불러오지 못해 판정할 수 없어요.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </section>

    <%-- 융합 흐름 표시: API ① 결과(측정소) → 위치 → API ② --%>
    <section class="fusion" aria-label="두 API 연결 과정">
        <span class="fusion-step air">에어코리아 측정소
            <b><c:choose>
                <c:when test="${card.airAvailable and not card.air.sidoAverage}"><c:out value="${card.air.stationName}"/></c:when>
                <c:when test="${card.airAvailable}">시·도 평균</c:when>
                <c:otherwise>조회 실패</c:otherwise>
            </c:choose></b>
        </span>
        <span class="fusion-arrow">→</span>
        <span class="fusion-step">격자 <b>(${card.region.nx}, ${card.region.ny})</b></span>
        <span class="fusion-arrow">→</span>
        <span class="fusion-step weather">기상청 날씨</span>
    </section>

    <div class="grid">
        <%-- ③ 미세먼지 --%>
        <section class="panel-box">
            <h3>미세먼지 <small>에어코리아</small></h3>
            <c:choose>
                <c:when test="${not empty card.airError}">
                    <p class="panel-error"><c:out value="${card.airError.message}"/></p>
                </c:when>
                <c:otherwise>
                    <div class="row">
                        <span class="label">PM10</span>
                        <c:choose>
                            <c:when test="${empty card.air.pm10}"><span class="na">정보 없음</span></c:when>
                            <c:otherwise>
                                <span class="value">${card.air.pm10}<small>㎍/㎥</small></span>
                                <span class="grade ${card.air.pm10Grade.cssClass}">${card.air.pm10Grade.label}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="row">
                        <span class="label">PM2.5</span>
                        <c:choose>
                            <c:when test="${empty card.air.pm25}"><span class="na">정보 없음</span></c:when>
                            <c:otherwise>
                                <span class="value">${card.air.pm25}<small>㎍/㎥</small></span>
                                <span class="grade ${card.air.pm25Grade.cssClass}">${card.air.pm25Grade.label}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <p class="meta">
                        <c:choose>
                            <c:when test="${card.air.sidoAverage}">
                                <c:out value="${card.region.sido}"/> 측정소 ${card.stationNames.size()}곳 평균
                            </c:when>
                            <c:otherwise>측정소: <c:out value="${card.air.stationName}"/></c:otherwise>
                        </c:choose>
                    </p>
                </c:otherwise>
            </c:choose>
        </section>

        <%-- ④ 날씨 --%>
        <section class="panel-box">
            <h3>날씨 <small>기상청</small></h3>
            <c:choose>
                <c:when test="${not empty card.weatherError}">
                    <p class="panel-error"><c:out value="${card.weatherError.message}"/></p>
                </c:when>
                <c:otherwise>
                    <div class="sky">
                        <span class="sky-icon" aria-hidden="true">
                            <c:choose>
                                <c:when test="${card.weather.precipitation.precipitating}">🌧</c:when>
                                <c:when test="${card.weather.sky eq 'CLEAR'}">☀</c:when>
                                <c:when test="${card.weather.sky eq 'PARTLY_CLOUDY'}">⛅</c:when>
                                <c:when test="${card.weather.sky eq 'CLOUDY'}">☁</c:when>
                                <c:otherwise>·</c:otherwise>
                            </c:choose>
                        </span>
                        <span class="sky-label">${card.weather.sky.label}</span>
                    </div>
                    <div class="row">
                        <span class="label">기온</span>
                        <c:choose>
                            <c:when test="${empty card.weather.temperature}"><span class="na">정보 없음</span></c:when>
                            <c:otherwise><span class="value"><fmt:formatNumber value="${card.weather.temperature}" maxFractionDigits="1"/><small>℃</small></span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="row">
                        <span class="label">습도</span>
                        <c:choose>
                            <c:when test="${empty card.weather.humidity}"><span class="na">정보 없음</span></c:when>
                            <c:otherwise><span class="value">${card.weather.humidity}<small>%</small></span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="row">
                        <span class="label">강수</span>
                        <span class="value text">${card.weather.precipitation.label}</span>
                    </div>
                    <div class="row">
                        <span class="label">풍속</span>
                        <c:choose>
                            <c:when test="${empty card.weather.windSpeed}"><span class="na">정보 없음</span></c:when>
                            <c:otherwise><span class="value"><fmt:formatNumber value="${card.weather.windSpeed}" maxFractionDigits="1"/><small>m/s</small></span></c:otherwise>
                        </c:choose>
                    </div>
                    <c:if test="${card.airAvailable and not card.air.sidoAverage and not card.region.hasStation}">
                        <p class="meta">측정소 위치 표에 없는 측정소라 시·도 대표 지점의 날씨예요.</p>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <%-- ⑤ 확장: 시간별 예보 (데이터가 있을 때만) --%>
    <c:if test="${not empty card.hourlyForecasts}">
        <section class="hourly">
            <h3>시간별 예보</h3>
            <ul>
                <c:forEach var="h" items="${card.hourlyForecasts}">
                    <li>
                        <span>${h.fcstTime}</span>
                        <span>${h.sky.label}</span>
                        <span><fmt:formatNumber value="${h.temperature}" maxFractionDigits="0"/>℃</span>
                        <c:if test="${not empty h.level}"><span class="grade ${h.level.cssClass}">${h.level.label}</span></c:if>
                    </li>
                </c:forEach>
            </ul>
        </section>
    </c:if>
</article>

<%-- 같은 시·도 다른 측정소 (API ① 결과가 다음 검색으로 이어짐) --%>
<c:if test="${not empty card.stationNames}">
    <section class="stations">
        <h3><c:out value="${card.region.sido}"/>의 다른 측정소 보기</h3>
        <div class="chips">
            <c:url var="avgUrl" value="/search"><c:param name="sido" value="${card.region.sido}"/></c:url>
            <a class="chip ${card.airAvailable and card.air.sidoAverage ? 'active' : ''}" href="${avgUrl}">시·도 평균</a>
            <c:forEach var="st" items="${card.stationNames}">
                <c:url var="stUrl" value="/search">
                    <c:param name="sido" value="${card.region.sido}"/>
                    <c:param name="station" value="${st}"/>
                </c:url>
                <a class="chip ${card.airAvailable and st eq card.air.stationName ? 'active' : ''}" href="${stUrl}"><c:out value="${st}"/></a>
            </c:forEach>
        </div>
    </section>
</c:if>

<p class="back"><a href="${pageContext.request.contextPath}/search">← 다른 지역 검색</a></p>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>
