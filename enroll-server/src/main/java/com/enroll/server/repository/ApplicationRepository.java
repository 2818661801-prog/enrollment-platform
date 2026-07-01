package com.enroll.server.repository;

import com.enroll.server.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 报名记录数据访问层
 *
 * 自定义查询方法命名规则（Spring Data JPA 自动解析）：
 *   findByIdCard              → WHERE id_card = ?
 *   findByIdCardAndClassId    → WHERE id_card = ? AND class_id = ?
 *   findByClassId             → WHERE class_id = ?
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    /** 按身份证号查该学生的所有报名记录 */
    List<Application> findByIdCard(String idCard);

    /** 按班级 ID 查该班的所有报名 */
    List<Application> findByClassId(Integer classId);

    /** 按身份证号 + 班级 ID 查（防止重复报名） */
    List<Application> findByIdCardAndClassId(String idCard, Integer classId);
}
