package com.outdoorindex.dto;

import com.outdoorindex.enums.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 결과 화면(result.jsp)에 넘기는 최종 카드.
 * - 만드는 곳: OutdoorCardService (A)
 * - request 속성 이름: "card"  → JSP에서 ${card.xxx}
 * - 규칙: 한쪽 API가 실패하면 그쪽 DTO는 null, 대신 airError / weatherError에 오류 코드
 */
public class OutdoorCardDTO {

    private RegionDTO region;                                         // 검색한 지역
    private AirQualityDTO air;                                        // 미세먼지 (실패 시 null)
    private WeatherDTO weather;                                       // 날씨 (실패 시 null)
    private OutdoorIndexResult index;                                 // 야외활동 지수 (둘 다 실패하면 null)
    private ErrorCode airError;                                       // 미세먼지 오류 (정상이면 null)
    private ErrorCode weatherError;                                   // 날씨 오류 (정상이면 null)
    private List<HourlyForecastDTO> hourlyForecasts = new ArrayList<>(); // 시간별 예보 (확장용, 지금은 빈 리스트)

    public OutdoorCardDTO() {}

    /** 미세먼지 정보를 화면에 보여줄 수 있는지 */
    public boolean isAirAvailable() { return air != null && airError == null; }

    /** 날씨 정보를 화면에 보여줄 수 있는지 */
    public boolean isWeatherAvailable() { return weather != null && weatherError == null; }

    public RegionDTO getRegion() { return region; }
    public void setRegion(RegionDTO region) { this.region = region; }
    public AirQualityDTO getAir() { return air; }
    public void setAir(AirQualityDTO air) { this.air = air; }
    public WeatherDTO getWeather() { return weather; }
    public void setWeather(WeatherDTO weather) { this.weather = weather; }
    public OutdoorIndexResult getIndex() { return index; }
    public void setIndex(OutdoorIndexResult index) { this.index = index; }
    public ErrorCode getAirError() { return airError; }
    public void setAirError(ErrorCode airError) { this.airError = airError; }
    public ErrorCode getWeatherError() { return weatherError; }
    public void setWeatherError(ErrorCode weatherError) { this.weatherError = weatherError; }
    public List<HourlyForecastDTO> getHourlyForecasts() { return hourlyForecasts; }
    public void setHourlyForecasts(List<HourlyForecastDTO> hourlyForecasts) { this.hourlyForecasts = hourlyForecasts == null ? new ArrayList<>() : hourlyForecasts; }
}
