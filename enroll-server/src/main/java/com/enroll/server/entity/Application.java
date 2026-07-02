package com.enroll.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 报名记录实体 — 对应数据库 applications 表
 *
 * 状态机（status）：
 *   1 = 已报名（正常，可撤回）
 *   0 = 已撤回（学生主动撤回，视为软删除）
 *   2 = 已录取（管理员批量录取）
 *
 * 每行 = 一个学生的报名记录
 */
@Entity
@Table(name = "applications")
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
     *   1 = 已报名
     *   0 = 已撤回
     *   2 = 已录取
     * 数据库存 TINYINT，Java 用 Integer 对接
     */
    @Column(nullable = false)
    private Integer status = 1;

    /** 是否已录取：0否 1是 */
    @Column(name = "is_admitted", nullable = false)
    private Integer isAdmitted = 0;

    /** 是否同意报名须知：0否 1是 */
    @Column(name = "notice_agreed", nullable = false)
    private Integer noticeAgreed = 0;

    /** 审核意见（管理员填写，通过/驳回原因） */
    @Column(name = "audit_comment", length = 500)
    private String auditComment;

    @Column(name = "apply_time", nullable = false)
    private LocalDateTime applyTime = LocalDateTime.now(); // 报名时间

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

    public Integer getIsAdmitted() { return isAdmitted; }
    public void setIsAdmitted(Integer isAdmitted) { this.isAdmitted = isAdmitted; }

    public Integer getNoticeAgreed() { return noticeAgreed; }
    public void setNoticeAgreed(Integer noticeAgreed) { this.noticeAgreed = noticeAgreed; }

    public String getAuditComment() { return auditComment; }
    public void setAuditComment(String auditComment) { this.auditComment = auditComment; }

    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
}
