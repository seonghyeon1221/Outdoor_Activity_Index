<%-- 검색 화면 (담당: D)
     - 시·도(필수) + 시·군·구/측정소(선택) 입력
     - 컨트롤러가 넘겨주는 값: sido, station, errorMessage, favorites, recentSearches --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>지역 검색 | 야외활동 지수</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=3">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="nav" value="search" />
</jsp:include>

<main class="main-container">
    <div class="search-wrap">
        <h1 class="search-title">오늘, 밖에 나가도 괜찮을까?</h1>
        <p class="search-lead">
            미세먼지와 날씨를 함께 보고 야외활동 지수를
            좋음 · 주의 · 나쁨으로 알려드립니다.
        </p>

        <form class="search-card"
              action="${pageContext.request.contextPath}/search"
              method="get">

            <div class="field">
                <label for="sido">시·도</label>
                <select id="sido" name="sido" required>
                    <option value="">시·도를 선택하세요</option>
                    <c:forTokens var="opt" delims=","
                                 items="서울:서울특별시,부산:부산광역시,대구:대구광역시,인천:인천광역시,광주:광주광역시,대전:대전광역시,울산:울산광역시,세종:세종특별자치시,경기:경기도,강원:강원특별자치도,충북:충청북도,충남:충청남도,전북:전북특별자치도,전남:전라남도,경북:경상북도,경남:경상남도,제주:제주특별자치도">
                        <c:set var="optValue" value="${fn:substringBefore(opt, ':')}" />
                        <option value="${optValue}" ${optValue eq sido ? 'selected' : ''}>
                            ${fn:substringAfter(opt, ':')}
                        </option>
                    </c:forTokens>
                </select>
            </div>

            <div class="field">
                <label for="station">시·군·구 / 측정소 <span class="optional">선택</span></label>
                <input type="text" id="station" name="station" maxlength="20"
                       value="<c:out value='${station}'/>"
                       placeholder="예: 중구 — 비우면 시·도 평균">
            </div>

            <button type="submit" class="btn">야외활동 지수 확인하기</button>

            <c:if test="${not empty errorMessage}">
                <p class="form-error" role="alert"><c:out value="${errorMessage}" /></p>
            </c:if>
        </form>

        <div class="shortcut">
            <div class="shortcut-row">
                <span class="shortcut-label">자주 찾는 곳</span>
                <div class="chips">
                    <c:url var="q1" value="/search"><c:param name="sido" value="서울"/><c:param name="station" value="중구"/></c:url>
                    <c:url var="q2" value="/search"><c:param name="sido" value="서울"/><c:param name="station" value="강남구"/></c:url>
                    <c:url var="q3" value="/search"><c:param name="sido" value="부산"/></c:url>
                    <c:url var="q4" value="/search"><c:param name="sido" value="제주"/></c:url>
                    <a class="chip" href="${q1}">서울 중구</a>
                    <a class="chip" href="${q2}">서울 강남구</a>
                    <a class="chip" href="${q3}">부산</a>
                    <a class="chip" href="${q4}">제주</a>
                </div>
            </div>

            <c:if test="${not empty favorites}">
                <div class="shortcut-row">
                    <span class="shortcut-label">즐겨찾기</span>
                    <div class="chips">
                        <c:forEach var="f" items="${favorites}" end="7">
                            <c:url var="fu" value="/search"><c:param name="sido" value="${f.sido}"/><c:param name="station" value="${f.station}"/></c:url>
                            <a class="chip" href="${fu}"><c:out value="${f.displayName}"/></a>
                        </c:forEach>
                    </div>
                </div>
            </c:if>

            <c:if test="${not empty recentSearches}">
                <div class="shortcut-row">
                    <span class="shortcut-label">최근 검색</span>
                    <div class="chips">
                        <c:forEach var="h" items="${recentSearches}">
                            <c:url var="hu" value="/search"><c:param name="sido" value="${h.sido}"/><c:param name="station" value="${h.station}"/></c:url>
                            <a class="chip" href="${hu}">
                                <c:if test="${not empty h.level}"><span class="dot ${h.level.cssClass}" title="${h.level.label}"></span></c:if>
                                <c:out value="${h.displayName}"/>
                            </a>
                        </c:forEach>
                    </div>
                </div>
            </c:if>
        </div>

        <p class="search-foot">
            에어코리아 측정소의 위치로 기상청 날씨를 찾아 두 정보를 합쳐 판정해요.
            <a class="text-link" href="${pageContext.request.contextPath}/guide.jsp">판정 기준 보기</a>
        </p>
    </div>
</main>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

</body>
</html>
