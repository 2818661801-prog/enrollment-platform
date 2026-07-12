package com.enroll.server.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ApplicationDTO implements Serializable {

    private Integer id;
    private String name;
    private String idCard;
    private String gender;
    private String phone;
    private String hasPhysics;
    private String hasEnglish;
    private Integer classId;
    private String className;
    private String appliedCategory;
    private String status;
    private LocalDateTime applyTime;
    private String auditComment;
    private String classPeriods;  // 班级多轮时间段 JSON
    private Integer round;       // 报名轮次：1=第一轮，2=第二轮（存提交时确定的值）
    private Integer isDeleted;   // 软删除：0=正常，1=已删除（管理员删除）

    public ApplicationDTO() {}

    public ApplicationDTO(Integer id, String name, String idCard, String gender,
                          String phone, String hasPhysics, String hasEnglish,
                          Integer classId, String className, String appliedCategory,
                          String status, LocalDateTime applyTime, String auditComment,
                          String classPeriods, Integer round, Integer isDeleted) {
        this.id = id;
        this.name = name;
        this.idCard = idCard;
        this.gender = gender;
        this.phone = phone;
        this.hasPhysics = hasPhysics;
        this.hasEnglish = hasEnglish;
        this.classId = classId;
        this.className = className;
        this.appliedCategory = appliedCategory;
        this.status = status;
        this.applyTime = applyTime;
        this.auditComment = auditComment;
        this.classPeriods = classPeriods;
        this.round = round;
        this.isDeleted = isDeleted;
    }

    // ==================== Getter / Setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

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

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getAppliedCategory() { return appliedCategory; }
    public void setAppliedCategory(String appliedCategory) { this.appliedCategory = appliedCategory; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }

    public String getAuditComment() { return auditComment; }
    public void setAuditComment(String auditComment) { this.auditComment = auditComment; }

    public String getClassPeriods() { return classPeriods; }
    public void setClassPeriods(String classPeriods) { this.classPeriods = classPeriods; }

    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    // ==================== Builder ====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private String name;
        private String idCard;
        private String gender;
        private String phone;
        private String hasPhysics;
        private String hasEnglish;
        private Integer classId;
        private String className;
        private String appliedCategory;
        private String status;
        private LocalDateTime applyTime;
        private String auditComment;
        private String classPeriods;
        private Integer round;
        private Integer isDeleted;

        public Builder id(Integer id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder idCard(String idCard) { this.idCard = idCard; return this; }
        public Builder gender(String gender) { this.gender = gender; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder hasPhysics(String hasPhysics) { this.hasPhysics = hasPhysics; return this; }
        public Builder hasEnglish(String hasEnglish) { this.hasEnglish = hasEnglish; return this; }
        public Builder classId(Integer classId) { this.classId = classId; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder appliedCategory(String appliedCategory) { this.appliedCategory = appliedCategory; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder applyTime(LocalDateTime applyTime) { this.applyTime = applyTime; return this; }
        public Builder auditComment(String auditComment) { this.auditComment = auditComment; return this; }
        public Builder classPeriods(String classPeriods) { this.classPeriods = classPeriods; return this; }
        public Builder round(Integer round) { this.round = round; return this; }
        public Builder isDeleted(Integer isDeleted) { this.isDeleted = isDeleted; return this; }

        public ApplicationDTO build() {
            return new ApplicationDTO(id, name, idCard, gender, phone, hasPhysics, hasEnglish,
                    classId, className, appliedCategory, status, applyTime, auditComment, classPeriods, round, isDeleted);
        }
    }
}