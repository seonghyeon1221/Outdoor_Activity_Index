<%-- 오류 화면: 지원하지 않는 시·도, 없는 주소(404), 처리 중 오류(500) --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>안내 | 야외활동 지수</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<main class="wrap narrow">
    <section class="box center">
        <%-- 404 와 그 밖의 오류를 다른 제목으로 --%>
        <h1>${pageContext.errorData.statusCode eq 404 ? '페이지를 찾을 수 없어요' : '결과를 보여드릴 수 없어요'}</h1>
        <p class="sub"><c:out value="${not empty message ? message : pageContext.errorData.statusCode eq 404 ? '주소를 다시 확인해 주세요.' : '잠시 후 다시 시도해 주세요.'}"/></p>
        <a class="btn" href="${pageContext.request.contextPath}/search">검색 화면으로</a>
    </section>
</main>
</body>
</html>
