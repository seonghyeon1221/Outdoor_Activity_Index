package com.outdoorindex.dao;

import com.outdoorindex.util.ApiConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * DB 연결 관리. MySQL을 기본으로 쓰고, MySQL 설정이 없으면 내장 H2 파일 DB로 동작한다.
 * - 담당: A
 * - MySQL: 환경변수(.env) OUTDOOR_DB_URL / OUTDOOR_DB_USER / OUTDOOR_DB_PASSWORD
 *          예) OUTDOOR_DB_URL=jdbc:mysql://localhost:3306/outdoor_index
 *          → 데이터베이스가 없으면 자동 생성(createDatabaseIfNotExist), 한글은 utf8mb4
 * - H2   : OUTDOOR_DB_URL이 비어 있을 때. {데이터 폴더}/outdoor-db.mv.db 파일
 * - 테이블: favorite(즐겨찾기), search_history(최근 검색) — 서버 시작 시 없으면 생성
 * - DB에 연결할 수 없으면 isAvailable() = false 로 두고, 검색·비교 기능은 그대로 동작
 */
public final class Db {

    private static final Logger LOG = Logger.getLogger(Db.class.getName());

    /** MySQL 주소에 자동으로 붙이는 옵션 (사용자가 직접 넣은 값이 있으면 그대로 둠) */
    private static final String[][] MYSQL_DEFAULT_OPTIONS = {
            {"useUnicode", "true"},
            {"characterEncoding", "UTF-8"},
            {"serverTimezone", "Asia/Seoul"},
            {"createDatabaseIfNotExist", "true"},
            {"allowPublicKeyRetrieval", "true"},
            {"useSSL", "false"},
            {"connectTimeout", "5000"},
    };

    private static volatile Boolean available;
    private static volatile String url;
    private static volatile boolean mysql;
    private static volatile String lastError;

    private Db() {}

    /** DB 사용 가능 여부 (처음 호출 시 연결 확인 + 테이블 생성) */
    public static boolean isAvailable() {
        Boolean a = available;
        if (a != null) return a;
        synchronized (Db.class) {
            if (available != null) return available;
            try {
                String configured = ApiConfig.getDbUrl();
                if (configured != null && !configured.isBlank()) {
                    mysql = configured.startsWith("jdbc:mysql:");
                    url = mysql ? withMysqlOptions(configured.trim()) : configured.trim();
                    if (mysql) Class.forName("com.mysql.cj.jdbc.Driver");
                } else {
                    mysql = false;
                    Class.forName("org.h2.Driver");
                    Path dir = ApiConfig.getDataDir();
                    Files.createDirectories(dir);
                    url = "jdbc:h2:file:" + dir.resolve("outdoor-db").toString().replace('\\', '/');
                }
                try (Connection c = open(); Statement st = c.createStatement()) {
                    for (String ddl : schema(mysql)) st.execute(ddl);
                }
                available = Boolean.TRUE;
                lastError = null;
                LOG.info("DB 준비 완료: " + getDescription());
            } catch (ClassNotFoundException e) {
                lastError = "DB 드라이버가 없습니다: " + e.getMessage();
                LOG.warning(lastError);
                available = Boolean.FALSE;
            } catch (Exception e) {
                lastError = friendlyError(e);
                LOG.warning("DB 연결 실패 → 즐겨찾기·최근 검색 기능을 끕니다: " + e);
                available = Boolean.FALSE;
            }
            return available;
        }
    }

    /** 새 연결 (try-with-resources로 닫을 것) */
    public static Connection getConnection() throws SQLException {
        if (!isAvailable()) throw new SQLException("DB를 사용할 수 없습니다.");
        return open();
    }

    /** 지금 쓰는 DB 설명 (화면·로그용). 예: "MySQL localhost:3306/outdoor_index" */
    public static String getDescription() {
        if (url == null) return "-";
        if (mysql) {
            String hostDb = url.substring("jdbc:mysql://".length());
            int q = hostDb.indexOf('?');
            return "MySQL " + (q < 0 ? hostDb : hostDb.substring(0, q));
        }
        return url.startsWith("jdbc:h2:") ? "H2 (내장 파일 DB)" : url;
    }

    /** MySQL을 쓰는 중인지 */
    public static boolean isMysql() {
        return mysql;
    }

    /** 마지막 연결 오류 (화면 안내용, 없으면 null) */
    public static String getLastError() {
        return lastError;
    }

    /** 설정을 다시 읽고 연결을 다시 시도 (즐겨찾기 화면의 "다시 연결" 버튼) */
    public static synchronized boolean retry() {
        available = null;
        return isAvailable();
    }

    private static Connection open() throws SQLException {
        return mysql
                ? DriverManager.getConnection(url, ApiConfig.getDbUser(), ApiConfig.getDbPassword())
                : DriverManager.getConnection(url, "sa", "");
    }

    /** 연결 오류를 원인별 한국어 안내로 바꾸기 */
    static String friendlyError(Throwable e) {
        StringBuilder all = new StringBuilder();
        String root = e.getMessage();
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null) {
                all.append(t.getMessage()).append('\n');
                root = t.getMessage();
            }
        }
        String m = all.toString();
        if (m.contains("Access denied")) {
            return "MySQL 아이디 또는 비밀번호가 틀렸어요. (설정 파일 .env 또는 outdoor-index.properties 의 OUTDOOR_DB_USER / OUTDOOR_DB_PASSWORD 확인)";
        }
        if (m.contains("Communications link failure") || m.contains("Connection refused")) {
            return "MySQL 서버에 연결할 수 없어요. MySQL이 켜져 있는지 확인하세요. (Windows: 서비스 앱에서 MySQL80 시작 / Mac: brew services start mysql)";
        }
        if (m.contains("Unknown database")) {
            return "데이터베이스가 없어요. 계정에 CREATE 권한을 주거나 docs/schema-mysql.sql 을 실행하세요.";
        }
        if (m.contains("command denied") || m.contains("CREATE command")) {
            return "테이블을 만들 권한이 없어요. 계정 권한(GRANT)을 확인하세요.";
        }
        return root == null ? e.toString() : root.split("\n")[0];
    }

    /** 테이블 생성 SQL (MySQL은 InnoDB + utf8mb4로 한글 저장) */
    static String[] schema(boolean forMysql) {
        String tail = forMysql ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci" : "";
        return new String[] {
                "CREATE TABLE IF NOT EXISTS favorite ("
                        + " id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + " sido VARCHAR(20) NOT NULL,"
                        + " station VARCHAR(50) NOT NULL DEFAULT '',"
                        + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + " CONSTRAINT uq_favorite UNIQUE (sido, station))" + tail,
                "CREATE TABLE IF NOT EXISTS search_history ("
                        + " id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + " sido VARCHAR(20) NOT NULL,"
                        + " station VARCHAR(50) NOT NULL DEFAULT '',"
                        + " level VARCHAR(20),"
                        + " searched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)" + tail,
        };
    }

    /** MySQL 주소에 한글·시간대·자동 생성 옵션 붙이기 */
    static String withMysqlOptions(String base) {
        StringBuilder u = new StringBuilder(base);
        String lower = base.toLowerCase();
        boolean hasQuery = base.contains("?");
        for (String[] opt : MYSQL_DEFAULT_OPTIONS) {
            if (lower.contains(opt[0].toLowerCase() + "=")) continue;
            u.append(hasQuery ? '&' : '?').append(opt[0]).append('=').append(opt[1]);
            hasQuery = true;
        }
        return u.toString();
    }
}
