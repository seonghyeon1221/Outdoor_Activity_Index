package com.outdoorindex.service;

import com.google.gson.JsonElement;
import com.outdoorindex.dto.Card.Air;
import com.outdoorindex.util.Api;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** API ① 에어코리아: 시·도 → 측정소별 미세먼지 → 측정소 하나 고르기 */
public class AirService {

    private static final int ROWS = 100;

    /** 시·도의 모든 측정소 (측정소가 100곳 넘는 경기 등은 다음 페이지도 받음) */
    public List<Air> stations(String sido) {
        List<Air> list = new ArrayList<>();
        for (int page = 1; page <= 5; page++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("returnType", "json");
            p.put("numOfRows", ROWS);
            p.put("pageNo", page);
            p.put("sidoName", sido);
            p.put("ver", "1.0");
            var items = Api.items(Api.AIRKOREA + "/getCtprvnRltmMesureDnsty", p);
            for (JsonElement it : items) {
                if (Api.text(it, "stationName") == null) continue;
                Integer pm10 = toInt(Api.text(it, "pm10Value")), pm25 = toInt(Api.text(it, "pm25Value"));
                String note = pm10 == null || pm25 == null ? Api.text(it, pm10 == null ? "pm10Flag" : "pm25Flag") : null;
                list.add(new Air(Api.text(it, "stationName"), pm10, pm25, Api.text(it, "dataTime"),
                        note, false, false, 1));
            }
            if (items.size() < ROWS) break;                 // 마지막 페이지면 그만
        }
        return list;
    }

    /**
     * 측정소 고르기: 이름이 같은 곳 → 이름이 포함되는 곳 → 없으면 시·도 평균.
     * 고른 측정소에 값이 비어 있으면(통신장애 등) 그 값만 시·도 평균으로 채운다.
     */
    public Air select(List<Air> list, String input) {
        Air avg = average(list);
        if (input == null || input.isBlank()) return avg;
        Air found = find(list, s -> s.station().equals(input));   // 1) 이름이 같은 측정소
        // 2) 이름이 포함되는 측정소 (두 글자 이상일 때만 거꾸로도 비교)
        if (found == null) found = find(list, s -> s.station().contains(input) || (s.station().length() >= 2 && input.contains(s.station())));
        if (found == null) return avg;
        if (found.pm10() != null && found.pm25() != null) return found;   // 값이 모두 있으면 그대로
        return new Air(found.station(),
                found.pm10() != null ? found.pm10() : avg.pm10(),
                found.pm25() != null ? found.pm25() : avg.pm25(),
                found.dataTime(), found.note(), false, true, 1);
    }

    /** 측정소 이름 목록: 구·군·시 이름 측정소 먼저, 나머지(도로변 등) 뒤에, 가나다순 */
    public List<String> names(List<Air> list) {
        Collator ko = Collator.getInstance(Locale.KOREAN);
        return list.stream().map(Air::station).distinct()
                .sorted(Comparator.comparing((String n) -> !n.matches(".+[구군시]")).thenComparing(ko::compare))
                .toList();
    }

    /** 값이 있는 측정소들의 평균 */
    private Air average(List<Air> list) {
        double s10 = 0, s25 = 0;
        int n10 = 0, n25 = 0;
        String time = null;
        for (Air a : list) {
            if (a.pm10() != null) { s10 += a.pm10(); n10++; }
            if (a.pm25() != null) { s25 += a.pm25(); n25++; }
            if (a.dataTime() != null && (time == null || a.dataTime().compareTo(time) > 0)) time = a.dataTime();
        }
        return new Air(null, n10 == 0 ? null : (int) Math.round(s10 / n10), n25 == 0 ? null : (int) Math.round(s25 / n25),
                time, null, true, false, list.size());
    }

    /** 조건에 맞는 측정소 중 값이 있는 곳을 먼저 (모두 비어 있으면 첫 번째) */
    private static Air find(List<Air> list, java.util.function.Predicate<Air> match) {
        Air first = null;
        for (Air a : list) {
            if (a.station() == null || !match.test(a)) continue;
            if (a.pm10() != null || a.pm25() != null) return a;
            if (first == null) first = a;
        }
        return first;
    }

    private static Integer toInt(String v) {
        Double d = Api.number(v);
        return d == null ? null : (int) Math.round(d);
    }
}
