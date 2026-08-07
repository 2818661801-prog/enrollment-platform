package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 解绑班级-类别请求 DTO（管理端）
 */
public class ClassCategoryDeleteRequest {

    @NotNull(message = "关联ID不能为空")
    private Integer id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
