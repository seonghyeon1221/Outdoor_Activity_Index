package com.outdoorindex.enums;

/**
 * 미세먼지 등급 (환경부 기준).
 * - 담당: D (등급 계산은 DustGradeCalculator)
 * - JSP 사용 예: ${card.air.pm10Grade.label}, class="grade ${card.air.pm10Grade.cssClass}"
 */
public enum DustGrade {
    GOOD("좋음", "good", 1),
    NORMAL("보통", "normal", 2),
    BAD("나쁨", "bad", 3),
    VERY_BAD("매우나쁨", "very-bad", 4),
    UNKNOWN("정보 없음", "unknown", 0);

    private final String label;
    private final String cssClass;
    private final int level; // 숫자가 클수록 나쁨, UNKNOWN = 0

    DustGrade(String label, String cssClass, int level) {
        this.label = label;
        this.cssClass = cssClass;
        this.level = level;
    }

    /** 화면에 보여줄 한글 이름 */
    public String getLabel() { return label; }

    /** CSS 클래스 이름 (색상 표시용) */
    public String getCssClass() { return cssClass; }

    /** 비교용 숫자 (1 좋음 ~ 4 매우나쁨, 정보 없음 0) */
    public int getLevel() { return level; }

    /** 두 등급 중 더 나쁜 등급을 반환 (PM10/PM2.5 대표 등급 계산용) */
    public static DustGrade worse(DustGrade a, DustGrade b) {
        if (a == null) a = UNKNOWN;
        if (b == null) b = UNKNOWN;
        return a.level >= b.level ? a : b;
    }
}
