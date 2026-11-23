package com.outdoorindex.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.outdoorindex.dto.HourlyForecastDTO;
import com.outdoorindex.dto.WeatherDTO;
import com.outdoorindex.dto.api.KmaResponse;
import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.enums.PrecipitationType;
import com.outdoorindex.enums.SkyCode;
import com.outdoorindex.exception.ApiParseException;
import com.outdoorindex.exception.ApiResponseException;
import com.outdoorindex.exception.OutdoorException;
import com.outdoorindex.util.ApiConfig;
import com.outdoorindex.util.ApiResponseValidator;
import com.outdoorindex.util.BaseTimeCalculator;
import com.outdoorindex.util.HttpClientUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

/**
 * 기상청 단기예보 조회서비스(API ②) 조회 서비스.
 * - 담당: B
 * - 입력 nx, ny는 에어코리아(API ①)가 돌려준 측정소의 위치 (RegionService가 변환)
 * - 초단기실황(getUltraSrtNcst): 기온 T1H, 습도 REH, 강수형태 PTY, 풍속 WSD
 * - 초단기예보(getUltraSrtFcst): 하늘상태 SKY (실황에는 SKY가 없음)
 * - 사용하는 공용 도구: ApiConfig.getServiceKey(), ApiConfig.getKmaEndpoint(),
 *   HttpClientUtil.get(url), ApiResponseValidator.validate(resultCode, resultMsg)
 */
public class WeatherService {

    private static final Logger LOG = Logger.getLogger(WeatherService.class.getName());
    private static final String PATH_NCST = "/getUltraSrtNcst";
    private static final String PATH_FCST = "/getUltraSrtFcst";
    private static final DateTimeFormatter FCST_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final Gson gson = new Gson();

    /**
     * 현재 날씨를 조회한다. (초단기실황 + 초단기예보의 하늘상태)
     * - 실황 조회가 실패하면 예외 → 화면에 날씨 오류 표시
     * - 하늘상태(예보)만 실패하면 sky = UNKNOWN 으로 두고 나머지는 정상 표시
     */
    public WeatherDTO getCurrentWeather(int nx, int ny) {
        LocalDateTime now = BaseTimeCalculator.now();
        WeatherDTO dto = new WeatherDTO();

        // 1) 초단기실황 — 데이터가 아직 없으면 한 단계 이전 발표 시각으로 1회 재시도
        String[] base = BaseTimeCalculator.forUltraSrtNcst(now);
        List<KmaResponse.Item> ncst = fetchOrEmpty(PATH_NCST, base, nx, ny, 10);
        if (ncst.isEmpty()) {
            base = BaseTimeCalculator.forUltraSrtNcst(now.minusHours(1));
            ncst = fetchOrEmpty(PATH_NCST, base, nx, ny, 10);
        }
        if (ncst.isEmpty()) {
            throw new OutdoorException(ErrorCode.NO_DATA, "초단기실황 데이터 없음 nx=" + nx + ", ny=" + ny, null);
        }
        dto.setBaseDate(base[0]);
        dto.setBaseTime(base[1]);
        for (KmaResponse.Item item : ncst) {
            if (item == null || item.category == null) continue;
            switch (item.category) {
                case "T1H": dto.setTemperature(toDouble(item.obsrValue)); break;
                case "REH": dto.setHumidity(toInteger(item.obsrValue)); break;
                case "PTY": dto.setPrecipitation(PrecipitationType.fromCode(item.obsrValue)); break;
                case "WSD": dto.setWindSpeed(toDouble(item.obsrValue)); break;
                default: break;
            }
        }

        // 2) 초단기예보에서 하늘상태(SKY)만 가져오기
        try {
            dto.setSky(getSky(nx, ny, now));
        } catch (OutdoorException e) {
            LOG.warning("하늘상태 조회 실패 (나머지 날씨는 표시): " + e.getMessage());
            dto.setSky(SkyCode.UNKNOWN);
        }
        return dto;
    }

