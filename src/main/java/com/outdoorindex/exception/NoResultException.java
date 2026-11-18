package com.outdoorindex.exception;

import com.outdoorindex.enums.ErrorCode;

/**
 * 검색 결과가 없을 때 (시·도 값이 목록에 없거나, API ①이 돌려준 측정소가 0건).
 * - 던지는 곳: AirQualityService, RegionService (C)
 * - 받는 곳: SearchController (A) → noResult.jsp
 */
public class NoResultException extends OutdoorException {
    public NoResultException(String detail) {
        super(ErrorCode.REGION_NOT_FOUND, detail, null);
    }
}
