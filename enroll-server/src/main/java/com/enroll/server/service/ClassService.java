package com.enroll.server.service;

import com.enroll.server.dto.ClassDTO;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.repository.ClassInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级业务层
 *
 * 多轮设计（periods JSON）：
 *   [{"round":1,"period":"2026/09/01 - 2026/09/13"},{"round":2,"period":"2026/09/15 - 2026/09/16"}]
 *   根据当前时间自动计算当前有效时间段
 */
@Service
@Transactional(readOnly = true)
public class ClassService {

    private static final Logger log = LoggerFactory.getLogger(ClassService.class);

    private final ClassInfoRepository classRepo;

    public ClassService(ClassInfoRepository classRepo) {
        this.classRepo = classRepo;
    }

    /** 查所有未删除班级（学生端用） */
    public List<ClassDTO> listAll() {
        return classRepo.findAll().stream()
                .filter(c -> c.getIsDeleted() == null || c.getIsDeleted() == 0)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** 按 ID 查班级 */
    public ClassDTO getById(Integer id) {
        ClassInfo cls = classRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));
        return toDTO(cls);
    }

    // ==================== 管理端方法 ====================

    /** 查所有班级（含已删除，供管理后台用） */
    public List<ClassDTO> listAllForAdmin() {
        return classRepo.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 新增班级
     * @param body {name, periods(JSON数组), quota, description}
     */
    @Transactional
    public ClassDTO createClass(java.util.Map<String, Object> body) {
        ClassInfo cls = new ClassInfo();
        cls.setName((String) body.get("name"));
        cls.setPeriods((String) body.get("periods"));
        // periods[0].period 作为默认 period
        cls.setPeriod(extractFirstPeriod((String) body.get("periods")));
        cls.setQuota((Integer) body.getOrDefault("quota", 0));
        cls.setEnrolled(0);
        cls.setDescription((String) body.get("description"));
        cls.setIsDeleted(0);
        ClassInfo saved = classRepo.save(cls);
        log.info("新增班级: id={}, name={}", saved.getId(), saved.getName());
        return toDTO(saved);
    }

    /**
     * 更新班级
     * @param id 班级ID
     * @param body 可编辑字段
     */
    @Transactional
    public ClassDTO updateClass(Integer id, java.util.Map<String, Object> body) {
        ClassInfo cls = classRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));
        if (body.containsKey("name"))        cls.setName((String) body.get("name"));
        if (body.containsKey("periods")) {
            cls.setPeriods((String) body.get("periods"));
            cls.setPeriod(extractFirstPeriod((String) body.get("periods")));
        }
        if (body.containsKey("period"))      cls.setPeriod((String) body.get("period"));
        if (body.containsKey("quota"))       cls.setQuota((Integer) body.get("quota"));
        if (body.containsKey("description")) cls.setDescription((String) body.get("description"));
        if (body.containsKey("isDeleted"))  cls.setIsDeleted((Integer) body.get("isDeleted"));
        ClassInfo saved = classRepo.save(cls);
        log.info("更新班级: id={}, name={}", saved.getId(), saved.getName());
        return toDTO(saved);
    }

    /** 修改报名时间段（兼容旧接口） */
    @Transactional
    public ClassDTO updatePeriod(Integer id, String period) {
        ClassInfo cls = classRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));
        cls.setPeriod(period);
        return toDTO(classRepo.save(cls));
    }

    /** 修改配额 */
    @Transactional
    public ClassDTO updateQuota(Integer id, Integer quota) {
        ClassInfo cls = classRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));
        cls.setQuota(quota);
        return toDTO(classRepo.save(cls));
    }

    /** 软删除班级 */
    @Transactional
    public void deleteClass(Integer id) {
        ClassInfo cls = classRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));
        cls.setIsDeleted(1);
        classRepo.save(cls);
        log.info("软删除班级: id={}, name={}", id, cls.getName());
    }

    // ==================== 内部：Entity → DTO ====================

    private ClassDTO toDTO(ClassInfo e) {
        return ClassDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .period(e.getPeriod())                    // 当前有效时间段
                .periods(e.getPeriods())                  // 原始 periods JSON
                .quota(e.getQuota())
                .enrolled(e.getEnrolled())
                .description(e.getDescription())
                .isDeleted(e.getIsDeleted() != null ? e.getIsDeleted() : 0)
                .build();
    }

    /**
     * 从 periods JSON 提取第一个 period（用于填充 period 字段）
     * @param periodsJson [{"round":1,"period":"2026/09/01 - 2026/09/13"},...]
     */
    private String extractFirstPeriod(String periodsJson) {
        if (periodsJson == null || periodsJson.isBlank()) return null;
        try {
            var list = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(periodsJson, java.util.List.class);
            if (!list.isEmpty()) {
                var first = (java.util.Map<String, Object>) list.get(0);
                return (String) first.get("period");
            }
        } catch (Exception e) {
            log.warn("periods JSON 解析失败: {}", periodsJson);
        }
        return null;
    }
}
