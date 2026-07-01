package com.enroll.server.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报名 DTO（API 出参专用）
 *
 * 关键设计：只返 idCardMasked（脱敏版），不返 idCard（完整版）
 *   防止敏感数据从 API 流出 → 数据库完整身份证仅 Service 内部使用
 */
public class ApplicationDTO implements Serializable {

    private Integer id;
    private String name;
    private String idCard;        // 脱敏版（如 330110********0626）
    private String gender;
    private String phone;
    private String hasPhysics;
    private String hasEnglish;
    private Integer classId;
    private String className;     // 班级名（冗余字段，前端展示不用再查）
    private String hdSubType;
    private String status;
    private LocalDateTime applyTime;

    // ==================== 构造器 ====================

    public ApplicationDTO() {}

    /** 全参构造器（Builder.build() 用） */
    public ApplicationDTO(Integer id, String name, String idCard, String gender,
                          String phone, String hasPhysics, String hasEnglish,
                          Integer classId, String className, String hdSubType,
                          String status, LocalDateTime applyTime) {
        this.id = id;
        this.name = name;
        this.idCard = idCard;
        this.gender = gender;
        this.phone = phone;
        this.hasPhysics = hasPhysics;
        this.hasEnglish = hasEnglish;
        this.classId = classId;
        this.className = className;
        this.hdSubType = hdSubType;
        this.status = status;
        this.applyTime = applyTime;
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

    public String getHdSubType() { return hdSubType; }
    public void setHdSubType(String hdSubType) { this.hdSubType = hdSubType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }

    // ==================== Builder（链式构建） ====================

    /** 静态入口：ApplicationDTO.builder() 开始链式调用 */
    public static Builder builder() {
        return new Builder();
    }

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
        private String hdSubType;
        private String status;
        private LocalDateTime applyTime;

        public Builder id(Integer id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder idCard(String idCard) { this.idCard = idCard; return this; }
        public Builder gender(String gender) { this.gender = gender; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder hasPhysics(String hasPhysics) { this.hasPhysics = hasPhysics; return this; }
        public Builder hasEnglish(String hasEnglish) { this.hasEnglish = hasEnglish; return this; }
        public Builder classId(Integer classId) { this.classId = classId; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder hdSubType(String hdSubType) { this.hdSubType = hdSubType; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder applyTime(LocalDateTime applyTime) { this.applyTime = applyTime; return this; }

        public ApplicationDTO build() {
            return new ApplicationDTO(id, name, idCard, gender, phone, hasPhysics, hasEnglish,
                    classId, className, hdSubType, status, applyTime);
        }
    }
}
