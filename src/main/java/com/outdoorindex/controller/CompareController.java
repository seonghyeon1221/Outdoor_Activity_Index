package com.outdoorindex.controller;

import com.outdoorindex.dao.FavoriteDAO;
import com.outdoorindex.dto.CompareItemDTO;
import com.outdoorindex.dto.OutdoorCardDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 여러 지역 비교 컨트롤러.
 * - GET /compare                                   → 비교할 지역 입력 화면
 * - GET /compare?sido1=서울&station1=중구&sido2=부산 → 최대 3곳 카드를 나란히 + 가장 좋은 곳 표시
 */
@WebServlet("/compare")
public class CompareController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(CompareController.class.getName());
    /** 한 번에 비교할 수 있는 최대 지역 수 */
    public static final int MAX_ITEMS = 3;

    private final OutdoorCardService outdoorCardService = new OutdoorCardService();
    private final FavoriteDAO favoriteDAO = new FavoriteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("sidoList", RegionService.getSidoList());
        req.setAttribute("favorites", favoriteDAO.findAll());

        // 입력값 (빈 칸도 화면에 그대로 다시 보여주기 위해 3칸 모두 보관)
        List<CompareItemDTO> inputs = new ArrayList<>();
        for (int i = 1; i <= MAX_ITEMS; i++) {
            inputs.add(new CompareItemDTO(trim(req.getParameter("sido" + i)), trim(req.getParameter("station" + i))));
        }
        req.setAttribute("inputs", inputs);

        List<CompareItemDTO> items = new ArrayList<>();
        for (CompareItemDTO in : inputs) {
            if (in.getSido().isEmpty()) continue;
            CompareItemDTO item = new CompareItemDTO(in.getSido(), in.getStation());
            try {
                item.setCard(outdoorCardService.getCard(in.getSido(), in.getStation(), false));
            } catch (NoResultException e) {
                item.setErrorMessage("검색 결과가 없습니다.");
            } catch (OutdoorException e) {
                item.setErrorMessage(e.getErrorCode().getMessage());
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "비교 조회 오류", e);
                item.setErrorMessage("알 수 없는 오류가 발생했습니다.");
            }
            items.add(item);
        }
        markBest(items);
        req.setAttribute("items", items);
        req.getRequestDispatcher("/WEB-INF/views/compare.jsp").forward(req, resp);
    }

    /**
     * 가장 좋은 곳 표시: 야외활동 지수가 가장 좋고, 같으면 미세먼지 등급·감점 수가 적은 곳.
     * 비교 대상이 2곳 이상일 때만 표시
     */
    static void markBest(List<CompareItemDTO> items) {
        CompareItemDTO best = null;
        int bestScore = Integer.MAX_VALUE;
        int valid = 0;
        for (CompareItemDTO it : items) {
            OutdoorCardDTO c = it.getCard();
            if (c == null || c.getIndex() == null) continue;
            valid++;
            int score = c.getIndex().getLevel().ordinal() * 100
                    + (c.isAirAvailable() ? c.getAir().getRepresentativeGrade().getLevel() * 10 : 50)
                    + c.getIndex().getReasons().size();
            if (score < bestScore) {
                bestScore = score;
                best = it;
            }
        }
        if (best != null && valid >= 2) best.setBest(true);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
