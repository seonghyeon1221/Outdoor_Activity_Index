package com.outdoorindex.service;

import com.outdoorindex.dto.Card;
import com.outdoorindex.dto.Card.Air;
import com.outdoorindex.dto.Card.Compare;
import com.outdoorindex.dto.Card.Hourly;
import com.outdoorindex.dto.Card.Result;
import com.outdoorindex.dto.Card.Weather;
import com.outdoorindex.service.IndexPolicy.Level;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 두 API를 이어 붙여 결과 카드를 만드는 서비스 (융합의 중심).
 * 시·도(검색어) → ① 에어코리아 → 측정소명 → station_grid.csv → 격자(nx, ny) → ② 기상청 → 지수 판정
 */
public class CardService {

    public static final List<String> SIDO = List.of("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주");

    /** "시도|측정소명" → {nx, ny}. 측정소명이 빈 줄은 시·도 대표 지점 */
    private static final Map<String, int[]> GRID = new HashMap<>();

    static {
        var in = CardService.class.getClassLoader().getResourceAsStream("station_grid.csv");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            br.readLine();                                         // 첫 줄은 컬럼명
            // "# 시도" 처럼 4칸이 아닌 줄은 시·도 구분용이라 건너뜀
            for (String line; (line = br.readLine()) != null; ) {
                String[] c = line.replace("\uFEFF", "").split(",", -1);
                if (c.length == 4) GRID.put(c[0] + "|" + c[1], new int[] {Integer.parseInt(c[2]), Integer.parseInt(c[3])});
            }
        } catch (Exception e) {
            throw new IllegalStateException("station_grid.csv 를 읽을 수 없습니다.", e);
        }
    }

    private final AirService airService = new AirService();
    private final WeatherService weatherService = new WeatherService();

    /**
     * 결과 카드 만들기. 한쪽 API가 실패해도 다른 쪽 결과로 카드를 만든다.
     * @param withHourly 시간대별 예보까지 받을지 (지역 비교에서는 false)
     * @throws IllegalArgumentException 지원하지 않는 시·도 (검색 결과 없음)
     */
    public Card card(String sido, String station, boolean withHourly) {
        if (!SIDO.contains(sido)) throw new IllegalArgumentException("지원하지 않는 시·도입니다: " + sido);

        // ① 에어코리아: 시·도 → 측정소 목록 → 측정소 선택 (실패하면 airError 에 이유를 남김)
        Air air = null;
        String airError = null;
        List<String> stations = List.of();
        try {
            List<Air> list = airService.stations(sido);
            stations = airService.names(list);
            air = airService.select(list, station);
        } catch (RuntimeException e) {
            airError = e.getMessage();
        }

        // 연결: 측정소명 → 격자 좌표 (표에 없거나 시·도 평균이면 시·도 대표 지점)
        int[] grid = air == null || air.average() ? null : GRID.get(sido + "|" + air.station());
        boolean gridFound = grid != null;
        if (grid == null) grid = GRID.get(sido + "|");

        // ② 기상청: 같은 위치의 날씨
        Weather weather = null;
        String weatherError = null;
        try {
            weather = weatherService.current(grid[0], grid[1]);
        } catch (RuntimeException e) {
            weatherError = e.getMessage();
        }

        List<Hourly> hourly = List.of();
        if (withHourly) {
            try {
                hourly = weatherService.hourly(grid[0], grid[1], air);
            } catch (RuntimeException ignored) { }       // 예보만 실패하면 나머지는 그대로
        }

        Result result = IndexPolicy.judge(air, weather);
        return new Card(sido, air, airError, weather, weatherError, grid[0], grid[1], gridFound,
                result, hourly, summary(hourly), stations);
    }

    /** 여러 지역 비교. 2곳 이상이면 가장 좋은 곳(지수 → 미세먼지 등급 → 감점 수 순)에 best 표시 */
    public List<Compare> compare(String[] sidos, String[] stations) {
        List<Card> cards = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int best = -1, bestScore = Integer.MAX_VALUE, valid = 0;
        for (int i = 0; i < sidos.length; i++) {
            Card c = null;
            String err = null;
            try {
                c = card(sidos[i], stations[i], false);
            } catch (RuntimeException e) {
                err = e.getMessage();
            }
            cards.add(c);
            errors.add(err);
            if (c == null || c.result() == null) continue;
            valid++;
            Result r = c.result();
            int score = r.level().ordinal() * 100 + r.dust().ordinal() * 10 + r.penalty();   // 작을수록 좋음
            if (score < bestScore) { bestScore = score; best = i; }
        }
        List<Compare> list = new ArrayList<>();
        for (int i = 0; i < sidos.length; i++) {
            list.add(new Compare(sidos[i], stations[i], cards.get(i), errors.get(i), valid >= 2 && i == best));
        }
        return list;
    }

    /** 시간대별 요약: "야외활동 좋음: 오늘 15시~18시, 내일 9시" */
    private static String summary(List<Hourly> hourly) {
        if (hourly.isEmpty()) return null;
        List<String> ranges = new ArrayList<>();
        for (int i = 0; i < hourly.size(); i++) {           // "좋음"이 이어지는 구간 [i, j] 찾기
            if (hourly.get(i).level() != Level.GOOD) continue;
            int j = i;
            while (j + 1 < hourly.size() && hourly.get(j + 1).level() == Level.GOOD) j++;
            Hourly a = hourly.get(i), b = hourly.get(j);
            ranges.add(a.day() + " " + a.hour() + (i == j ? "" : "~" + (a.day().equals(b.day()) ? "" : b.day() + " ") + b.hour()));
            i = j;
        }
        return ranges.isEmpty() ? "앞으로 " + hourly.size() + "시간 동안 '좋음'인 시간이 없어요."
                : "야외활동 좋음: " + String.join(", ", ranges.subList(0, Math.min(3, ranges.size())));
    }
}
