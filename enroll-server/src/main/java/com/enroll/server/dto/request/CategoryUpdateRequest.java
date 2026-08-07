package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 更新类别请求 DTO（管理端）
 */
public class CategoryUpdateRequest {

    @NotNull(message = "类别ID不能为空")
    private Integer id;
    @NotBlank(message = "类别名称不能为空")
    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
