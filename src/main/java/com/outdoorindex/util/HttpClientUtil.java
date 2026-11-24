package com.outdoorindex.util;

import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.exception.ApiConnectionException;
import com.outdoorindex.exception.ApiParseException;
import com.outdoorindex.exception.ApiResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 외부 API에 HTTP GET 요청을 보내는 공용 도구 (java.net.http.HttpClient 사용).
 * - 담당: A
 * - 사용: AirQualityService(C), WeatherService(B)
 * - 연결 실패/타임아웃 → ApiConnectionException
 * - JSON 대신 XML 오류 응답 → 서비스키/트래픽 오류면 ApiResponseException, 그 외 ApiParseException
 */
public final class HttpClientUtil {

    private static final Logger LOG = Logger.getLogger(HttpClientUtil.class.getName());
    private static final Pattern XML_REASON = Pattern.compile("<returnReasonCode>\\s*(\\d+)\\s*</returnReasonCode>");
    private static final Pattern XML_AUTH_MSG = Pattern.compile("<returnAuthMsg>\\s*([^<]+?)\\s*</returnAuthMsg>");
    private static final Pattern XML_RESULT_CODE = Pattern.compile("<resultCode>\\s*(\\d+)\\s*</resultCode>");
    private static final Pattern XML_RESULT_MSG = Pattern.compile("<resultMsg>\\s*([^<]+?)\\s*</resultMsg>");

    private static final Pattern RESULT_OK = Pattern.compile("\"resultCode\"\\s*:\\s*\"00\"");

    private static volatile HttpClient client;

    private HttpClientUtil() {}

    /**
     * URL로 GET 요청을 보내고 응답 본문(JSON 문자열)을 돌려준다.
     * @param url 파라미터까지 인코딩이 끝난 전체 URL
     * @return 응답 본문 (앞뒤 공백 제거)
     */
    public static String get(String url) {
        String cacheKey = cacheKey(url);
        String demoKey = demoKey(url);
        String mode = ApiConfig.getDemoMode();

        // 1) 시연 재생 모드: 인터넷 없이 저장본만 사용
        if ("replay".equals(mode)) {
            String saved = DemoStore.load(demoKey);
            if (saved == null) {
                throw new ApiConnectionException(ErrorCode.API_CONNECTION_FAILED,
                        "시연 재생 모드인데 저장된 응답이 없음: " + mask(url), null);
            }
            DemoStore.markUsed();
            return saved;
        }

        // 2) 메모리 캐시
        String cached = ApiCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 3) 실제 요청 (연결 실패·타임아웃이면 저장본으로 대체)
        String body;
        try {
            body = request(url);
        } catch (ApiConnectionException e) {
            if ("auto".equals(mode)) {
                String saved = DemoStore.load(demoKey);
                if (saved != null) {
                    LOG.warning("연결 실패 → 저장된 응답 사용: " + mask(url));
                    DemoStore.markUsed();
                    return saved;
                }
            }
            throw e;
        }

        // 4) 정상 응답("00")만 캐시·저장
        if (RESULT_OK.matcher(body).find()) {
            ApiCache.put(cacheKey, body);
            if ("auto".equals(mode)) {
                DemoStore.save(demoKey, body);
            }
        }
        return body;
    }

    /** 실제 HTTP GET 요청 */
    private static String request(String url) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(ApiConfig.getRequestTimeoutMs()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ApiConnectionException(ErrorCode.API_CONNECTION_FAILED, "잘못된 URL: " + mask(url), e);
        }

        HttpResponse<String> response;
        try {
            response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            LOG.warning("API 타임아웃: " + mask(url));
            throw new ApiConnectionException(ErrorCode.API_TIMEOUT, "타임아웃: " + mask(url), e);
        } catch (IOException e) {
            LOG.warning("API 연결 실패: " + mask(url) + " (" + e + ")");
            throw new ApiConnectionException(ErrorCode.API_CONNECTION_FAILED, "연결 실패: " + mask(url), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiConnectionException(ErrorCode.API_CONNECTION_FAILED, "요청 중단: " + mask(url), e);
        }

        String body = response.body() == null ? "" : response.body().trim();
        int status = response.statusCode();

        if (body.startsWith("<")) {
            throw fromXml(body, status);
        }
        if (status == 401 || status == 403) {
            throw new ApiResponseException(ErrorCode.API_KEY_ERROR, "HTTP_" + status, body);
        }
        if (status == 429) {
            throw new ApiResponseException(ErrorCode.API_TRAFFIC_EXCEEDED, "HTTP_" + status, body);
        }
        if (status < 200 || status >= 300) {
            throw new ApiResponseException(ErrorCode.API_RESPONSE_ERROR, "HTTP_" + status, body);
        }
        if (body.isEmpty()) {
            throw new ApiParseException("빈 응답: " + mask(url), null);
        }
        if (!body.startsWith("{") && !body.startsWith("[")) {
            // "Unauthorized" 같은 일반 텍스트 응답
            throw new ApiParseException("JSON이 아닌 응답: " + abbreviate(body), null);
        }
        return body;
    }

    /** XML 오류 응답을 해석해서 알맞은 예외로 바꾼다 */
    private static RuntimeException fromXml(String xml, int status) {
        String code = find(XML_REASON, xml);
        String msg = find(XML_AUTH_MSG, xml);
        if (code == null) {
            code = find(XML_RESULT_CODE, xml);
            msg = find(XML_RESULT_MSG, xml);
        }
        LOG.warning("XML 응답 수신 (HTTP " + status + "): code=" + code + ", msg=" + msg);
        if (code != null && !"00".equals(code)) {
            try {
                ApiResponseValidator.validate(code, msg);
            } catch (ApiResponseException e) {
                return e;
            }
        }
        return new ApiParseException("JSON 대신 XML 응답: " + abbreviate(xml), null);
    }

    /** 공용 HttpClient (처음 한 번만 생성) */
    private static HttpClient client() {
        HttpClient c = client;
        if (c != null) return c;
        synchronized (HttpClientUtil.class) {
            if (client == null) {
                client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(ApiConfig.getConnectTimeoutMs()))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
            }
            return client;
        }
    }

    private static String find(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /** 캐시 키: 서비스키를 뺀 주소 */
    static String cacheKey(String url) {
        return stripParams(url, java.util.Set.of("servicekey"));
    }

    /** 시연 저장 키: 서비스키·발표 날짜·발표 시각을 뺀 주소 */
    static String demoKey(String url) {
        return stripParams(url, java.util.Set.of("servicekey", "base_date", "base_time"));
    }

    /** 주소에서 지정한 파라미터(대소문자 무시)를 뺀다 */
    private static String stripParams(String url, java.util.Set<String> lowerNames) {
        int q = url.indexOf('?');
        if (q < 0) return url;
        java.util.StringJoiner kept = new java.util.StringJoiner("&");
        for (String part : url.substring(q + 1).split("&")) {
            if (part.isEmpty()) continue;
            int eq = part.indexOf('=');
            String name = (eq < 0 ? part : part.substring(0, eq)).toLowerCase();
            if (!lowerNames.contains(name)) kept.add(part);
        }
        String rest = kept.toString();
        return rest.isEmpty() ? url.substring(0, q) : url.substring(0, q + 1) + rest;
    }

    /** 로그에 서비스키가 찍히지 않도록 가림 */
    static String mask(String url) {
        return url == null ? null : url.replaceAll("(?i)(serviceKey=)[^&]*", "$1****");
    }

    private static String abbreviate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
