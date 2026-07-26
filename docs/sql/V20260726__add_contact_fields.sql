-- 联系方式字段（2026-07-26）
-- contact_info: sys_config 全局联系方式（管理员在 Tab 1 填写）
-- group_info: classes 班级社群信息（管理员在 Tab 3 填写）

ALTER TABLE ssc_sys_config
  ADD COLUMN contact_info TEXT DEFAULT NULL
  COMMENT '全局联系方式（微信/QQ/电话等）';

ALTER TABLE ssc_classes
  ADD COLUMN group_info VARCHAR(200) DEFAULT NULL
  COMMENT '班级社群信息（群号/群二维码描述等）';
