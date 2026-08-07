package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 撤回报名请求 DTO（只传报名记录 id）
 */
public class WithdrawRequest {

    @NotNull(message = "报名ID不能为空")
    private Integer id;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
