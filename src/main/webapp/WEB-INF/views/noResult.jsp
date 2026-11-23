<%-- 검색 결과 없음 화면 (담당: D)
     - 시·도가 목록에 없거나, 에어코리아가 돌려준 측정소가 0건일 때 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="/WEB-INF/views/common/header.jsp"/>

<section class="panel empty">
    <div class="empty-icon" aria-hidden="true">🔍</div>
    <h2>검색 결과가 없습니다</h2>
    <p>
        <c:if test="${not empty sido}">
            '<c:out value="${sido}"/><c:if test="${not empty station}"> <c:out value="${station}"/></c:if>'에 대한
        </c:if>
        측정 정보를 찾지 못했어요.<br>시·도를 다시 선택하거나, 측정소 이름을 비우고 검색해 보세요.
    </p>
    <a class="btn-primary" href="${pageContext.request.contextPath}/search">다시 검색하기</a>
</section>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>
