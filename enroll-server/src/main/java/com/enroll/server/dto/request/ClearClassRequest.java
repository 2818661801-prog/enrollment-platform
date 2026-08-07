package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 清空某班报名请求 DTO（管理端）
 */
public class ClearClassRequest {

    @NotNull(message = "班级ID不能为空")
    private Integer classId;

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
}
