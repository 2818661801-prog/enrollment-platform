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
 * 职责：
 *   1. 调用 Repository 拿数据
 *   2. Entity → DTO 转换（隐藏数据库结构）
 *   3. 业务规则校验（不存在则抛 BusinessException）
 *
 * @Service 标记为 Spring Bean，Controller 注入使用
 * 构造器注入（Spring 推荐，避免字段注入）
 * @Transactional(readOnly = true) 类级别只读事务，查询自动复用
 */
@Service
@Transactional(readOnly = true)
public class ClassService {

    private static final Logger log = LoggerFactory.getLogger(ClassService.class);

    private final ClassInfoRepository classRepo;

    /** 构造器注入 */
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

    /** 按类别查班级 */
    public List<ClassDTO> listByCategory(String category) {
        return classRepo.findByCategory(category).stream()
                .filter(c -> c.getIsDeleted() == null || c.getIsDeleted() == 0)
                .map(this::toDTO)
                .collect(Collectors.toList());
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
     * @param body {name, period, round, round1Period, quota, description}
     */
    @Transactional
    public ClassDTO createClass(java.util.Map<String, Object> body) {
        ClassInfo cls = new ClassInfo();
        cls.setName((String) body.get("name"));
        cls.setPeriod((String) body.get("period"));
        cls.setRound((Integer) body.getOrDefault("round", 0));
        cls.setRound1Period((String) body.get("round1Period"));
        cls.setQuota((Integer) body.getOrDefault("quota", 0));
        cls.setEnrolled(0);
        cls.setDescription((String) body.get("description"));
        cls.setIsDeleted(0);
        ClassInfo saved = classRepo.save(cls);
        log.info("新增班级: id={}, name={}", saved.getId(), saved.getName());
        return toDTO(saved);
    }

    /**
     * 更新班级（含 round/quota/is_deleted 等所有可编辑字段）
     * @param id 班级ID
     * @param body 更新字段
     */
    @Transactional
    public ClassDTO updateClass(Integer id, java.util.Map<String, Object> body) {
        ClassInfo cls = classRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));
        if (body.containsKey("name"))         cls.setName((String) body.get("name"));
        if (body.containsKey("period"))      cls.setPeriod((String) body.get("period"));
        if (body.containsKey("round"))       cls.setRound((Integer) body.get("round"));
        if (body.containsKey("round1Period")) cls.setRound1Period((String) body.get("round1Period"));
        if (body.containsKey("quota"))        cls.setQuota((Integer) body.get("quota"));
        if (body.containsKey("description"))  cls.setDescription((String) body.get("description"));
        if (body.containsKey("isDeleted"))   cls.setIsDeleted((Integer) body.get("isDeleted"));
        ClassInfo saved = classRepo.save(cls);
        log.info("更新班级: id={}, name={}", saved.getId(), saved.getName());
        return toDTO(saved);
    }

    /** 修改报名时间段 */
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
                .period(e.getPeriod())
                .round(e.getRound() != null ? e.getRound() : 0)
                .round1Period(e.getRound1Period())
                .quota(e.getQuota())
                .enrolled(e.getEnrolled())
                .description(e.getDescription())
                .isDeleted(e.getIsDeleted() != null ? e.getIsDeleted() : 0)
                .build();
    }
}
