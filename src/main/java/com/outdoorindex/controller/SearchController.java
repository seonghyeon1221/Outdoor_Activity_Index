package com.outdoorindex.controller;

import com.outdoorindex.dao.FavoriteDAO;
import com.outdoorindex.dao.SearchHistoryDAO;
import com.outdoorindex.dto.OutdoorCardDTO;
import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.exception.NoResultException;
import com.outdoorindex.exception.OutdoorException;
import com.outdoorindex.service.OutdoorCardService;
import com.outdoorindex.service.RegionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 검색 요청을 받아 결과 화면으로 넘기는 컨트롤러. (Model 2의 Controller)
 * - 담당: A
 * - GET /search                       → 검색 화면 (search.jsp)
 * - GET /search?sido=서울&station=중구 → 결과 카드 (result.jsp)
 * - 시·도가 비었으면 검색 화면 + 안내 문구, 결과 없음은 noResult.jsp, 그 외 오류는 error.jsp
 */
@WebServlet("/search")
public class SearchController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(SearchController.class.getName());
    private static final String VIEW = "/WEB-INF/views/";

    private final OutdoorCardService outdoorCardService = new OutdoorCardService();
    private final FavoriteDAO favoriteDAO = new FavoriteDAO();
    private final SearchHistoryDAO historyDAO = new SearchHistoryDAO();

    /** 검색 화면 표시 또는 검색 실행 */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("sidoList", RegionService.getSidoList());

        String sidoParam = req.getParameter("sido");
        String sido = trim(sidoParam);
        String station = trim(req.getParameter("station"));
        req.setAttribute("sido", sido);
        req.setAttribute("station", station);

        // 처음 들어온 경우: 검색 화면 (즐겨찾기·최근 검색 함께)
        if (sidoParam == null && req.getParameter("station") == null) {
            showSearch(req, resp, null);
            return;
        }
        // 시·도를 고르지 않은 경우
        if (sido.isEmpty()) {
            showSearch(req, resp, ErrorCode.EMPTY_INPUT.getMessage());
            return;
        }

        try {
            OutdoorCardDTO card = outdoorCardService.getCard(sido, station);
            req.setAttribute("card", card);
            // 즐겨찾기·비교에 쓸 측정소 이름 (시·도 평균이면 빈 값)
            String favStation = card.isAirAvailable() && !card.getAir().isSidoAverage()
                    ? card.getAir().getStationName() : station;
            req.setAttribute("favStation", favStation == null ? "" : favStation);
            req.setAttribute("isFavorite", favoriteDAO.exists(sido, favStation));
            req.setAttribute("dbAvailable", com.outdoorindex.dao.Db.isAvailable());
            historyDAO.add(sido, favStation, card.getIndex() == null ? null : card.getIndex().getLevel());
            forward(req, resp, "result.jsp");
        } catch (NoResultException e) {
            LOG.info("검색 결과 없음: " + e.getMessage());
            forward(req, resp, "noResult.jsp");
        } catch (OutdoorException e) {
            if (e.getErrorCode() == ErrorCode.EMPTY_INPUT) {
                showSearch(req, resp, e.getErrorCode().getMessage());
                return;
            }
            LOG.warning("검색 처리 오류: " + e.getMessage());
            req.setAttribute("errorCode", e.getErrorCode());
            forward(req, resp, "error.jsp");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "예상치 못한 오류", e);
            req.setAttribute("errorCode", ErrorCode.UNKNOWN_ERROR);
            forward(req, resp, "error.jsp");
        }
    }

    /** 폼을 POST로 보내도 같은 처리 */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    /** 검색 화면 (즐겨찾기·최근 검색 포함) */
    private void showSearch(HttpServletRequest req, HttpServletResponse resp, String errorMessage)
            throws ServletException, IOException {
        if (errorMessage != null) req.setAttribute("errorMessage", errorMessage);
        req.setAttribute("favorites", favoriteDAO.findAll());
        req.setAttribute("recentSearches", historyDAO.findRecent(6));
        forward(req, resp, "search.jsp");
    }

    private static void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(VIEW + jsp).forward(req, resp);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
