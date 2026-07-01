package com.enroll.server.repository;

import com.enroll.server.entity.ClassInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 班级数据访问层
 *
 * 类比：餐厅的"菜单"——你只需说"我要查理工类的菜"，
 *      Spring Data JPA 自动帮你写 SQL 并执行。
 *
 * 继承 JpaRepository 后自动获得的方法（不用写）：
 *   findAll()           → SELECT * FROM classes
 *   findById(id)        → SELECT * FROM classes WHERE id = ?
 *   save(entity)        → INSERT / UPDATE
 *   deleteById(id)      → DELETE FROM classes WHERE id = ?
 *
 * 自定义方法：方法名 = 查询条件，Spring 自动解析
 *   findByCategory("理工类") → WHERE category = '理工类'
 */
@Repository // 标记为数据访问层组件（可省略，但明确意图）
public interface ClassInfoRepository extends JpaRepository<ClassInfo, Integer> {

    /**
     * 按类别筛选班级
     * findBy + Category（字段名首字母大写）= WHERE category = ?
     */
    List<ClassInfo> findByCategory(String category);
}
