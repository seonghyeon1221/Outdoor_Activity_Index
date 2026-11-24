<%-- 즐겨찾기 · 최근 검색 화면 (담당: D)
     - 컨트롤러가 넘겨주는 값: favorites, recentSearches, dbAvailable, dbDescription, dbError, maxFavorites
     - 데이터는 MySQL(outdoor_index DB)에 저장. MySQL 설정이 없으면 내장 H2 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>즐겨찾기 | 야외활동 지수</title>
    <link rel="stylesheet" href="${ctx}/css/style.css?v=3">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="nav" value="favorites" />
</jsp:include>

<main class="main-container">

    <section class="page-head">
        <h1>즐겨찾기 · 최근 검색</h1>
        <p>자주 보는 지역을 저장해 두고 바로 확인하거나 한 번에 비교할 수 있어요.</p>
    </section>

    <p class="db-status ${dbAvailable ? 'ok' : 'fail'}">
        저장소 <c:out value="${dbDescription}"/> · <b>${dbAvailable ? '연결됨' : '연결 안 됨'}</b>
    </p>

    <c:choose>
    <c:when test="${not dbAvailable}">
        <section class="empty-state" style="margin-top:0">
            <h1>DB에 연결할 수 없어요</h1>
            <c:if test="${not empty dbError}">
                <p class="error-message"><c:out value="${dbError}"/></p>
            </c:if>
            <p>
                MySQL이 켜져 있는지, <code>.env</code>의 OUTDOOR_DB_URL · OUTDOOR_DB_USER ·
                OUTDOOR_DB_PASSWORD가 맞는지 확인해 주세요.
                검색과 비교는 DB 없이도 쓸 수 있어요.
            </p>
            <form method="post" action="${ctx}/favorites">
                <input type="hidden" name="action" value="retryDb">
                <input type="hidden" name="back" value="/favorites">
                <button type="submit" class="btn">다시 연결</button>
            </form>
        </section>
    </c:when>
    <c:otherwise>
        <section class="box">
            <div class="list-head">
                <h2 class="box-title">즐겨찾기 <small>${favorites.size()} / ${maxFavorites}</small></h2>
                <c:if test="${favorites.size() >= 2}">
                    <c:url var="favCmp" value="/compare">
                        <c:forEach var="f" items="${favorites}" end="2" varStatus="fs">
                            <c:param name="sido${fs.count}" value="${f.sido}"/>
                            <c:param name="station${fs.count}" value="${f.station}"/>
                        </c:forEach>
                    </c:url>
                    <a class="btn-line" href="${favCmp}">상위 3곳 비교</a>
                </c:if>
            </div>
            <c:choose>
                <c:when test="${empty favorites}">
                    <p class="hint">아직 즐겨찾기가 없어요. 결과 화면의 &#9734; 즐겨찾기 버튼으로 추가할 수 있어요.</p>
                </c:when>
                <c:otherwise>
                    <ul class="item-list">
                        <c:forEach var="f" items="${favorites}">
                            <li>
                                <c:url var="fu" value="/search"><c:param name="sido" value="${f.sido}"/><c:param name="station" value="${f.station}"/></c:url>
                                <a href="${fu}"><c:out value="${f.displayName}"/></a>
                                <span class="when">${f.createdAt}</span>
                                <form method="post" action="${ctx}/favorites">
                                    <input type="hidden" name="action" value="remove">
                                    <input type="hidden" name="sido" value="<c:out value='${f.sido}'/>">
                                    <input type="hidden" name="station" value="<c:out value='${f.station}'/>">
                                    <input type="hidden" name="back" value="/favorites">
                                    <button type="submit" class="btn-text">삭제</button>
                                </form>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="box">
            <div class="list-head">
                <h2 class="box-title">최근 검색</h2>
                <c:if test="${not empty recentSearches}">
                    <form method="post" action="${ctx}/favorites">
                        <input type="hidden" name="action" value="clearHistory">
                        <input type="hidden" name="back" value="/favorites">
                        <button type="submit" class="btn-text">기록 지우기</button>
                    </form>
                </c:if>
            </div>
            <c:choose>
                <c:when test="${empty recentSearches}">
                    <p class="hint">검색 기록이 없어요.</p>
                </c:when>
                <c:otherwise>
                    <ul class="item-list">
                        <c:forEach var="h" items="${recentSearches}">
                            <li>
                                <c:url var="hu" value="/search"><c:param name="sido" value="${h.sido}"/><c:param name="station" value="${h.station}"/></c:url>
                                <a href="${hu}"><c:out value="${h.displayName}"/></a>
                                <c:if test="${not empty h.level}"><span class="grade ${h.level.cssClass}">${h.level.label}</span></c:if>
                                <span class="when">${h.searchedAt}</span>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </c:otherwise>
    </c:choose>

</main>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

</body>
</html>
