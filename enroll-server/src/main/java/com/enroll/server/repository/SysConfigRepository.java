package com.enroll.server.repository;

import com.enroll.server.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统配置 Repository
 *
 * 提供常用查询方法：
 *   - findByCfgKey  按键查配置
 *   - findAll        查所有配置（管理后台列表用）
 */
@Repository
public interface SysConfigRepository extends JpaRepository<SysConfig, Integer> {

    /**
     * 按配置键查配置
     * @param cfgKey 配置键（如 "notice"、"admin_phone"）
     * @return 配置对象（无则抛 NoSuchElementException）
     */
    Optional<SysConfig> findByCfgKey(String cfgKey);
}
