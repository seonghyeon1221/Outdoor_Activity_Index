package com.outdoorindex.dto;

import com.outdoorindex.enums.ActivityLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * 야외활동 지수 판정 결과.
 * - 만드는 곳: OutdoorIndexPolicy.judge(air, weather) (D)
 * - reasons 예: ["미세먼지 나쁨", "비가 와요", "바람이 강해요"]
 */
public class OutdoorIndexResult {

    private ActivityLevel level;                      // 최종 등급
    private String message;                           // 추천 문구
    private List<String> reasons = new ArrayList<>(); // 감점 사유 (없으면 빈 리스트)
    private boolean airMissing;                       // 미세먼지 정보 없이 판정했으면 true

    public OutdoorIndexResult() {}

    public OutdoorIndexResult(ActivityLevel level, String message, List<String> reasons, boolean airMissing) {
        this.level = level;
        this.message = message;
        this.reasons = reasons == null ? new ArrayList<>() : reasons;
        this.airMissing = airMissing;
    }

    public ActivityLevel getLevel() { return level; }
    public void setLevel(ActivityLevel level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons == null ? new ArrayList<>() : reasons; }
    public boolean isAirMissing() { return airMissing; }
    public void setAirMissing(boolean airMissing) { this.airMissing = airMissing; }
}
