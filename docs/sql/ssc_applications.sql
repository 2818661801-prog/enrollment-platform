/*
 Navicat Premium Data Transfer

 Source Server         : 开发环境
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : ***REMOVED***:3306
 Source Schema         : smart_academic_affairs

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 15/07/2026 10:23:44
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ssc_applications
-- ----------------------------
DROP TABLE IF EXISTS `ssc_applications`;
CREATE TABLE `ssc_applications`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '身份证号',
  `id_card_masked` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '脱敏身份证（中间8位***）',
  `gender` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '性别',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `has_physics` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '是否选考物理',
  `has_english` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '是否选考英语',
  `class_id` int NOT NULL COMMENT '申报班级ID',
  `applied_category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '报考类别（学生报名时选择）',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态：0=未报名，1=已报名，2=已撤回，3=已录取，4=未录取',
  `notice_agreed` int NOT NULL COMMENT '是否同意报名须知（0=否，1=是）',
  `audit_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核意见（管理员填写）',
  `apply_time` datetime NOT NULL COMMENT '报名时间',
  `round` int NOT NULL DEFAULT 1 COMMENT '报名轮次：1=第一轮，2=第二轮（存报名时确定的值）',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'student' COMMENT '数据来源：student(学生自报)/admin(管理员导入)/sync(低代码同步)',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '管理员软删除：0=正常，1=已删除（2026-07-10 新增）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_id_card`(`id_card` ASC) USING BTREE,
  INDEX `idx_class_id`(`class_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '报名记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
