package com.outdoorindex.dto.api;

import java.util.List;

/**
 * 에어코리아 시도별 실시간 측정정보(getCtprvnRltmMesureDnsty) JSON 응답 매핑용.
 * - 담당: C
 * - 사용: new Gson().fromJson(json, AirKoreaResponse.class)
 * - 필드 이름은 API JSON 키와 똑같아야 하므로 바꾸지 말 것
 * - 측정값은 "-"가 올 수 있어서 전부 String으로 받고, Service에서 숫자로 변환
 *
 * JSON 구조: { "response": { "header": {...}, "body": { "items": [ {...}, ... ] } } }
 */
public class AirKoreaResponse {

    public Response response;

    public static class Response {
        public Header header;
        public Body body;
    }

    public static class Header {
        public String resultCode;   // "00"이면 정상
        public String resultMsg;
    }

    public static class Body {
        public int totalCount;
        public int pageNo;
        public int numOfRows;
        public List<Item> items;
    }

    /** 측정소 한 곳의 측정값 */
    public static class Item {
        public String stationName;  // 측정소 이름 (예: "중구")
        public String sidoName;     // 시·도 이름
        public String dataTime;     // 측정 시각 (예: "2026-11-18 17:00")
        public String pm10Value;    // PM10 농도 ("-" 가능)
        public String pm25Value;    // PM2.5 농도 ("-" 가능)
        public String pm10Grade;    // 에어코리아 자체 등급 (참고용, 판정에는 사용 안 함)
        public String pm25Grade;
    }
}
