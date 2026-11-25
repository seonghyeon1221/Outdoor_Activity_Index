package com.outdoorindex.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 설정(WEB-INF/api.properties) 보관 + 공공데이터 API 호출 + JSON 해석 공통 기능 */
public final class Api {

    public static final String AIRKOREA = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc";
    public static final String KMA = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";

    private static final Properties CONFIG = new Properties();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** 공공데이터포털 공통 오류 코드 → 화면 문구 */
    private static final Map<String, String> ERRORS = Map.of(
            "10", "요청 값이 잘못되었습니다.",
            "11", "필수 요청 값이 빠졌습니다.",
            "12", "해당 API 서비스를 찾을 수 없습니다.",
            "20", "이 서비스 키로는 API를 사용할 수 없습니다. (활용 신청 확인)",
            "22", "오늘 사용할 수 있는 API 호출 횟수를 모두 썼습니다. 내일 다시 시도해 주세요.",
            "30", "등록되지 않은 서비스 키입니다. api.properties 의 service.key 를 확인해 주세요.",
            "31", "서비스 키 사용 기간이 끝났습니다.",
            "32", "등록되지 않은 IP에서 요청했습니다.");

    private Api() {}

    /** 서버 시작 때 MainController 가 WEB-INF/api.properties 를 직접 열어 넘겨 준다 */
    public static void load(InputStream in) throws IOException {
        if (in == null) {
            System.err.println("WEB-INF/api.properties 가 없습니다. 서비스 키와 DB 정보를 읽지 못했습니다.");
            return;
        }
        CONFIG.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** 설정값 (없으면 빈 문자열) */
    public static String config(String key) {
        return CONFIG.getProperty(key, "").trim();
    }

    /**
     * API를 호출하고 응답의 item 배열을 돌려준다.
     * - resultCode 03(데이터 없음)이면 빈 배열
     * - 그 밖의 오류 코드나 통신 실패는 RuntimeException (화면에 보여줄 문구 포함)
     */
    public static JsonArray items(String url, Map<String, Object> params) {
        String query = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        String key = URLEncoder.encode(config("service.key"), StandardCharsets.UTF_8);   // 키의 +, / 등을 인코딩
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "?serviceKey=" + key + "&" + query))
                .timeout(Duration.ofSeconds(10)).GET().build();

        String body;
        int status;
        try {
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            status = res.statusCode();
            body = res.body() == null ? "" : res.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("데이터 요청이 중단되었습니다. 다시 검색해 주세요.");
        } catch (IOException e) {
            throw new RuntimeException("공공데이터 서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요.");
        }
        // 서비스 키 오류 등은 XML 또는 JSON 의 returnReasonCode 로 온다 (HTTP 403 과 함께 오기도 함)
        Matcher m = Pattern.compile("returnReasonCode\\W*(\\d+)").matcher(body);
        if (m.find()) throw new RuntimeException(message(m.group(1)));
        if (status == 401 || status == 403) {               // 키가 틀렸거나 활용 신청이 안 된 경우
            throw new RuntimeException("서비스 키가 올바르지 않거나 사용 권한이 없습니다. api.properties 의 service.key 를 확인해 주세요. (HTTP " + status + ")");
        }
        if (status != 200) {
            throw new RuntimeException("공공데이터 서버가 응답하지 않습니다. 잠시 후 다시 시도해 주세요. (HTTP " + status + ")");
        }
        if (!body.stripLeading().startsWith("{")) {
            throw new RuntimeException("서비스 키가 올바르지 않거나 아직 승인되지 않았습니다. api.properties 의 service.key 를 확인해 주세요.");
        }

        JsonObject response;
        String code;
        try {
            response = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("response");
            code = response.getAsJsonObject("header").get("resultCode").getAsString();
        } catch (RuntimeException e) {                     // JSON 이 잘리거나 형식이 다를 때
            throw new RuntimeException("API 응답 형식이 올바르지 않습니다. 잠시 후 다시 시도해 주세요.");
        }
        if ("03".equals(code)) return new JsonArray();     // 03 = 데이터 없음 (오류 아님)
        if (!"00".equals(code)) throw new RuntimeException(message(code));
        JsonObject b = response.getAsJsonObject("body");
        JsonElement items = b == null ? null : b.get("items");
        if (items == null || items.isJsonNull()) return new JsonArray();
        if (items.isJsonArray()) return items.getAsJsonArray();                 // 에어코리아
        JsonElement item = items.getAsJsonObject().get("item");                  // 기상청
        return item != null && item.isJsonArray() ? item.getAsJsonArray() : new JsonArray();
    }

    /** 오류 코드 → 사용자에게 보여 줄 문구 */
    private static String message(String code) {
        return ERRORS.getOrDefault(code, "공공데이터 API 처리 중 오류가 발생했습니다.") + " (오류 코드 " + code + ")";
    }

    /** item 하나에서 문자열 값 꺼내기 (없으면 null) */
    public static String text(JsonElement obj, String field) {
        JsonElement v = obj.getAsJsonObject().get(field);
        return v == null || v.isJsonNull() ? null : v.getAsString().trim();
    }

    /** "-", 빈 값, 기상청 결측값(±900 이상) → null */
    public static Double number(String v) {
        if (v == null || v.isBlank() || "-".equals(v)) return null;
        try {
            double d = Double.parseDouble(v);
            return Math.abs(d) >= 900 ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
