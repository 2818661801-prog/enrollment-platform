-- ============================================================
-- 特色班报名系统 · 内网初始化 SQL（低代码平台用）
--
-- 执行：直接全部跑一遍即可
-- MySQL 8.0+
--
-- 与外网 INIT.sql 的区别：
--   - class_rounds.period_start/end 用 VARCHAR(50) 存格式化字符串
--   - 避免 DateTime 转换坑
-- ============================================================

-- ---------- 0. categories 表（类别字典）----------
CREATE TABLE `ssc_categories` (
  `id`   INT          PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50)  NOT NULL UNIQUE COMMENT '类别名称，如：经管类/理工类/ACCA班'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级类别字典表';

-- ---------- 1. classes 表（特色班）----------
CREATE TABLE `ssc_classes` (
  `id`          INT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(200) NOT NULL                COMMENT '班级名称',
  `period`      VARCHAR(50)  NOT NULL                COMMENT '当前有效时间段（由 class_rounds 第一轮自动计算）',
  `quota`       INT          DEFAULT 0               COMMENT '名额上限，-1=不限',
  `enrolled`    INT          DEFAULT 0               COMMENT '已报名人数（冗余字段，由后端 enrolled_count 视图或触发器维护）',
  `description` TEXT                               COMMENT '班级说明',
  `is_deleted`  INT          NOT NULL DEFAULT 0      COMMENT '软删除：0=正常，1=已删除',
  `source`      VARCHAR(20)  NOT NULL DEFAULT 'admin' COMMENT '数据来源：admin(管理员建)/sync(低代码同步)/student(预留)',
  `outer_id`    INT          DEFAULT NULL            COMMENT '外网班级ID（sync后回填，用于跨系统 id 映射）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='特色班表';

-- ---------- 2. class_category 中间表（班级-类别 N:N 关联）----------
CREATE TABLE `ssc_class_category` (
  `id`          INT      PRIMARY KEY AUTO_INCREMENT,
  `class_id`    INT      NOT NULL COMMENT '班级ID，关联 ssc_classes.id',
  `category_id` INT      NOT NULL COMMENT '类别ID，关联 ssc_categories.id',
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_class_category` (`class_id`, `category_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_class_id`    (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级-类别中间表';

-- ---------- 3. class_rounds 表（班级轮次）—— VARCHAR 版本 ----------
CREATE TABLE `ssc_class_rounds` (
  `id`           INT         PRIMARY KEY AUTO_INCREMENT,
  `class_id`     INT         NOT NULL                       COMMENT '班级ID，关联 ssc_classes.id',
  `round_num`    INT         NOT NULL                       COMMENT '轮次编号（1/2/3...）',
  `period_start` VARCHAR(50) NOT NULL                       COMMENT '报名开始时间（格式：yyyy-MM-dd HH:mm:ss）',
  `period_end`   VARCHAR(50) NOT NULL                       COMMENT '报名结束时间（格式：yyyy-MM-dd HH:mm:ss）',
  `created_at`   DATETIME    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_class_round` (`class_id`, `round_num`),
  KEY `idx_class_id`     (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级轮次表';

-- ---------- 4. applications 表（报名记录）----------
CREATE TABLE `ssc_applications` (
  `id`               INT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  `name`             VARCHAR(20)  NOT NULL                COMMENT '姓名',
  `id_card`          VARCHAR(18)  NOT NULL                COMMENT '身份证号',
  `id_card_masked`   VARCHAR(18)  NOT NULL                COMMENT '脱敏身份证（中间8位***）',
  `gender`           VARCHAR(2)   NOT NULL                COMMENT '性别',
  `phone`            VARCHAR(11)  NOT NULL                COMMENT '联系电话',
  `has_physics`      VARCHAR(2)   NOT NULL                COMMENT '是否选考物理',
  `has_english`      VARCHAR(2)   NOT NULL                COMMENT '是否选考英语',
  `class_id`         INT          NOT NULL                COMMENT '申报班级ID',
  `applied_category` VARCHAR(20)  DEFAULT NULL            COMMENT '报考类别（学生报名时选择）',
  `status`           INT          NOT NULL DEFAULT 0      COMMENT '状态：0=未报名，1=已报名，2=已撤回，3=已录取，4=未录取',
  `notice_agreed`    INT          NOT NULL                COMMENT '是否同意报名须知（0=否，1=是）',
  `audit_comment`    VARCHAR(500) DEFAULT NULL            COMMENT '审核意见（管理员填写）',
  `apply_time`       DATETIME     NOT NULL                COMMENT '报名时间',
  `round`            INT          NOT NULL DEFAULT 1      COMMENT '报名轮次：1=第一轮，2=第二轮',
  `source`           VARCHAR(20)  NOT NULL DEFAULT 'student' COMMENT '数据来源：student(学生自报)/admin(管理员导入)/sync(低代码同步)',
  `is_deleted`       INT          NOT NULL DEFAULT 0      COMMENT '软删除：0=正常，1=已删除',
  `outer_id`         INT          DEFAULT NULL            COMMENT '外网报名记录ID（sync后回填）',
  `enrollment_year`  INT          DEFAULT NULL            COMMENT '报名年级：2026=2026年，2027=2027年，后端自动写入',
  INDEX `idx_id_card`  (`id_card`),
  INDEX `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名记录表';

-- ---------- 5. sys_config 表（系统配置）----------
CREATE TABLE `ssc_sys_config` (
  `id`         INT          PRIMARY KEY AUTO_INCREMENT,
  `title`      VARCHAR(200) DEFAULT NULL COMMENT '报名须知标题',
  `conditions` TEXT                      COMMENT '报名条件（换行分隔）',
  `notices`    TEXT                      COMMENT '报名须知（换行分隔）',
  `updated_at` DATETIME(6)  DEFAULT NULL  COMMENT '最后修改时间',
  `updated_by` VARCHAR(50)  DEFAULT NULL  COMMENT '最后修改人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ---------- 6. 初始数据：类别字典 ----------
INSERT INTO `ssc_categories` (`id`, `name`) VALUES
(1, '经管类'),
(2, '理工类');

-- ---------- 7. 初始数据：7个班级 ----------
INSERT INTO `ssc_classes` (`id`, `name`, `period`, `quota`, `enrolled`, `is_deleted`, `source`) VALUES
(1, '2026级拔尖创新人才实验班（杭电班）',
 '2026/09/01 08:00 - 2026/09/13 23:59',
 -1, 0, 0, 'admin'),
(2, '2026级计算机科学与技术（成电联合培养·计算机学院成电班）',
 '2026/09/01 08:00 - 2026/09/13 23:59',
 -1, 0, 0, 'admin'),
(3, '2026级电子信息工程（成电联合培养·电子工程学院成电班）',
 '2026/09/01 08:00 - 2026/09/13 23:59',
 -1, 0, 0, 'admin'),
(4, '2026级会计学ACCA班',
 '2026/08/15 08:00 - 2026/09/16 23:59',
 -1, 0, 0, 'admin'),
(5, '2026级金融学CFA班',
 '2026/08/15 08:00 - 2026/09/16 23:59',
 -1, 0, 0, 'admin'),
(6, '2026级会计学（智能财务）特色方向班',
 '2026/08/15 08:00 - 2026/08/25 23:59',
 -1, 0, 0, 'admin'),
(7, '2026级湖畔实验班（计算机）',
 '2026/08/05 08:00 - 2026/08/15 23:59',
 -1, 0, 0, 'admin');

-- ---------- 8. 初始数据：班级-类别中间表 ----------
INSERT INTO `ssc_class_category` (`class_id`, `category_id`) VALUES
(1, 1),
(1, 2);

-- ---------- 9. 初始数据：班级轮次（VARCHAR 格式）----------
INSERT INTO `ssc_class_rounds` (`class_id`, `round_num`, `period_start`, `period_end`) VALUES
(1, 1, '2026-09-01 08:00:00', '2026-09-13 23:59:00'),
(2, 1, '2026-09-01 08:00:00', '2026-09-13 23:59:00'),
(2, 2, '2026-09-15 08:00:00', '2026-09-16 23:59:00'),
(3, 1, '2026-09-01 08:00:00', '2026-09-13 23:59:00'),
(3, 2, '2026-09-15 08:00:00', '2026-09-16 23:59:00'),
(4, 1, '2026-08-15 08:00:00', '2026-09-16 23:59:00'),
(5, 1, '2026-08-15 08:00:00', '2026-09-16 23:59:00'),
(6, 1, '2026-08-15 08:00:00', '2026-08-25 23:59:00'),
(7, 1, '2026-08-05 08:00:00', '2026-08-15 23:59:00');

-- ---------- 10. 初始数据：sys_config（报名须知）----------
INSERT INTO `ssc_sys_config` (`id`, `title`, `conditions`, `notices`, `updated_by`) VALUES
(1,
 '2026年特色班报名须知',
 '报名者须为2026级新生\n每人限报1个特色班\n报名信息填写须真实有效',
 '部分特色班设有两轮报名（第一轮+第二轮），两轮时间不同，请注意查看所报班级的具体时间段\n报名时间截止后不可修改\n录取结果另行通知\n如有疑问请联系教务处',
 'admin');
