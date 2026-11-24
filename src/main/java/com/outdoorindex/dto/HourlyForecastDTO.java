package com.outdoorindex.dto;

import com.outdoorindex.enums.ActivityLevel;
import com.outdoorindex.enums.PrecipitationType;
import com.outdoorindex.enums.SkyCode;

/**
 * 시간별 예보 한 칸 (단기예보 기반, 결과 화면의 시간대별 추이).
 * - 만드는 곳: WeatherService.getHourlyForecast() (B), 단기예보(getVilageFcst) 기반
 * - level은 OutdoorCardService가 OutdoorIndexPolicy로 판정해서 채운다.
 */
public class HourlyForecastDTO {

    private String fcstDate;                                        // 예보 날짜 (yyyyMMdd)
    private String fcstTime;                                        // 예보 시각 (HHmm)
    private Double temperature;                                     // 기온 (TMP)
    private Integer rainProbability;                                // 강수확률 (POP, %)
    private PrecipitationType precipitation = PrecipitationType.UNKNOWN; // 강수형태 (PTY)
    private SkyCode sky = SkyCode.UNKNOWN;                          // 하늘상태 (SKY)
    private Double windSpeed;                                       // 풍속 (WSD, m/s)
    private String dayLabel;                                        // "오늘" / "내일" / "모레"
    private ActivityLevel level;                                    // 시간별 활동 지수

    public HourlyForecastDTO() {}

    /** 화면 표시용 시각. 예: "18시" */
    public String getHourLabel() {
        if (fcstTime == null || fcstTime.length() < 2) return "";
        return Integer.parseInt(fcstTime.substring(0, 2)) + "시";
    }

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
    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    public String getDayLabel() { return dayLabel; }
    public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }
    public ActivityLevel getLevel() { return level; }
    public void setLevel(ActivityLevel level) { this.level = level; }
}
