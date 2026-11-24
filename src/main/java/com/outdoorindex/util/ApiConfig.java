package com.outdoorindex.util;

import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.exception.OutdoorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * API 서비스키·엔드포인트·타임아웃 설정을 읽는 클래스.
 * - 담당: A
 *
 * 값을 찾는 순서 (먼저 찾은 값 사용)
 *   1) 환경변수            예: OUTDOOR_SERVICE_KEY
 *   2) JVM 시스템 속성     예: -DOUTDOOR_SERVICE_KEY=...  (Launcher가 .env 파일을 읽어 여기에 넣음)
 *   3) api.properties      예: service.key=...  (선택, src/main/resources)
 *   4) 기본값              (엔드포인트·타임아웃만. 서비스키는 기본값 없음)
 *
 * 서비스키는 Decoding 키 기준. Encoding 키(%2B 등 포함)를 넣어도 여기서 한 번 디코딩하므로
 * 서비스에서 URLEncoder로 1번만 인코딩하면 된다. (이중 인코딩 방지)
 */
public final class ApiConfig {

    // ===== 환경변수 이름 =====
    public static final String ENV_SERVICE_KEY = "OUTDOOR_SERVICE_KEY";
    public static final String ENV_AIRKOREA_ENDPOINT = "OUTDOOR_AIRKOREA_ENDPOINT";
    public static final String ENV_KMA_ENDPOINT = "OUTDOOR_KMA_ENDPOINT";
    public static final String ENV_CONNECT_TIMEOUT = "OUTDOOR_HTTP_CONNECT_TIMEOUT";
    public static final String ENV_REQUEST_TIMEOUT = "OUTDOOR_HTTP_REQUEST_TIMEOUT";
    public static final String ENV_CACHE_TTL = "OUTDOOR_CACHE_TTL_SECONDS";
    public static final String ENV_DEMO_MODE = "OUTDOOR_DEMO_MODE";
    public static final String ENV_DATA_DIR = "OUTDOOR_DATA_DIR";
    public static final String ENV_DB_URL = "OUTDOOR_DB_URL";
    public static final String ENV_DB_USER = "OUTDOOR_DB_USER";
    public static final String ENV_DB_PASSWORD = "OUTDOOR_DB_PASSWORD";

    // ===== 기본값 =====
    static final String DEFAULT_AIRKOREA_ENDPOINT = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc";
    static final String DEFAULT_KMA_ENDPOINT = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";
    static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    static final int DEFAULT_REQUEST_TIMEOUT_MS = 10000;
    static final int DEFAULT_CACHE_TTL_SECONDS = 600;

    private static final String FILE_NAME = "api.properties";
    private static volatile Properties fileProps;

    private ApiConfig() {}

    /** 공공데이터포털 서비스키 (Decoding 키, URL 인코딩 전 원본 값) */
    public static String getServiceKey() {
        String key = lookup(ENV_SERVICE_KEY, "service.key", null);
        if (key == null || key.isBlank()) {
            throw new OutdoorException(ErrorCode.API_KEY_ERROR,
                    "서비스키가 없습니다. 환경변수 " + ENV_SERVICE_KEY + " 또는 .env 파일을 확인하세요.", null);
        }
        key = key.trim();
        if (key.contains("%")) {
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        }
        return key;
    }

    /** 서비스키가 설정되어 있는지 (서버 시작 시 안내용) */
    public static boolean hasServiceKey() {
        String key = lookup(ENV_SERVICE_KEY, "service.key", null);
        return key != null && !key.isBlank();
    }

    /** 에어코리아 대기오염정보 엔드포인트 */
    public static String getAirKoreaEndpoint() {
        return trimSlash(lookup(ENV_AIRKOREA_ENDPOINT, "airkorea.endpoint", DEFAULT_AIRKOREA_ENDPOINT));
    }

    /** 기상청 단기예보 조회서비스 엔드포인트 */
    public static String getKmaEndpoint() {
        return trimSlash(lookup(ENV_KMA_ENDPOINT, "kma.endpoint", DEFAULT_KMA_ENDPOINT));
    }

    /** 연결 타임아웃 (밀리초, 기본 5000) */
    public static int getConnectTimeoutMs() {
        return intValue(lookup(ENV_CONNECT_TIMEOUT, "http.connect.timeout", null), DEFAULT_CONNECT_TIMEOUT_MS);
    }

    /** 응답 타임아웃 (밀리초, 기본 10000) */
    public static int getRequestTimeoutMs() {
        return intValue(lookup(ENV_REQUEST_TIMEOUT, "http.request.timeout", null), DEFAULT_REQUEST_TIMEOUT_MS);
    }

    /** API 응답 캐시 유지 시간 (초, 기본 600 = 10분, 0이면 캐시 끔) */
    public static int getCacheTtlSeconds() {
        return intValue(lookup(ENV_CACHE_TTL, "cache.ttl.seconds", null), DEFAULT_CACHE_TTL_SECONDS);
    }

    /**
     * 시연 모드.
     * - auto  (기본): 실제 API 사용 + 성공한 응답을 저장. 연결 실패·타임아웃이면 저장본으로 대신 보여줌
     * - replay       : 인터넷 없이 저장본만 사용 (발표장 인터넷이 불안할 때)
     * - off          : 저장도 대체도 하지 않음
     */
    public static String getDemoMode() {
        String v = lookup(ENV_DEMO_MODE, "demo.mode", "auto").toLowerCase();
        return ("replay".equals(v) || "off".equals(v)) ? v : "auto";
    }

