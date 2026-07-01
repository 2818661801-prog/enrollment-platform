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

    /** 查所有班级 */
    public List<ClassDTO> listAll() {
        return classRepo.findAll().stream()
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
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==================== 内部：Entity → DTO ====================

    private ClassDTO toDTO(ClassInfo e) {
        return ClassDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .category(e.getCategory())
                .period(e.getPeriod())
                .quota(e.getQuota())
                .enrolled(e.getEnrolled())
                .needPhysics(e.getNeedPhysics())
                .description(e.getDescription())
                .build();
    }
}