    /**
     * 시간별 예보 (확장용 — 이번에는 구조만 준비, 빈 리스트 반환).
     * 확장 시 BaseTimeCalculator.forVilageFcst()로 발표 시각을 구하고
     * /getVilageFcst 의 TMP, POP, PTY, SKY를 fcstDate+fcstTime별로 묶어 HourlyForecastDTO로 만든다.
     */
    public List<HourlyForecastDTO> getHourlyForecast(int nx, int ny) {
        return new ArrayList<>();
    }

    /** 초단기예보에서 지금과 가장 가까운 시각의 SKY 값 */
    private SkyCode getSky(int nx, int ny, LocalDateTime now) {
        String[] base = BaseTimeCalculator.forUltraSrtFcst(now);
        List<KmaResponse.Item> fcst = fetchOrEmpty(PATH_FCST, base, nx, ny, 60);
        if (fcst.isEmpty()) {
            base = BaseTimeCalculator.forUltraSrtFcst(now.minusHours(1));
            fcst = fetchOrEmpty(PATH_FCST, base, nx, ny, 60);
        }

        KmaResponse.Item best = null;
        long bestDiff = Long.MAX_VALUE;
        for (KmaResponse.Item item : fcst) {
            if (item == null || !"SKY".equals(item.category)) continue;
            LocalDateTime t = parseFcstTime(item.fcstDate, item.fcstTime);
            if (t == null) continue;
            long diff = Math.abs(java.time.Duration.between(now, t).toMinutes());
            if (diff < bestDiff) {
                bestDiff = diff;
                best = item;
            }
        }
        return best == null ? SkyCode.UNKNOWN : SkyCode.fromCode(best.fcstValue);
    }

    /**
     * 기상청 API를 호출해 item 목록을 돌려준다.
     * resultCode "03"(데이터 없음)이면 빈 리스트 → 호출한 쪽에서 이전 발표 시각으로 재시도
     */
    private List<KmaResponse.Item> fetchOrEmpty(String path, String[] base, int nx, int ny, int rows) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("serviceKey", ApiConfig.getServiceKey());
        params.put("pageNo", "1");
        params.put("numOfRows", String.valueOf(rows));
        params.put("dataType", "JSON");
        params.put("base_date", base[0]);
        params.put("base_time", base[1]);
        params.put("nx", String.valueOf(nx));
        params.put("ny", String.valueOf(ny));

        String json = HttpClientUtil.get(buildUrl(ApiConfig.getKmaEndpoint() + path, params));

        KmaResponse res;
        try {
            res = gson.fromJson(json, KmaResponse.class);
        } catch (JsonParseException e) {
            throw new ApiParseException("기상청 JSON 파싱 실패", e);
        }
        if (res == null || res.response == null || res.response.header == null) {
            throw new ApiParseException("기상청 응답 구조가 올바르지 않음", null);
        }
        try {
            ApiResponseValidator.validate(res.response.header.resultCode, res.response.header.resultMsg);
        } catch (ApiResponseException e) {
            if (e.getErrorCode() == ErrorCode.NO_DATA) {
                return Collections.emptyList();
            }
            throw e;
        }
        if (res.response.body == null || res.response.body.items == null
                || res.response.body.items.item == null) {
            return Collections.emptyList();
        }
        return res.response.body.items.item;
    }

    /** 숫자 변환. 기상청 결측값(±900 이상)이나 잘못된 값은 null */
    static Double toDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            double v = Double.parseDouble(raw.trim());
            return Math.abs(v) >= 900 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Integer toInteger(String raw) {
        Double v = toDouble(raw);
        return v == null ? null : (int) Math.round(v);
    }

    private static LocalDateTime parseFcstTime(String date, String time) {
        if (date == null || time == null) return null;
        try {
            return LocalDateTime.parse(date.trim() + time.trim(), FCST_DATETIME);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 파라미터를 UTF-8로 1번만 인코딩해서 URL을 만든다 */
    private static String buildUrl(String base, Map<String, String> params) {
        StringJoiner q = new StringJoiner("&");
        for (Map.Entry<String, String> e : params.entrySet()) {
            q.add(e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return base + "?" + q;
    }
}
