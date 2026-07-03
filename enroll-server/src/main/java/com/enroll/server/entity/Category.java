package com.enroll.server.entity;

import jakarta.persistence.*;

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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
