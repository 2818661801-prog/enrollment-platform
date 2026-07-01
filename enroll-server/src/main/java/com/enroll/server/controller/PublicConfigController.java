package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.repository.SysConfigRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学生端公开配置接口（无需鉴权）
 *
 * GET /api/config/notice — 获取报名须知内容
 */
@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    private final SysConfigRepository sysConfigRepo;

    public PublicConfigController(SysConfigRepository sysConfigRepo) {
        this.sysConfigRepo = sysConfigRepo;
    }

    /** 获取报名须知（学生端弹窗内容） */
    @GetMapping("/notice")
    public Map<String, Object> getNotice() {
        return sysConfigRepo.findByCfgKey("notice")
                .map(cfg -> R.ok(cfg.getCfgValue()))
                .orElse(R.ok("{}")); // 无配置时返空 JSON
    }
}
