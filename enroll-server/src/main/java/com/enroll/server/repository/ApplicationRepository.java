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

    /** 按身份证号 + status列表 查（全局唯一报名检查：已报名/已录取/未录取状态不能重复报） */
    List<Application> findByIdCardAndStatusIn(String idCard, List<Integer> statuses);

    /** 按班级 ID 查该班的所有报名 */
    List<Application> findByClassId(Integer classId);

    /** 按身份证号 + 班级 ID 查（防止重复报名，只查 status=0 的） */
    List<Application> findByIdCardAndClassIdAndStatus(String idCard, Integer classId, Integer status);

    /** 按身份证号 + 班级 ID + status列表 查（查某几状态的报名） */
    List<Application> findByIdCardAndClassIdAndStatusIn(String idCard, Integer classId, List<Integer> statuses);

    /** 按手机号 + status 查（学生端 JWT 认证查询） */
    List<Application> findByPhoneAndStatus(String phone, Integer status);

    /** 按手机号 + status列表 查（登录时判断是否有有效报名） */
    List<Application> findByPhoneAndStatusIn(String phone, List<Integer> statuses);

    // ===== is_deleted=0 过滤新增方法（2026-07-10） =====

    /** 查所有未删除的报名记录 */
    List<Application> findByIsDeleted(Integer isDeleted);

    /** 按班级ID + 未删除 查 */
    List<Application> findByClassIdAndIsDeleted(Integer classId, Integer isDeleted);

    /** 按身份证号 + 未删除 查 */
    List<Application> findByIdCardAndIsDeleted(String idCard, Integer isDeleted);

    /** 按身份证号 + status列表 + 未删除 查（登录时判断有效报名） */
    List<Application> findByIdCardAndStatusInAndIsDeleted(String idCard, List<Integer> statuses, Integer isDeleted);

    /** 按身份证号 + status + 未删除 查 */
    List<Application> findByIdCardAndStatusAndIsDeleted(String idCard, Integer status, Integer isDeleted);

    /** 按手机号 + status列表 + 未删除 查（学生端 JWT 认证） */
    List<Application> findByPhoneAndStatusInAndIsDeleted(String phone, List<Integer> statuses, Integer isDeleted);

    /** 按身份证号+班级ID+status + 未删除（防重复报名） */
    List<Application> findByIdCardAndClassIdAndStatusAndIsDeleted(String idCard, Integer classId, Integer status, Integer isDeleted);

    /** 按身份证号+班级ID+status列表 + 未删除（查某几状态） */
    List<Application> findByIdCardAndClassIdAndStatusInAndIsDeleted(String idCard, Integer classId, List<Integer> statuses, Integer isDeleted);

    /**
     * 管理员分页查询（支持多条件组合）
     * 动态 SQL：只有传入非 null 的条件才会拼入 WHERE
     */
    @Query("SELECT a FROM Application a WHERE a.isDeleted = 0 AND " +
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
    @Query("UPDATE Application a SET a.status = :status WHERE a.id IN :ids AND a.isDeleted = 0")
    void batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") Integer status);

    /**
     * 批量更新 status + auditComment（如批量录取/未录取时写入审核意见）
     * @param ids           报名记录 ID 列表
     * @param status        目标状态值
     * @param auditComment  审核意见
     */
    @Modifying
    @Query("UPDATE Application a SET a.status = :status, a.auditComment = :auditComment WHERE a.id IN :ids AND a.isDeleted = 0")
    void batchUpdateStatusAndComment(@Param("ids") List<Integer> ids, @Param("status") Integer status, @Param("auditComment") String auditComment);
}
