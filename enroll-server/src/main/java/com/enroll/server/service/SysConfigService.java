package com.enroll.server.service;

import com.enroll.server.entity.SysConfig;
import com.enroll.server.repository.SysConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系统配置 Service（报名须知 CRUD）
 *
 * 为什么抽这个 Service：PublicConfigController 和 AdminController 都有
 * "读/写 sys_config id=1" 的重复代码，下沉到一处，两个 Controller 都调它。
 */
@Service
@Transactional(readOnly = true)
public class SysConfigService {

    private final SysConfigRepository sysConfigRepo;

    public SysConfigService(SysConfigRepository sysConfigRepo) {
        this.sysConfigRepo = sysConfigRepo;
    }

    /** 获取报名须知（合并 PublicConfigController + AdminController 的重复代码） */
    public Map<String, String> getNotice() {
        return sysConfigRepo.findById(1)
                .map(cfg -> Map.of(
                    "title", cfg.getTitle() != null ? cfg.getTitle() : "",
                    "conditions", cfg.getConditions() != null ? cfg.getConditions() : "",
                    "notices", cfg.getNotices() != null ? cfg.getNotices() : "",
                    "contactInfo", cfg.getContactInfo() != null ? cfg.getContactInfo() : ""
                ))
                .orElse(Map.of("title", "", "conditions", "", "notices", "", "contactInfo", ""));
    }

    /** 更新报名须知 */
    @Transactional
    public void updateNotice(String title, String conditions, String notices, String contactInfo, String updatedBy) {
        SysConfig cfg = sysConfigRepo.findById(1).orElse(new SysConfig());
        cfg.setTitle(title);
        cfg.setConditions(conditions);
        cfg.setNotices(notices);
        cfg.setContactInfo(contactInfo);
        cfg.setUpdatedBy(updatedBy);
        cfg.setUpdatedAt(LocalDateTime.now());
        sysConfigRepo.save(cfg);
    }
}
