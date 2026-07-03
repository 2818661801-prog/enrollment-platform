-- V20260703__add_round_field.sql
-- 用途：applications 表加 round 字段 + 删 source 字段
-- 执行：mysql -uenroll -p***REMOVED*** enroll_db < docs/sql/V20260703__add_round_field.sql

-- 1. applications 表加 round 字段
ALTER TABLE applications
  ADD COLUMN round INT NOT NULL DEFAULT 1
  COMMENT '报名轮次：1=第一轮，2=第二轮';

-- 存量数据回填为 1
UPDATE applications SET round = 1 WHERE round IS NULL OR round = 0;

-- 2. 删除无用字段 applications.source（classes.source 保留）
ALTER TABLE applications DROP COLUMN IF EXISTS source;
