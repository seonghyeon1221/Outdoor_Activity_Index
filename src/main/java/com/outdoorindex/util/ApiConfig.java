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
 * API 서비스키와 엔드포인트를 설정 파일(api.properties)에서 읽어오는 클래스.
 * - 담당: A
 * - 설정 파일 위치: src/main/resources/api.properties (클래스패스 최상단)
 * - 서비스키는 Decoding 키 기준. 실수로 Encoding 키(%2B 등 포함)를 넣어도
 *   여기서 한 번 디코딩해서 돌려주므로, 서비스에서 URLEncoder로 1번만 인코딩하면 된다. (이중 인코딩 방지)
 */
public final class ApiConfig {

    private static final String FILE_NAME = "api.properties";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 10000;

    private static volatile Properties props;

    private ApiConfig() {}

    /** 공공데이터포털 서비스키 (Decoding 키, URL 인코딩 전 원본 값) */
    public static String getServiceKey() {
        String key = required("service.key");
        // Encoding 키(%xx 포함)를 넣은 경우 원래 값으로 되돌림
        if (key.contains("%")) {
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        }
        return key;
    }

    /** 에어코리아 대기오염정보 엔드포인트 (끝의 / 제거) */
    public static String getAirKoreaEndpoint() {
        return trimSlash(required("airkorea.endpoint"));
    }

    /** 기상청 단기예보 조회서비스 엔드포인트 (끝의 / 제거) */
    public static String getKmaEndpoint() {
        return trimSlash(required("kma.endpoint"));
    }

    /** 연결 타임아웃 (밀리초, 기본 5000) */
    public static int getConnectTimeoutMs() {
        return intValue("http.connect.timeout", DEFAULT_CONNECT_TIMEOUT_MS);
    }

    /** 응답 타임아웃 (밀리초, 기본 10000) */
    public static int getRequestTimeoutMs() {
        return intValue("http.request.timeout", DEFAULT_REQUEST_TIMEOUT_MS);
    }

    /** 필수 설정값 읽기. 없거나 비어 있으면 키 오류로 처리 */
    private static String required(String name) {
        String value = load().getProperty(name);
        if (value == null || value.isBlank()) {
            throw new OutdoorException(ErrorCode.API_KEY_ERROR,
                    FILE_NAME + "에 '" + name + "' 값이 없습니다.", null);
        }
        return value.trim();
    }

    /** 숫자 설정값 읽기 (없거나 잘못되면 기본값) */
    private static int intValue(String name, int defaultValue) {
        String value = load().getProperty(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 설정 파일을 처음 한 번만 읽어서 보관 (UTF-8) */
    private static Properties load() {
        Properties p = props;
        if (p != null) return p;
        synchronized (ApiConfig.class) {
            if (props != null) return props;
            Properties loaded = new Properties();
            try (InputStream in = ApiConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                if (in == null) {
                    throw new OutdoorException(ErrorCode.API_KEY_ERROR,
                            FILE_NAME + " 파일이 없습니다. api.properties.example을 복사해서 만들어 주세요.", null);
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    loaded.load(reader);
                }
            } catch (IOException e) {
                throw new OutdoorException(ErrorCode.API_KEY_ERROR, FILE_NAME + " 읽기 실패", e);
            }
            props = loaded;
            return loaded;
        }
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
