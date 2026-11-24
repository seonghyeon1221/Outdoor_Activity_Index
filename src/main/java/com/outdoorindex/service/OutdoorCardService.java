package com.outdoorindex.service;

import com.outdoorindex.dto.AirQualityDTO;
import com.outdoorindex.dto.HourlyForecastDTO;
import com.outdoorindex.dto.OutdoorIndexResult;
import com.outdoorindex.enums.ActivityLevel;
import com.outdoorindex.util.DemoStore;
import com.outdoorindex.dto.OutdoorCardDTO;
import com.outdoorindex.dto.RegionDTO;
import com.outdoorindex.dto.WeatherDTO;
import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.exception.NoResultException;
import com.outdoorindex.exception.OutdoorException;
import com.outdoorindex.policy.OutdoorIndexPolicy;

import java.util.ArrayList;
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
    // WeatherService 는 static 메서드만 있어서 객체를 들고 있지 않음 (팀원 B 코드)

    public OutdoorCardService() {
        this(new AirQualityService(), new RegionService());
    }

    /** 테스트 등에서 서비스를 바꿔 끼울 수 있도록 하는 생성자 */
    public OutdoorCardService(AirQualityService air, RegionService region) {
        this.airQualityService = air;
        this.regionService = region;
    }

    /**
     * 시·도와 측정소 이름으로 결과 카드를 만든다.
     * @param sido        시·도 (필수)
     * @param stationName 측정소/구 이름 (선택, 비우면 시·도 평균)
     * @throws OutdoorException  시·도가 비었을 때 (EMPTY_INPUT)
     * @throws NoResultException 시·도가 목록에 없거나 측정소가 0건일 때
     */
    public OutdoorCardDTO getCard(String sido, String stationName) {
        return getCard(sido, stationName, true);
    }

    /**
     * 결과 카드 만들기.
     * @param includeHourly 시간대별 추이(단기예보)까지 조회할지 (지역 비교 화면에서는 false)
     */
    public OutdoorCardDTO getCard(String sido, String stationName, boolean includeHourly) {
        DemoStore.resetUsage();
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
            WeatherDTO weather = WeatherService.getCurrentWeather(region.getNx(), region.getNy());
            card.setWeather(weather);
        } catch (OutdoorException e) {
            LOG.warning("날씨 조회 실패: " + e.getMessage());
            card.setWeatherError(e.getErrorCode());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "날씨 조회 중 예상치 못한 오류", e);
            card.setWeatherError(ErrorCode.UNKNOWN_ERROR);
        }

        // 두 결과를 합쳐 야외활동 지수 판정 (둘 다 실패하면 null)
        AirQualityDTO airForJudge = card.isAirAvailable() ? card.getAir() : null;
        card.setIndex(OutdoorIndexPolicy.judge(airForJudge,
                card.isWeatherAvailable() ? card.getWeather() : null));

        // 시간대별 추이 (실패해도 카드의 나머지는 그대로)
        if (includeHourly) {
            try {
                List<HourlyForecastDTO> hourly = WeatherService.getHourlyForecast(region.getNx(), region.getNy());
                for (HourlyForecastDTO h : hourly) {
                    OutdoorIndexResult r = OutdoorIndexPolicy.judge(airForJudge, toWeather(h));
                    if (r != null) h.setLevel(r.getLevel());
                }
                card.setHourlyForecasts(hourly);
                card.setHourlySummary(summarize(hourly));
            } catch (RuntimeException e) {
                LOG.warning("시간대별 예보 조회 실패 (나머지는 표시): " + e.getMessage());
            }
        }

        card.setSavedDataUsed(DemoStore.wasUsed());
        return card;
    }

    /** 시간별 예보 한 칸을 판정용 날씨로 변환 (미세먼지는 현재 값을 그대로 사용) */
    private static WeatherDTO toWeather(HourlyForecastDTO h) {
        WeatherDTO w = new WeatherDTO();
        w.setTemperature(h.getTemperature());
        w.setWindSpeed(h.getWindSpeed());
        w.setPrecipitation(h.getPrecipitation());
        w.setSky(h.getSky());
        return w;
    }

    /**
     * 시간대별 요약 문구.
     * 예) "야외활동 좋음: 오늘 21시~23시" / "앞으로 12시간 동안 좋음인 시간이 없어요"
     */
    static String summarize(List<HourlyForecastDTO> hourly) {
        if (hourly == null || hourly.isEmpty()) return null;
        List<String> ranges = new ArrayList<>();
        int i = 0;
        while (i < hourly.size()) {
            if (hourly.get(i).getLevel() != ActivityLevel.GOOD) { i++; continue; }
            int j = i;
            while (j + 1 < hourly.size() && hourly.get(j + 1).getLevel() == ActivityLevel.GOOD) j++;
            HourlyForecastDTO a = hourly.get(i), b = hourly.get(j);
            String range = a.getDayLabel() + " " + a.getHourLabel()
                    + (i == j ? "" : "~" + (b.getDayLabel().equals(a.getDayLabel()) ? "" : b.getDayLabel() + " ") + b.getHourLabel());
            ranges.add(range);
            i = j + 1;
        }
        if (ranges.isEmpty()) {
            return "앞으로 " + hourly.size() + "시간 동안 야외활동 '좋음'인 시간이 없어요.";
        }
        return "야외활동 좋음: " + String.join(", ", ranges.subList(0, Math.min(3, ranges.size())));
    }
}
