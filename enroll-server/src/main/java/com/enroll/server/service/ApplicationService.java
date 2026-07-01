package com.enroll.server.service;

import com.enroll.server.dto.ApplicationDTO;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.entity.Application;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.repository.ApplicationRepository;
import com.enroll.server.repository.ClassInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报名业务层（核心）
 *
 * 业务规则：
 *   1. 防重复报名：同身份证 + 同班级 只能报一次
 *   2. 校验名额：班级 enrolled >= quota 则拒绝
 *   3. 提交后自动 +1
 *   4. 撤回后自动 -1
 *   5. 身份证脱敏：中间 8 位 → ********
 *
 * 事务边界：
 *   - 写方法（submit/withdraw）@Transactional，失败自动回滚
 *   - 读方法（findMy）继承类级别 readOnly = true
 */
@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository appRepo;
    private final ClassInfoRepository classRepo;

    /** 构造器注入 */
    public ApplicationService(ApplicationRepository appRepo, ClassInfoRepository classRepo) {
        this.appRepo = appRepo;
        this.classRepo = classRepo;
    }

    // ==================== 提交报名（写） ====================

    /**
     * 提交报名
     * @param form 前端提交的表单 {name, idCard, gender, phone, ...}
     * @return 报名 DTO（含新分配的 ID）
     */
    @Transactional
    public ApplicationDTO submit(Map<String, Object> form) {
        String idCard = (String) form.get("idCard");
        Integer classId = (Integer) form.get("classId");

        // 1) 防重复
        List<Application> existing = appRepo.findByIdCardAndClassId(idCard, classId);
        if (!existing.isEmpty()) {
            throw new BusinessException(ResultCode.DUPLICATE_APPLICATION);
        }

        // 2) 校验班级 + 名额
        ClassInfo cls = classRepo.findById(classId)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));
        if (cls.getEnrolled() >= cls.getQuota()) {
            throw new BusinessException(ResultCode.CLASS_FULL);
        }

        // 3) 构造报名记录
        Application app = new Application();
        app.setName((String) form.get("name"));
        app.setIdCard(idCard);
        app.setIdCardMasked(maskIdCard(idCard));
        app.setGender((String) form.get("gender"));
        app.setPhone((String) form.get("phone"));
        app.setHasPhysics((String) form.get("hasPhysics"));
        // hasEnglish：只有 ACCA/CFA/智能财务 才会填，非这三类默认"否"
        app.setHasEnglish((String) form.getOrDefault("hasEnglish", "否"));
        app.setClassId(classId);
        app.setHdSubType((String) form.get("hdSubType"));
        app.setStatus("已报名");
        app.setApplyTime(LocalDateTime.now());
        Application saved = appRepo.save(app);

        // 4) 班级已报名人数 +1
        cls.setEnrolled(cls.getEnrolled() + 1);
        classRepo.save(cls);

        log.info("报名成功: id={}, name={}, classId={}", saved.getId(), saved.getName(), classId);
        return toDTO(saved, cls.getName());
    }

    // ==================== 撤回报名（写） ====================

    /**
     * 撤回报名
     * @param id 报名记录 ID
     */
    @Transactional
    public void withdraw(Integer id) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.APPLICATION_NOT_FOUND));

        // 班级已报名人数 -1（保底不为负）
        classRepo.findById(app.getClassId()).ifPresent(cls -> {
            cls.setEnrolled(Math.max(0, cls.getEnrolled() - 1));
            classRepo.save(cls);
        });

        appRepo.delete(app);
        log.info("撤回报名: id={}, name={}", id, app.getName());
    }

    // ==================== 我的报名（读） ====================

    /**
     * 按身份证查我的报名列表
     */
    public List<ApplicationDTO> findMy(String idCard) {
        return appRepo.findByIdCard(idCard).stream()
                .map(app -> {
                    String className = classRepo.findById(app.getClassId())
                            .map(ClassInfo::getName)
                            .orElse("未知班级");
                    return toDTO(app, className);
                })
                .collect(Collectors.toList());
    }

    // ==================== 内部工具 ====================

    /** 身份证脱敏：保留前 6 后 4，中间 8 位换 * */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) return idCard;
        return idCard.replaceAll("(?<=^.{6}).{8}(?=.{4}$)", "********");
    }

    /** Entity → DTO（隐藏完整身份证） */
    private ApplicationDTO toDTO(Application e, String className) {
        return ApplicationDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .idCard(e.getIdCardMasked())  // 返脱敏版
                .gender(e.getGender())
                .phone(e.getPhone())
                .hasPhysics(e.getHasPhysics())
                .hasEnglish(e.getHasEnglish())
                .classId(e.getClassId())
                .className(className)
                .hdSubType(e.getHdSubType())
                .status(e.getStatus())
                .applyTime(e.getApplyTime())
                .build();
    }
}
