-- ============================================================
-- 特色班报名系统 · V20260702 增加 source 字段
-- 背景：内外网分离架构（2026-07-02 确定）
--   内网低代码平台 → 外网本项目 API，单向同步
--   需要在数据上记录"来源"以便审计
--
-- 执行方式（二选一）：
--   方式 1：Flyway 自动执行（推荐，开发期）
--     - 把此文件放到 enroll-server/src/main/resources/db/migration/
--   方式 2：手动 mysql 命令行
--     - mysql -uenroll -p***REMOVED*** enroll_db < V20260702__add_source_field.sql
-- ============================================================

-- ---------- 1. classes 表加 source 字段 ----------
-- DEFAULT 'admin'：存量数据默认是管理员建的（保守起见）
ALTER TABLE ssc_classes
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'admin'
  COMMENT '数据来源：admin(管理员建)/sync(内网低代码同步)/student(预留,班级通常非学生建)'
  AFTER category_names;

-- ---------- 2. applications 表加 source 字段 ----------
-- DEFAULT 'student'：存量报名记录默认是学生报的
ALTER TABLE ssc_applications
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'student'
  COMMENT '数据来源：student(学生自报)/admin(管理员导入)/sync(内网低代码同步)'
  AFTER apply_time;

-- ---------- 3. 存量数据 backfill（防御性，正常 DEFAULT 已经覆盖）----------
UPDATE ssc_classes SET source = 'admin' WHERE source IS NULL OR source = '';
UPDATE ssc_applications SET source = 'student' WHERE source IS NULL OR source = '';

-- ---------- 4. 加索引（按 source 查/统计用）----------
ALTER TABLE ssc_classes ADD INDEX idx_source (source);
ALTER TABLE ssc_applications ADD INDEX idx_source (source);

-- ============================================================
-- 验证脚本（执行后跑一遍）
-- ============================================================
-- SHOW COLUMNS FROM classes LIKE 'source';
-- SHOW COLUMNS FROM applications LIKE 'source';
-- SELECT source, COUNT(*) FROM classes GROUP BY source;
-- SELECT source, COUNT(*) FROM applications GROUP BY source;