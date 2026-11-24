package com.outdoorindex.controller;

import com.outdoorindex.dao.Db;
import com.outdoorindex.dao.FavoriteDAO;
import com.outdoorindex.dao.SearchHistoryDAO;
import com.outdoorindex.service.RegionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 즐겨찾기 · 최근 검색 컨트롤러 (MySQL, 설정이 없으면 내장 H2).
 * - GET  /favorites                                  → 즐겨찾기 + 최근 검색 화면
 * - POST /favorites  action=add|remove, sido, station → 추가/삭제 후 원래 화면으로
 * - POST /favorites  action=clearHistory              → 최근 검색 기록 지우기
 * - POST /favorites  action=retryDb                   → DB 다시 연결 (MySQL을 나중에 켠 경우)
 */
@WebServlet("/favorites")
public class FavoriteController extends HttpServlet {

    private final FavoriteDAO favoriteDAO = new FavoriteDAO();
    private final SearchHistoryDAO historyDAO = new SearchHistoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("dbAvailable", Db.isAvailable());
        req.setAttribute("dbDescription", Db.getDescription());
        req.setAttribute("dbError", Db.getLastError());
        req.setAttribute("favorites", favoriteDAO.findAll());
        req.setAttribute("recentSearches", historyDAO.findRecent(20));
        req.setAttribute("maxFavorites", FavoriteDAO.MAX_FAVORITES);
        req.getRequestDispatcher("/WEB-INF/views/favorites.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = trim(req.getParameter("action"));
        String sido = trim(req.getParameter("sido"));
        String station = trim(req.getParameter("station"));

        if ("add".equals(action) && RegionService.isValidSido(sido)) {
            favoriteDAO.add(sido, station);     // 최대 개수를 넘으면 추가되지 않음
        } else if ("remove".equals(action) && RegionService.isValidSido(sido)) {
            favoriteDAO.remove(sido, station);
        } else if ("clearHistory".equals(action)) {
            historyDAO.clear();
        } else if ("retryDb".equals(action)) {
            Db.retry();
            req.getServletContext().setAttribute("dbAvailable", Db.isAvailable());
        }
        resp.sendRedirect(safeBack(req));
    }

    /** 처리 후 돌아갈 주소 (우리 사이트 안의 화면만 허용) */
    private static String safeBack(HttpServletRequest req) {
        String ctx = req.getContextPath();
        String back = trim(req.getParameter("back"));
        if (back.startsWith("/search") || back.startsWith("/compare") || back.startsWith("/favorites")) {
            if (!back.contains("//") && !back.contains("\r") && !back.contains("\n")) {
                return ctx + back;
            }
        }
        return ctx + "/favorites";
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
