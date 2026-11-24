
package com.outdoorindex.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.outdoorindex.dto.WeatherDTO;
import com.outdoorindex.dto.HourlyForecastDTO;
import com.outdoorindex.dto.api.KmaResponse;
import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.enums.PrecipitationType;
import com.outdoorindex.enums.SkyCode;
import com.outdoorindex.exception.ApiParseException;
import com.outdoorindex.exception.OutdoorException;
import com.outdoorindex.util.ApiConfig;
import com.outdoorindex.util.HttpClientUtil;
import com.outdoorindex.util.ApiResponseValidator;
import com.outdoorindex.util.BaseTimeCalculator;
import com.outdoorindex.util.BaseTimeCalculator.BaseTime;

/**
 * 기상청에서 현재 날씨를 받아 WeatherDTO에 담는 서비스이다. (담당: B)
 * - 현재 날씨: 초단기실황(기온·습도·강수형태·풍속) + 초단기예보(하늘 상태)
 * - 시간대별 추이: 단기예보(getVilageFcst) 12시간
 */
public class WeatherService {

    private static final Gson GSON = new Gson();
    private static final Logger LOG = Logger.getLogger(WeatherService.class.getName());

    /** 위치 번호(nx, ny)를 받아 현재 날씨를 조회한다. */
    public static WeatherDTO getCurrentWeather(int nx, int ny) {
        if (nx <= 0 || ny <= 0)
            throw new OutdoorException(ErrorCode.REGION_NOT_FOUND);

        BaseTime observationTime = BaseTimeCalculator.forObservation();
        BaseTime forecastTime = BaseTimeCalculator.forForecast();

        KmaResponse observation = fetch("getUltraSrtNcst", observationTime, nx, ny);

        if (items(observation).isEmpty()) {
            observationTime = BaseTimeCalculator.previous(observationTime);
            observation = fetch("getUltraSrtNcst", observationTime, nx, ny);
        }

        if (items(observation).isEmpty())
            throw new OutdoorException(ErrorCode.NO_DATA);

        // 하늘 상태(초단기예보)만 실패하면 sky는 "정보 없음"으로 두고 나머지 날씨는 그대로 보여준다.
        KmaResponse forecast = null;
        try {
            forecast = fetch("getUltraSrtFcst", forecastTime, nx, ny);

            if (items(forecast).isEmpty() || !hasSky(forecast)) {
                forecastTime = BaseTimeCalculator.previous(forecastTime);
                forecast = fetch("getUltraSrtFcst", forecastTime, nx, ny);
            }
        } catch (OutdoorException e) {
            LOG.warning("하늘 상태 조회 실패 (나머지 날씨는 표시): " + e.getMessage());
        }

        WeatherDTO weather = new WeatherDTO();
        weather.setBaseDate(observationTime.getBaseDate());
        weather.setBaseTime(observationTime.getBaseTime());

        for (KmaResponse.Item item : items(observation)) {
            if (item == null || item.category == null)
                continue;

            String value = item.obsrValue;

            switch (item.category) {
                case "T1H":
                    weather.setTemperature(toDouble(value));
                    break;
                case "REH":
                    weather.setHumidity(toInteger(value));
                    break;
                case "WSD":
                    weather.setWindSpeed(toDouble(value));
                    break;
                case "PTY":
                    weather.setPrecipitation(PrecipitationType.fromCode(value));
                    break;
            }
        }

        weather.setSky(findSky(forecast));
        return weather;
    }

    /** 시간대별 추이에 보여줄 시간 수 */
    public static final int HOURLY_COUNT = 12;

    /**
     * 앞으로 HOURLY_COUNT시간의 시간별 예보를 조회한다. (단기예보 getVilageFcst)
     * TMP(기온), POP(강수확률), PTY(강수형태), SKY(하늘), WSD(풍속)를 예보 시각별로 묶는다.
     * 시간별 야외활동 지수(level)는 OutdoorCardService가 채운다.
     */
    public static List<HourlyForecastDTO> getHourlyForecast(int nx, int ny) {
        if (nx <= 0 || ny <= 0)
            return Collections.emptyList();

        BaseTime villageTime = BaseTimeCalculator.forVillage();
        KmaResponse village = fetch("getVilageFcst", villageTime, nx, ny);

        if (items(village).isEmpty()) {
            villageTime = BaseTimeCalculator.previousVillage(villageTime);
            village = fetch("getVilageFcst", villageTime, nx, ny);
        }

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        Map<String, HourlyForecastDTO> byTime = new TreeMap<>();

        for (KmaResponse.Item item : items(village)) {
            if (item == null || item.category == null || item.fcstDate == null || item.fcstTime == null)
                continue;

            String timeKey = item.fcstDate + item.fcstTime;
            HourlyForecastDTO hour = byTime.get(timeKey);
            if (hour == null) {
                hour = new HourlyForecastDTO();
                hour.setFcstDate(item.fcstDate);
                hour.setFcstTime(item.fcstTime);
                byTime.put(timeKey, hour);
            }

            switch (item.category) {
                case "TMP":
                    hour.setTemperature(toDouble(item.fcstValue));
                    break;
                case "POP":
                    hour.setRainProbability(toInteger(item.fcstValue));
                    break;
                case "PTY":
                    hour.setPrecipitation(PrecipitationType.fromCode(item.fcstValue));
                    break;
                case "SKY":
                    hour.setSky(SkyCode.fromCode(item.fcstValue));
                    break;
                case "WSD":
                    hour.setWindSpeed(toDouble(item.fcstValue));
                    break;
            }
        }

        LocalDateTime now = BaseTimeCalculator.now();
        LocalDateTime currentHour = now.withMinute(0).withSecond(0).withNano(0);
        List<HourlyForecastDTO> result = new ArrayList<>();

        for (HourlyForecastDTO hour : byTime.values()) {
            LocalDateTime when;
            try {
                when = LocalDateTime.parse(hour.getFcstDate() + hour.getFcstTime(), format);
            } catch (RuntimeException e) {
                continue;
            }
            // 지난 시각이나 기온이 없는 칸은 건너뛴다.
            if (!when.isAfter(currentHour) || hour.getTemperature() == null)
                continue;

            long days = ChronoUnit.DAYS.between(now.toLocalDate(), when.toLocalDate());
            hour.setDayLabel(days == 0 ? "오늘" : days == 1 ? "내일" : "모레");
            result.add(hour);

            if (result.size() >= HOURLY_COUNT)
                break;
        }
        return result;
    }

