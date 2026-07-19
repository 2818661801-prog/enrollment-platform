package com.enroll.server.entity;

import jakarta.persistence.*;

/**
 * 班级信息实体 — 对应数据库 ssc_classes 表
 *
 * 班级类别（category_names JSON → 中间表 ssc_class_category）：
 *   一个班级可属多个类别，通过中间表实现 N:N 关联
 */
@Entity
@Table(name = "ssc_classes")
public class ClassInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 30)
    private String period;   // 当前有效时间段（由 class_rounds 表实时计算得出）

    @Column(nullable = false)
    private Integer quota;

    @Column(nullable = false)
    private Integer enrolled = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 软删除：0=正常 1=已删除 */
    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;

    /**
     * 数据来源（2026-07-02 内外网架构新增）
     *   admin  — 管理员通过 AdminDashboard 创建
     *   sync   — 内网低代码平台同步过来
     *   student — 学生自建（极少，通常班级由管理员建）
     */
    @Column(name = "source", nullable = false, length = 20)
    private String source = "admin";  // 默认 admin（保守起见）

    /**
     * 内网班级ID（2026-07-15 新增，用于跨系统 id 映射）
     * 外网收到内网 sync 数据后，将内网的 class id 存到此字段
     */
    @Column(name = "outer_id")
    private Integer outerId;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** 当前有效时间段（由 class_rounds 表实时计算得出） */
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getOuterId() { return outerId; }
    public void setOuterId(Integer outerId) { this.outerId = outerId; }
}
