package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 班级-类别绑定请求 DTO（管理端）
 */
public class ClassCategoryRequest {

    @NotNull(message = "班级ID不能为空")
    private Integer classId;
    @NotNull(message = "类别ID不能为空")
    private Integer categoryId;

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
}
