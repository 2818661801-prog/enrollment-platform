package com.enroll.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统配置实体 — 对应数据库 sys_config 表
 *
 * 设计思路：
 *   - 报名须知（notice）、管理员手机号（admin_phone）等键值对存这里
 *   - cfg_value 用 TEXT 存 JSON 字符串，应用层自己解析
 *   - updated_at 自动更新，updated_by 由调用方写入
 *
 * 用法：
 *   cfgRepo.findByCfgKey("notice").ifPresent(cfg -> {
 *       JSONObject json = JSONObject.parseObject(cfg.getCfgValue());
 *   });
 */
@Entity
@Table(name = "sys_config")
public class SysConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 配置键（UNIQUE，如 "notice"、"admin_phone"） */
    @Column(name = "cfg_key", nullable = false, unique = true, length = 50)
    private String cfgKey;

    /** 配置值（JSON 字符串，如 "{\"title\":\"报名须知\",\"conditions\":[...]}"） */
    @Column(name = "cfg_value", columnDefinition = "TEXT")
    private String cfgValue;

    /** 最后修改时间（自动更新） */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 最后修改人（如 "admin138"） */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCfgKey() { return cfgKey; }
    public void setCfgKey(String cfgKey) { this.cfgKey = cfgKey; }

    public String getCfgValue() { return cfgValue; }
    public void setCfgValue(String cfgValue) { this.cfgValue = cfgValue; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
