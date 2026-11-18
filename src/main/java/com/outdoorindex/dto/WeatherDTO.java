package com.outdoorindex.dto;

import com.outdoorindex.enums.PrecipitationType;
import com.outdoorindex.enums.SkyCode;

/**
 * 현재 날씨 조회 결과.
 * - 만드는 곳: WeatherService (B)
 * - 초단기실황: temperature(T1H), humidity(REH), precipitation(PTY), windSpeed(WSD)
 * - 초단기예보: sky(SKY)
 * - 규칙: 값이 없으면 숫자는 null, 코드는 UNKNOWN
 */
public class WeatherDTO {

    private Double temperature;                                     // 기온 (℃)
    private Integer humidity;                                       // 습도 (%)
    private Double windSpeed;                                       // 풍속 (m/s)
    private PrecipitationType precipitation = PrecipitationType.UNKNOWN; // 강수형태
    private SkyCode sky = SkyCode.UNKNOWN;                          // 하늘상태
    private String baseDate;                                        // 발표 날짜 (yyyyMMdd)
    private String baseTime;                                        // 발표 시각 (HHmm)

    public WeatherDTO() {}

    /** 화면 표시용 발표 시각. 예: "17:00 기준" (값이 없으면 빈 문자열) */
    public String getBaseTimeDisplay() {
        if (baseTime == null || baseTime.length() != 4) return "";
        return baseTime.substring(0, 2) + ":" + baseTime.substring(2) + " 기준";
    }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getHumidity() { return humidity; }
    public void setHumidity(Integer humidity) { this.humidity = humidity; }
    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    public PrecipitationType getPrecipitation() { return precipitation; }
    public void setPrecipitation(PrecipitationType precipitation) { this.precipitation = precipitation == null ? PrecipitationType.UNKNOWN : precipitation; }
    public SkyCode getSky() { return sky; }
    public void setSky(SkyCode sky) { this.sky = sky == null ? SkyCode.UNKNOWN : sky; }
    public String getBaseDate() { return baseDate; }
    public void setBaseDate(String baseDate) { this.baseDate = baseDate; }
    public String getBaseTime() { return baseTime; }
    public void setBaseTime(String baseTime) { this.baseTime = baseTime; }
}
