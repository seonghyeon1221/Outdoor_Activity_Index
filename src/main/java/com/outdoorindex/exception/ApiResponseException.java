package com.outdoorindex.exception;

import com.outdoorindex.enums.ErrorCode;

/**
 * API 응답의 resultCode가 "00"이 아닐 때 (키 오류, 트래픽 초과, 데이터 없음 등).
 * - 던지는 곳: ApiResponseValidator (A)
 * - ErrorCode는 resultCode에 맞게 API_KEY_ERROR / API_TRAFFIC_EXCEEDED / NO_DATA / API_RESPONSE_ERROR 중 선택
 */
public class ApiResponseException extends OutdoorException {

    private final String resultCode;

    public ApiResponseException(ErrorCode errorCode, String resultCode, String resultMsg) {
        super(errorCode, "resultCode=" + resultCode + ", resultMsg=" + resultMsg, null);
        this.resultCode = resultCode;
    }

    /** API가 돌려준 원본 resultCode */
    public String getResultCode() { return resultCode; }
}
