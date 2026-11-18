package com.outdoorindex.exception;

import com.outdoorindex.enums.ErrorCode;

/**
 * 프로젝트의 최상위 예외. 모든 커스텀 예외는 이 클래스를 상속한다.
 * - 담당: A
 * - getErrorCode()로 화면에 보여줄 안내 문구를 꺼낸다.
 */
public class OutdoorException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 오류 코드만으로 생성 (메시지는 ErrorCode의 안내 문구 사용) */
    public OutdoorException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 개발자용 상세 메시지(로그용)와 원인 예외를 함께 지정 */
    public OutdoorException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }

    /** 이 예외의 오류 코드 */
    public ErrorCode getErrorCode() { return errorCode; }
}
