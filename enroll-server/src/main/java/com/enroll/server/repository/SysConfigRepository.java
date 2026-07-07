package com.enroll.server.repository;

import com.enroll.server.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统配置 Repository
 *
 * v2.2 起 sys_config 只有一条记录（id=1），直接用 findById(1) 查询
 */
@Repository
public interface SysConfigRepository extends JpaRepository<SysConfig, Integer> {
    // 不再需要 findByCfgKey，实体已无 cfgKey 字段
}
