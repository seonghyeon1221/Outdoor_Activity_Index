package com.outdoorindex.dto;

import com.outdoorindex.enums.ActivityLevel;

/**
 * 최근 검색 한 개 (DB search_history 테이블의 한 줄).
 * - 만드는 곳: SearchHistoryDAO
 */
public class SearchHistoryDTO {

    private String sido;
    private String station;
    private ActivityLevel level;   // 검색 당시 판정 (없으면 null)
    private String searchedAt;     // "11/23 18:10"

    public SearchHistoryDTO() {}

    public SearchHistoryDTO(String sido, String station, ActivityLevel level, String searchedAt) {
        this.sido = sido;
        this.station = station == null ? "" : station;
        this.level = level;
        this.searchedAt = searchedAt;
    }

    /** 화면 표시용 이름 */
    public String getDisplayName() {
        return station.isBlank() ? sido : sido + " " + station;
    }

    public String getSido() { return sido; }
    public String getStation() { return station; }
    public ActivityLevel getLevel() { return level; }
    public String getSearchedAt() { return searchedAt; }
}
