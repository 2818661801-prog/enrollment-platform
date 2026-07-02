-- ============================================================
-- 特色班报名系统 · 初始化 SQL（全新建表）
-- 执行：直接全部跑一遍即可
-- MySQL 8.0+
--
-- 注意：现有数据库由 JPA ddl-auto=update 生成，表结构以此文件为准
--       初始数据为最小参考集（7个班），生产环境请按需调整
-- ============================================================

-- ---------- 0. categories 表（类别字典，在 classes 之前创建）----------
CREATE TABLE `categories` (
  `id`   INT          PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '类别名称，如：杭电班/成电班/ACCA班'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级类别字典表';

-- ---------- 1. classes 表 ----------
-- periods 格式：[{"round":1,"period":"2026/09/01 - 2026/09/13"},{"round":2,"period":"2026/09/15 - 2026/09/16"}]
--   成电班有 2 轮，其他班 1 轮
CREATE TABLE `classes` (
  `id`          INT          PRIMARY KEY AUTO_INCREMENT,
  `name`        VARCHAR(200) NOT NULL                COMMENT '班级名称',
  `period`      VARCHAR(50)  NOT NULL                COMMENT '当前有效时间段（由 periods 计算得出）',
  `periods`     TEXT                                 COMMENT '多轮时间段 JSON，格式：[{"round":1,"period":"2026/09/01 - 2026/09/13"}]',
  `quota`       INT          DEFAULT 0               COMMENT '名额上限，-1不限',
  `enrolled`    INT          DEFAULT 0                COMMENT '已报名人数',
  `description` TEXT                                 COMMENT '班级说明',
  `is_deleted`     INT          NOT NULL                 COMMENT '软删除：0正常 1已删除',
  `category_names` JSON         DEFAULT NULL             COMMENT '班级类别数组，格式：["杭电班","成电班"]，可为空'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='特色班表';

-- ---------- 2. applications 表 ----------
CREATE TABLE `applications` (
  `id`             INT          PRIMARY KEY AUTO_INCREMENT,
  `name`           VARCHAR(20)  NOT NULL                COMMENT '姓名',
  `id_card`        VARCHAR(18)  NOT NULL                COMMENT '身份证号',
  `id_card_masked` VARCHAR(18)  NOT NULL                COMMENT '脱敏身份证（中间8位***）',
  `gender`         VARCHAR(2)   NOT NULL                COMMENT '性别',
  `phone`          VARCHAR(11)  NOT NULL                COMMENT '联系电话',
  `has_physics`    VARCHAR(2)   NOT NULL                COMMENT '是否选考物理',
  `has_english`    VARCHAR(2)   NOT NULL                COMMENT '是否选考英语',
  `class_id`       INT          NOT NULL                COMMENT '申报班级ID',
  `applied_category` VARCHAR(20)  DEFAULT NULL            COMMENT '班级类别（学生报名时选择）',
  `status`         INT          NOT NULL DEFAULT 0       COMMENT '0未报名 1已报名 2已撤回 3已录取 4未录取',
  `notice_agreed`  INT          NOT NULL                COMMENT '是否同意报名须知',
  `audit_comment`  VARCHAR(500) DEFAULT NULL               COMMENT '审核意见（管理员填写）',
  `apply_time`     DATETIME     NOT NULL                COMMENT '报名时间',
  INDEX `idx_id_card`  (`id_card`),
  INDEX `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名记录表';

-- ---------- 3. sys_config 表 ----------
CREATE TABLE `sys_config` (
  `id`         INT         PRIMARY KEY AUTO_INCREMENT,
  `cfg_key`    VARCHAR(50) UNIQUE NOT NULL                COMMENT '配置键',
  `cfg_value`  TEXT                                      COMMENT '配置值（JSON）',
  `updated_at` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` VARCHAR(50)  DEFAULT NULL               COMMENT '最后修改人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ---------- 4. 初始数据：类别字典 ----------
-- 初始只有经管类/理工类，管理员后续在页面自行添加
INSERT INTO `categories` (`id`, `name`) VALUES
(1, '经管类'),
(2, '理工类');

-- ---------- 5. 初始数据：7个班级 ----------
-- 成电班第二轮时间带具体小时：2026/09/15 08:00（开始） 至 2026/09/16 23:59（截止）
-- category_names 为 JSON 数组，可存多个类别；不设置类别则填 NULL
INSERT INTO `classes` (`id`, `name`, `period`, `periods`, `quota`, `enrolled`, `description`, `is_deleted`, `category_names`) VALUES
(1, '2026级拔尖创新人才实验班（杭电班）',
 '2026/09/01 - 2026/09/13',
 '[{"round":1,"period":"2026/09/01 - 2026/09/13"}]',
 60, 0, '与杭州电子科技大学卓越学院"1+2+1"联合培养', 0, '["经管类","理工类"]'),
(2, '2026级计算机科学与技术（成电联合培养·计算机学院成电班）',
 '2026/09/01 - 2026/09/13',
 '[{"round":1,"period":"2026/09/01 - 2026/09/13"},{"round":2,"period":"2026/09/15 08:00 - 2026/09/16 23:59"}]',
 45, 0, '与电子科技大学联合培养，大二、大三赴电子科技大学学习', 0, NULL),
(3, '2026级电子信息工程（成电联合培养·电子工程学院成电班）',
 '2026/09/01 - 2026/09/13',
 '[{"round":1,"period":"2026/09/01 - 2026/09/13"},{"round":2,"period":"2026/09/15 08:00 - 2026/09/16 23:59"}]',
 35, 0, '与电子科技大学联合培养，嵌入式与通信技术方向', 0, NULL),
(4, '2026级会计学ACCA班',
 '2026/08/15 - 2026/09/16',
 '[{"round":1,"period":"2026/08/15 - 2026/09/16"}]',
 50, 0, '与上海高顿教育合作，嵌入ACCA全部课程（F1~SBR）', 0, NULL),
(5, '2026级金融学CFA班',
 '2026/08/15 - 2026/09/16',
 '[{"round":1,"period":"2026/08/15 - 2026/09/16"}]',
 50, 0, '与上海高顿教育合作，CFA考试10门核心课程，中英文授课', 0, NULL),
(6, '2026级会计学（智能财务）特色方向班',
 '2026/08/15 - 2026/08/25',
 '[{"round":1,"period":"2026/08/15 - 2026/08/25"}]',
 39, 0, '结合电子信息特色，培养数智化财会人才', 0, NULL),
(7, '2026级湖畔实验班（计算机）',
 '2026/08/05 - 2026/08/15',
 '[{"round":1,"period":"2026/08/05 - 2026/08/15"}]',
 30, 0, '计算机专业校企合作实验班', 0, NULL);

-- ---------- 6. 初始数据：sys_config ----------
INSERT INTO `sys_config` (`cfg_key`, `cfg_value`, `updated_by`) VALUES
('notice', '{"title":"报名须知","conditions":["报名者须为2026级新生","每人限报1个特色班","报名信息填写须真实有效"],"notices":["部分特色班设有两轮报名（第一轮+第二轮），两轮时间不同，请注意查看所报班级的具体时间段","报名时间截止后不可修改","录取结果另行通知","如有疑问请联系教务处"]}', 'system');
