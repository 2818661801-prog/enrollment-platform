package com.enroll.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 报名记录实体 — 对应数据库 applications 表
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

    @Column(name = "has_physics", nullable = false, length = 2)
    private String hasPhysics; // 是否选考物理：是/否

    @Column(name = "has_english", nullable = false, length = 2)
    private String hasEnglish; // 是否选考英语：是/否

    @Column(name = "class_id", nullable = false)
    private Integer classId;  // 申报班级 ID（外键，关联 classes.id）

    @Column(name = "hd_sub_type", length = 20)
    private String hdSubType; // 杭电班类别（仅 class_id=1 时有值，可为 null）

    @Column(nullable = false, length = 10)
    private String status = "已报名"; // 状态：已报名 / 已录取 / 已撤回

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

    public String getHdSubType() { return hdSubType; }
    public void setHdSubType(String hdSubType) { this.hdSubType = hdSubType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
}
