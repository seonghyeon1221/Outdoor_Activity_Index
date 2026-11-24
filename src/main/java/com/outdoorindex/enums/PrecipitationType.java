package com.outdoorindex.enums;

/**
 * 기상청 강수형태(PTY) 코드 (초단기실황·초단기예보·단기예보).
 * - 담당: B
 * - 판정(D)에서는 isPrecipitating()만 사용
 */
public enum PrecipitationType {
    NONE("0", "없음"),
    RAIN("1", "비"),
    RAIN_SNOW("2", "비/눈"),
    SNOW("3", "눈"),
    SHOWER("4", "소나기"),                // 단기예보에만 있음
    DRIZZLE("5", "빗방울"),
    DRIZZLE_SNOW("6", "빗방울눈날림"),
    SNOW_FLURRY("7", "눈날림"),
    UNKNOWN("", "정보 없음");

    private final String code;
    private final String label;

    PrecipitationType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 기상청 원본 코드 값 */
    public String getCode() { return code; }

    /** 화면에 보여줄 한글 이름 */
    public String getLabel() { return label; }

    /** 비나 눈이 오는지 여부 (날씨 감점 판정용). NONE, UNKNOWN이면 false */
    public boolean isPrecipitating() {
        return this != NONE && this != UNKNOWN;
    }

    /** 기상청 코드 문자열 → enum. 모르는 값이면 UNKNOWN */
    public static PrecipitationType fromCode(String code) {
        if (code == null) return UNKNOWN;
        for (PrecipitationType p : values()) {
            if (p != UNKNOWN && p.code.equals(code.trim())) return p;
        }
        return UNKNOWN;
    }
}
