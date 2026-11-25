<%-- 결과 카드: 지수 → 계산 과정 → 현재 값 → 시간대별 예보 → 다른 측정소 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.outdoorindex.service.IndexPolicy" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- 자주 쓰는 값을 짧은 이름으로 --%>
<c:set var="air" value="${card.air}" />
<c:set var="wx" value="${card.weather}" />
<c:set var="r" value="${card.result}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title><c:out value="${card.place()}"/> | 야외활동 지수</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"><jsp:param name="nav" value="search" /></jsp:include>
<main class="wrap">

    <div class="head">
        <div>
            <h1><c:out value="${card.place()}"/></h1>
            <p class="sub">
                <c:if test="${not empty air.dataTime}">미세먼지 ${air.dataTime}</c:if>
                <c:if test="${not empty wx}"> · 날씨 ${wx.baseTime} 관측</c:if>
            </p>
        </div>
        <div>
            <%-- 즐겨찾기 버튼: DB 를 쓸 수 있을 때만, POST 후 이 화면으로 돌아옴 --%>
            <c:if test="${empty dbError}">
                <c:url var="back" value="/search"><c:param name="sido" value="${card.sido}"/><c:param name="station" value="${card.station()}"/></c:url>
                <form method="post" action="${ctx}/favorites" class="inline">
                    <input type="hidden" name="action" value="${isFavorite ? 'remove' : 'add'}">
                    <input type="hidden" name="sido" value="${card.sido}">
                    <input type="hidden" name="station" value="<c:out value='${card.station()}'/>">
                    <input type="hidden" name="back" value="<c:out value='${back.substring(ctx.length())}'/>">
                    <button class="btn-line">${isFavorite ? '★ 즐겨찾기됨' : '☆ 즐겨찾기'}</button>
                </form>
            </c:if>
            <c:url var="cmp" value="/compare"><c:param name="sido1" value="${card.sido}"/><c:param name="station1" value="${card.station()}"/></c:url>
            <a class="btn-line" href="${cmp}">다른 지역과 비교</a>
        </div>
    </div>

    <%-- 안내: 측정소를 못 찾았거나, 빈 값을 시·도 평균으로 채운 경우 --%>
    <c:if test="${not empty station and air.average}">
        <p class="notice">'<c:out value="${station}"/>' 측정소를 찾지 못해 ${card.sido} 전체 평균으로 보여드려요. 아래 목록에서 골라 보세요.</p>
    </c:if>
    <c:if test="${air.filled}">
        <p class="notice">'<c:out value="${air.station}"/>' 측정소가 지금 <b>${empty air.note ? '측정값 없음' : air.note}</b> 상태라 빈 값은 ${card.sido} 평균으로 채웠어요. (날씨는 이 측정소 위치 기준)</p>
    </c:if>

    <%-- ① 야외활동 지수 --%>
    <section class="verdict ${empty r ? 'none' : r.level.css}">
        <div class="level"><small>야외활동 지수</small><b>${empty r ? '-' : r.level.label}</b></div>
        <div>
            <p class="msg">${empty r ? '미세먼지와 날씨를 모두 불러오지 못해 판정할 수 없어요.' : r.level.message}</p>
            <c:if test="${not empty r}">
                <p class="sub">${empty r.reasons ? '감점 요인 없음' : String.join(' · ', r.reasons)}</p>
            </c:if>
        </div>
    </section>

    <%-- ② 계산 과정 (기준값은 IndexPolicy 상수를 그대로 표시) --%>
    <c:if test="${not empty r}">
    <section class="box">
        <h2>어떻게 계산했나요 <small>두 API의 값을 합쳐 판정</small></h2>
        <div class="calc">
            <div>
                <h3>1. 미세먼지 등급</h3>
                <p class="line"><span>PM10</span><span>${empty air.pm10 ? '정보 없음' : air.pm10} <i class="grade ${air.pm10Grade().css}">${air.pm10Grade().label}</i></span></p>
                <p class="line"><span>PM2.5</span><span>${empty air.pm25 ? '정보 없음' : air.pm25} <i class="grade ${air.pm25Grade().css}">${air.pm25Grade().label}</i></span></p>
                <p class="total">${r.dust eq 'NONE' ? '정보 없음 → 날씨로만 판정' : '더 나쁜 쪽 적용 → '}
                    <c:if test="${r.dust ne 'NONE'}"><i class="grade ${r.dust.css}">${r.dust.label}</i></c:if></p>
            </div>
            <div>
                <h3>2. 날씨 감점</h3>
                <p class="line ${r.rain ? 'hit' : ''}"><span>비 · 눈</span><span>${empty wx ? '-' : wx.rain}</span></p>
                <p class="line ${r.cold ? 'hit' : ''}"><span>기온 ${IndexPolicy.COLD}℃ 이하</span><span>${IndexPolicy.num(wx.temp)}℃</span></p>
                <p class="line ${r.hot ? 'hit' : ''}"><span>기온 ${IndexPolicy.HOT}℃ 이상</span><span>${IndexPolicy.num(wx.temp)}℃</span></p>
                <p class="line ${r.windy ? 'hit' : ''}"><span>바람 ${IndexPolicy.WIND}m/s 이상</span><span>${IndexPolicy.num(wx.wind)}m/s</span></p>
                <p class="total">감점 ${r.penalty}개${empty wx ? ' (날씨 정보 없음)' : ''}</p>
            </div>
            <div>
                <h3>3. 최종 판정</h3>
                <p class="rule ${r.level eq 'BAD' ? 'match' : ''}"><i class="grade very-bad">나쁨</i> 미세먼지 매우나쁨 또는 감점 ${IndexPolicy.PENALTY_BAD}개 이상</p>
                <p class="rule ${r.level eq 'CAUTION' ? 'match' : ''}"><i class="grade bad">주의</i> 미세먼지 나쁨 또는 감점 1개</p>
                <p class="rule ${r.level eq 'GOOD' ? 'match' : ''}"><i class="grade good">좋음</i> 미세먼지 보통 이하, 감점 없음</p>
            </div>
        </div>
        <p class="flow">에어코리아 측정소 <b>${empty air ? '조회 실패' : air.average ? '시·도 평균' : air.station}</b> →
            기상청 격자 <b>(${card.nx}, ${card.ny})</b> → 기상청 날씨
            <a href="${ctx}/guide">판정 기준 자세히</a></p>
    </section>
    </c:if>

    <%-- ③ 현재 값 --%>
    <div class="two">
        <section class="box">
            <h2>미세먼지 <small>에어코리아</small></h2>
            <c:choose>
                <c:when test="${empty air}"><p class="error">${card.airError}</p></c:when>
                <c:otherwise>
                    <p class="row"><span>PM10</span><b>${empty air.pm10 ? '-' : air.pm10}<small> ㎍/㎥</small></b><i class="grade ${air.pm10Grade().css}">${air.pm10Grade().label}</i></p>
                    <p class="row"><span>PM2.5</span><b>${empty air.pm25 ? '-' : air.pm25}<small> ㎍/㎥</small></b><i class="grade ${air.pm25Grade().css}">${air.pm25Grade().label}</i></p>
                    <p class="sub"><c:out value="${air.average ? card.sido += ' 측정소 ' += air.count += '곳 평균' : '측정소: ' += air.station}"/></p>
                </c:otherwise>
            </c:choose>
        </section>
        <section class="box">
            <h2>날씨 <small>기상청 초단기실황</small></h2>
            <c:choose>
                <c:when test="${empty wx}"><p class="error">${card.weatherError}</p></c:when>
                <c:otherwise>
                    <p class="row"><span>하늘</span><b>${wx.sky}</b></p>
                    <p class="row"><span>기온</span><b>${IndexPolicy.num(wx.temp)}<small> ℃</small></b></p>
                    <p class="row"><span>습도</span><b>${empty wx.humidity ? '-' : wx.humidity}<small> %</small></b></p>
                    <p class="row"><span>강수</span><b>${wx.rain}</b></p>
                    <p class="row"><span>풍속</span><b>${IndexPolicy.num(wx.wind)}<small> m/s</small></b></p>
                    <c:if test="${not empty air and not air.average and not card.gridFound}"><p class="sub">위치 표에 없는 측정소라 시·도 대표 지점의 날씨예요.</p></c:if>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <%-- ④ 시간대별 예보 (시간별 지수 = 예보 날씨 + 현재 미세먼지) --%>
    <c:if test="${not empty card.hourly}">
    <section class="box">
        <h2>시간대별 예보 <small>앞으로 ${card.hourly.size()}시간 · 기상청 단기예보</small></h2>
        <p class="msg">${card.summary}</p>
        <ol class="hours">
            <c:forEach var="h" items="${card.hourly}" varStatus="st">
                <li class="${h.level.css}" title="${h.level.label}">
                    <small>${st.first or h.hour eq '0시' ? h.day : '&nbsp;'}</small>${h.hour}
                    <span class="icon">${h.icon()}</span><b>${IndexPolicy.num(h.temp)}°</b>
                    <small>강수 ${empty h.pop ? '-' : h.pop}%</small>
                </li>
            </c:forEach>
        </ol>
        <p class="sub">아래 색: 초록 좋음 · 노랑 주의 · 빨강 나쁨</p>
    </section>
    </c:if>

    <%-- ⑤ 같은 시·도의 다른 측정소 (API ① 결과가 다음 검색으로 이어짐) --%>
    <c:if test="${not empty card.stations}">
    <section class="box">
        <h2>${card.sido} 다른 측정소 <small>${card.stations.size()}곳</small></h2>
        <p class="chips">
            <c:url var="u" value="/search"><c:param name="sido" value="${card.sido}"/></c:url>
            <a class="chip ${air.average ? 'on' : ''}" href="${u}">시·도 평균</a>
            <c:forEach var="s" items="${card.stations}">
                <c:url var="u" value="/search"><c:param name="sido" value="${card.sido}"/><c:param name="station" value="${s}"/></c:url>
                <a class="chip ${s eq air.station ? 'on' : ''}" href="${u}"><c:out value="${s}"/></a>
            </c:forEach>
        </p>
    </section>
    </c:if>
</main>
</body>
</html>