    // 공통 유틸리티를 사용하여 기상청 API에 날씨 정보를 요청한다.
    private static KmaResponse fetch(String apiName, BaseTime time, int nx, int ny) {
        String key = ApiConfig.getServiceKey();
        String endpoint = ApiConfig.getKmaEndpoint();

        if (key == null || key.isBlank() || endpoint == null || endpoint.isBlank())
            throw new OutdoorException(ErrorCode.API_KEY_ERROR);

        String url = endpoint.replaceAll("/+$", "") + "/" + apiName
                + "?serviceKey=" + encode(key)
                + "&dataType=JSON&numOfRows=1000&pageNo=1"
                + "&base_date=" + time.getBaseDate()
                + "&base_time=" + time.getBaseTime()
                + "&nx=" + nx
                + "&ny=" + ny;

        String json = HttpClientUtil.get(url);

        if (json == null || !json.stripLeading().startsWith("{"))
            throw new ApiParseException("기상청 응답이 JSON 형식이 아닙니다.", null);

        KmaResponse response;

        try {
            response = GSON.fromJson(json, KmaResponse.class);
        } catch (JsonParseException e) {
            throw new ApiParseException("날씨 데이터 해석에 실패했습니다.", e);
        }

        if (response == null || response.response == null || response.response.header == null)
            throw new ApiParseException("날씨 데이터 구조가 올바르지 않습니다.", null);

        // API 응답 코드를 확인하고 데이터가 없는 경우 재조회할 수 있도록 처리한다.
        String code = response.response.header.resultCode;

        if ("03".equals(code))
            return null;

        ApiResponseValidator.validate(code, response.response.header.resultMsg);

        return response;
    }

    // API 응답에서 날씨 데이터 목록을 추출한다.
    private static List<KmaResponse.Item> items(KmaResponse data) {
        if (data == null || data.response == null || data.response.body == null
                || data.response.body.items == null || data.response.body.items.item == null)
            return Collections.emptyList();

        return data.response.body.items.item;
    }

    // 초단기예보에 하늘 상태(SKY) 데이터가 존재하는지 확인한다.
    private static boolean hasSky(KmaResponse data) {
        for (KmaResponse.Item item : items(data)) {
            if (item != null && "SKY".equals(item.category)
                    && hasValue(item.fcstValue))
                return true;
        }

        return false;
    }

    // 현재 시각과 가장 가까운 하늘 상태를 찾아 반환한다.
    private static SkyCode findSky(KmaResponse data) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        long shortest = Long.MAX_VALUE;
        String sky = null;

        for (KmaResponse.Item item : items(data)) {
            if (item == null || !"SKY".equals(item.category)
                    || !hasValue(item.fcstValue))
                continue;

            try {
                LocalDateTime when = LocalDateTime.parse(
                        item.fcstDate + item.fcstTime, format);

                long minutes = Math.abs(Duration.between(now, when).toMinutes());

                if (minutes < shortest) {
                    shortest = minutes;
                    sky = item.fcstValue;
                }
            } catch (RuntimeException ignored) {
                // 날짜 형식이 올바르지 않은 예보 항목은 제외한다.
            }
        }

        return SkyCode.fromCode(sky);
    }

    // 값이 비어 있거나 "-"이면 유효하지 않은 데이터로 처리한다.
    private static boolean hasValue(String value) {
        return value != null && !value.isBlank()
                && !"-".equals(value.trim());
    }

    // 문자열로 전달된 기온과 풍속을 실수형으로 변환한다. 기상청 결측값(±900 이상)은 값 없음으로 처리한다.
    private static Double toDouble(String value) {
        try {
            if (!hasValue(value)) return null;
            double v = Double.parseDouble(value.trim());
            return Math.abs(v) >= 900 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 문자열로 전달된 습도·강수확률을 정수형으로 변환한다.
    private static Integer toInteger(String value) {
        Double v = toDouble(value);
        return v == null ? null : (int) Math.round(v);
    }

    // API 인증키를 UTF-8 방식으로 URL 인코딩한다.
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
