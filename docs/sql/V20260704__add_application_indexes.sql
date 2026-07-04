-- V20260704__add_application_indexes.sql（补建缺失索引，已存在的跳过）
-- 特色班报名系统 - applications 表索引优化
-- 修复：P0-2 applications 表缺索引问题

-- phone 字段索引（学生验证码登录、管理端按手机号查）
ALTER TABLE ssc_applications ADD INDEX idx_phone (phone);

-- status 字段索引（管理端按状态筛选报名记录）
ALTER TABLE ssc_applications ADD INDEX idx_status (status);

-- 联合索引：学生按身份证+状态查（效率最高）
ALTER TABLE ssc_applications ADD INDEX idx_id_card_status (id_card, status);

-- 联合索引：管理端按班级+状态查
ALTER TABLE ssc_applications ADD INDEX idx_class_status (class_id, status);
