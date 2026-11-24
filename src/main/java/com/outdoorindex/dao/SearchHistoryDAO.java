package com.outdoorindex.dao;

import com.outdoorindex.dto.SearchHistoryDTO;
import com.outdoorindex.enums.ActivityLevel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 최근 검색 기록 DB 처리 (search_history 테이블, MySQL · H2 공통 SQL).
 * - 담당: A
 * - 같은 지역을 여러 번 검색해도 목록에는 가장 최근 1번만 보여줌
 * - 오래된 기록은 KEEP_ROWS 개만 남기고 정리
 */
public class SearchHistoryDAO {

    private static final Logger LOG = Logger.getLogger(SearchHistoryDAO.class.getName());
    private static final int KEEP_ROWS = 200;

    /** 검색 기록 저장 */
    public void add(String sido, String station, ActivityLevel level) {
        if (!Db.isAvailable()) return;
        try (Connection c = Db.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO search_history (sido, station, level) VALUES (?, ?, ?)")) {
                ps.setString(1, sido);
                ps.setString(2, station == null ? "" : station.trim());
                ps.setString(3, level == null ? null : level.name());
                ps.executeUpdate();
            }
            // 최근 KEEP_ROWS개만 남기기 (MySQL·H2 공통 문법: 기준 id를 먼저 구한 뒤 삭제)
            long cutoff = -1;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM search_history ORDER BY id DESC LIMIT 1 OFFSET " + KEEP_ROWS);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) cutoff = rs.getLong(1);
            }
            if (cutoff > 0) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM search_history WHERE id <= ?")) {
                    ps.setLong(1, cutoff);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOG.warning("검색 기록 저장 실패: " + e);
        }
    }

    /** 최근 검색 (지역별 가장 최근 1건, 최신순) */
    public List<SearchHistoryDTO> findRecent(int limit) {
        List<SearchHistoryDTO> list = new ArrayList<>();
        if (!Db.isAvailable()) return list;
        String sql = "SELECT h.sido, h.station, h.level, h.searched_at FROM search_history h "
                + "JOIN (SELECT sido, station, MAX(id) AS max_id FROM search_history GROUP BY sido, station) m "
                + "ON h.id = m.max_id ORDER BY h.id DESC LIMIT ?";
        SimpleDateFormat f = new SimpleDateFormat("MM/dd HH:mm");
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityLevel level = null;
                    String lv = rs.getString(3);
                    if (lv != null) {
                        try { level = ActivityLevel.valueOf(lv); } catch (IllegalArgumentException ignored) { }
                    }
                    list.add(new SearchHistoryDTO(rs.getString(1), rs.getString(2), level,
                            f.format(rs.getTimestamp(4))));
                }
            }
        } catch (SQLException e) {
            LOG.warning("최근 검색 조회 실패: " + e);
        }
        return list;
    }

    /** 기록 전체 삭제 */
    public void clear() {
        if (!Db.isAvailable()) return;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM search_history")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("검색 기록 삭제 실패: " + e);
        }
    }
}
