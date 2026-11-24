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
    /** 경기처럼 측정소가 100곳을 넘는 시·도를 위해 최대 몇 페이지까지 받을지 */
    private static final int MAX_PAGES = 5;
    private final Gson gson = new Gson();

    /**
     * 시·도 안의 모든 측정소 미세먼지를 조회한다. (측정소가 100곳을 넘으면 다음 페이지도 받는다)
     * @param sido 에어코리아 sidoName ("서울")
     * @return 측정소별 미세먼지 목록 (등급 계산 완료)
     * @throws NoResultException 측정소가 0건일 때
     */
    public List<AirQualityDTO> getStations(String sido) {
        List<AirQualityDTO> list = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            AirKoreaResponse res = fetchPage(sido, page);
            List<AirKoreaResponse.Item> items =
                    res.response.body == null ? null : res.response.body.items;
            if (items == null || items.isEmpty()) {
                break;
            }
            for (AirKoreaResponse.Item item : items) {
                if (item == null || item.stationName == null || item.stationName.isBlank()) continue;
                list.add(toDto(item));
            }
            if (res.response.body.totalCount <= page * MAX_ROWS) {
                break;                                  // 마지막 페이지
            }
        }
        if (list.isEmpty()) {
            throw new NoResultException("측정소 0건: " + sido);
        }
        return list;
    }

    /**
     * 목록에서 측정소를 고른다.
     * 1) 이름이 정확히 같은 측정소 → 2) 이름이 포함되는 측정소 → 3) 없으면 시·도 평균
     * 후보가 여러 곳이면 측정값이 있는 측정소를 먼저 고른다.
     * @param stations    getStations() 결과
     * @param stationName 사용자가 입력한 측정소/구 이름 (null/빈 값이면 시·도 평균)
     */
    public AirQualityDTO selectStation(List<AirQualityDTO> stations, String stationName) {
        if (stations == null || stations.isEmpty()) {
            return average(Collections.emptyList());
        }
        if (stationName != null && !stationName.isBlank()) {
            String input = stationName.trim();

            AirQualityDTO exact = firstWithValue(stations, input, true);
            if (exact != null) return fillMissing(exact, stations);
            AirQualityDTO partial = firstWithValue(stations, input, false);
            if (partial != null) return fillMissing(partial, stations);
        }
        return average(stations);
    }

    /**
     * 측정소 목록에서 이름만 뽑기 (결과 화면 "다른 측정소 보기" 링크용).
     * 구·군·시 이름 측정소(도시대기)를 앞에, 도로변·기타 측정소를 뒤에, 각각 가나다순으로 정렬
     */
    public List<String> getStationNames(List<AirQualityDTO> stations) {
        List<String> main = new ArrayList<>();
        List<String> others = new ArrayList<>();
        if (stations == null) return main;
        for (AirQualityDTO s : stations) {
            String n = s.getStationName();
            if (n == null || main.contains(n) || others.contains(n)) continue;
            if (n.matches(".+[구군시]")) main.add(n); else others.add(n);
        }
        java.text.Collator ko = java.text.Collator.getInstance(java.util.Locale.KOREAN);
        main.sort(ko);
        others.sort(ko);
        main.addAll(others);
        return main;
    }

    /**
     * 대기질 예보 조회 (확장용 — 이번에는 구조만 준비).
     * 확장 시 /getMinuDustFrcstDspth 를 호출해 내일 미세먼지 예보를 돌려준다.
     */
    public List<String> getDustForecast(String sido) {
        return new ArrayList<>();
    }

    /** 측정값이 있는 측정소들의 평균 (값이 하나도 없으면 수치는 null) */
    AirQualityDTO average(List<AirQualityDTO> stations) {
        long pm10Sum = 0, pm25Sum = 0;
        int pm10Count = 0, pm25Count = 0;
        String dataTime = null;
        for (AirQualityDTO s : stations) {
            boolean hasValue = false;
            if (s.getPm10() != null) { pm10Sum += s.getPm10(); pm10Count++; hasValue = true; }
            if (s.getPm25() != null) { pm25Sum += s.getPm25(); pm25Count++; hasValue = true; }
            // "yyyy-MM-dd HH:mm" 형식이라 문자열 비교로 가장 최근 시각을 고를 수 있다
            if (hasValue && s.getDataTime() != null
                    && (dataTime == null || s.getDataTime().compareTo(dataTime) > 0)) {
                dataTime = s.getDataTime();
            }
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

    /**
     * 고른 측정소에 PM10/PM2.5 값이 없으면(통신장애·점검 등) 그 값만 시·도 평균으로 채운다.
     * 측정소 이름은 그대로 두므로 측정소 → 격자 좌표 → 기상청 연결은 유지된다.
     */
    AirQualityDTO fillMissing(AirQualityDTO station, List<AirQualityDTO> stations) {
        if (station.getPm10() != null && station.getPm25() != null) return station;
        AirQualityDTO avg = average(stations);
        boolean filled = false;
        if (station.getPm10() == null && avg.getPm10() != null) {
            station.setPm10(avg.getPm10());
            station.setPm10Grade(avg.getPm10Grade());
            filled = true;
        }
        if (station.getPm25() == null && avg.getPm25() != null) {
            station.setPm25(avg.getPm25());
            station.setPm25Grade(avg.getPm25Grade());
            filled = true;
        }
        station.setFilledFromAverage(filled);
        return station;
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

    // ───────────── 내부 처리 ─────────────

    /** 한 페이지를 요청해서 JSON 해석 + resultCode 검사까지 마친 응답을 돌려준다 */
    private AirKoreaResponse fetchPage(String sido, int pageNo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("serviceKey", ApiConfig.getServiceKey());
        params.put("returnType", "json");
        params.put("numOfRows", String.valueOf(MAX_ROWS));
        params.put("pageNo", String.valueOf(pageNo));
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
        return res;
    }

    /** API 항목 1개를 AirQualityDTO로 바꾼다 (등급 계산 포함) */
    private static AirQualityDTO toDto(AirKoreaResponse.Item item) {
        AirQualityDTO dto = new AirQualityDTO();
        dto.setStationName(item.stationName.trim());
        dto.setPm10(parseValue(item.pm10Value));
        dto.setPm25(parseValue(item.pm25Value));
        dto.setPm10Grade(DustGradeCalculator.pm10Grade(dto.getPm10()));
        dto.setPm25Grade(DustGradeCalculator.pm25Grade(dto.getPm25()));
        dto.setDataTime(item.dataTime);
        dto.setSidoAverage(false);
        // 값이 비어 있으면 에어코리아가 준 이유(통신장애, 장비점검 등)를 함께 보관
        if (dto.getPm10() == null || dto.getPm25() == null) {
            String flag = dto.getPm10() == null ? item.pm10Flag : item.pm25Flag;
            if (flag == null || flag.isBlank()) flag = item.pm10Flag != null ? item.pm10Flag : item.pm25Flag;
            dto.setStatusNote(flag == null || flag.isBlank() ? null : flag.trim());
        }
        return dto;
    }

    /**
     * 입력과 이름이 맞는 측정소 중 하나를 고른다. 측정값이 있는 곳을 먼저, 모두 비어 있으면 첫 후보.
     * @param exact true면 이름이 정확히 같은 곳만, false면 이름에 입력이 포함된 곳(또는 반대)
     * @return 없으면 null
     */
    private static AirQualityDTO firstWithValue(List<AirQualityDTO> stations, String input, boolean exact) {
        AirQualityDTO firstMatch = null;
        for (AirQualityDTO s : stations) {
            String name = s.getStationName();
            if (name == null) continue;
            boolean match = exact
                    ? input.equals(name)
                    : name.contains(input) || (name.length() >= 2 && input.contains(name));
            if (!match) continue;
            if (!s.isEmpty()) return s;
            if (firstMatch == null) firstMatch = s;
        }
        return firstMatch;
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
