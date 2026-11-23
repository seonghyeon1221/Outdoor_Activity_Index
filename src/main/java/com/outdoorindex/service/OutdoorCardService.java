package com.outdoorindex.service;

import com.outdoorindex.dto.AirQualityDTO;
import com.outdoorindex.dto.OutdoorCardDTO;
import com.outdoorindex.dto.RegionDTO;
import com.outdoorindex.dto.WeatherDTO;
import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.exception.NoResultException;
import com.outdoorindex.exception.OutdoorException;
import com.outdoorindex.policy.OutdoorIndexPolicy;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 두 API 결과를 하나의 카드로 합치는 서비스. (융합의 중심)
 * - 담당: A
 * - 순서: ① 에어코리아 → 측정소명 → 측정소 위치 표(nx, ny) → ② 기상청 → 야외활동 지수 판정
 * - 부분 실패: 한쪽 API가 실패해도 다른 쪽 결과는 카드에 담고, 실패한 쪽은 airError / weatherError에 기록
 */
public class OutdoorCardService {

    private static final Logger LOG = Logger.getLogger(OutdoorCardService.class.getName());

    private final AirQualityService airQualityService;
    private final RegionService regionService;
    private final WeatherService weatherService;

    public OutdoorCardService() {
        this(new AirQualityService(), new RegionService(), new WeatherService());
    }

    /** 테스트 등에서 서비스를 바꿔 끼울 수 있도록 하는 생성자 */
    public OutdoorCardService(AirQualityService air, RegionService region, WeatherService weather) {
        this.airQualityService = air;
        this.regionService = region;
        this.weatherService = weather;
    }

    /**
     * 시·도와 측정소 이름으로 결과 카드를 만든다.
     * @param sido        시·도 (필수)
     * @param stationName 측정소/구 이름 (선택, 비우면 시·도 평균)
     * @throws OutdoorException  시·도가 비었을 때 (EMPTY_INPUT)
     * @throws NoResultException 시·도가 목록에 없거나 측정소가 0건일 때
     */
    public OutdoorCardDTO getCard(String sido, String stationName) {
        if (sido == null || sido.isBlank()) {
            throw new OutdoorException(ErrorCode.EMPTY_INPUT);
        }
        String s = sido.trim();
        if (!RegionService.isValidSido(s)) {
            throw new NoResultException("지원하지 않는 시·도: " + s);
        }

        OutdoorCardDTO card = new OutdoorCardDTO();

        // ① 에어코리아: 시·도 → 측정소 목록 + 미세먼지 → 측정소 선택
        String selectedStation = null;
        try {
            List<AirQualityDTO> stations = airQualityService.getStations(s);
            card.setStationNames(airQualityService.getStationNames(stations));
            AirQualityDTO air = airQualityService.selectStation(stations, stationName);
            card.setAir(air);
            if (!air.isSidoAverage()) {
                selectedStation = air.getStationName();
            }
        } catch (NoResultException e) {
            throw e;                                    // 측정소 0건 → 검색 결과 없음 화면
        } catch (OutdoorException e) {
            LOG.warning("미세먼지 조회 실패: " + e.getMessage());
            card.setAirError(e.getErrorCode());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "미세먼지 조회 중 예상치 못한 오류", e);
            card.setAirError(ErrorCode.UNKNOWN_ERROR);
        }

        // 연결 고리: API ①의 측정소명 → nx, ny (실패했거나 시·도 평균이면 대표 지점)
        RegionDTO region = regionService.findGrid(s, selectedStation);
        card.setRegion(region);

        // ② 기상청: 같은 위치의 날씨
        try {
            WeatherDTO weather = weatherService.getCurrentWeather(region.getNx(), region.getNy());
            card.setWeather(weather);
            card.setHourlyForecasts(weatherService.getHourlyForecast(region.getNx(), region.getNy()));
        } catch (OutdoorException e) {
            LOG.warning("날씨 조회 실패: " + e.getMessage());
            card.setWeatherError(e.getErrorCode());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "날씨 조회 중 예상치 못한 오류", e);
            card.setWeatherError(ErrorCode.UNKNOWN_ERROR);
        }

        // 두 결과를 합쳐 야외활동 지수 판정 (둘 다 실패하면 null)
        card.setIndex(OutdoorIndexPolicy.judge(
                card.isAirAvailable() ? card.getAir() : null,
                card.isWeatherAvailable() ? card.getWeather() : null));
        return card;
    }
}
