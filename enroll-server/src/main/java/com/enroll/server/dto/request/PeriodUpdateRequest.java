package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 更新班级时间段请求 DTO（管理端）
 */
public class PeriodUpdateRequest {

    @NotNull(message = "班级ID不能为空")
    private Integer id;
    private String period;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
