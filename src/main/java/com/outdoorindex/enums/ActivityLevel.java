package com.outdoorindex.enums;

/**
 * 야외활동 지수 최종 판정 등급.
 * - 담당: D (판정은 OutdoorIndexPolicy)
 * - JSP 사용 예: <div class="badge ${card.index.level.cssClass}">${card.index.level.label}</div>
 */
public enum ActivityLevel {
    GOOD("좋음", "good"),
    CAUTION("주의", "caution"),
    BAD("나쁨", "bad");

    private final String label;
    private final String cssClass;

    ActivityLevel(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    /** 화면에 보여줄 한글 이름 */
    public String getLabel() { return label; }

    /** CSS 클래스 이름 (배지 색상용) */
    public String getCssClass() { return cssClass; }
}
