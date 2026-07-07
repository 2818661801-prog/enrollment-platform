package com.enroll.server.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 班级类别字典表
 * 用于管理员维护"经管类""理工类"等类别，供班级管理下拉选择
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

    /** 该类别下的所有班级（通过中间表 ssc_class_category） */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ssc_class_category",
        joinColumns = @JoinColumn(name = "category_id"),
        inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    private List<ClassInfo> classes = new ArrayList<>();

    // ==================== getter / setter ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<ClassInfo> getClasses() { return classes; }
    public void setClasses(List<ClassInfo> classes) { this.classes = classes; }
}
