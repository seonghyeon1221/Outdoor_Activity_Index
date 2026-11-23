package com.outdoorindex.service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.outdoorindex.dto.AirQualityDTO;
import com.outdoorindex.dto.api.AirKoreaResponse;
import com.outdoorindex.exception.ApiParseException;
import com.outdoorindex.exception.NoResultException;
import com.outdoorindex.util.ApiConfig;
import com.outdoorindex.util.ApiResponseValidator;
import com.outdoorindex.util.DustGradeCalculator;
import com.outdoorindex.util.HttpClientUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 에어코리아 대기오염정보(API ①) 조회 서비스.
 * - 담당: C
 * - 사용 기능: /getCtprvnRltmMesureDnsty (시도별 실시간 측정정보)
 * - 흐름: 시·도 → 측정소 목록 + 미세먼지 → 측정소 선택(없으면 시·도 평균)
 * - 여기서 고른 측정소명이 RegionService를 거쳐 기상청(API ②)의 입력이 된다.
 */
public class AirQualityService {

    private static final String PATH_SIDO_REALTIME = "/getCtprvnRltmMesureDnsty";
    private static final int MAX_ROWS = 100;
    private final Gson gson = new Gson();

    /**
     * 시·도 안의 모든 측정소 미세먼지를 조회한다.
     * @param sido 에어코리아 sidoName ("서울")
     * @return 측정소별 미세먼지 목록 (등급 계산 완료)
     * @throws NoResultException 측정소가 0건일 때
     */
    public List<AirQualityDTO> getStations(String sido) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("serviceKey", ApiConfig.getServiceKey());
        params.put("returnType", "json");
        params.put("numOfRows", String.valueOf(MAX_ROWS));
        params.put("pageNo", "1");
        params.put("sidoName", sido);
        params.put("ver", "1.0");

        String json = HttpClientUtil.get(buildUrl(ApiConfig.getAirKoreaEndpoint() + PATH_SIDO_REALTIME, params));

        AirKoreaResponse res;
        try {
            res = gson.fromJson(json, AirKoreaResponse.class);
        } catch (JsonParseException e) {
            throw new ApiParseException("에어코리아 JSON 파싱 실패", e);
        }
        if (res == null || res.response == null || res.response.header == null) {
            throw new ApiParseException("에어코리아 응답 구조가 올바르지 않음", null);
        }
        ApiResponseValidator.validate(res.response.header.resultCode, res.response.header.resultMsg);

        List<AirKoreaResponse.Item> items =
                res.response.body == null ? null : res.response.body.items;
        if (items == null || items.isEmpty()) {
            throw new NoResultException("측정소 0건: " + sido);
        }

        List<AirQualityDTO> list = new ArrayList<>();
        for (AirKoreaResponse.Item item : items) {
            if (item == null || item.stationName == null || item.stationName.isBlank()) continue;
            AirQualityDTO dto = new AirQualityDTO();
            dto.setStationName(item.stationName.trim());
            dto.setPm10(parseValue(item.pm10Value));
            dto.setPm25(parseValue(item.pm25Value));
            dto.setPm10Grade(DustGradeCalculator.pm10Grade(dto.getPm10()));
            dto.setPm25Grade(DustGradeCalculator.pm25Grade(dto.getPm25()));
            dto.setDataTime(item.dataTime);
            dto.setSidoAverage(false);
            list.add(dto);
        }
        if (list.isEmpty()) {
            throw new NoResultException("측정소 0건: " + sido);
        }
        return list;
    }

    /**
     * 목록에서 측정소를 고른다.
     * 1) 이름이 정확히 같은 측정소 → 2) 이름이 포함되는 측정소 → 3) 없으면 시·도 평균
     * @param stations    getStations() 결과
     * @param stationName 사용자가 입력한 측정소/구 이름 (null/빈 값이면 시·도 평균)
     */
    public AirQualityDTO selectStation(List<AirQualityDTO> stations, String stationName) {
        if (stations == null || stations.isEmpty()) {
            return average(Collections.emptyList());
        }
        if (stationName != null && !stationName.isBlank()) {
            String input = stationName.trim();
            for (AirQualityDTO s : stations) {
                if (input.equals(s.getStationName())) return s;
            }
            for (AirQualityDTO s : stations) {
                String name = s.getStationName();
                if (name.contains(input) || input.contains(name)) return s;
            }
        }
        return average(stations);
    }

    /** 측정소 목록에서 이름만 뽑기 (결과 화면 "다른 측정소 보기" 링크용) */
    public List<String> getStationNames(List<AirQualityDTO> stations) {
        List<String> names = new ArrayList<>();
        if (stations == null) return names;
        for (AirQualityDTO s : stations) {
            if (s.getStationName() != null && !names.contains(s.getStationName())) {
                names.add(s.getStationName());
            }
        }
        return names;
    }

    /**
     * 대기질 예보 조회 (확장용 — 이번에는 구조만 준비).
     * 확장 시 /getMinuDustFrcstDspth 를 호출해 내일 미세먼지 예보를 돌려준다.
     */
    public List<String> getDustForecast(String sido) {
        return new ArrayList<>();
    }

    /** 측정값이 있는 측정소들의 평균 (값이 하나도 없으면 null) */
    AirQualityDTO average(List<AirQualityDTO> stations) {
        long pm10Sum = 0, pm25Sum = 0;
        int pm10Count = 0, pm25Count = 0;
        String dataTime = null;
        for (AirQualityDTO s : stations) {
            if (s.getPm10() != null) { pm10Sum += s.getPm10(); pm10Count++; }
            if (s.getPm25() != null) { pm25Sum += s.getPm25(); pm25Count++; }
            if (dataTime == null && s.getDataTime() != null) dataTime = s.getDataTime();
        }
        AirQualityDTO avg = new AirQualityDTO();
        avg.setStationName(null);
        avg.setPm10(pm10Count == 0 ? null : (int) Math.round((double) pm10Sum / pm10Count));
        avg.setPm25(pm25Count == 0 ? null : (int) Math.round((double) pm25Sum / pm25Count));
        avg.setPm10Grade(DustGradeCalculator.pm10Grade(avg.getPm10()));
        avg.setPm25Grade(DustGradeCalculator.pm25Grade(avg.getPm25()));
        avg.setDataTime(dataTime);
        avg.setSidoAverage(true);
        return avg;
    }

    /** "-", 빈 값, null → null / 숫자 문자열 → 정수 */
    static Integer parseValue(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty() || "-".equals(v)) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            try {
                return (int) Math.round(Double.parseDouble(v));
            } catch (NumberFormatException e2) {
                return null;
            }
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
