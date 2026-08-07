package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 创建班级请求 DTO（管理端）
 * classRounds: [{"round":1,"period":"2026/09/01 - 2026/09/13"},...]
 */
public class ClassCreateRequest {

    @NotBlank(message = "班级名称不能为空")
    private String name;
    private Integer quota;
    private String description;
    private List<Map<String, Object>> classRounds;
    private List<String> categoryNames;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Map<String, Object>> getClassRounds() { return classRounds; }
    public void setClassRounds(List<Map<String, Object>> classRounds) { this.classRounds = classRounds; }

    public List<String> getCategoryNames() { return categoryNames; }
    public void setCategoryNames(List<String> categoryNames) { this.categoryNames = categoryNames; }
}