    /** 데이터 폴더 (시연용 저장 응답, DB 파일). 기본: 실행 폴더의 data */
    public static java.nio.file.Path getDataDir() {
        String v = lookup(ENV_DATA_DIR, "data.dir", null);
        java.nio.file.Path p;
        if (v != null && !v.isBlank()) {
            p = java.nio.file.Paths.get(v);
        } else if (System.getProperty("outdoor.home") == null && System.getProperty("catalina.base") != null) {
            // 설치형 Tomcat(WAR 배포): Tomcat 폴더 안 outdoor-data (bin 폴더에 파일이 생기지 않게)
            p = java.nio.file.Paths.get(System.getProperty("catalina.base"), "outdoor-data");
        } else {
            p = java.nio.file.Paths.get("data");     // 내장 실행: 프로젝트 폴더의 data
        }
        return p.toAbsolutePath().normalize();
    }

    /** DB 접속 주소 (예: jdbc:mysql://localhost:3306/outdoor_index). 비어 있으면 내장 H2 사용 */
    public static String getDbUrl() {
        return lookup(ENV_DB_URL, "db.url", "");
    }

    /** DB 사용자 (기본 root) */
    public static String getDbUser() {
        return lookup(ENV_DB_USER, "db.user", "root");
    }

    /** DB 비밀번호 (기본 빈 값) */
    public static String getDbPassword() {
        String v = System.getenv(ENV_DB_PASSWORD);
        if (v == null) v = System.getProperty(ENV_DB_PASSWORD);
        if (v == null) v = loadExternal().getProperty(ENV_DB_PASSWORD);
        if (v == null) v = loadFile().getProperty("db.password");
        return v == null ? "" : v;
    }

    /** 환경변수 → 시스템 속성 → api.properties → 기본값 순서로 찾기 */
    static String lookup(String envName, String propName, String defaultValue) {
        String v = System.getenv(envName);
        if (isBlank(v)) v = System.getProperty(envName);
        if (isBlank(v)) v = loadExternal().getProperty(envName);
        if (isBlank(v)) v = loadFile().getProperty(propName);
        if (isBlank(v)) v = defaultValue;
        return v == null ? null : v.trim();
    }

    private static int intValue(String value, int defaultValue) {
        if (isBlank(value)) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** api.properties (선택 사항). 없으면 빈 설정 */
    private static Properties loadFile() {
        Properties p = fileProps;
        if (p != null) return p;
        synchronized (ApiConfig.class) {
            if (fileProps != null) return fileProps;
            Properties loaded = new Properties();
            try (InputStream in = ApiConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (in != null) {
                    try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        loaded.load(reader);
                    }
                }
            } catch (IOException ignored) {
                // 파일이 없거나 읽지 못하면 환경변수/기본값만 사용
            }
            fileProps = loaded;
            return loaded;
        }
    }

    // ───────── 외부 설정 파일 (설치형 Tomcat 배포용) ─────────
    public static final String EXTERNAL_FILE_NAME = "outdoor-index.properties";
    private static volatile Properties externalProps;
    private static volatile String externalPath;

    /**
     * 외부 설정 파일을 읽는다. 키 이름은 .env 와 같다 (OUTDOOR_SERVICE_KEY=...).
     * 찾는 순서: -Doutdoor.config / 환경변수 OUTDOOR_CONFIG → {Tomcat}/conf/outdoor-index.properties
     *          → {사용자 홈}/outdoor-index.properties
     * 윈도우 경로(C:\...)의 역슬래시가 깨지지 않도록 Properties.load 대신 한 줄씩 KEY=VALUE 로 읽는다.
     */
    private static Properties loadExternal() {
        Properties p = externalProps;
        if (p != null) return p;
        synchronized (ApiConfig.class) {
            if (externalProps != null) return externalProps;
            Properties loaded = new Properties();
            java.util.List<String> candidates = new java.util.ArrayList<>();
            String explicit = System.getProperty("outdoor.config");
            if (isBlank(explicit)) explicit = System.getenv("OUTDOOR_CONFIG");
            if (!isBlank(explicit)) candidates.add(explicit.trim());
            String base = System.getProperty("catalina.base");
            if (!isBlank(base)) candidates.add(java.nio.file.Paths.get(base, "conf", EXTERNAL_FILE_NAME).toString());
            String home = System.getProperty("user.home");
            if (!isBlank(home)) candidates.add(java.nio.file.Paths.get(home, EXTERNAL_FILE_NAME).toString());
            for (String c : candidates) {
                java.nio.file.Path f = java.nio.file.Paths.get(c);
                if (!java.nio.file.Files.isRegularFile(f)) continue;
                try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.replace("\uFEFF", "").trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq <= 0) continue;
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        loaded.setProperty(key, value);
                    }
                    externalPath = f.toAbsolutePath().toString();
                } catch (IOException ignored) {
                    // 읽지 못하면 다음 후보로
                    continue;
                }
                break;
            }
            externalProps = loaded;
            return loaded;
        }
    }

    /** 읽은 외부 설정 파일 경로 (없으면 null) — 시작 로그용 */
    public static String getExternalConfigPath() {
        loadExternal();
        return externalPath;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
