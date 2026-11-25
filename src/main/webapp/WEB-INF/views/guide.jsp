<%-- 판정 기준 안내 (숫자는 IndexPolicy 상수를 그대로 읽음) --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.outdoorindex.service.IndexPolicy" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>판정 기준 | 야외활동 지수</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"><jsp:param name="nav" value="guide" /></jsp:include>
<main class="wrap narrow">
    <h1>야외활동 지수 판정 기준</h1>
    <p class="sub">미세먼지(에어코리아)와 날씨(기상청)를 합쳐 두 API 어디에도 없는 새 지표를 만듭니다.</p>

    <section class="box">
        <h2>계산 순서</h2>
        <ol>
            <li>에어코리아에서 시·도의 측정소별 PM10 · PM2.5를 받고 측정소(없으면 시·도 평균)를 고릅니다.</li>
            <li>측정소 위치를 기상청 격자 좌표(nx, ny)로 바꿔 그 지점의 현재 날씨를 받습니다.</li>
            <li>PM10과 PM2.5 중 <b>더 나쁜 등급</b>을 미세먼지 등급으로 씁니다.</li>
            <li>날씨 감점 개수를 세고, 미세먼지 등급과 함께 최종 지수를 정합니다.</li>
        </ol>
    </section>

    <section class="box">
        <%-- 등급표: PM10_GOOD 등 IndexPolicy 상수를 EL 로 바로 읽음 --%>
        <h2>① 미세먼지 등급 <small>환경부 기준, ㎍/㎥</small></h2>
        <table>
            <tr><th>등급</th><th>PM10</th><th>PM2.5</th></tr>
            <tr><td><i class="grade good">좋음</i></td><td>0 ~ ${IndexPolicy.PM10_GOOD}</td><td>0 ~ ${IndexPolicy.PM25_GOOD}</td></tr>
            <tr><td><i class="grade normal">보통</i></td><td>~ ${IndexPolicy.PM10_NORMAL}</td><td>~ ${IndexPolicy.PM25_NORMAL}</td></tr>
            <tr><td><i class="grade bad">나쁨</i></td><td>~ ${IndexPolicy.PM10_BAD}</td><td>~ ${IndexPolicy.PM25_BAD}</td></tr>
            <tr><td><i class="grade very-bad">매우나쁨</i></td><td>${IndexPolicy.PM10_BAD + 1} 이상</td><td>${IndexPolicy.PM25_BAD + 1} 이상</td></tr>
        </table>
    </section>

    <section class="box">
        <%-- 감점 기준: COLD, HOT, WIND 상수 --%>
        <h2>② 날씨 감점 <small>해당하면 1개씩</small></h2>
        <table>
            <tr><th>항목</th><th>조건</th><th>기상청 항목</th></tr>
            <tr><td>비 · 눈</td><td>강수가 있을 때</td><td>PTY</td></tr>
            <tr><td>추위</td><td>기온 ${IndexPolicy.COLD}℃ 이하</td><td>T1H</td></tr>
            <tr><td>더위</td><td>기온 ${IndexPolicy.HOT}℃ 이상</td><td>T1H</td></tr>
            <tr><td>강풍</td><td>풍속 ${IndexPolicy.WIND}m/s 이상</td><td>WSD</td></tr>
        </table>
    </section>

    <section class="box">
        <h2>③ 최종 판정 <small>위에서부터 먼저 맞는 것</small></h2>
        <table>
            <tr><td><i class="grade very-bad">나쁨</i></td><td>미세먼지 매우나쁨 <b>또는</b> 감점 ${IndexPolicy.PENALTY_BAD}개 이상</td></tr>
            <tr><td><i class="grade bad">주의</i></td><td>미세먼지 나쁨 <b>또는</b> 감점 1개</td></tr>
            <tr><td><i class="grade good">좋음</i></td><td>미세먼지 좋음 · 보통 <b>그리고</b> 감점 없음</td></tr>
        </table>
        <p class="sub">미세먼지를 못 받으면 날씨만으로, 날씨를 못 받으면 미세먼지만으로 판정해요.
            시간대별 지수는 예보 날씨와 <b>현재</b> 미세먼지로 같은 규칙을 적용해요.</p>
    </section>
</main>
</body>
</html>
