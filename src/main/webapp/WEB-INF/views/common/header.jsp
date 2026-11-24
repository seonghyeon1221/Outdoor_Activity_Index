<%-- 공통 헤더 (담당: D)
     - 각 화면 body 맨 위에 jsp:include, 현재 메뉴는 param nav 로 표시 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<header class="site-header">
    <div class="header-inner">
        <a class="site-logo" href="${ctx}/search">
            <span class="logo-mark" aria-hidden="true"></span>
            야외활동 지수
        </a>

        <nav class="site-nav">
            <a href="${ctx}/search" class="${param.nav eq 'search' ? 'current' : ''}">검색</a>
            <a href="${ctx}/compare" class="${param.nav eq 'compare' ? 'current' : ''}">지역 비교</a>
            <a href="${ctx}/favorites" class="${param.nav eq 'favorites' ? 'current' : ''}">즐겨찾기</a>
            <a href="${ctx}/guide.jsp" class="${param.nav eq 'guide' ? 'current' : ''}">판정 기준</a>
            <c:if test="${applicationScope.demoMode eq 'replay'}">
                <span class="demo-badge"
                      title="인터넷 없이 저장된 데이터로 보여주는 중">시연 모드</span>
            </c:if>
        </nav>
    </div>
</header>
