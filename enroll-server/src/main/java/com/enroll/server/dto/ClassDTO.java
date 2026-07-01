package com.enroll.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * 班级 DTO（API 出参专用）
 *
 * Builder 让 Service 转换时用链式调用
 */
@JsonInclude(JsonInclude.Include.ALWAYS) // null 字段也返回，保证前端能拿到 category
public class ClassDTO implements Serializable {

    private Integer id;
    private String name;
    private String period;     // 当前有效时间段（后端计算后返回）
    private String periods;    // 多轮时间段 JSON 数组
    private Integer quota;
    private Integer enrolled;
    private String description;
    private Integer isDeleted; // 0正常 1已删除
    private String category;  // 班级类别（管理员自定义）

    public ClassDTO() {}

    public ClassDTO(Integer id, String name, String period, String periods,
                    Integer quota, Integer enrolled, String description, Integer isDeleted, String category) {
        this.id = id;
        this.name = name;
        this.period = period;
        this.periods = periods;
        this.quota = quota;
        this.enrolled = enrolled;
        this.description = description;
        this.isDeleted = isDeleted;
        this.category = category;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getPeriods() { return periods; }
    public void setPeriods(String periods) { this.periods = periods; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // ==================== Builder ====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private String name;
        private String period;
        private String periods;
        private Integer quota;
        private Integer enrolled;
        private String description;
        private Integer isDeleted;
        private String category;

        public Builder id(Integer v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder period(String v) { this.period = v; return this; }
        public Builder periods(String v) { this.periods = v; return this; }
        public Builder quota(Integer v) { this.quota = v; return this; }
        public Builder enrolled(Integer v) { this.enrolled = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder isDeleted(Integer v) { this.isDeleted = v; return this; }
        public Builder category(String v) { this.category = v; return this; }

        public ClassDTO build() {
            return new ClassDTO(id, name, period, periods, quota, enrolled, description, isDeleted, category);
        }
    }
}
