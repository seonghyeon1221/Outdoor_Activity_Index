<%-- 오류 안내 화면 (담당: D)
     - 컨트롤러가 넘겨준 errorCode(ErrorCode)의 안내 문구 표시
     - web.xml의 error-page(404, 500 등)로 들어온 경우 errorCode가 없으므로 기본 문구 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp"/>

<section class="panel empty">
    <div class="empty-icon" aria-hidden="true">⚠</div>
    <h2>요청을 처리하지 못했어요</h2>
    <p>
        <c:choose>
            <c:when test="${not empty errorCode}"><c:out value="${errorCode.message}"/></c:when>
            <c:when test="${requestScope['jakarta.servlet.error.status_code'] eq 404}">찾는 페이지가 없어요.</c:when>
            <c:otherwise>알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.</c:otherwise>
        </c:choose>
    </p>
    <a class="btn-primary" href="${pageContext.request.contextPath}/search">검색 화면으로</a>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>
