package com.enroll.server.repository;

import com.enroll.server.entity.ClassCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 班级-类别中间表数据访问层
 */
@Repository
public interface ClassCategoryRepository extends JpaRepository<ClassCategory, Integer> {

    /** 按班级ID查所有类别ID */
    List<ClassCategory> findByClassId(Integer classId);

    /** 按班级ID删所有关联（同步前清理用，立即执行） */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ClassCategory cc WHERE cc.classId = :classId")
    void deleteByClassId(@Param("classId") Integer classId);

    /** 按班级ID查所有类别名称 */
    @Query("SELECT cat.name FROM ClassCategory cc JOIN Category cat ON cat.id = cc.categoryId WHERE cc.classId = :classId")
    List<String> findCategoryNamesByClassId(@Param("classId") Integer classId);

    /** 按班级ID集合批量查类别名称 */
    @Query("SELECT cc.classId, cat.name FROM ClassCategory cc JOIN Category cat ON cat.id = cc.categoryId WHERE cc.classId IN :classIds")
    List<Object[]> findCategoryNamesByClassIds(@Param("classIds") List<Integer> classIds);
}
