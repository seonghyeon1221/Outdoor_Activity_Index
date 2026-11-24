<%-- 검색 결과 없음 화면 (담당: D)
     - 시·도가 목록에 없거나, 에어코리아가 돌려준 측정소가 0건일 때 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>검색 결과 없음 | 야외활동 지수</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=3">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="nav" value="search" />
</jsp:include>

<main class="main-container">

    <section class="empty-state">
        <h1>검색 결과를 찾을 수 없습니다</h1>
        <p>
            <c:if test="${not empty sido or not empty station}">
                '<c:out value="${empty station ? sido : sido.concat(' ').concat(station)}" />'의
            </c:if>
            측정 정보를 찾지 못했어요.<br>
            시·도를 다시 고르거나, 시·군·구를 비우고 검색해 보세요.
        </p>
        <c:if test="${not empty errorMessage}">
            <p class="error-message"><c:out value="${errorMessage}" /></p>
        </c:if>
        <a class="btn" href="${pageContext.request.contextPath}/search">다시 검색하기</a>
    </section>

</main>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

</body>
</html>
