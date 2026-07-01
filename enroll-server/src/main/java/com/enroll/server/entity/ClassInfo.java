package com.enroll.server.entity;

import jakarta.persistence.*;

/**
 * 班级信息实体 — 对应数据库 classes 表
 *
 * 多轮报名设计：
 *   periods = [{"round":1,"period":"2026/09/01 - 2026/09/13"},{"round":2,"period":"2026/09/15 - 2026/09/16"}]
 *   根据当前时间在 periods 中匹配，判断当前是第几轮
 */
@Entity
@Table(name = "classes")
public class ClassInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 30)
    private String period;   // 当前报名时间段（自动从 periods 中取，非直接存储）

    /**
     * 多轮时间段 JSON 数组：
     * [{"round":1,"period":"2026/09/01 - 2026/09/13"},{"round":2,"period":"2026/09/15 - 2026/09/16"}]
     * 数据库存 TEXT，应用层解析
     */
    @Column(name = "periods", columnDefinition = "TEXT")
    private String periods;  // JSON 字符串

    @Column(nullable = false)
    private Integer quota;

    @Column(nullable = false)
    private Integer enrolled = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 软删除：0=正常 1=已删除 */
    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;

    /** 班级类别（管理员自定义，如：理工类/经管类/无类别） */
    @Column(name = "category", length = 50)
    private String category;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** 当前有效时间段（根据当前日期从 periods 计算得出） */
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getPeriods() { return periods; }
    public void setPeriods(String periods) { this.periods = periods; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
