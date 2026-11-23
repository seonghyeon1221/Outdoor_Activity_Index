package com.outdoorindex.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

import java.io.IOException;

/**
 * 모든 요청/응답의 문자 인코딩을 UTF-8로 맞추는 필터. (한글 깨짐 방지)
 * - 담당: A
 * - getParameter()보다 먼저 실행되어야 하므로 모든 URL(/*)에 적용
 */
@WebFilter("/*")
public class EncodingFilter implements Filter {

    private static final String UTF_8 = "UTF-8";

    /** 요청과 응답 인코딩을 UTF-8로 설정한 뒤 다음 단계로 넘긴다 */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(UTF_8);
        response.setCharacterEncoding(UTF_8);
        chain.doFilter(request, response);
    }
}
