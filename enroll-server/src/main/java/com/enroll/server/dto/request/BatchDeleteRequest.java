package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除报名请求 DTO（管理端，软删）
 */
public class BatchDeleteRequest {

    @NotEmpty(message = "ids不能为空")
    private List<Integer> ids;

    public List<Integer> getIds() { return ids; }
    public void setIds(List<Integer> ids) { this.ids = ids; }
}
