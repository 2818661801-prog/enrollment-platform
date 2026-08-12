-- ============================================================
-- V20260812 · apply_time 列加默认值（报名时间由 MySQL NOW() 自动填入）
--
-- 背景：
--   代码 Application.java 用 @DynamicInsert + 依赖 MySQL DEFAULT CURRENT_TIMESTAMP
--   填入 apply_time（2026-08-05 时区修复：不依赖 JVM 时区，避免 UTC 服务器 +8h 偏移）。
--   但 V20260701 建表脚本里 apply_time 是 DATETIME NOT NULL（无 DEFAULT），
--   且 Hibernate ddl-auto=update/validate 都【不会】给已有列补默认值。
--   若外网重建库/新环境部署时漏掉本脚本，学生提交报名会全部报错：
--     "Field 'apply_time' doesn't have a default value"（HTTP 500）
--
-- 执行方式：
--   mysql -uenroll -p***REMOVED*** enroll_db < V20260812__apply_time_default.sql
--
-- 校验是否已生效：
--   SHOW COLUMNS FROM ssc_applications LIKE 'apply_time';  -- Default 应为 CURRENT_TIMESTAMP
-- ============================================================

ALTER TABLE ssc_applications
  MODIFY apply_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
  COMMENT '报名时间，MySQL NOW()自动填入，不依赖JVM时区';
