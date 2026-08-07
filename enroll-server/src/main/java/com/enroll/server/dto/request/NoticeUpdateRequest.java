package com.enroll.server.dto.request;

/**
 * 更新报名须知请求 DTO（管理端）
 */
public class NoticeUpdateRequest {

    private String title;
    private String conditions;
    private String notices;
    private String contactInfo;
    private String updatedBy;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }

    public String getNotices() { return notices; }
    public void setNotices(String notices) { this.notices = notices; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
