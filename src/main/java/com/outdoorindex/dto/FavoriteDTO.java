package com.outdoorindex.dto;

/**
 * 즐겨찾기 한 개 (DB favorite 테이블의 한 줄).
 * - 만드는 곳: FavoriteDAO
 */
public class FavoriteDTO {

    private long id;
    private String sido;
    private String station;     // 비어 있으면 시·도 평균
    private String createdAt;   // "2026-11-23 18:10"

    public FavoriteDTO() {}

    public FavoriteDTO(long id, String sido, String station, String createdAt) {
        this.id = id;
        this.sido = sido;
        this.station = station == null ? "" : station;
        this.createdAt = createdAt;
    }

    /** 화면 표시용 이름. 예: "서울 중구", "부산 (시·도 평균)" */
    public String getDisplayName() {
        return station.isBlank() ? sido + " (시·도 평균)" : sido + " " + station;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSido() { return sido; }
    public void setSido(String sido) { this.sido = sido; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station == null ? "" : station; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
