<%-- 오류 안내 화면 (담당: D)
     - 컨트롤러가 넘겨준 errorCode(ErrorCode)의 안내 문구 표시
     - web.xml의 error-page(404, 500 등)로 들어온 경우 errorCode가 없으므로 기본 문구 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>오류 안내 | 야외활동 지수</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=3">
</head>
<body>

<jsp:include page="/WEB-INF/views/common/header.jsp">
    <jsp:param name="nav" value="" />
</jsp:include>

<main class="main-container">

    <section class="empty-state">
        <h1>요청을 처리하지 못했어요</h1>
        <p class="error-message">
            <c:choose>
                <c:when test="${not empty errorCode}"><c:out value="${errorCode.message}" /></c:when>
                <c:when test="${requestScope['jakarta.servlet.error.status_code'] eq 404}">찾는 페이지가 없어요.</c:when>
                <c:otherwise>알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.</c:otherwise>
            </c:choose>
        </p>
        <a class="btn" href="${pageContext.request.contextPath}/search">검색 화면으로</a>
    </section>

</main>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />

</body>
</html>
