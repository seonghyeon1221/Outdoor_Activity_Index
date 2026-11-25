package com.outdoorindex.service;

import com.outdoorindex.dto.Card.Air;
import com.outdoorindex.dto.Card.Result;
import com.outdoorindex.dto.Card.Weather;

import java.util.ArrayList;
import java.util.List;

/**
 * 야외활동 지수 판정 기준 (두 API 결과를 합쳐 새 지표를 만드는 융합 부분).
 * 좋음: 미세먼지 좋음·보통 + 날씨 감점 없음 / 주의: 미세먼지 나쁨 또는 감점 1개
 * 나쁨: 미세먼지 매우나쁨 또는 감점 2개 이상
 */
public final class IndexPolicy {

    // 미세먼지 등급 기준 (환경부, ㎍/㎥)
    public static final int PM10_GOOD = 30, PM10_NORMAL = 80, PM10_BAD = 150;
    public static final int PM25_GOOD = 15, PM25_NORMAL = 35, PM25_BAD = 75;
    // 날씨 감점 기준
    public static final int COLD = 0, HOT = 33, WIND = 9;
    public static final int PENALTY_BAD = 2;

    /** 미세먼지 등급 */
    public enum Grade {
        GOOD("좋음", "good"), NORMAL("보통", "normal"), BAD("나쁨", "bad"), VERY_BAD("매우나쁨", "very-bad"), NONE("정보 없음", "none");
        private final String label, css;
        Grade(String label, String css) { this.label = label; this.css = css; }
        public String getLabel() { return label; }
        public String getCss() { return css; }
    }

    /** 야외활동 지수 */
    public enum Level {
        GOOD("좋음", "good", "산책이나 운동하기 좋은 날이에요."),
        CAUTION("주의", "caution", "마스크나 준비물을 챙기고 야외 활동 시간을 줄이세요."),
        BAD("나쁨", "bad", "오늘은 야외 활동보다 실내 활동을 추천해요.");
        private final String label, css, message;
        Level(String label, String css, String message) { this.label = label; this.css = css; this.message = message; }
        public String getLabel() { return label; }
        public String getCss() { return css; }
        public String getMessage() { return message; }
    }

    private IndexPolicy() {}

    /** 숫자 표시용: 12.0 → "12", 3.45 → "3.5", null → "-" */
    public static String num(Double v) {
        if (v == null) return "-";
        double r = Math.round(v * 10) / 10.0;
        return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
    }

    /** 수치 → 등급 (값이 없으면 NONE) */
    public static Grade grade(Integer value, int good, int normal, int bad) {
        if (value == null) return Grade.NONE;
        return value <= good ? Grade.GOOD : value <= normal ? Grade.NORMAL : value <= bad ? Grade.BAD : Grade.VERY_BAD;
    }

    /** 미세먼지(air)와 날씨(wx)로 판정. 둘 다 없으면 null */
    public static Result judge(Air air, Weather wx) {
        if (air == null && wx == null) return null;
        Grade dust = air == null ? Grade.NONE : air.grade();
        boolean rain = wx != null && wx.raining();
        boolean cold = wx != null && wx.temp() != null && wx.temp() <= COLD;
        boolean hot = wx != null && wx.temp() != null && wx.temp() >= HOT;
        boolean windy = wx != null && wx.wind() != null && wx.wind() >= WIND;
        // 날씨 감점: 해당하는 항목마다 1개씩
        int penalty = (rain ? 1 : 0) + (cold ? 1 : 0) + (hot ? 1 : 0) + (windy ? 1 : 0);

        List<String> reasons = new ArrayList<>();          // 화면에 보여 줄 감점 이유
        if (dust == Grade.BAD || dust == Grade.VERY_BAD) reasons.add("미세먼지 " + dust.getLabel());
        if (rain) reasons.add("강수 (" + wx.rain() + ")");
        if (cold) reasons.add("기온이 너무 낮아요 (" + num(wx.temp()) + "℃)");
        if (hot) reasons.add("기온이 너무 높아요 (" + num(wx.temp()) + "℃)");
        if (windy) reasons.add("바람이 강해요 (" + num(wx.wind()) + "m/s)");

        // 최종 판정: 나쁨 → 주의 → 좋음 순서로 먼저 맞는 것
        Level level = dust == Grade.VERY_BAD || penalty >= PENALTY_BAD ? Level.BAD
                : dust == Grade.BAD || penalty == 1 ? Level.CAUTION : Level.GOOD;
        return new Result(level, reasons, dust, penalty, rain, cold, hot, windy);
    }
}
