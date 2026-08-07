package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 删除类别请求 DTO（管理端）
 */
public class CategoryDeleteRequest {

    @NotNull(message = "类别ID不能为空")
    private Integer id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
