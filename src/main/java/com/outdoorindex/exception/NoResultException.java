package com.outdoorindex.exception;

import com.outdoorindex.enums.ErrorCode;

/**
 * 지역 표에서 검색한 지역을 찾지 못했을 때.
 * - 던지는 곳: RegionService (C)
 * - 받는 곳: SearchController (A) → noResult.jsp
 */
public class NoResultException extends OutdoorException {
    public NoResultException(String detail) {
        super(ErrorCode.REGION_NOT_FOUND, detail, null);
    }
}
