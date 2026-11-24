package com.outdoorindex.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 응답을 잠깐 저장해 두는 메모리 캐시. (같은 요청을 짧은 시간에 반복하지 않도록)
 * - 담당: A
 * - 키: 서비스키를 뺀 요청 주소, 값: 응답 JSON
 * - 유지 시간: OUTDOOR_CACHE_TTL_SECONDS (기본 600초), 0이면 사용 안 함
 * - 하루 호출 한도를 아끼고, 시연 중 새로고침해도 빠르게 보여주기 위함
 */
public final class ApiCache {

    private static final int MAX_ENTRIES = 500;
    private static final Map<String, Entry> MAP = new ConcurrentHashMap<>();

    private ApiCache() {}

    private static final class Entry {
        final String body;
        final long expiresAt;
        Entry(String body, long expiresAt) { this.body = body; this.expiresAt = expiresAt; }
    }

    /** 유효한 캐시가 있으면 응답, 없으면 null */
    public static String get(String key) {
        Entry e = MAP.get(key);
        if (e == null) return null;
        if (System.currentTimeMillis() > e.expiresAt) {
            MAP.remove(key);
            return null;
        }
        return e.body;
    }

    /** 응답 저장 (TTL이 0이면 저장 안 함) */
    public static void put(String key, String body) {
        int ttl = ApiConfig.getCacheTtlSeconds();
        if (ttl <= 0) return;
        if (MAP.size() >= MAX_ENTRIES) evictExpired();
        if (MAP.size() >= MAX_ENTRIES) MAP.clear();
        MAP.put(key, new Entry(body, System.currentTimeMillis() + ttl * 1000L));
    }

    /** 전체 비우기 (테스트용) */
    public static void clear() {
        MAP.clear();
    }

    private static void evictExpired() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Entry>> it = MAP.entrySet().iterator(); it.hasNext(); ) {
            if (now > it.next().getValue().expiresAt) it.remove();
        }
    }
}
