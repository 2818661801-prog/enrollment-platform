package com.enroll.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统配置实体 — 对应数据库 sys_config 表
 *
 * 表结构（v2.2 起）：
 *   title       — 报名须知标题
 *   conditions  — 报名条件（换行分隔）
 *   notices     — 报名须知（换行分隔）
 *   updatedAt   — 最后修改时间（自动更新）
 *   updatedBy   — 最后修改人
 */
@Entity
@Table(name = "ssc_sys_config")
public class SysConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 报名须知标题，如"2026年特色班报名须知" */
    @Column(name = "title", length = 200)
    private String title;

    /** 报名条件（换行分隔），如"报名者须为2026级新生\n每人限报1个特色班" */
    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    /** 报名须知（换行分隔） */
    @Column(name = "notices", columnDefinition = "TEXT")
    private String notices;

    /** 最后修改时间（自动更新） */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 最后修改人 */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /** 全局联系方式（2026-07-26 新增）*/
    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }

    public String getNotices() { return notices; }
    public void setNotices(String notices) { this.notices = notices; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
