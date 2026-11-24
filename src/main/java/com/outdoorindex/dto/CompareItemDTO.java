package com.outdoorindex.dto;

/**
 * 지역 비교 화면의 한 칸.
 * - 만드는 곳: CompareController
 * - card가 null이면 errorMessage에 이유 (검색 결과 없음 등)
 */
public class CompareItemDTO {

    private String sido;
    private String station;
    private OutdoorCardDTO card;
    private String errorMessage;
    private boolean best;           // 비교한 곳 중 야외활동하기 가장 좋은 곳

    public CompareItemDTO() {}

    public CompareItemDTO(String sido, String station) {
        this.sido = sido;
        this.station = station == null ? "" : station;
    }

    /** 화면 표시용 이름 */
    public String getLabel() {
        if (card != null && card.getRegion() != null) {
            String name = card.getRegion().getDisplayName();
            if (card.isAirAvailable() && !card.getAir().isSidoAverage()
                    && !card.getRegion().isHasStation() && card.getAir().getStationName() != null) {
                name = sido + " " + card.getAir().getStationName();
            }
            return name;
        }
        return station.isBlank() ? sido : sido + " " + station;
    }

    public String getSido() { return sido; }
    public void setSido(String sido) { this.sido = sido; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public OutdoorCardDTO getCard() { return card; }
    public void setCard(OutdoorCardDTO card) { this.card = card; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isBest() { return best; }
    public void setBest(boolean best) { this.best = best; }
}
