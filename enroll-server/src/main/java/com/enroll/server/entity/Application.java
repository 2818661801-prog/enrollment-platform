package com.enroll.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 报名记录实体 — 对应数据库 applications 表
 *
 * 状态机（status）：
 *   0 = 未报名（学生登录后无任何记录，默认值）
 *   1 = 已报名（学生提交报名，待审核）
 *   2 = 已撤回（学生主动撤回，可重新报名）
 *   3 = 已录取（管理员录取，永久锁定）
 *   4 = 未录取（管理员驳回，可报名其他班）
 *
 * 每行 = 一个学生的报名记录
 */
@Entity
@Table(name = "ssc_applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String name;      // 学生姓名

    @Column(name = "id_card", nullable = false, length = 18)
    private String idCard;    // 身份证号（完整版，查重用）

    @Column(name = "id_card_masked", nullable = false, length = 18)
    private String idCardMasked; // 脱敏身份证号（显示用，中间8位 ***）

    @Column(nullable = false, length = 2)
    private String gender;    // 性别

    @Column(nullable = false, length = 11)
    private String phone;     // 联系电话

    @Column(name = "has_physics", length = 2)
    private String hasPhysics; // 是否选考物理：是/否

    @Column(name = "has_english", length = 2)
    private String hasEnglish; // 是否选考英语：是/否

    @Column(name = "class_id", nullable = false)
    private Integer classId;  // 申报班级 ID（外键，关联 classes.id）

    @Column(name = "applied_category", length = 20)
    private String appliedCategory; // 班级类别（学生报名时选择）

    /**
     * 状态：
     *   0 = 未报名（登录后无记录）
     *   1 = 已报名（学生提交）
     *   2 = 已撤回
     *   3 = 已录取
     *   4 = 未录取
     * 数据库存 INT，Java 用 Integer 对接
     */
    @Column(nullable = false)
    private Integer status;  // 默认值由 @PrePersist 设置为 0（未报名）

    /** 是否同意报名须知：0否 1是 */
    @Column(name = "notice_agreed", nullable = false)
    private Integer noticeAgreed;

    /** 审核意见（管理员填写，通过/驳回原因） */
    @Column(name = "audit_comment", length = 500)
    private String auditComment;

    @Column(name = "apply_time", nullable = false)
    private LocalDateTime applyTime;

    /**
     * 数据来源（2026-07-02 内外网架构新增）
     *   student — 学生自报（默认值，存量数据全部 backfill 为 student）
     *   admin   — 管理员代录
     *   sync    — 内网低代码平台同步
     */
    /** 报名轮次：1=第一轮，2=第二轮（存的是报名提交时确定的轮次，非当前时间计算） */
    @Column(name = "round", nullable = false)
    private Integer round = 1;

    /**
     * 管理员软删除标记（2026-07-10 新增）
     *   0 = 正常（未删除）
     *   1 = 已删除
     * 与 status=2（学生撤回）是两个独立机制，互不干扰
     */
    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;

    /**
     * 内网报名记录ID（2026-07-15 新增，用于跨系统 id 映射）
     * 外网收到内网 sync 数据后，将内网的 application id 存到此字段
     */
    @Column(name = "outer_id")
    private Integer outerId;

    /**
     * 报名年级（2026-07-19 新增）
     * 2026 年报名存 26，2027 年报名存 27
     * 后端自动从当前年份计算写入，无需前端传参
     */
    @Column(name = "enrollment_year", columnDefinition = "INT DEFAULT NULL COMMENT '报名年级：26=2026年，27=2027年，后端自动写入'")
    private Integer enrollmentYear;

    // ==================== 生命周期回调 ====================

    @PrePersist
    void onCreate() {
        if (this.status == null) this.status = 0;       // 默认 0 = 未报名
        if (this.noticeAgreed == null) this.noticeAgreed = 0;
        if (this.applyTime == null) this.applyTime = LocalDateTime.now();
        if (this.isDeleted == null) this.isDeleted = 0; // 默认 0 = 未删除
    }

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getIdCardMasked() { return idCardMasked; }
    public void setIdCardMasked(String idCardMasked) { this.idCardMasked = idCardMasked; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHasPhysics() { return hasPhysics; }
    public void setHasPhysics(String hasPhysics) { this.hasPhysics = hasPhysics; }

    public String getHasEnglish() { return hasEnglish; }
    public void setHasEnglish(String hasEnglish) { this.hasEnglish = hasEnglish; }

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public String getAppliedCategory() { return appliedCategory; }
    public void setAppliedCategory(String appliedCategory) { this.appliedCategory = appliedCategory; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getNoticeAgreed() { return noticeAgreed; }
    public void setNoticeAgreed(Integer noticeAgreed) { this.noticeAgreed = noticeAgreed; }

    public String getAuditComment() { return auditComment; }
    public void setAuditComment(String auditComment) { this.auditComment = auditComment; }

    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }

    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public Integer getOuterId() { return outerId; }
    public void setOuterId(Integer outerId) { this.outerId = outerId; }

    public Integer getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(Integer enrollmentYear) { this.enrollmentYear = enrollmentYear; }
}
