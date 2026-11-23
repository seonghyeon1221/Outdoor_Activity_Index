package com.outdoorindex.util;

import com.outdoorindex.enums.ErrorCode;
import com.outdoorindex.exception.ApiResponseException;

/**
 * 공공데이터포털 API 응답의 resultCode를 검사하는 클래스.
 * - 담당: A
 * - 사용: AirQualityService(C), WeatherService(B)에서 JSON 파싱 직후 호출
 * - "00"이면 정상, 그 외에는 코드에 맞는 ErrorCode로 ApiResponseException을 던진다.
 */
public final class ApiResponseValidator {

    private ApiResponseValidator() {}

    /**
     * resultCode가 "00"(정상)인지 검사한다.
     * @param resultCode API가 돌려준 결과 코드
     * @param resultMsg  API가 돌려준 결과 메시지 (로그용)
     * @throws ApiResponseException 정상이 아닐 때
     */
    public static void validate(String resultCode, String resultMsg) {
        if (resultCode == null) {
            throw new ApiResponseException(ErrorCode.API_RESPONSE_ERROR, null, "resultCode 없음");
        }
        String code = resultCode.trim();
        if ("00".equals(code)) {
            return;
        }
        throw new ApiResponseException(toErrorCode(code), code, resultMsg);
    }

    /** 공공데이터포털 공통 오류 코드 → 우리 ErrorCode */
    static ErrorCode toErrorCode(String code) {
        switch (code) {
            case "03":                       // NODATA_ERROR
                return ErrorCode.NO_DATA;
            case "20":                       // SERVICE_ACCESS_DENIED_ERROR
            case "30":                       // SERVICE_KEY_IS_NOT_REGISTERED_ERROR
            case "31":                       // DEADLINE_HAS_EXPIRED_ERROR
            case "32":                       // UNREGISTERED_IP_ERROR
            case "33":                       // UNSIGNED_CALL_ERROR
                return ErrorCode.API_KEY_ERROR;
            case "22":                       // LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR
                return ErrorCode.API_TRAFFIC_EXCEEDED;
            default:
                return ErrorCode.API_RESPONSE_ERROR;
        }
    }
}
