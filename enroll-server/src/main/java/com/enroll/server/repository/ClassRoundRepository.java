package com.enroll.server.repository;

import com.enroll.server.entity.ClassRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * 全量清空（同步前用 native SQL 避免乐观锁问题）
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "TRUNCATE TABLE ssc_class_rounds", nativeQuery = true)
    void truncateAll();

    /**
     * 查某班级当前有效轮次（根据 MySQL NOW() 当前时间匹配）
     * @return 当前在报名时间内的那一轮，null 表示当前不在任何报名时间内
     * @see #findCurrentRound(Integer) 用 MySQL NOW() 做时间基准，消除应用服务器与数据库服务器时钟差
     */
    @Query(value = "SELECT * FROM ssc_class_rounds r WHERE r.class_id = :classId " +
           "AND NOW() >= r.period_start AND NOW() <= r.period_end " +
           "ORDER BY r.round_num LIMIT 1", nativeQuery = true)
    ClassRound findCurrentRound(@Param("classId") Integer classId);

    /**
     * 判断某班级某轮次是否已过截止时间（用 MySQL NOW()，消除应用服务器与数据库服务器时钟差）
     * @return true 表示已过期（NOW() > period_end），false 表示还在报名时间内或轮次不存在
     */
    @Query(value = "SELECT COUNT(*) FROM ssc_class_rounds " +
           "WHERE class_id = :classId AND round_num = :roundNum AND NOW() > period_end",
           nativeQuery = true)
    int countExpired(@Param("classId") Integer classId, @Param("roundNum") Integer roundNum);

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
