package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * 内网低代码平台同步类别请求 DTO
 */
public class SyncCategoriesRequest {

    @NotEmpty(message = "data不能为空")
    private List<Map<String, Object>> data;

    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
}
