-- =========================================================
--  야외활동 지수 MySQL 스키마
--  서버가 처음 켜질 때 자동으로 만들어지므로 직접 실행할 필요는 없어요.
--  (직접 만들고 싶을 때: mysql -u root -p < docs/schema-mysql.sql)
-- =========================================================

CREATE DATABASE IF NOT EXISTS outdoor_index
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE outdoor_index;

-- 즐겨찾기 (같은 지역은 한 번만)
CREATE TABLE IF NOT EXISTS favorite (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sido        VARCHAR(20)  NOT NULL,              -- 시·도 (서울)
    station     VARCHAR(50)  NOT NULL DEFAULT '',   -- 측정소 (중구), 시·도 평균이면 ''
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_favorite UNIQUE (sido, station)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 최근 검색 (최근 200건만 유지)
CREATE TABLE IF NOT EXISTS search_history (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sido         VARCHAR(20)  NOT NULL,
    station      VARCHAR(50)  NOT NULL DEFAULT '',
    level        VARCHAR(20),                       -- GOOD / CAUTION / BAD
    searched_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 전용 계정을 쓰고 싶을 때 (선택)
-- CREATE USER 'outdoor'@'localhost' IDENTIFIED BY '비밀번호';
-- GRANT ALL PRIVILEGES ON outdoor_index.* TO 'outdoor'@'localhost';
-- FLUSH PRIVILEGES;
