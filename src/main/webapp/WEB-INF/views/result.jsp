<%-- 결과 화면 (담당: D)
     - 컨트롤러가 넘겨주는 값: card(OutdoorCardDTO), station, favStation, isFavorite, dbAvailable
     - 미세먼지/날씨 영역은 서로 독립: 한쪽이 실패해도 다른 쪽은 정상 표시
     - 계산 과정의 기준값은 OutdoorIndexPolicy 상수를 그대로 읽어 표시 (기준을 바꾸면 화면도 같이 바뀜) --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="com.outdoorindex.policy.OutdoorIndexPolicy" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="air" value="${card.air}" />
<c:set var="wx" value="${card.weather}" />
<%-- 화면에 보일 지역 이름: 측정소를 찾았으면 "시·도 측정소", 시·도 평균이면 "시·도" --%>
<c:set var="placeName" value="${card.airAvailable and not air.sidoAverage and not empty air.stationName ? card.region.sido.concat(' ').concat(air.stationName) : card.region.displayName}" />
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${placeName}"/> | 야외활동 지수</title>
    <link rel="stylesheet" href="${ctx}/css/style.css?v=3">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="nav" value="search" />
</jsp:include>

<main class="main-container">

    <%-- 지역 · 기준 시각 · 즐겨찾기/비교 --%>
    <div class="result-head">
        <div>
            <h1><c:out value="${placeName}" /></h1>
            <p class="result-meta">
                <c:if test="${card.airAvailable and not empty air.dataTime}">
                    미세먼지 <c:out value="${air.dataTime}" />
                </c:if>
                <c:if test="${card.airAvailable and not empty air.dataTime and card.weatherAvailable}"> · </c:if>
                <c:if test="${card.weatherAvailable}">날씨 ${wx.baseTimeDisplay}</c:if>
            </p>
        </div>
        <div class="result-tools">
            <c:if test="${dbAvailable}">
                <form method="post" action="${ctx}/favorites">
                    <input type="hidden" name="action" value="${isFavorite ? 'remove' : 'add'}">
                    <input type="hidden" name="sido" value="<c:out value='${card.region.sido}'/>">
                    <input type="hidden" name="station" value="<c:out value='${favStation}'/>">
                    <c:url var="backUrl" value="/search"><c:param name="sido" value="${card.region.sido}"/><c:param name="station" value="${favStation}"/></c:url>
                    <input type="hidden" name="back" value="<c:out value='${fn:substringAfter(backUrl, ctx)}'/>">
                    <button type="submit" class="btn-line ${isFavorite ? 'on' : ''}">
                        ${isFavorite ? '&#9733; 즐겨찾기됨' : '&#9734; 즐겨찾기'}
                    </button>
                </form>
            </c:if>
            <c:url var="cmpUrl" value="/compare"><c:param name="sido1" value="${card.region.sido}"/><c:param name="station1" value="${favStation}"/></c:url>
            <a class="btn-line" href="${cmpUrl}">다른 지역과 비교</a>
        </div>
    </div>

    <%-- 입력한 측정소를 찾지 못해 시·도 평균으로 대신 보여주는 경우 안내 --%>
    <c:if test="${not empty station and card.airAvailable and air.sidoAverage}">
        <p class="notice">'<c:out value="${station}"/>' 측정소를 찾지 못해 <c:out value="${card.region.sido}"/> 전체 평균으로 보여드려요. 아래 측정소 목록에서 골라 보세요.</p>
    </c:if>

    <%-- 고른 측정소에 값이 없어(통신장애 등) 시·도 평균으로 채운 경우 안내 --%>
    <c:if test="${card.airAvailable and air.filledFromAverage}">
        <p class="notice">'<c:out value="${air.stationName}"/>' 측정소가 지금
            <b><c:out value="${empty air.statusNote ? '측정값 없음' : air.statusNote}"/></b> 상태라 에어코리아가 값을 주지 않았어요.
            비어 있는 값은 <c:out value="${card.region.sido}"/> 평균으로 채워 판정했어요. (날씨는 그대로 이 측정소 위치 기준)</p>
    </c:if>

    <c:if test="${card.savedDataUsed}">
        <p class="notice">지금은 실시간 조회가 되지 않아 마지막으로 저장된 데이터를 보여주고 있어요.</p>
    </c:if>

    <%-- ① 야외활동 지수 --%>
    <c:choose>
        <c:when test="${not empty card.index}">
            <section class="verdict ${card.index.level.cssClass}">
                <div class="verdict-level">
                    <div class="verdict-label">야외활동 지수</div>
                    <p class="verdict-value">${card.index.level.label}</p>
                </div>
                <div class="verdict-body">
                    <p class="verdict-message"><c:out value="${card.index.message}" /></p>
                    <p class="verdict-reasons">
                        <c:choose>
                            <c:when test="${not empty card.index.reasons}">
                                <c:forEach var="r" items="${card.index.reasons}" varStatus="rs">
                                    <c:out value="${r}"/><c:if test="${not rs.last}"> · </c:if>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>감점 요인 없음</c:otherwise>
                        </c:choose>
                        <c:if test="${card.airAvailable and air.sidoAverage}"> · 미세먼지는 시·도 평균값</c:if>
                    </p>
                </div>
            </section>
        </c:when>
        <c:otherwise>
            <section class="verdict unknown">
                <div class="verdict-level">
                    <div class="verdict-label">야외활동 지수</div>
                    <p class="verdict-value">-</p>
                </div>
                <div class="verdict-body">
                    <p class="verdict-message">미세먼지와 날씨 정보를 모두 불러오지 못해 판정할 수 없어요.</p>
                </div>
            </section>
        </c:otherwise>
    </c:choose>

    <%-- ② 계산 과정: 미세먼지 등급 → 날씨 감점 → 최종 판정 --%>
    <c:if test="${not empty card.index}">
        <c:set var="wxOk" value="${card.weatherAvailable}" />
        <c:set var="hitRain" value="${wxOk and wx.precipitation.precipitating}" />
        <c:set var="hitCold" value="${wxOk and not empty wx.temperature and wx.temperature le OutdoorIndexPolicy.TEMP_COLD_MAX}" />
        <c:set var="hitHot" value="${wxOk and not empty wx.temperature and wx.temperature ge OutdoorIndexPolicy.TEMP_HOT_MIN}" />
        <c:set var="hitWind" value="${wxOk and not empty wx.windSpeed and wx.windSpeed ge OutdoorIndexPolicy.WIND_STRONG_MIN}" />
        <c:set var="penalty" value="${(hitRain ? 1 : 0) + (hitCold ? 1 : 0) + (hitHot ? 1 : 0) + (hitWind ? 1 : 0)}" />
        <c:set var="dust" value="${card.airAvailable ? air.representativeGrade : null}" />

        <section class="box">
            <h2 class="box-title">어떻게 계산했나요 <small>두 API의 값을 합쳐 판정</small></h2>

            <div class="calc">
                <div class="calc-step">
                    <h3><b>1</b>미세먼지 등급</h3>
                    <div class="calc-line">
                        <span class="k">PM10</span>
                        <span class="num">
                            <c:choose>
                                <c:when test="${card.airAvailable and not empty air.pm10}">${air.pm10} <span class="grade ${air.pm10Grade.cssClass}">${air.pm10Grade.label}</span></c:when>
                                <c:otherwise><span class="env-na">정보 없음</span></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="calc-line">
                        <span class="k">PM2.5</span>
                        <span class="num">
                            <c:choose>
                                <c:when test="${card.airAvailable and not empty air.pm25}">${air.pm25} <span class="grade ${air.pm25Grade.cssClass}">${air.pm25Grade.label}</span></c:when>
                                <c:otherwise><span class="env-na">정보 없음</span></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="calc-result">
                        <c:choose>
                            <c:when test="${card.index.airMissing}">정보 없음 → 날씨로만 판정</c:when>
                            <c:otherwise>더 나쁜 쪽 적용 → <span class="grade ${dust.cssClass}">${dust.label}</span></c:otherwise>
                        </c:choose>
                        <c:choose>
                            <c:when test="${air.filledFromAverage}"><div class="sub" style="margin-top:4px">빈 값은 <c:out value="${card.region.sido}"/> 평균으로 채움</div></c:when>
                        </c:choose>
                    </div>
                </div>

                <div class="calc-step">
                    <h3><b>2</b>날씨 감점</h3>
                    <c:choose>
                        <c:when test="${wxOk}">
                            <div class="calc-line ${hitRain ? 'hit' : 'off'}">
                                <span class="k">비 · 눈</span>
                                <span>${wx.precipitation.label}</span>
                            </div>
                            <div class="calc-line ${hitCold ? 'hit' : 'off'}">
                                <span class="k">기온 <fmt:formatNumber value="${OutdoorIndexPolicy.TEMP_COLD_MAX}" pattern="0.#"/>℃ 이하</span>
                                <span class="num"><fmt:formatNumber value="${wx.temperature}" maxFractionDigits="1"/>℃</span>
                            </div>
                            <div class="calc-line ${hitHot ? 'hit' : 'off'}">
                                <span class="k">기온 <fmt:formatNumber value="${OutdoorIndexPolicy.TEMP_HOT_MIN}" pattern="0.#"/>℃ 이상</span>
                                <span class="num"><fmt:formatNumber value="${wx.temperature}" maxFractionDigits="1"/>℃</span>
                            </div>
                            <div class="calc-line ${hitWind ? 'hit' : 'off'}">
                                <span class="k">바람 <fmt:formatNumber value="${OutdoorIndexPolicy.WIND_STRONG_MIN}" pattern="0.#"/>m/s 이상</span>
                                <span class="num"><fmt:formatNumber value="${wx.windSpeed}" maxFractionDigits="1"/>m/s</span>
                            </div>
                            <div class="calc-result">감점 ${penalty}개</div>
                        </c:when>
                        <c:otherwise>
                            <div class="calc-line off"><span class="k">날씨 정보 없음</span></div>
                            <div class="calc-result">감점 0개로 계산</div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="calc-step">
                    <h3><b>3</b>최종 판정</h3>
                    <div class="rule ${card.index.level eq 'BAD' ? 'match' : ''}">
                        <span class="grade very-bad">나쁨</span>
                        <span>미세먼지 매우나쁨 또는 감점 ${OutdoorIndexPolicy.PENALTY_BAD_COUNT}개 이상</span>
                    </div>
                    <div class="rule ${card.index.level eq 'CAUTION' ? 'match' : ''}">
                        <span class="grade caution">주의</span>
                        <span>미세먼지 나쁨 또는 감점 1개</span>
                    </div>
                    <div class="rule ${card.index.level eq 'GOOD' ? 'match' : ''}">
                        <span class="grade good">좋음</span>
                        <span>미세먼지 보통 이하이고 감점 없음</span>
                    </div>
                </div>
            </div>

            <div class="flow" aria-label="두 API 연결 과정">
                <span>에어코리아 측정소
                    <b><c:choose>
                        <c:when test="${card.airAvailable and not air.sidoAverage}"><c:out value="${air.stationName}"/></c:when>
                        <c:when test="${card.airAvailable}">시·도 평균</c:when>
                        <c:otherwise>조회 실패</c:otherwise>
                    </c:choose></b></span>
                <span class="flow-sep">→</span>
                <span>기상청 격자 <b class="num">(${card.region.nx}, ${card.region.ny})</b></span>
                <span class="flow-sep">→</span>
                <span>기상청 날씨</span>
                <a class="text-link" style="margin-left:auto" href="${ctx}/guide.jsp">판정 기준 자세히</a>
            </div>
        </section>
    </c:if>

    <%-- ③ 현재 값 --%>
    <div class="env-grid" style="margin-top:16px">
        <section class="box">
            <h2 class="box-title">미세먼지 <small>에어코리아</small></h2>
            <c:choose>
                <c:when test="${not empty card.airError}">
                    <p class="error-message"><c:out value="${card.airError.message}" /></p>
                </c:when>
                <c:otherwise>
                    <div class="env-row">
                        <span class="env-label">PM10</span>
                        <c:choose>
                            <c:when test="${empty air.pm10}"><span class="env-na">정보 없음</span></c:when>
                            <c:otherwise>
                                <span class="env-value">${air.pm10}<small>㎍/㎥</small></span>
                                <span class="grade ${air.pm10Grade.cssClass}">${air.pm10Grade.label}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="env-row">
                        <span class="env-label">PM2.5</span>
                        <c:choose>
                            <c:when test="${empty air.pm25}"><span class="env-na">정보 없음</span></c:when>
                            <c:otherwise>
                                <span class="env-value">${air.pm25}<small>㎍/㎥</small></span>
                                <span class="grade ${air.pm25Grade.cssClass}">${air.pm25Grade.label}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <p class="hint">
                        <c:choose>
                            <c:when test="${air.sidoAverage}"><c:out value="${card.region.sido}"/> 측정소 ${card.stationNames.size()}곳 평균</c:when>
                            <c:when test="${air.filledFromAverage}">측정소: <c:out value="${air.stationName}"/> · <c:out value="${empty air.statusNote ? '측정값 없음' : air.statusNote}"/> → <c:out value="${card.region.sido}"/> 평균값 사용</c:when>
                            <c:otherwise>측정소: <c:out value="${air.stationName}"/></c:otherwise>
                        </c:choose>
                    </p>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="box">
            <h2 class="box-title">날씨 <small>기상청 초단기실황</small></h2>
            <c:choose>
                <c:when test="${not empty card.weatherError}">
                    <p class="error-message"><c:out value="${card.weatherError.message}" /></p>
                </c:when>
                <c:otherwise>
                    <div class="env-row">
                        <span class="env-label">하늘</span>
                        <span class="env-value text">${wx.sky.label}</span>
                    </div>
                    <div class="env-row">
                        <span class="env-label">기온</span>
                        <c:choose>
                            <c:when test="${empty wx.temperature}"><span class="env-na">정보 없음</span></c:when>
                            <c:otherwise><span class="env-value"><fmt:formatNumber value="${wx.temperature}" maxFractionDigits="1"/><small>℃</small></span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="env-row">
                        <span class="env-label">습도</span>
                        <c:choose>
                            <c:when test="${empty wx.humidity}"><span class="env-na">정보 없음</span></c:when>
                            <c:otherwise><span class="env-value">${wx.humidity}<small>%</small></span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="env-row">
                        <span class="env-label">강수</span>
                        <span class="env-value text">${wx.precipitation.label}</span>
                    </div>
                    <div class="env-row">
                        <span class="env-label">풍속</span>
                        <c:choose>
                            <c:when test="${empty wx.windSpeed}"><span class="env-na">정보 없음</span></c:when>
                            <c:otherwise><span class="env-value"><fmt:formatNumber value="${wx.windSpeed}" maxFractionDigits="1"/><small>m/s</small></span></c:otherwise>
                        </c:choose>
                    </div>
                    <c:if test="${card.airAvailable and not air.sidoAverage and not card.region.hasStation}">
                        <p class="hint">측정소 위치 표에 없는 측정소라 시·도 대표 지점의 날씨예요.</p>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <%-- ④ 시간대별 예보 --%>
    <c:if test="${card.hasHourly}">
        <section class="box">
            <h2 class="box-title">시간대별 예보 <small>앞으로 ${card.hourlyForecasts.size()}시간 · 기상청 단기예보</small></h2>
            <c:if test="${not empty card.hourlySummary}">
                <p class="hourly-summary"><c:out value="${card.hourlySummary}" /></p>
            </c:if>
            <ol class="hourly-list">
                <c:forEach var="h" items="${card.hourlyForecasts}" varStatus="st">
                    <li class="hour ${not empty h.level ? h.level.cssClass : ''}" title="${not empty h.level ? h.level.label : ''}">
                        <span class="hour-day"><c:if test="${st.first or h.fcstTime eq '0000'}">${h.dayLabel}</c:if>&nbsp;</span>
                        <span class="hour-time">${h.hourLabel}</span>
                        <span class="hour-icon" aria-hidden="true"><c:choose>
                            <c:when test="${h.precipitation eq 'SNOW' or h.precipitation eq 'SNOW_FLURRY'}">&#10052;</c:when>
                            <c:when test="${h.precipitation.precipitating}">&#9730;</c:when>
                            <c:when test="${h.sky eq 'CLEAR'}">&#9728;</c:when>
                            <c:when test="${h.sky eq 'PARTLY_CLOUDY'}">&#9925;</c:when>
                            <c:when test="${h.sky eq 'CLOUDY'}">&#9729;</c:when>
                            <c:otherwise>·</c:otherwise>
                        </c:choose></span>
                        <span class="hour-temp num"><fmt:formatNumber value="${h.temperature}" maxFractionDigits="0"/>°</span>
                        <span class="hour-pop num">강수 ${empty h.rainProbability ? '-' : h.rainProbability}%</span>
                    </li>
                </c:forEach>
            </ol>
            <p class="hint">아래 색은 시간별 지수(초록 좋음 · 노랑 주의 · 빨강 나쁨)예요. 예보된 날씨와 현재 미세먼지로 판정했어요.</p>
        </section>
    </c:if>

    <%-- ⑤ 같은 시·도 다른 측정소 (API ① 결과가 다음 검색으로 이어짐) --%>
    <c:if test="${not empty card.stationNames}">
        <section class="box">
            <h2 class="box-title"><c:out value="${card.region.sido}"/> 다른 측정소 <small>${card.stationNames.size()}곳</small></h2>
            <div>
                <c:url var="avgUrl" value="/search"><c:param name="sido" value="${card.region.sido}"/></c:url>
                <a class="chip ${card.airAvailable and air.sidoAverage ? 'active' : ''}" href="${avgUrl}">시·도 평균</a>
                <c:forEach var="stn" items="${card.stationNames}" end="11">
                    <c:url var="stUrl" value="/search"><c:param name="sido" value="${card.region.sido}"/><c:param name="station" value="${stn}"/></c:url>
                    <a class="chip ${card.airAvailable and stn eq air.stationName ? 'active' : ''}" href="${stUrl}"><c:out value="${stn}"/></a>
                </c:forEach>
            </div>
            <c:if test="${card.stationNames.size() > 12}">
                <details class="more-stations">
                    <summary>${card.stationNames.size() - 12}곳 더 보기</summary>
                    <div>
                        <c:forEach var="stn" items="${card.stationNames}" begin="12">
                            <c:url var="stUrl2" value="/search"><c:param name="sido" value="${card.region.sido}"/><c:param name="station" value="${stn}"/></c:url>
                            <a class="chip ${card.airAvailable and stn eq air.stationName ? 'active' : ''}" href="${stUrl2}"><c:out value="${stn}"/></a>
                        </c:forEach>
                    </div>
                </details>
            </c:if>
        </section>
    </c:if>

    <p class="back-link"><a class="text-link" href="${ctx}/search">← 다른 지역 검색</a></p>

</main>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

</body>
</html>
