package com.enroll.server.dto.request;

import java.util.List;
import java.util.Map;

/**
 * 内网低代码平台同步班级请求 DTO
 *
 * 注意：不用 @NotEmpty——低代码平台同步时内网可能无数据（空列表），
 * 此时应正常返回"同步成功"而非 400（见 empty-ids-return-200 经验）。
 */
public class SyncClassesRequest {

    private List<Map<String, Object>> data;

    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
}
