-- 야외활동 지수 MySQL 스크립트 (즐겨찾기 테이블)
-- 서버가 처음 즐겨찾기를 쓸 때 자동으로 만들어지므로, 미리 만들어 두고 싶을 때만 실행하세요.
-- 실행: MySQL Workbench → File → Open SQL Script → 번개 아이콘 / 명령줄: mysql -u root -p < outdoor_index.sql

CREATE DATABASE IF NOT EXISTS outdoor_index DEFAULT CHARACTER SET utf8mb4;
USE outdoor_index;

CREATE TABLE IF NOT EXISTS favorite (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sido       VARCHAR(20) NOT NULL,              -- 시·도 (예: 서울)
    station    VARCHAR(50) NOT NULL DEFAULT '',   -- 측정소 (예: 중구), 시·도 평균이면 ''
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (sido, station)
) DEFAULT CHARSET = utf8mb4;

-- 확인용
SELECT * FROM favorite ORDER BY id DESC;
