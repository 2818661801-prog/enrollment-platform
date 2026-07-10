-- ============================================================
-- V20260705：applications 表加 is_deleted 字段（管理员软删除）
-- ============================================================
-- 执行方式：mysql -uenroll -p***REMOVED*** enroll_db < docs/sql/V20260705__add_applications_is_deleted.sql
-- ============================================================

-- 1. 加字段
ALTER TABLE `ssc_applications`
  ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0
  COMMENT '管理员软删除：0=正常，1=已删除'
  AFTER `source`;

-- 2. 存量数据回填（确保无 NULL）
UPDATE `ssc_applications` SET `is_deleted` = 0 WHERE `is_deleted` IS NULL;

-- 3. 加索引（按需，加速"查未删除报名"的过滤）
ALTER TABLE `ssc_applications` ADD INDEX `idx_is_deleted` (`is_deleted`);
