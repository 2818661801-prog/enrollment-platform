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
    private String period;                          // 当前有效时间段（兼容旧字段，可由 classRounds 算出来）
    private List<ClassRoundDTO> classRounds;        // 轮次列表（替代 periods JSON）
    private Integer quota;
    private Integer enrolled;
    private String description;
    private Integer isDeleted; // 0正常 1已删除
    private List<String> categories; // 班级类别数组，如 ["杭电班","成电班"]
    private String source; // 数据来源：admin/sync/student（2026-07-02 新增）

    public ClassDTO() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    /** 轮次列表（替代 periods JSON） */
    public List<ClassRoundDTO> getClassRounds() { return classRounds; }
    public void setClassRounds(List<ClassRoundDTO> classRounds) { this.classRounds = classRounds; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    // ==================== Builder ====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private String name;
        private String period;
        private List<ClassRoundDTO> classRounds;
        private Integer quota;
        private Integer enrolled;
        private String description;
        private Integer isDeleted;
        private List<String> categories;
        private String source;

        public Builder id(Integer v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder period(String v) { this.period = v; return this; }
        public Builder classRounds(List<ClassRoundDTO> v) { this.classRounds = v; return this; }
        public Builder quota(Integer v) { this.quota = v; return this; }
        public Builder enrolled(Integer v) { this.enrolled = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder isDeleted(Integer v) { this.isDeleted = v; return this; }
        public Builder categories(List<String> v) { this.categories = v; return this; }
        public Builder source(String v) { this.source = v; return this; }

        public ClassDTO build() {
            ClassDTO dto = new ClassDTO();
            dto.setId(id);
            dto.setName(name);
            dto.setPeriod(period);
            dto.setClassRounds(classRounds);
            dto.setQuota(quota);
            dto.setEnrolled(enrolled);
            dto.setDescription(description);
            dto.setIsDeleted(isDeleted);
            dto.setCategories(categories);
            dto.setSource(source);
            return dto;
        }
    }
}
