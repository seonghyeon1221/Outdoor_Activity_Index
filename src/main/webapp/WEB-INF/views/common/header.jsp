<%-- 공통 머리글 (담당: D) — 모든 화면 맨 위에 jsp:include 로 포함 --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>야외활동 지수 · 오늘 밖에 나가도 될까?</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<header class="site-header">
    <div class="container header-inner">
        <a class="logo" href="${pageContext.request.contextPath}/search">
            <span class="logo-mark" aria-hidden="true">☀</span>
            <span>야외활동 지수</span>
        </a>
        <span class="tagline">미세먼지 + 날씨로 보는 오늘의 외출 지수</span>
    </div>
</header>
<main class="container">
