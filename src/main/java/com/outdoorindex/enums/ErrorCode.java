package com.outdoorindex.enums;

/**
 * 오류 종류와 사용자 안내 문구.
 * - 담당: A
 * - 예외를 던질 때 반드시 이 중 하나를 지정 (안내 문구를 각자 따로 쓰지 않기)
 * - JSP 사용 예: ${card.airError.message}
 */
public enum ErrorCode {
    EMPTY_INPUT("시·도를 선택해 주세요."),
    REGION_NOT_FOUND("검색 결과가 없습니다."),
    API_KEY_ERROR("서비스 키에 문제가 있어 정보를 불러오지 못했습니다."),
    API_TRAFFIC_EXCEEDED("오늘 사용할 수 있는 조회 횟수를 초과했습니다. 내일 다시 시도해 주세요."),
    API_RESPONSE_ERROR("정보 제공 기관에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    API_TIMEOUT("응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."),
    API_CONNECTION_FAILED("정보 제공 서버에 연결할 수 없습니다."),
    API_PARSE_ERROR("받아온 정보를 해석하지 못했습니다."),
    NO_DATA("현재 제공되는 데이터가 없습니다."),
    UNKNOWN_ERROR("알 수 없는 오류가 발생했습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    /** 사용자에게 보여줄 안내 문구 */
    public String getMessage() { return message; }
}
