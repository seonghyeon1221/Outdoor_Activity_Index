package com.outdoorindex.dto;

/**
 * 측정소 위치 정보 (측정소 위치 표 station_grid.csv의 한 줄).
 * - 만드는 곳: RegionService (C)
 * - 융합 연결 고리: 에어코리아(API ①)가 돌려준 측정소명 → 이 DTO의 nx, ny → 기상청(API ②) 입력
 * - 쓰는 곳: WeatherService(nx, ny), 화면(displayName)
 */
public class RegionDTO {

    private String sido;         // 시·도 (에어코리아 sidoName과 같은 글자: "서울", "부산" …)
    private String stationName;  // 측정소명 ("중구"). 시·도 대표 지점이면 빈 문자열 ""
    private int nx;              // 기상청 격자 X
    private int ny;              // 기상청 격자 Y

    public RegionDTO() {}

    public RegionDTO(String sido, String stationName, int nx, int ny) {
        this.sido = sido;
        this.stationName = stationName == null ? "" : stationName;
        this.nx = nx;
        this.ny = ny;
    }

    /** 특정 측정소 위치인지 여부 (false면 시·도 대표 지점) */
    public boolean isHasStation() {
        return stationName != null && !stationName.isBlank();
    }

    /** 화면 표시용 이름. 예: "서울 중구", 대표 지점이면 "서울" */
    public String getDisplayName() {
        return isHasStation() ? sido + " " + stationName : sido;
    }

    public String getSido() { return sido; }
    public void setSido(String sido) { this.sido = sido; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName == null ? "" : stationName; }
    public int getNx() { return nx; }
    public void setNx(int nx) { this.nx = nx; }
    public int getNy() { return ny; }
    public void setNy(int ny) { this.ny = ny; }
}
