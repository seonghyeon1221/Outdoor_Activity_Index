package com.outdoorindex.enums;

/**
 * 기상청 하늘상태(SKY) 코드.
 * - 담당: B
 * - 초단기예보(getUltraSrtFcst)의 SKY 값에서 변환
 */
public enum SkyCode {
    CLEAR("1", "맑음"),
    PARTLY_CLOUDY("3", "구름많음"),
    CLOUDY("4", "흐림"),
    UNKNOWN("", "정보 없음");

    private final String code;
    private final String label;

    SkyCode(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 기상청 원본 코드 값 */
    public String getCode() { return code; }

    /** 화면에 보여줄 한글 이름 */
    public String getLabel() { return label; }

    /** 기상청 코드 문자열("1","3","4") → enum. 모르는 값이면 UNKNOWN */
    public static SkyCode fromCode(String code) {
        if (code == null) return UNKNOWN;
        for (SkyCode s : values()) {
            if (s != UNKNOWN && s.code.equals(code.trim())) return s;
        }
        return UNKNOWN;
    }
}
