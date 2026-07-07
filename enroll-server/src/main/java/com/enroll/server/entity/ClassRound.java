package com.enroll.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 班级轮次实体 — 对应数据库 class_rounds 表
 *
 * 替代 classes.periods JSON 字段，轮次时间分开存储，方便索引和查询
 *
 * 表结构：
 *   class_id     → 关联 ssc_classes.id
 *   round_num    → 轮次编号（1/2/3...）
 *   period_start → 报名开始时间
 *   period_end   → 报名结束时间
 */
@Entity
@Table(name = "ssc_class_rounds",
       uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "round_num"}),
       indexes = {
           @Index(name = "idx_class_id",     columnList = "class_id"),
           @Index(name = "idx_period_start", columnList = "period_start"),
           @Index(name = "idx_period_end",   columnList = "period_end")
       })
public class ClassRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 班级ID，关联 ssc_classes.id */
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    /** 轮次编号（1/2/3...） */
    @Column(name = "round_num", nullable = false)
    private Integer roundNum;

    /** 报名开始时间 */
    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    /** 报名结束时间 */
    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Integer getRoundNum() { return roundNum; }
    public void setRoundNum(Integer roundNum) { this.roundNum = roundNum; }

    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
