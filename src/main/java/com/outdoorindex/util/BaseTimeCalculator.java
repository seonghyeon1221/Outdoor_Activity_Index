package com.outdoorindex.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 기상청 API 요청에 필요한 발표 날짜(base_date)와 발표 시각(base_time)을 계산하는 클래스.
 * - 담당: B
 * - 반환값: String[] { base_date(yyyyMMdd), base_time(HHmm) }
 * - 한 시간 전으로 되돌릴 때 자정을 넘으면 날짜도 전날로 바뀐다 (LocalDateTime이 자동 처리).
 * - 기준 시간대는 Asia/Seoul 고정
 */
public final class BaseTimeCalculator {

    /** 한국 시간대 */
    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 초단기실황: 매시 정각 발표, 40분 이후 조회 가능 */
    static final int NCST_AVAILABLE_MINUTE = 40;
    /** 초단기예보: 매시 30분 발표, 45분 이후 조회 가능 */
    static final int FCST_AVAILABLE_MINUTE = 45;
    /** 단기예보 발표 시각 (확장용): 02, 05, 08, 11, 14, 17, 20, 23시, 10분 이후 조회 가능 */
    static final int[] VILAGE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    static final int VILAGE_AVAILABLE_MINUTE = 10;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH");

    private BaseTimeCalculator() {}

    /** 현재 한국 시각 */
    public static LocalDateTime now() {
        return LocalDateTime.now(SEOUL);
    }

    /**
     * 초단기실황(getUltraSrtNcst)용 발표 시각.
     * 예) 17:39 → 1600, 17:40 → 1700, 00:10 → 전날 2300
     */
    public static String[] forUltraSrtNcst(LocalDateTime now) {
        LocalDateTime t = now.getMinute() < NCST_AVAILABLE_MINUTE ? now.minusHours(1) : now;
        return new String[] { t.format(DATE), t.format(HOUR) + "00" };
    }

    /**
     * 초단기예보(getUltraSrtFcst)용 발표 시각.
     * 예) 17:44 → 1630, 17:45 → 1730, 00:20 → 전날 2330
     */
    public static String[] forUltraSrtFcst(LocalDateTime now) {
        LocalDateTime t = now.getMinute() < FCST_AVAILABLE_MINUTE ? now.minusHours(1) : now;
        return new String[] { t.format(DATE), t.format(HOUR) + "30" };
    }

    /**
     * 단기예보(getVilageFcst)용 발표 시각 (확장용).
     * 예) 17:05 → 1400, 17:10 → 1700, 01:00 → 전날 2300
     */
    public static String[] forVilageFcst(LocalDateTime now) {
        LocalDateTime t = now;
        for (int i = 0; i < 2; i++) {               // 오늘 → 안 되면 전날까지 확인
            for (int k = VILAGE_HOURS.length - 1; k >= 0; k--) {
                LocalDateTime candidate = t.toLocalDate().atTime(VILAGE_HOURS[k], VILAGE_AVAILABLE_MINUTE);
                if (!candidate.isAfter(now)) {
                    return new String[] { candidate.format(DATE), candidate.format(HOUR) + "00" };
                }
            }
            t = t.minusDays(1);
        }
        LocalDateTime fallback = now.toLocalDate().minusDays(1).atTime(23, 0);
        return new String[] { fallback.format(DATE), "2300" };
    }
}
