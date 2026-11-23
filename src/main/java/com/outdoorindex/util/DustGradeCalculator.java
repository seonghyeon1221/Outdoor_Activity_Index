package com.outdoorindex.util;

import com.outdoorindex.enums.DustGrade;
import com.outdoorindex.policy.OutdoorIndexPolicy;

/**
 * 미세먼지 수치를 환경부 기준 등급으로 바꾸는 클래스.
 * - 담당: D
 * - 기준값은 OutdoorIndexPolicy 한 곳에서 관리 (여기서는 비교만)
 * - 값이 null이면 DustGrade.UNKNOWN
 */
public final class DustGradeCalculator {

    private DustGradeCalculator() {}

    /** PM10 수치 → 등급 (좋음 0~30 / 보통 31~80 / 나쁨 81~150 / 매우나쁨 151~) */
    public static DustGrade pm10Grade(Integer value) {
        return grade(value, OutdoorIndexPolicy.PM10_GOOD_MAX,
                OutdoorIndexPolicy.PM10_NORMAL_MAX, OutdoorIndexPolicy.PM10_BAD_MAX);
    }

    /** PM2.5 수치 → 등급 (좋음 0~15 / 보통 16~35 / 나쁨 36~75 / 매우나쁨 76~) */
    public static DustGrade pm25Grade(Integer value) {
        return grade(value, OutdoorIndexPolicy.PM25_GOOD_MAX,
                OutdoorIndexPolicy.PM25_NORMAL_MAX, OutdoorIndexPolicy.PM25_BAD_MAX);
    }

    private static DustGrade grade(Integer value, int goodMax, int normalMax, int badMax) {
        if (value == null || value < 0) return DustGrade.UNKNOWN;
        if (value <= goodMax) return DustGrade.GOOD;
        if (value <= normalMax) return DustGrade.NORMAL;
        if (value <= badMax) return DustGrade.BAD;
        return DustGrade.VERY_BAD;
    }
}
