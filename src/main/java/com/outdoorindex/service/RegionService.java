package com.outdoorindex.service;

import com.outdoorindex.dto.RegionDTO;
import com.outdoorindex.exception.NoResultException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 측정소명 → 기상청 격자(nx, ny) 변환 서비스.
 * - 담당: C
 * - 융합 연결 고리: 에어코리아(API ①)가 돌려준 측정소명을 기상청(API ②)이 쓰는 위치로 바꾼다.
 * - 데이터: src/main/resources/station_grid.csv (서버 시작 후 처음 사용할 때 1번만 읽음)
 */
public class RegionService {

    private static final Logger LOG = Logger.getLogger(RegionService.class.getName());
    private static final String CSV_FILE = "station_grid.csv";

    /** 에어코리아 sidoName 값 (검색 화면 드롭다운 순서) */
    private static final List<String> SIDO_LIST = List.of(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주");

    /** key = "시도|측정소명" (대표 지점은 "시도|") */
    private static volatile Map<String, RegionDTO> table;

    /** 검색 화면에 보여줄 시·도 목록 */
    public static List<String> getSidoList() {
        return SIDO_LIST;
    }

    /** 에어코리아가 지원하는 시·도 이름인지 확인 */
    public static boolean isValidSido(String sido) {
        return sido != null && SIDO_LIST.contains(sido.trim());
    }

    /**
     * 측정소명으로 기상청 격자 좌표를 찾는다.
     * @param sido        시·도 ("서울")
     * @param stationName API ①이 돌려준 측정소명 ("중구"). null/빈 값이면 시·도 대표 지점
     * @return 위치 정보. 표에 없는 측정소면 시·도 대표 지점 (stationName = "")
     * @throws NoResultException 시·도가 목록에 없거나 대표 지점도 없을 때
     */
    public RegionDTO findGrid(String sido, String stationName) {
        if (!isValidSido(sido)) {
            throw new NoResultException("지원하지 않는 시·도: " + sido);
        }
        String s = sido.trim();
        Map<String, RegionDTO> t = load();

        if (stationName != null && !stationName.isBlank()) {
            RegionDTO found = t.get(key(s, stationName.trim()));
            if (found != null) {
                return copy(found);
            }
            LOG.info("측정소 위치 표에 없음 → 시·도 대표 지점 사용: " + s + " " + stationName);
        }

        RegionDTO representative = t.get(key(s, ""));
        if (representative == null) {
            throw new NoResultException("시·도 대표 지점이 표에 없음: " + s);
        }
        return copy(representative);
    }

    /** CSV를 읽어 메모리에 보관 (처음 한 번만) */
    private static Map<String, RegionDTO> load() {
        Map<String, RegionDTO> t = table;
        if (t != null) return t;
        synchronized (RegionService.class) {
            if (table != null) return table;
            Map<String, RegionDTO> map = new HashMap<>();
            InputStream in = RegionService.class.getClassLoader().getResourceAsStream(CSV_FILE);
            if (in == null) {
                LOG.severe(CSV_FILE + " 파일이 없습니다.");
                table = Collections.emptyMap();
                return table;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = br.readLine()) != null) {
                    line = line.replace("﻿", "").trim();       // BOM 제거
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (header) { header = false; continue; }        // 첫 줄은 컬럼명
                    String[] c = line.split(",", -1);
                    if (c.length < 4) continue;
                    try {
                        RegionDTO r = new RegionDTO(c[0].trim(), c[1].trim(),
                                Integer.parseInt(c[2].trim()), Integer.parseInt(c[3].trim()));
                        map.put(key(r.getSido(), r.getStationName()), r);
                    } catch (NumberFormatException e) {
                        LOG.warning(CSV_FILE + " 잘못된 줄 건너뜀: " + line);
                    }
                }
            } catch (IOException e) {
                LOG.severe(CSV_FILE + " 읽기 실패: " + e);
            }
            table = Collections.unmodifiableMap(map);
            return table;
        }
    }

    private static String key(String sido, String stationName) {
        return sido + "|" + stationName;
    }

    /** 표의 원본이 바뀌지 않도록 복사본을 돌려줌 */
    private static RegionDTO copy(RegionDTO r) {
        return new RegionDTO(r.getSido(), r.getStationName(), r.getNx(), r.getNy());
    }
}
