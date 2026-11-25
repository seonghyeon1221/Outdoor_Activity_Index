package com.outdoorindex.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.outdoorindex.dto.Card.Air;
import com.outdoorindex.dto.Card.Hourly;
import com.outdoorindex.dto.Card.Weather;
import com.outdoorindex.util.Api;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** API ② 기상청 단기예보: 격자(nx, ny) → 현재 날씨 + 시간대별 예보 */
public class WeatherService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String[] RAIN = {"없음", "비", "비/눈", "눈", "소나기", "빗방울", "빗방울눈날림", "눈날림"};
    private static final int HOURS = 12;

    /**
     * 현재 날씨. 초단기실황(매시 40분 발표)에서 기온·습도·강수·풍속,
     * 초단기예보(매시 45분 발표)에서 하늘 상태를 받는다. 아직 발표 전이면 한 시간 전 자료.
     */
    public Weather current(int nx, int ny) {
        LocalDateTime now = now();
        LocalDateTime obs = now.minusMinutes(40).truncatedTo(ChronoUnit.HOURS);   // 40분 전이면 한 시간 전 정각
        JsonArray items = call("getUltraSrtNcst", obs, nx, ny);
        if (items.isEmpty()) items = call("getUltraSrtNcst", obs = obs.minusHours(1), nx, ny);   // 자료가 없으면 한 시간 더 앞
        if (items.isEmpty()) throw new RuntimeException("기상청 관측 자료가 아직 발표되지 않았습니다. 잠시 후 다시 시도해 주세요.");

        Map<String, String> v = new LinkedHashMap<>();
        for (JsonElement it : items) v.put(Api.text(it, "category"), Api.text(it, "obsrValue"));

        String sky = "정보 없음";
        try {   // 하늘 상태만 실패하면 나머지 날씨는 그대로 보여준다
            LocalDateTime fc = now.minusMinutes(45).truncatedTo(ChronoUnit.HOURS).plusMinutes(30);   // 초단기예보 발표 시각 (HH:30)
            for (JsonElement it : call("getUltraSrtFcst", fc, nx, ny)) {
                if ("SKY".equals(Api.text(it, "category"))) { sky = sky(Api.text(it, "fcstValue")); break; }
            }
        } catch (RuntimeException ignored) { }

        Double hum = Api.number(v.get("REH"));
        return new Weather(Api.number(v.get("T1H")), hum == null ? null : (int) Math.round(hum),
                Api.number(v.get("WSD")), rain(v.get("PTY")), sky, obs.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    /**
     * 앞으로 12시간 예보 (단기예보: 02·05·…·23시 발표, 10분 뒤부터 조회 가능).
     * 시간마다 그 시간의 날씨 + 현재 미세먼지(air)로 야외활동 지수를 판정해 둔다.
     */
    public List<Hourly> hourly(int nx, int ny, Air air) {
        LocalDateTime now = now(), base = now.minusMinutes(10).truncatedTo(ChronoUnit.HOURS);
        while ((base.getHour() + 1) % 3 != 0) base = base.minusHours(1);   // 02·05·08·…·23시 중 가장 최근
        JsonArray items = call("getVilageFcst", base, nx, ny);
        if (items.isEmpty()) items = call("getVilageFcst", base.minusHours(3), nx, ny);

        Map<String, Map<String, String>> byTime = new TreeMap<>();   // "yyyyMMddHHmm" → {TMP: 3, POP: 20, ...}
        for (JsonElement it : items) {
            String time = Api.text(it, "fcstDate") + Api.text(it, "fcstTime");
            byTime.putIfAbsent(time, new LinkedHashMap<>());
            byTime.get(time).put(Api.text(it, "category"), Api.text(it, "fcstValue"));
        }

        List<Hourly> list = new ArrayList<>();
        for (var e : byTime.entrySet()) {
            LocalDateTime t = LocalDateTime.parse(e.getKey(), FMT);
            Map<String, String> v = e.getValue();
            Double temp = Api.number(v.get("TMP"));
            if (!t.isAfter(now) || temp == null) continue;  // 지난 시간과 기온이 없는 시간은 건너뜀
            long d = ChronoUnit.DAYS.between(now.toLocalDate(), t.toLocalDate());
            Double pop = Api.number(v.get("POP"));
            Weather w = new Weather(temp, null, Api.number(v.get("WSD")), rain(v.get("PTY")), sky(v.get("SKY")), null);
            list.add(new Hourly(d == 0 ? "오늘" : d == 1 ? "내일" : "모레", t.getHour() + "시", temp,
                    pop == null ? null : pop.intValue(), w.rain(), w.sky(), IndexPolicy.judge(air, w).level()));
            if (list.size() == HOURS) break;                // 12칸까지만
        }
        return list;
    }

    /** 기상청 공통 요청 (발표 일시 + 격자 좌표) */
    private JsonArray call(String api, LocalDateTime base, int nx, int ny) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("dataType", "JSON");
        p.put("numOfRows", 1000);
        p.put("pageNo", 1);
        p.put("base_date", base.format(DateTimeFormatter.BASIC_ISO_DATE));
        p.put("base_time", base.format(DateTimeFormatter.ofPattern("HHmm")));
        p.put("nx", nx);
        p.put("ny", ny);
        return Api.items(Api.KMA + "/" + api, p);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /** 강수형태(PTY) 코드 → 글자 */
    private static String rain(String code) {
        Double c = Api.number(code);
        return c == null || c < 0 || c >= RAIN.length ? "없음" : RAIN[c.intValue()];
    }

    /** 하늘상태(SKY) 코드 → 글자 */
    private static String sky(String code) {
        return "1".equals(code) ? "맑음" : "3".equals(code) ? "구름많음" : "4".equals(code) ? "흐림" : "정보 없음";
    }
}
