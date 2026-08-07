package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建类别请求 DTO（管理端）
 */
public class CategoryCreateRequest {

    @NotBlank(message = "类别名称不能为空")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
