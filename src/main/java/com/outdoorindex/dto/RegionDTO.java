package com.outdoorindex.dto;

/**
 * 지역 정보 (지역 표 region_grid.csv의 한 줄).
 * - 만드는 곳: RegionService (C)
 * - 쓰는 곳: AirQualityService(sido, sigungu), WeatherService(nx, ny), 화면(displayName)
 */
public class RegionDTO {

    private String sido;     // 시·도 (에어코리아 sidoName과 같은 글자: "서울", "부산" …)
    private String sigungu;  // 시군구 ("중구"). 입력하지 않았으면 빈 문자열 ""
    private int nx;          // 기상청 격자 X
    private int ny;          // 기상청 격자 Y

    public RegionDTO() {}

    public RegionDTO(String sido, String sigungu, int nx, int ny) {
        this.sido = sido;
        this.sigungu = sigungu == null ? "" : sigungu;
        this.nx = nx;
        this.ny = ny;
    }

    /** 시군구가 입력되었는지 여부 */
    public boolean isHasSigungu() {
        return sigungu != null && !sigungu.isBlank();
    }

    /** 화면 표시용 이름. 예: "서울 중구", 시군구 없으면 "서울" */
    public String getDisplayName() {
        return isHasSigungu() ? sido + " " + sigungu : sido;
    }

    public String getSido() { return sido; }
    public void setSido(String sido) { this.sido = sido; }
    public String getSigungu() { return sigungu; }
    public void setSigungu(String sigungu) { this.sigungu = sigungu == null ? "" : sigungu; }
    public int getNx() { return nx; }
    public void setNx(int nx) { this.nx = nx; }
    public int getNy() { return ny; }
    public void setNy(int ny) { this.ny = ny; }
}
