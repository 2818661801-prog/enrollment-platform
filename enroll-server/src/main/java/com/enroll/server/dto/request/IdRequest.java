package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 通用 ID 请求 DTO（复用于 deleteClass 等按 id 操作的接口）
 */
public class IdRequest {

    @NotNull(message = "ID不能为空")
    private Integer id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
