package com.outdoorindex.controller;

import com.outdoorindex.dao.FavoriteDao;
import com.outdoorindex.dto.Card;
import com.outdoorindex.dto.Card.Favorite;
import com.outdoorindex.service.CardService;
import com.outdoorindex.util.Api;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;

/**
 * Model 2 컨트롤러: 요청을 받아 서비스·DAO를 부르고 결과를 JSP(View)로 넘긴다.
 * GET /search?sido=서울&station=중구 → 검색 화면 또는 결과 카드
 * GET /compare?sido1=..&station1=..  → 지역 비교 (최대 3곳)
 * GET·POST /favorites               → 즐겨찾기 목록 / 추가·삭제
 * GET /guide                        → 판정 기준
 */
@WebServlet(urlPatterns = {"/search", "/compare", "/favorites", "/guide"}, loadOnStartup = 1)
public class MainController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    // 서비스와 DAO 는 서블릿 하나에 한 번만 만들어 모든 요청이 같이 씀
    private final CardService cardService = new CardService();
    private final FavoriteDao favoriteDao = new FavoriteDao();

    /** 서버 시작 때 WEB-INF/api.properties 를 직접 열어 설정으로 읽는다 (classes 로 복사하지 않음) */
    @Override
    public void init() throws ServletException {
        try (InputStream in = getServletContext().getResourceAsStream("/WEB-INF/api.properties")) {
            Api.load(in);
        } catch (IOException e) {
            throw new ServletException("WEB-INF/api.properties 를 읽을 수 없습니다.", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");                  // POST 로 온 한글 파라미터 깨짐 방지
        req.setAttribute("sidoList", CardService.SIDO);     // 모든 화면의 시·도 선택 목록
        String view;
        try {
            view = switch (req.getServletPath()) {           // 주소별 처리 메서드 선택 → 보여 줄 JSP 이름
                case "/compare" -> compare(req);
                case "/favorites" -> favorites(req, resp);
                case "/guide" -> "guide";
                default -> search(req);
            };
        } catch (IllegalArgumentException e) {               // 지원하지 않는 시·도 등 → 오류 화면
            req.setAttribute("message", e.getMessage());
            view = "error";
        }
        // view 가 null 이면 이미 redirect 한 경우
        if (view != null) req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    /** 검색 화면 또는 결과 카드 */
    private String search(HttpServletRequest req) {
        String sido = param(req, "sido"), station = param(req, "station");
        List<Favorite> favorites = favoriteDao.findAll();
        req.setAttribute("favorites", favorites);
        req.setAttribute("sido", sido);
        req.setAttribute("station", station);
        if (sido.isEmpty()) {                                // 시·도가 없으면 검색 화면 (검색 버튼을 눌렀다면 안내 문구)
            if (req.getParameter("sido") != null) req.setAttribute("message", "시·도를 선택해 주세요.");
            return "search";
        }
        Card card = cardService.card(sido, station, true);  // 두 API 를 이어 결과 카드 생성 (시간대별 예보 포함)
        req.setAttribute("card", card);
        req.setAttribute("isFavorite", favorites.contains(new Favorite(sido, card.station())));
        req.setAttribute("dbError", FavoriteDao.error());
        return "result";
    }

    /** 지역 비교: sido1~3 중 입력된 곳만 모아서 비교 */
    private String compare(HttpServletRequest req) {
        String[] sidos = new String[3], stations = new String[3];
        int n = 0;
        for (int i = 1; i <= 3; i++) {
            if (param(req, "sido" + i).isEmpty()) continue;
            sidos[n] = param(req, "sido" + i);
            stations[n++] = param(req, "station" + i);
        }
        req.setAttribute("items", n == 0 ? Collections.emptyList()
                : cardService.compare(java.util.Arrays.copyOf(sidos, n), java.util.Arrays.copyOf(stations, n)));
        return "compare";
    }

    /** 즐겨찾기: POST 는 추가·삭제 후 redirect, GET 은 목록 화면 */
    private String favorites(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("POST".equals(req.getMethod())) {
            String sido = param(req, "sido");
            if (CardService.SIDO.contains(sido)) favoriteDao.save(sido, param(req, "station"), "add".equals(param(req, "action")));
            String back = param(req, "back");          // 우리 사이트의 /search 화면으로만 돌아감
            resp.sendRedirect(req.getContextPath() + (back.startsWith("/search?") && !back.contains("//") ? back : "/favorites"));
            return null;
        }
        req.setAttribute("favorites", favoriteDao.findAll());
        req.setAttribute("dbError", FavoriteDao.error());
        return "favorites";
    }

    /** 파라미터 읽기 (없으면 빈 문자열, 앞뒤 공백 제거) */
    private static String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    /** 서버 종료·재시작 때 MySQL 드라이버 정리 (메모리 누수 경고 방지) */
    @Override
    public void destroy() {
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            if (d.getClass().getClassLoader() == getClass().getClassLoader()) {
                try { DriverManager.deregisterDriver(d); } catch (Exception ignored) { }
            }
        }
        com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
    }
}
