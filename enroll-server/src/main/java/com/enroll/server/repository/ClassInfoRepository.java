package com.enroll.server.repository;

import com.enroll.server.entity.ClassInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * 原子增加已报名人数（⚠️ S16 修复：名额校验 + enrolled+1 合并为一条 UPDATE）
     * @return 受影响行数，0 = 名额已满或班级不存在
     */
    @Modifying
    @Query(value = "UPDATE classes SET enrolled = enrolled + 1 WHERE id = :id AND (quota = -1 OR enrolled < quota)", nativeQuery = true)
    int incrementEnrolledIfQuotaAvailable(Integer id);

    /**
     * 原子减少已报名人数（⚠️ S16 修复：撤回时 enrolled-1 不超卖）
     * @return 受影响行数，0 = 班级不存在或 enrolled 已经是 0
     */
    @Modifying
    @Query(value = "UPDATE classes SET enrolled = GREATEST(0, enrolled - 1) WHERE id = :id", nativeQuery = true)
    int decrementEnrolled(Integer id);
}
