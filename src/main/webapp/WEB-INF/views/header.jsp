<%-- 공통 머리말: 각 화면이 jsp:include 하고, 현재 메뉴는 param.nav 로 표시 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<header class="top">
    <div class="top-in">
        <a class="logo" href="${ctx}/search"><span class="dot"></span>야외활동 지수</a>
        <%-- 현재 화면(param.nav)과 같은 메뉴에 on 클래스 --%>
        <nav>
            <a href="${ctx}/search" class="${param.nav eq 'search' ? 'on' : ''}">검색</a>
            <a href="${ctx}/compare" class="${param.nav eq 'compare' ? 'on' : ''}">지역 비교</a>
            <a href="${ctx}/favorites" class="${param.nav eq 'favorites' ? 'on' : ''}">즐겨찾기</a>
            <a href="${ctx}/guide" class="${param.nav eq 'guide' ? 'on' : ''}">판정 기준</a>
        </nav>
    </div>
</header>
