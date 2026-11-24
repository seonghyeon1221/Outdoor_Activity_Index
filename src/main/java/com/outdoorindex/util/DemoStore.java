package com.outdoorindex.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Logger;

/**
 * 시연용 API 응답 저장소. (발표장 인터넷이 끊겨도 화면이 나오도록)
 * - 담당: A
 * - 저장 위치: {데이터 폴더}/api-saved/*.json
 * - 키: 요청 주소에서 서비스키·발표 날짜·발표 시각을 뺀 것 → 시간이 지나도 같은 지역이면 같은 파일
 * - 저장본을 쓴 요청이 있었는지 표시(usedSaved)해서 화면에 "저장된 데이터" 안내를 띄운다.
 */
public final class DemoStore {

    private static final Logger LOG = Logger.getLogger(DemoStore.class.getName());
    private static final ThreadLocal<Boolean> USED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DemoStore() {}

    /** 저장본 읽기. 없으면 null */
    public static String load(String key) {
        Path f = file(key);
        try {
            return Files.isRegularFile(f) ? Files.readString(f, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            LOG.warning("저장 응답 읽기 실패: " + e);
            return null;
        }
    }

    /** 성공한 응답 저장 (실패해도 서비스에는 영향 없음) */
    public static void save(String key, String body) {
        try {
            Path f = file(key);
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, body, StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.warning("저장 응답 쓰기 실패: " + e);
        }
    }

    /** 이번 요청 처리 중 저장본을 썼다고 표시 */
    public static void markUsed() { USED.set(Boolean.TRUE); }

    /** 이번 요청 처리 중 저장본을 썼는지 */
    public static boolean wasUsed() { return USED.get(); }

    /** 요청 처리 시작 시 표시 초기화 */
    public static void resetUsage() { USED.set(Boolean.FALSE); }

    private static Path file(String key) {
        return ApiConfig.getDataDir().resolve("api-saved").resolve(sha1(key) + ".json");
    }

    private static String sha1(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
