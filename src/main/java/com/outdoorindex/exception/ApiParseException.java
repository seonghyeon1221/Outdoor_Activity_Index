package com.outdoorindex.exception;

import com.outdoorindex.enums.ErrorCode;

/**
 * JSON 파싱 실패, 또는 JSON 대신 XML 오류 응답이 왔을 때.
 * - 던지는 곳: HttpClientUtil (A), 각 Service의 Gson 변환 부분 (B, C)
 */
public class ApiParseException extends OutdoorException {

    private static final long serialVersionUID = 1L;
    public ApiParseException(String detail, Throwable cause) {
        super(ErrorCode.API_PARSE_ERROR, detail, cause);
    }
}
