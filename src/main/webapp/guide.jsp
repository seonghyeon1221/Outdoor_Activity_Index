<%-- 판정 기준 안내 화면 (담당: D)
     - 표의 숫자는 OutdoorIndexPolicy 상수를 그대로 읽어 표시 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ page import="com.outdoorindex.policy.OutdoorIndexPolicy" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>판정 기준 | 야외활동 지수</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=3">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="nav" value="guide" />
</jsp:include>

<main class="main-container">

    <section class="page-head">
        <h1>야외활동 지수 판정 기준</h1>
        <p>미세먼지(에어코리아)와 날씨(기상청)를 합쳐, 두 API 어디에도 없는 새 지표를 만듭니다.</p>
    </section>

    <section class="box">
        <h2 class="box-title">계산 순서</h2>
        <ol class="steps">
            <li>에어코리아에서 시·도의 측정소별 PM10 · PM2.5를 받고, 입력한 측정소(없으면 시·도 평균)를 고릅니다.</li>
            <li>고른 측정소의 위치를 기상청 격자 좌표(nx, ny)로 바꿔 그 지점의 현재 날씨를 받습니다.</li>
            <li>PM10과 PM2.5를 각각 등급으로 바꾸고, 둘 중 <b>더 나쁜 등급</b>을 미세먼지 등급으로 씁니다.</li>
            <li>날씨에서 감점 항목이 몇 개인지 셉니다.</li>
            <li>미세먼지 등급과 감점 개수로 최종 지수를 정합니다.</li>
        </ol>
    </section>

    <section class="box">
        <h2 class="box-title">① 미세먼지 등급 <small>환경부 기준, 단위 ㎍/㎥</small></h2>
        <table class="table">
            <thead>
                <tr><th>등급</th><th>PM10 (미세먼지)</th><th>PM2.5 (초미세먼지)</th></tr>
            </thead>
            <tbody>
                <tr>
                    <td><span class="grade good">좋음</span></td>
                    <td class="num">0 ~ ${OutdoorIndexPolicy.PM10_GOOD_MAX}</td>
                    <td class="num">0 ~ ${OutdoorIndexPolicy.PM25_GOOD_MAX}</td>
                </tr>
                <tr>
                    <td><span class="grade normal">보통</span></td>
                    <td class="num">${OutdoorIndexPolicy.PM10_GOOD_MAX + 1} ~ ${OutdoorIndexPolicy.PM10_NORMAL_MAX}</td>
                    <td class="num">${OutdoorIndexPolicy.PM25_GOOD_MAX + 1} ~ ${OutdoorIndexPolicy.PM25_NORMAL_MAX}</td>
                </tr>
                <tr>
                    <td><span class="grade bad">나쁨</span></td>
                    <td class="num">${OutdoorIndexPolicy.PM10_NORMAL_MAX + 1} ~ ${OutdoorIndexPolicy.PM10_BAD_MAX}</td>
                    <td class="num">${OutdoorIndexPolicy.PM25_NORMAL_MAX + 1} ~ ${OutdoorIndexPolicy.PM25_BAD_MAX}</td>
                </tr>
                <tr>
                    <td><span class="grade very-bad">매우나쁨</span></td>
                    <td class="num">${OutdoorIndexPolicy.PM10_BAD_MAX + 1} 이상</td>
                    <td class="num">${OutdoorIndexPolicy.PM25_BAD_MAX + 1} 이상</td>
                </tr>
            </tbody>
        </table>
        <p class="hint">예) PM10 47(보통), PM2.5 40(나쁨) → 더 나쁜 쪽인 <b>나쁨</b>을 사용</p>
    </section>

    <section class="box">
        <h2 class="box-title">② 날씨 감점 <small>해당하면 1개씩</small></h2>
        <table class="table">
            <thead>
                <tr><th>항목</th><th>감점 조건</th><th>기상청 항목</th></tr>
            </thead>
            <tbody>
                <tr><td>비 · 눈</td><td>비, 비/눈, 눈, 소나기 등 강수가 있을 때</td><td><code>PTY</code></td></tr>
                <tr><td>추위</td><td>기온 <fmt:formatNumber value="${OutdoorIndexPolicy.TEMP_COLD_MAX}" pattern="0.#"/>℃ 이하</td><td><code>T1H</code></td></tr>
                <tr><td>더위</td><td>기온 <fmt:formatNumber value="${OutdoorIndexPolicy.TEMP_HOT_MIN}" pattern="0.#"/>℃ 이상</td><td><code>T1H</code></td></tr>
                <tr><td>강풍</td><td>풍속 <fmt:formatNumber value="${OutdoorIndexPolicy.WIND_STRONG_MIN}" pattern="0.#"/>m/s 이상</td><td><code>WSD</code></td></tr>
            </tbody>
        </table>
    </section>

    <section class="box">
        <h2 class="box-title">③ 최종 판정 <small>위에서부터 먼저 맞는 것 적용</small></h2>
        <table class="table">
            <thead>
                <tr><th>지수</th><th>조건</th><th>안내 문구</th></tr>
            </thead>
            <tbody>
                <tr>
                    <td><span class="grade very-bad">나쁨</span></td>
                    <td>미세먼지 매우나쁨 <b>또는</b> 날씨 감점 ${OutdoorIndexPolicy.PENALTY_BAD_COUNT}개 이상</td>
                    <td class="muted">실내 활동 추천</td>
                </tr>
                <tr>
                    <td><span class="grade caution">주의</span></td>
                    <td>미세먼지 나쁨 <b>또는</b> 날씨 감점 1개</td>
                    <td class="muted">마스크 · 준비물 챙기고 짧게</td>
                </tr>
                <tr>
                    <td><span class="grade good">좋음</span></td>
                    <td>미세먼지 좋음 · 보통 <b>그리고</b> 날씨 감점 없음</td>
                    <td class="muted">산책 · 운동하기 좋음</td>
                </tr>
            </tbody>
        </table>
    </section>

    <section class="box">
        <h2 class="box-title">정보가 빠졌을 때</h2>
        <ul class="steps">
            <li>미세먼지를 못 받으면 날씨 감점만으로 판정하고, 결과에 그 사실을 표시합니다.</li>
            <li>날씨를 못 받으면 감점 0개로 보고 미세먼지만으로 판정합니다.</li>
            <li>둘 다 못 받으면 판정하지 않습니다.</li>
            <li>시간대별 지수는 기상청 단기예보의 시간별 날씨와 <b>현재</b> 미세먼지 값으로 같은 규칙을 적용합니다.</li>
        </ul>
    </section>

</main>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

</body>
</html>
