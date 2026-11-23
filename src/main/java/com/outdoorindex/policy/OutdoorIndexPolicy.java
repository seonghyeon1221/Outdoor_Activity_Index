package com.outdoorindex.policy;

import com.outdoorindex.dto.AirQualityDTO;
import com.outdoorindex.dto.OutdoorIndexResult;
import com.outdoorindex.dto.WeatherDTO;
import com.outdoorindex.enums.ActivityLevel;
import com.outdoorindex.enums.DustGrade;

import java.util.ArrayList;
import java.util.List;

/**
 * 야외활동 지수 판정 기준을 한 곳에 모은 클래스.
 * - 담당: D
 * - 기준을 바꿀 때는 이 파일의 상수만 고치면 된다.
 * - 두 API 결과(미세먼지 + 날씨)를 합쳐 어느 API에도 없는 새 지표를 만드는 "융합" 부분
 *
 * 최종 판정
 *   좋음: 미세먼지 좋음~보통 + 날씨 감점 없음
 *   주의: 미세먼지 나쁨 또는 날씨 감점 1개
 *   나쁨: 미세먼지 매우나쁨 또는 날씨 감점 2개 이상
 */
public final class OutdoorIndexPolicy {

    // ===== 미세먼지 등급 기준 (환경부, ㎍/㎥) =====
    public static final int PM10_GOOD_MAX = 30;
    public static final int PM10_NORMAL_MAX = 80;
    public static final int PM10_BAD_MAX = 150;
    public static final int PM25_GOOD_MAX = 15;
    public static final int PM25_NORMAL_MAX = 35;
    public static final int PM25_BAD_MAX = 75;

    // ===== 날씨 감점 기준 =====
    /** 이 기온 이하면 감점 (℃) */
    public static final double TEMP_COLD_MAX = 0.0;
    /** 이 기온 이상이면 감점 (℃) */
    public static final double TEMP_HOT_MIN = 33.0;
    /** 이 풍속 이상이면 감점 (m/s) */
    public static final double WIND_STRONG_MIN = 9.0;

    // ===== 판정 기준 =====
    /** 날씨 감점이 이 개수 이상이면 '나쁨' */
    public static final int PENALTY_BAD_COUNT = 2;

    // ===== 추천 문구 =====
    static final String MSG_GOOD = "산책이나 운동하기 좋은 날이에요.";
    static final String MSG_CAUTION_DUST = "미세먼지가 나빠요. 마스크를 쓰고 야외 활동 시간을 줄이세요.";
    static final String MSG_CAUTION_WEATHER = "날씨 때문에 주의가 필요해요. 준비물을 챙겨서 짧게 다녀오세요.";
    static final String MSG_BAD = "오늘은 야외 활동보다 실내 활동을 추천해요.";

    private OutdoorIndexPolicy() {}

    /**
     * 미세먼지와 날씨를 합쳐 야외활동 지수를 판정한다.
     * @param air     미세먼지 (API 실패 시 null)
     * @param weather 날씨 (API 실패 시 null)
     * @return 판정 결과. 둘 다 null이면 판정할 수 없으므로 null
     */
    public static OutdoorIndexResult judge(AirQualityDTO air, WeatherDTO weather) {
        if (air == null && weather == null) {
            return null;
        }

        DustGrade dust = air == null ? DustGrade.UNKNOWN : air.getRepresentativeGrade();
        boolean airMissing = dust == DustGrade.UNKNOWN;
        List<String> reasons = new ArrayList<>();

        if (dust == DustGrade.BAD) reasons.add("미세먼지 나쁨");
        if (dust == DustGrade.VERY_BAD) reasons.add("미세먼지 매우나쁨");

        List<String> penalties = weatherPenalties(weather);
        reasons.addAll(penalties);

        ActivityLevel level;
        String message;
        if (dust == DustGrade.VERY_BAD || penalties.size() >= PENALTY_BAD_COUNT) {
            level = ActivityLevel.BAD;
            message = MSG_BAD;
        } else if (dust == DustGrade.BAD) {
            level = ActivityLevel.CAUTION;
            message = MSG_CAUTION_DUST;
        } else if (penalties.size() == 1) {
            level = ActivityLevel.CAUTION;
            message = MSG_CAUTION_WEATHER;
        } else {
            level = ActivityLevel.GOOD;
            message = MSG_GOOD;
        }

        return new OutdoorIndexResult(level, message, reasons, airMissing);
    }

    /** 날씨 감점 사유 목록 (날씨가 없으면 빈 리스트) */
    static List<String> weatherPenalties(WeatherDTO weather) {
        List<String> penalties = new ArrayList<>();
        if (weather == null) return penalties;

        if (weather.getPrecipitation() != null && weather.getPrecipitation().isPrecipitating()) {
            penalties.add("강수 (" + weather.getPrecipitation().getLabel() + ")");
        }
        Double t = weather.getTemperature();
        if (t != null && t <= TEMP_COLD_MAX) {
            penalties.add("기온이 너무 낮아요 (" + format(t) + "℃)");
        }
        if (t != null && t >= TEMP_HOT_MIN) {
            penalties.add("기온이 너무 높아요 (" + format(t) + "℃)");
        }
        Double w = weather.getWindSpeed();
        if (w != null && w >= WIND_STRONG_MIN) {
            penalties.add("바람이 강해요 (" + format(w) + "m/s)");
        }
        return penalties;
    }

    private static String format(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(Math.round(v * 10) / 10.0);
    }
}
