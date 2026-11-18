package com.outdoorindex.dto;

import com.outdoorindex.enums.DustGrade;

/**
 * 미세먼지 조회 결과.
 * - 만드는 곳: AirQualityService (C)
 * - 규칙: 측정값이 "-", null, 빈 문자열이면 숫자는 null, 등급은 DustGrade.UNKNOWN
 * - 등급은 C가 DustGradeCalculator(D)를 호출해서 채운다.
 */
public class AirQualityDTO {

    private String stationName;                         // 측정소 이름 (시·도 평균이면 null) → RegionService 입력으로 이어짐
    private Integer pm10;                               // 미세먼지 PM10 (㎍/㎥), 없으면 null
    private Integer pm25;                               // 초미세먼지 PM2.5 (㎍/㎥), 없으면 null
    private DustGrade pm10Grade = DustGrade.UNKNOWN;    // PM10 등급
    private DustGrade pm25Grade = DustGrade.UNKNOWN;    // PM2.5 등급
    private String dataTime;                            // 측정 시각 (예: "2026-11-18 17:00")
    private boolean sidoAverage;                        // true면 시·도 평균값

    public AirQualityDTO() {}

    /** PM10, PM2.5 중 더 나쁜 등급 (판정에 사용) */
    public DustGrade getRepresentativeGrade() {
        return DustGrade.worse(pm10Grade, pm25Grade);
    }

    /** 두 수치가 모두 없으면 true */
    public boolean isEmpty() {
        return pm10 == null && pm25 == null;
    }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public Integer getPm10() { return pm10; }
    public void setPm10(Integer pm10) { this.pm10 = pm10; }
    public Integer getPm25() { return pm25; }
    public void setPm25(Integer pm25) { this.pm25 = pm25; }
    public DustGrade getPm10Grade() { return pm10Grade; }
    public void setPm10Grade(DustGrade pm10Grade) { this.pm10Grade = pm10Grade == null ? DustGrade.UNKNOWN : pm10Grade; }
    public DustGrade getPm25Grade() { return pm25Grade; }
    public void setPm25Grade(DustGrade pm25Grade) { this.pm25Grade = pm25Grade == null ? DustGrade.UNKNOWN : pm25Grade; }
    public String getDataTime() { return dataTime; }
    public void setDataTime(String dataTime) { this.dataTime = dataTime; }
    public boolean isSidoAverage() { return sidoAverage; }
    public void setSidoAverage(boolean sidoAverage) { this.sidoAverage = sidoAverage; }
}
