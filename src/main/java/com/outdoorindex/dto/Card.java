package com.outdoorindex.dto;

import com.outdoorindex.service.IndexPolicy;
import com.outdoorindex.service.IndexPolicy.Grade;
import com.outdoorindex.service.IndexPolicy.Level;

import java.util.List;

/**
 * 결과 카드 1장 (화면에 보낼 데이터 전부). JSP에서는 ${card.air().pm10()} 처럼 읽는다.
 * @param air     에어코리아 결과 (실패하면 null, 이유는 airError)
 * @param weather 기상청 결과 (실패하면 null, 이유는 weatherError)
 * @param nx      측정소 위치를 바꾼 기상청 격자 X  @param ny 격자 Y
 * @param gridFound 측정소가 위치 표에 있었는지 (없으면 시·도 대표 지점)
 */
public record Card(String sido, Air air, String airError, Weather weather, String weatherError,
                   int nx, int ny, boolean gridFound, Result result,
                   List<Hourly> hourly, String summary, List<String> stations) {

    /** 화면 제목: "서울 중구" / 시·도 평균이면 "서울" */
    public String place() {
        return air != null && !air.average() ? sido + " " + air.station() : sido;
    }

    /** 즐겨찾기·비교에 넘길 측정소 이름 (시·도 평균이면 "") */
    public String station() {
        return air != null && !air.average() ? air.station() : "";
    }

    /** 미세먼지. average = 시·도 평균, filled = 빈 값을 평균으로 채움, note = 값이 없는 이유(통신장애 등) */
    public record Air(String station, Integer pm10, Integer pm25, String dataTime,
                      String note, boolean average, boolean filled, int count) {
        public Grade pm10Grade() {
            return IndexPolicy.grade(pm10, IndexPolicy.PM10_GOOD, IndexPolicy.PM10_NORMAL, IndexPolicy.PM10_BAD);
        }
        public Grade pm25Grade() {
            return IndexPolicy.grade(pm25, IndexPolicy.PM25_GOOD, IndexPolicy.PM25_NORMAL, IndexPolicy.PM25_BAD);
        }
        /** 둘 중 더 나쁜 등급 (하나만 있으면 그 값) */
        public Grade grade() {
            Grade a = pm10Grade(), b = pm25Grade();
            if (a == Grade.NONE) return b;
            if (b == Grade.NONE) return a;
            return a.ordinal() >= b.ordinal() ? a : b;
        }
    }

    /** 현재 날씨 (초단기실황 + 하늘 상태) */
    public record Weather(Double temp, Integer humidity, Double wind, String rain, String sky, String baseTime) {
        public boolean raining() {
            return rain != null && !"없음".equals(rain);
        }
    }

    /** 시간대별 예보 한 칸 (level = 그 시간 날씨 + 현재 미세먼지로 판정) */
    public record Hourly(String day, String hour, Double temp, Integer pop, String rain, String sky, Level level) {
        public String icon() {
            if (rain != null && rain.contains("눈")) return "❄";
            if (rain != null && !"없음".equals(rain)) return "☂";
            return "맑음".equals(sky) ? "☀" : "구름많음".equals(sky) ? "⛅" : "☁";
        }
    }

    /** 판정 결과 + 계산 과정 (dust = 적용한 미세먼지 등급, rain·cold·hot·windy = 감점 여부) */
    public record Result(Level level, List<String> reasons, Grade dust, int penalty,
                         boolean rain, boolean cold, boolean hot, boolean windy) {}

    /** 즐겨찾기 한 곳 */
    public record Favorite(String sido, String station) {
        public String name() {
            return station.isEmpty() ? sido : sido + " " + station;
        }
    }

    /** 지역 비교 한 칸 (card 또는 error 중 하나) */
    public record Compare(String sido, String station, Card card, String error, boolean best) {}
}
