package com.enroll.server.dto;

import java.io.Serializable;

/**
 * 班级 DTO（API 出参专用）
 *
 * Builder 让 Service 转换时用链式调用
 */
public class ClassDTO implements Serializable {

    private Integer id;
    private String name;
    private String period;
    private Integer round;      // 0单轮 1两轮（成电班）
    private Integer quota;
    private Integer enrolled;
    private String description;
    private Integer isDeleted;  // 0正常 1已删除

    public ClassDTO() {}

    public ClassDTO(Integer id, String name, String period, Integer round,
                    Integer quota, Integer enrolled, String description, Integer isDeleted) {
        this.id = id;
        this.name = name;
        this.period = period;
        this.round = round;
        this.quota = quota;
        this.enrolled = enrolled;
        this.description = description;
        this.isDeleted = isDeleted;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    // ==================== Builder ====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private String name;
        private String period;
        private Integer round;
        private Integer quota;
        private Integer enrolled;
        private String description;
        private Integer isDeleted;

        public Builder id(Integer v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder period(String v) { this.period = v; return this; }
        public Builder round(Integer v) { this.round = v; return this; }
        public Builder quota(Integer v) { this.quota = v; return this; }
        public Builder enrolled(Integer v) { this.enrolled = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder isDeleted(Integer v) { this.isDeleted = v; return this; }

        public ClassDTO build() {
            return new ClassDTO(id, name, period, round, quota, enrolled, description, isDeleted);
        }
    }
}
