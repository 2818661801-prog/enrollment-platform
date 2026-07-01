package com.enroll.server.entity;

import jakarta.persistence.*;

/**
 * 班级信息实体 — 对应数据库 classes 表
 *
 * 类比：每个 ClassInfo 对象 = 数据库 classes 表里的一行数据
 *      @Entity 告诉 JPA："这个类对应一张数据库表"
 *      @Column  = 表的列定义（类型、长度、是否必填）
 *
 * 用 Lombok @Data 可省写 getter/setter，这里手写以便主人看懂
 */
@Entity
@Table(name = "classes")     // 数据库表名
public class ClassInfo {

    @Id                       // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL 自增
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;      // 班级名称

    @Column(nullable = false, length = 30)
    private String period;    // 报名时间段，如 "2026/07/01 - 2026/08/15"

    @Column(nullable = false)
    private Integer quota;    // 招生名额

    @Column(nullable = false)
    private Integer enrolled = 0; // 已报名人数（默认 0）

    @Column(columnDefinition = "TEXT")
    private String description;    // 班级简介（TEXT = 长文本）

    /** 0=单轮 1=两轮（成电班） */
    @Column(nullable = false)
    private Integer round = 0;

    /** 软删除：0=正常 1=已删除 */
    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }

    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
