package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 更新班级请求 DTO（管理端）
 */
public class ClassUpdateRequest {

    @NotNull(message = "班级ID不能为空")
    private Integer id;
    private String name;
    private Integer quota;
    private String description;
    private Integer isDeleted;
    private String groupInfo;
    private List<Map<String, Object>> classRounds;
    private List<String> categoryNames;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public String getGroupInfo() { return groupInfo; }
    public void setGroupInfo(String groupInfo) { this.groupInfo = groupInfo; }

    public List<Map<String, Object>> getClassRounds() { return classRounds; }
    public void setClassRounds(List<Map<String, Object>> classRounds) { this.classRounds = classRounds; }

    public List<String> getCategoryNames() { return categoryNames; }
    public void setCategoryNames(List<String> categoryNames) { this.categoryNames = categoryNames; }
}
