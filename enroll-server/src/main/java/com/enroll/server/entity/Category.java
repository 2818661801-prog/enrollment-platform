package com.enroll.server.entity;

import jakarta.persistence.*;

/**
 * 班级类别字典表
 * 用于管理员维护"经管类""理工类"等类别，供班级管理下拉选择
 *
 * ⚠️ 28.3 已删除 @ManyToMany classes 字段：
 *   ssc_class_category 中间表 + 双向关联当前业务根本没用到
 *   （类别只是标签字符串，班级-类别关系不走这个表，而是走 ClassCategoryRepository）
 */
@Entity
@Table(name = "ssc_categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 类别名称，如"经管类"、"理工类"、"成电班" */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
