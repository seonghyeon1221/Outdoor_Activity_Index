<%-- 즐겨찾기 목록 (MySQL favorite 테이블) --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>즐겨찾기 | 야외활동 지수</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<jsp:include page="header.jsp"><jsp:param name="nav" value="favorites" /></jsp:include>
<main class="wrap narrow">
    <h1>즐겨찾기</h1>
    <c:if test="${not empty dbError}"><p class="notice"><c:out value="${dbError}"/></p></c:if>

    <%-- 즐겨찾기 목록: 이름을 누르면 결과 화면, 삭제는 POST --%>
    <section class="box">
        <c:if test="${empty favorites}"><p class="sub">아직 없어요. 결과 화면의 ☆ 즐겨찾기 버튼으로 추가하세요.</p></c:if>
        <c:forEach var="f" items="${favorites}">
            <div class="row">
                <c:url var="u" value="/search"><c:param name="sido" value="${f.sido}"/><c:param name="station" value="${f.station}"/></c:url>
                <a class="link" href="${u}"><c:out value="${f.name()}"/></a>
                <form method="post" action="${ctx}/favorites" class="inline">
                    <input type="hidden" name="action" value="remove">
                    <input type="hidden" name="sido" value="${f.sido}">
                    <input type="hidden" name="station" value="<c:out value='${f.station}'/>">
                    <button class="btn-line">삭제</button>
                </form>
            </div>
        </c:forEach>
    </section>

    <%-- 2곳 이상이면 최근 3곳 비교 링크 --%>
    <c:if test="${favorites.size() >= 2}">
        <c:url var="cmp" value="/compare">
            <c:forEach var="f" items="${favorites}" end="2" varStatus="st">
                <c:param name="sido${st.count}" value="${f.sido}"/><c:param name="station${st.count}" value="${f.station}"/>
            </c:forEach>
        </c:url>
        <a class="btn-line" href="${cmp}">최근 3곳 비교하기</a>
    </c:if>
</main>
</body>
</html>
