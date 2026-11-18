package com.outdoorindex.dto;

import com.outdoorindex.enums.ActivityLevel;
import com.outdoorindex.enums.PrecipitationType;
import com.outdoorindex.enums.SkyCode;

/**
 * 시간별 예보 한 칸 (확장용 — 이번에는 구조만 준비).
 * - 만드는 곳: WeatherService.getHourlyForecast() (B), 단기예보(getVilageFcst) 기반
 * - level은 나중에 OutdoorIndexPolicy(D)가 채운다.
 */
public class HourlyForecastDTO {

    private String fcstDate;                                        // 예보 날짜 (yyyyMMdd)
    private String fcstTime;                                        // 예보 시각 (HHmm)
    private Double temperature;                                     // 기온 (TMP)
    private Integer rainProbability;                                // 강수확률 (POP, %)
    private PrecipitationType precipitation = PrecipitationType.UNKNOWN; // 강수형태 (PTY)
    private SkyCode sky = SkyCode.UNKNOWN;                          // 하늘상태 (SKY)
    private ActivityLevel level;                                    // 시간별 활동 지수 (확장)

    public HourlyForecastDTO() {}

    public String getFcstDate() { return fcstDate; }
    public void setFcstDate(String fcstDate) { this.fcstDate = fcstDate; }
    public String getFcstTime() { return fcstTime; }
    public void setFcstTime(String fcstTime) { this.fcstTime = fcstTime; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getRainProbability() { return rainProbability; }
    public void setRainProbability(Integer rainProbability) { this.rainProbability = rainProbability; }
    public PrecipitationType getPrecipitation() { return precipitation; }
    public void setPrecipitation(PrecipitationType precipitation) { this.precipitation = precipitation == null ? PrecipitationType.UNKNOWN : precipitation; }
    public SkyCode getSky() { return sky; }
    public void setSky(SkyCode sky) { this.sky = sky == null ? SkyCode.UNKNOWN : sky; }
    public ActivityLevel getLevel() { return level; }
    public void setLevel(ActivityLevel level) { this.level = level; }
}
