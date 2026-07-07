package com.enroll.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 班级-类别中间表
 * 对应数据库 ssc_class_category 表
 *
 * 设计：一个班级可属于多个类别，通过中间表实现 N:N 关联
 *   classes (id) ←→ ssc_class_category (class_id) ←→ categories (id)
 */
@Entity
@Table(name = "ssc_class_category")
public class ClassCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 班级ID，关联 ssc_classes.id */
    @Column(name = "class_id", nullable = false)
    private Integer classId;

    /** 类别ID，关联 ssc_categories.id */
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
