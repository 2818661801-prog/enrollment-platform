package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量录取请求 DTO（管理端）
 */
public class BatchAdmitRequest {

    @NotEmpty(message = "ids不能为空")
    private List<Integer> ids;
    private String auditComment;

    public List<Integer> getIds() { return ids; }
    public void setIds(List<Integer> ids) { this.ids = ids; }

    public String getAuditComment() { return auditComment; }
    public void setAuditComment(String auditComment) { this.auditComment = auditComment; }
}
