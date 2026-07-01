package com.enroll.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

/**
 * 班级 DTO（API 出参专用）
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ClassDTO implements Serializable {

    private Integer id;
    private String name;
    private String period;
    private String periods;
    private Integer quota;
    private Integer enrolled;
    private String description;
    private Integer isDeleted; // 0正常 1已删除
    private List<String> categoryNames; // 班级类别数组，如 ["杭电班","成电班"]

    public ClassDTO() {}

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

    public List<String> getCategoryNames() { return categoryNames; }
    public void setCategoryNames(List<String> categoryNames) { this.categoryNames = categoryNames; }

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
        private List<String> categoryNames;

        public Builder id(Integer v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder period(String v) { this.period = v; return this; }
        public Builder periods(String v) { this.periods = v; return this; }
        public Builder quota(Integer v) { this.quota = v; return this; }
        public Builder enrolled(Integer v) { this.enrolled = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder isDeleted(Integer v) { this.isDeleted = v; return this; }
        public Builder categoryNames(List<String> v) { this.categoryNames = v; return this; }

        public ClassDTO build() {
            ClassDTO dto = new ClassDTO();
            dto.setId(id);
            dto.setName(name);
            dto.setPeriod(period);
            dto.setPeriods(periods);
            dto.setQuota(quota);
            dto.setEnrolled(enrolled);
            dto.setDescription(description);
            dto.setIsDeleted(isDeleted);
            dto.setCategoryNames(categoryNames);
            return dto;
        }
    }
}
