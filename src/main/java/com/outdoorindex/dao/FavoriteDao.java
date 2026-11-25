package com.outdoorindex.dao;

import com.outdoorindex.dto.Card.Favorite;
import com.outdoorindex.util.Api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 즐겨찾기 DB 처리 (MySQL favorite 테이블).
 * 접속 정보는 api.properties 의 db.url / db.user / db.password.
 * DB에 연결할 수 없으면 error 에 이유를 남기고 빈 목록으로 동작한다 (검색·비교는 그대로 사용 가능).
 */
public class FavoriteDao {

    private static final String OPTIONS = "?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul"
            + "&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=3000";
    private static final String TABLE = "CREATE TABLE IF NOT EXISTS favorite ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, sido VARCHAR(20) NOT NULL, station VARCHAR(50) NOT NULL DEFAULT '', "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE (sido, station)) DEFAULT CHARSET=utf8mb4";

    /** 마지막 DB 오류 (정상이면 null) */
    private static String error;
    private static boolean tableReady;   // 테이블 생성 SQL 을 한 번만 실행하기 위한 표시

    public static String error() {
        return error;
    }

    private static Connection open() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("WEB-INF/lib 에 mysql-connector-j 가 없습니다.");
        }
        Connection c = DriverManager.getConnection(Api.config("db.url") + OPTIONS, Api.config("db.user"), Api.config("db.password"));
        if (!tableReady) {
            try (Statement st = c.createStatement()) { st.execute(TABLE); }
            tableReady = true;
        }
        return c;
    }

    /** 즐겨찾기 목록 (최근 추가 순) */
    public List<Favorite> findAll() {
        List<Favorite> list = new ArrayList<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT sido, station FROM favorite ORDER BY id DESC")) {
            while (rs.next()) list.add(new Favorite(rs.getString(1), rs.getString(2)));
            error = null;
        } catch (SQLException e) {
            error = "DB에 연결할 수 없습니다. MySQL 실행 여부와 api.properties 의 db.password 를 확인하세요. (" + e.getMessage() + ")";
        }
        return list;
    }

    /** 추가(add = true) 또는 삭제. 이미 있는 곳을 추가하면 무시 */
    public void save(String sido, String station, boolean add) {
        String sql = add ? "INSERT IGNORE INTO favorite (sido, station) VALUES (?, ?)"
                         : "DELETE FROM favorite WHERE sido = ? AND station = ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sido);           // ? 자리에 값을 넣어 SQL 인젝션 방지
            ps.setString(2, station);
            ps.executeUpdate();
        } catch (SQLException e) {
            error = e.getMessage();
        }
    }
}
