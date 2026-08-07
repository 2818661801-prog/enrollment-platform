package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 更新班级配额请求 DTO（管理端）
 */
public class QuotaUpdateRequest {

    @NotNull(message = "班级ID不能为空")
    private Integer id;
    @NotNull(message = "配额不能为空")
    private Integer quota;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }
}
