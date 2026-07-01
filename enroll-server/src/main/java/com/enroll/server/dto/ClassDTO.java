package com.enroll.server.dto;

import java.io.Serializable;

/**
 * 班级 DTO（API 出参专用）
 *
 * 设计目的：
 *   1. 与 Entity 解耦 — 数据库改了字段不影响 API
 *   2. 过滤敏感字段 — Entity 有 description 等，DTO 按需暴露
 *   3. 显式契约 — 字段确定，前端 TypeScript 类型可直接生成
 *
 * Builder 让 Service 转换时用链式调用
 */
public class ClassDTO implements Serializable {

    private Integer id;
    private String name;
    private String category;
    private String period;
    private Integer quota;
    private Integer enrolled;
    private Boolean needPhysics;
    private String description;

    // ==================== 构造器 ====================

    public ClassDTO() {}

    /** 全参构造器（Builder.build() 用） */
    public ClassDTO(Integer id, String name, String category, String period,
                    Integer quota, Integer enrolled, Boolean needPhysics, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.period = period;
        this.quota = quota;
        this.enrolled = enrolled;
        this.needPhysics = needPhysics;
        this.description = description;
    }

    // ==================== Getter / Setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public Boolean getNeedPhysics() { return needPhysics; }
    public void setNeedPhysics(Boolean needPhysics) { this.needPhysics = needPhysics; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ==================== Builder（链式构建） ====================

    /** 静态入口：ClassDTO.builder() 开始链式调用 */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private String name;
        private String category;
        private String period;
        private Integer quota;
        private Integer enrolled;
        private Boolean needPhysics;
        private String description;

        public Builder id(Integer id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder period(String period) { this.period = period; return this; }
        public Builder quota(Integer quota) { this.quota = quota; return this; }
        public Builder enrolled(Integer enrolled) { this.enrolled = enrolled; return this; }
        public Builder needPhysics(Boolean needPhysics) { this.needPhysics = needPhysics; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public ClassDTO build() {
            return new ClassDTO(id, name, category, period, quota, enrolled, needPhysics, description);
        }
    }
}
