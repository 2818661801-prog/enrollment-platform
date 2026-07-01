package com.enroll.server.repository;

import com.enroll.server.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** 按身份证号 + status 查（查某状态下的报名） */
    List<Application> findByIdCardAndStatus(String idCard, Integer status);

    /** 按班级 ID 查该班的所有报名 */
    List<Application> findByClassId(Integer classId);

    /** 按身份证号 + 班级 ID 查（防止重复报名，只查 status=1 的） */
    List<Application> findByIdCardAndClassIdAndStatus(String idCard, Integer classId, Integer status);

    /** 按身份证号 + 班级 ID + status列表 查（查某几状态的报名） */
    List<Application> findByIdCardAndClassIdAndStatusIn(String idCard, Integer classId, List<Integer> statuses);

    /** 按手机号 + status 查（学生端 JWT 认证查询） */
    List<Application> findByPhoneAndStatus(String phone, Integer status);

    /**
     * 管理员分页查询（支持多条件组合）
     * 动态 SQL：只有传入非 null 的条件才会拼入 WHERE
     */
    @Query("SELECT a FROM Application a WHERE " +
           "(:classId  IS NULL OR a.classId  = :classId) AND " +
           "(:status   IS NULL OR a.status   = :status)  AND " +
           "(:idCard   IS NULL OR a.idCard   LIKE CONCAT('%',:idCard,'%')) AND " +
           "(:name     IS NULL OR a.name     LIKE CONCAT('%',:name,'%'))")
    Page<Application> adminSearch(
            @Param("classId") Integer classId,
            @Param("status")  Integer status,
            @Param("idCard")  String idCard,
            @Param("name")    String name,
            Pageable pageable);

    /**
     * 批量更新 status（如批量撤回、批量录取）
     * @param ids     报名记录 ID 列表
     * @param status  目标状态值
     */
    @Modifying
    @Query("UPDATE Application a SET a.status = :status WHERE a.id IN :ids")
    void batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") Integer status);

    /**
     * 批量更新 is_admitted（录取）
     * @param ids          报名记录 ID 列表
     * @param isAdmitted   录取标志：0否 1是
     */
    @Modifying
    @Query("UPDATE Application a SET a.isAdmitted = :isAdmitted WHERE a.id IN :ids")
    void batchUpdateAdmitted(@Param("ids") List<Integer> ids, @Param("isAdmitted") Integer isAdmitted);
}
