package com.enroll.server.repository;

import com.enroll.server.entity.ClassRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班级轮次 Repository
 */
@Repository
public interface ClassRoundRepository extends JpaRepository<ClassRound, Integer> {

    /**
     * 查某班级所有轮次（按轮次号升序）
     */
    List<ClassRound> findByClassIdOrderByRoundNum(Integer classId);

    /**
     * 删某班级所有轮次（同步前清理用，原生 SQL 立即执行）
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM ssc_class_rounds WHERE class_id = :classId", nativeQuery = true)
    void deleteByClassId(Integer classId);

    /**
     * 查某班级当前有效轮次（根据当前时间匹配）
     * @return 当前在报名时间内的那一轮，null 表示当前不在任何报名时间内
     */
    @Query("SELECT r FROM ClassRound r WHERE r.classId = :classId " +
           "AND :now >= r.periodStart AND :now <= r.periodEnd " +
           "ORDER BY r.roundNum LIMIT 1")
    ClassRound findCurrentRound(@Param("classId") Integer classId, @Param("now") LocalDateTime now);

    /**
     * 查某班级最大轮次号（用于动态算当前是第几轮）
     */
    @Query("SELECT MAX(r.roundNum) FROM ClassRound r WHERE r.classId = :classId")
    Integer findMaxRoundNum(@Param("classId") Integer classId);

    /**
     * 批量查多个班级的轮次（用于首页列表展示）
     */
    List<ClassRound> findByClassIdIn(List<Integer> classIds);
}
