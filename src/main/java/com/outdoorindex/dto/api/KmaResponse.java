package com.outdoorindex.dto.api;

import java.util.List;

/**
 * 기상청 단기예보 조회서비스 JSON 응답 매핑용 (초단기실황 / 초단기예보 / 단기예보 공통).
 * - 담당: B
 * - 사용: new Gson().fromJson(json, KmaResponse.class)
 * - 필드 이름은 API JSON 키와 똑같아야 하므로 바꾸지 말 것
 * - 실황은 obsrValue, 예보는 fcstDate/fcstTime/fcstValue에 값이 들어옴
 *
 * JSON 구조: { "response": { "header": {...}, "body": { "items": { "item": [ {...}, ... ] } } } }
 * (에어코리아와 달리 items 안에 item 배열이 한 번 더 있음)
 */
public class KmaResponse {

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
        public String dataType;
        public Items items;
        public int pageNo;
        public int numOfRows;
        public int totalCount;
    }

    public static class Items {
        public List<Item> item;
    }

    /** 항목 하나 (category 하나당 한 줄씩 온다. 예: T1H 한 줄, REH 한 줄 …) */
    public static class Item {
        public String baseDate;
        public String baseTime;
        public String category;     // T1H, REH, PTY, WSD, SKY, TMP, POP …
        public int nx;
        public int ny;
        public String obsrValue;    // 실황 값 (getUltraSrtNcst)
        public String fcstDate;     // 예보 날짜 (getUltraSrtFcst, getVilageFcst)
        public String fcstTime;     // 예보 시각
        public String fcstValue;    // 예보 값
    }
}
