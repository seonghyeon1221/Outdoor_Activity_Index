package com.outdoorindex.dao;

import com.outdoorindex.dto.FavoriteDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 즐겨찾기 DB 처리 (favorite 테이블, MySQL · H2 공통 SQL).
 * - 담당: A
 * - DB 오류는 로그만 남기고 빈 결과/false 로 처리 (화면이 깨지지 않게)
 */
public class FavoriteDAO {

    private static final Logger LOG = Logger.getLogger(FavoriteDAO.class.getName());
    /** 즐겨찾기 최대 개수 */
    public static final int MAX_FAVORITES = 20;

    /** 즐겨찾기 목록 (최근에 추가한 순) */
    public List<FavoriteDTO> findAll() {
        List<FavoriteDTO> list = new ArrayList<>();
        if (!Db.isAvailable()) return list;
        String sql = "SELECT id, sido, station, created_at FROM favorite ORDER BY created_at DESC, id DESC";
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new FavoriteDTO(rs.getLong(1), rs.getString(2), rs.getString(3),
                        f.format(rs.getTimestamp(4))));
            }
        } catch (SQLException e) {
            LOG.warning("즐겨찾기 조회 실패: " + e);
        }
        return list;
    }

    /** 이미 즐겨찾기에 있는지 */
    public boolean exists(String sido, String station) {
        if (!Db.isAvailable()) return false;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM favorite WHERE sido = ? AND station = ?")) {
            ps.setString(1, sido);
            ps.setString(2, nz(station));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.warning("즐겨찾기 확인 실패: " + e);
            return false;
        }
    }

    /** 추가 (이미 있으면 그대로, 최대 개수 넘으면 false) */
    public boolean add(String sido, String station) {
        if (!Db.isAvailable()) return false;
        if (exists(sido, station)) return true;
        try (Connection c = Db.getConnection()) {
            try (PreparedStatement cnt = c.prepareStatement("SELECT COUNT(*) FROM favorite");
                 ResultSet rs = cnt.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= MAX_FAVORITES) return false;
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO favorite (sido, station) VALUES (?, ?)")) {
                ps.setString(1, sido);
                ps.setString(2, nz(station));
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            LOG.warning("즐겨찾기 추가 실패: " + e);
            return false;
        }
    }

    /** 삭제 */
    public void remove(String sido, String station) {
        if (!Db.isAvailable()) return;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM favorite WHERE sido = ? AND station = ?")) {
            ps.setString(1, sido);
            ps.setString(2, nz(station));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("즐겨찾기 삭제 실패: " + e);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
