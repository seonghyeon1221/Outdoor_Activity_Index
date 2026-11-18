package com.outdoorindex.exception;

import com.outdoorindex.enums.ErrorCode;

/**
 * API 서버 연결 실패 또는 타임아웃.
 * - 던지는 곳: HttpClientUtil (A)
 * - ErrorCode는 API_TIMEOUT 또는 API_CONNECTION_FAILED
 */
public class ApiConnectionException extends OutdoorException {
    public ApiConnectionException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }
}
