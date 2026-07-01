package com.enroll.server.repository;

import com.enroll.server.entity.ClassInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 班级数据访问层
 *
 * 继承 JpaRepository 后自动获得的方法（不用写）：
 *   findAll()           → SELECT * FROM classes
 *   findById(id)        → SELECT * FROM classes WHERE id = ?
 *   save(entity)        → INSERT / UPDATE
 *   deleteById(id)      → DELETE FROM classes WHERE id = ?
 */
@Repository
public interface ClassInfoRepository extends JpaRepository<ClassInfo, Integer> {
}
