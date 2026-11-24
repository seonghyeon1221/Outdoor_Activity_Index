
package com.outdoorindex.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 기상청 API에 보낼 발표 날짜와 시간을 계산한다. */
public class BaseTimeCalculator {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

    /** 단기예보 발표 시각 (02, 05, 08, 11, 14, 17, 20, 23시) */
    private static final int[] VILLAGE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    /** 단기예보는 발표 후 10분부터 조회할 수 있다. */
    private static final int VILLAGE_AVAILABLE_MINUTE = 10;

    /** 현재 한국 시각을 반환한다. */
    public static LocalDateTime now() {
        return LocalDateTime.now(KOREA);
    }

    /** 초단기실황 발표 시각을 계산한다. 매시 40분 전에는 이전 시간을 사용한다. */
    public static BaseTime forObservation() {
        return forObservation(now());
    }

    /** 초단기예보 발표 시각을 계산한다. 매시 45분 전에는 이전 시간을 사용한다. */
    public static BaseTime forForecast() {
        return forForecast(now());
    }

    /** 기준 시각을 지정해서 초단기실황 발표 시각을 계산한다. (테스트용) */
    public static BaseTime forObservation(LocalDateTime now) {
        LocalDateTime base = now.withMinute(0).withSecond(0).withNano(0);
        if (now.getMinute() < 40) base = base.minusHours(1);
        return new BaseTime(base);
    }

    /** 기준 시각을 지정해서 초단기예보 발표 시각을 계산한다. (테스트용) */
    public static BaseTime forForecast(LocalDateTime now) {
        LocalDateTime base = now.withMinute(30).withSecond(0).withNano(0);
        if (now.getMinute() < 45) base = base.minusHours(1);
        return new BaseTime(base);
    }

    /** 단기예보(시간대별 추이) 발표 시각을 계산한다. 발표 후 10분이 지나야 조회할 수 있다. */
    public static BaseTime forVillage() {
        return forVillage(now());
    }

    /** 기준 시각을 지정해서 단기예보 발표 시각을 계산한다. 예) 17:05 → 14:00, 01:00 → 전날 23:00 */
    public static BaseTime forVillage(LocalDateTime now) {
        for (int day = 0; day < 2; day++) {
            for (int i = VILLAGE_HOURS.length - 1; i >= 0; i--) {
                LocalDateTime base = now.toLocalDate().minusDays(day).atTime(VILLAGE_HOURS[i], 0);
                if (!base.plusMinutes(VILLAGE_AVAILABLE_MINUTE).isAfter(now)) {
                    return new BaseTime(base);
                }
            }
        }
        return new BaseTime(now.toLocalDate().minusDays(1).atTime(23, 0));
    }

    /** 단기예보 데이터가 없을 때 바로 이전 발표 시각(3시간 전)을 반환한다. */
    public static BaseTime previousVillage(BaseTime time) {
        return new BaseTime(time.value.minusHours(3));
    }

    /** 데이터가 없을 때 한 시간 전의 발표 시각을 반환한다. */
    public static BaseTime previous(BaseTime time) {
        return new BaseTime(time.value.minusHours(1));
    }

    /** 발표 날짜와 발표 시각을 함께 담는 클래스이다. */
    public static class BaseTime {
        private final LocalDateTime value;

        /** 계산된 날짜와 시각을 보관한다. */
        public BaseTime(LocalDateTime value) { this.value = value; }

        /** 발표 날짜를 yyyyMMdd 형식으로 반환한다. */
        public String getBaseDate() { return value.format(DATE); }

        /** 발표 시각을 HHmm 형식으로 반환한다. */
        public String getBaseTime() { return value.format(TIME); }
    }
}
