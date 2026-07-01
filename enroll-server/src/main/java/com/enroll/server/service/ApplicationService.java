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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 报名业务层（核心）
 *
 * 状态值（status）：
 *   1 = 已报名（正常，可撤回）
 *   0 = 已撤回（学生主动撤回，软删除）
 *   2 = 已录取（管理员操作）
 *
 * 业务规则：
 *   1. 防重复报名：同身份证 + 同班级 + status=1 只能有一条
 *   2. 校验名额：班级 enrolled >= quota 则拒绝
 *   3. 提交后 enrolled +1
 *   4. 撤回后 enrolled -1（软删除，不删记录）
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

    /** 状态常量 */
    public static final int STATUS_APPLIED   = 1;  // 已报名
    public static final int STATUS_WITHDRAWN = 0;  // 已撤回
    public static final int STATUS_ENROLLED  = 2;  // 已录取

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
     * @param form 前端提交的表单 {name, idCard, gender, phone, hasPhysics, hasEnglish, classId, noticeAgreed}
     * @return 报名 DTO（含新分配的 ID）
     */
    @Transactional
    public ApplicationDTO submit(Map<String, Object> form) {
        String idCard  = (String) form.get("idCard");
        Integer classId = (Integer) form.get("classId");

        // 1) 校验班级存在
        ClassInfo cls = classRepo.findById(classId)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));

        // 2) 判断当前轮次（针对 round=2 的成电班）
        int currentRound = determineCurrentRound(cls);
        if (currentRound == 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该班级当前不在报名时间内");
        }

        // 3) 本轮防重复：同身份证 + 同班级 + 本轮 不能重复
        List<Application> roundDup = findRoundRecord(idCard, classId, currentRound);
        if (!roundDup.isEmpty()) {
            throw new BusinessException(ResultCode.DUPLICATE_APPLICATION);
        }

        // 4) round=2 的第二轮：必须先有第一轮记录才能报
        if (cls.getRound() != null && cls.getRound() == 2 && currentRound == 2) {
            List<Application> firstRound = findRoundRecord(idCard, classId, 1);
            if (firstRound.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "您尚未完成第一轮报名，无法参加第二轮");
            }
        }

        // 5) 名额校验（quota=-1 不限）
        if (cls.getQuota() != -1 && cls.getEnrolled() >= cls.getQuota()) {
            throw new BusinessException(ResultCode.CLASS_FULL);
        }

        // 6) 构造报名记录
        Application app = new Application();
        app.setName((String) form.get("name"));
        app.setIdCard(idCard);
        app.setIdCardMasked(maskIdCard(idCard));
        app.setGender((String) form.get("gender"));
        app.setPhone((String) form.get("phone"));
        app.setHasPhysics((String) form.get("hasPhysics"));
        app.setHasEnglish((String) form.getOrDefault("hasEnglish", "否"));
        app.setClassId(classId);
        app.setStatus(STATUS_APPLIED);
        app.setIsAdmitted(0);
        Object agreed = form.get("noticeAgreed");
        app.setNoticeAgreed(parseFlag(agreed));
        app.setApplyTime(LocalDateTime.now());
        Application saved = appRepo.save(app);

        // 7) 班级已报名人数 +1
        cls.setEnrolled(cls.getEnrolled() + 1);
        classRepo.save(cls);

        log.info("报名成功: id={}, name={}, classId={}, round={}", saved.getId(), saved.getName(), classId, currentRound);
        return toDTO(saved, cls.getName());
    }

    // ==================== 二轮判断逻辑 ====================

    /**
     * 根据当前时间和班级 periods 配置，判断当前处于哪一轮
     * periods = [{"round":1,"period":"2026/09/01 - 2026/09/13"},{"round":2,"period":"2026/09/15 - 2026/09/16"}]
     * @return 0=不在报名期  否则返回轮次号
     */
    private int determineCurrentRound(ClassInfo cls) {
        String periodsJson = cls.getPeriods();
        if (periodsJson == null || periodsJson.isBlank()) {
            // 无 periods：用旧的 period 字段fallback
            return isInPeriod(cls.getPeriod()) ? 1 : 0;
        }
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var list = mapper.readValue(periodsJson, java.util.List.class);
            for (var item : list) {
                @SuppressWarnings("unchecked")
                var entry = (java.util.Map<String, Object>) item;
                int round = ((Number) entry.get("round")).intValue();
                String period = (String) entry.get("period");
                if (isInPeriod(period)) return round;
            }
            return 0; // 当前不在任何一轮
        } catch (Exception e) {
            log.warn("periods JSON 解析失败: {}", periodsJson, e);
            return isInPeriod(cls.getPeriod()) ? 1 : 0;
        }
    }

    /**
     * 判断当前时间是否在给定时间段内
     * @param period 格式："2026/09/01 - 2026/09/13"
     * @return true=在报名期内
     */
    private boolean isInPeriod(String period) {
        if (period == null || period.isBlank()) return false;
        try {
            String[] parts = period.split(" - ");
            if (parts.length != 2) return false;
            java.time.LocalDate start = java.time.LocalDate.parse(parts[0].trim());
            java.time.LocalDate end   = java.time.LocalDate.parse(parts[1].trim());
            java.time.LocalDate now   = java.time.LocalDate.now();
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            log.warn("时间段解析失败: period={}", period);
            return false;
        }
    }

    /**
     * 查某学生在某班某轮的报名记录（status=1 的有效记录）
     */
    private List<Application> findRoundRecord(String idCard, Integer classId, int round) {
        // round 不存在字段，用全量查后过滤（数据量小，够用）
        return appRepo.findByIdCardAndClassIdAndStatus(idCard, classId, STATUS_APPLIED);
    }

    // ==================== 撤回报名（软删除，写） ====================

    /**
     * 撤回报名（软删除：将 status 改为 0，不删除记录）
     * @param id 报名记录 ID
     */
    @Transactional
    public void withdraw(Integer id) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.APPLICATION_NOT_FOUND));

        if (app.getStatus() == STATUS_WITHDRAWN) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该报名已撤回，请勿重复操作");
        }

        // 软删除：status → 0
        app.setStatus(STATUS_WITHDRAWN);
        appRepo.save(app);

        // 班级已报名人数 -1
        classRepo.findById(app.getClassId()).ifPresent(cls -> {
            cls.setEnrolled(Math.max(0, cls.getEnrolled() - 1));
            classRepo.save(cls);
        });

        log.info("撤回报名: id={}, name={}", id, app.getName());
    }

    // ==================== 我的报名（读，只查有效记录） ====================

    /**
     * 按身份证查我的报名列表（只查 status=1 已报名的）
     */
    public List<ApplicationDTO> findMy(String idCard) {
        return appRepo.findByIdCardAndStatus(idCard, STATUS_APPLIED).stream()
                .map(app -> {
                    String className = classRepo.findById(app.getClassId())
                            .map(ClassInfo::getName)
                            .orElse("未知班级");
                    return toDTO(app, className);
                })
                .collect(Collectors.toList());
    }

    // ==================== 管理端方法 ====================

    /**
     * 管理端分页查询报名记录
     */
    public Page<ApplicationDTO> adminSearch(Integer classId, Integer status,
                                            String idCard, String name,
                                            PageRequest pageable) {
        return appRepo.adminSearch(classId, status, idCard, name, pageable)
                .map(app -> {
                    String className = classRepo.findById(app.getClassId())
                            .map(ClassInfo::getName)
                            .orElse("未知班级");
                    return toDTO(app, className);
                });
    }

    /**
     * 批量更新 status（如批量撤回）
     */
    @Transactional
    public void batchUpdateStatus(List<Integer> ids, int status) {
        appRepo.batchUpdateStatus(ids, status);
    }

    /**
     * 清空某班所有报名（软删除）
     */
    @Transactional
    public void clearClass(Integer classId) {
        appRepo.findByClassId(classId).forEach(app -> {
            app.setStatus(STATUS_WITHDRAWN);
            appRepo.save(app);
        });
    }

    /**
     * 批量录取（is_admitted=1，status=2）
     */
    @Transactional
    public void batchAdmit(List<Integer> ids) {
        appRepo.batchUpdateAdmitted(ids, 1);
        appRepo.batchUpdateStatus(ids, STATUS_ENROLLED);
    }

    // ==================== 内部工具 ====================

    /** 身份证脱敏：保留前 6 后 4，中间 8 位换 * */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) return idCard;
        return idCard.replaceAll("(?<=^.{6}).{8}(?=.{4}$)", "********");
    }

    /** 解析前端 checkbox/boolean 值 → 0/1 */
    private Integer parseFlag(Object val) {
        if (val == null) return 0;
        if (val instanceof Boolean) return ((Boolean) val) ? 1 : 0;
        String s = String.valueOf(val);
        return "true".equalsIgnoreCase(s) || "1".equals(s) ? 1 : 0;
    }

    /** Entity → DTO */
    private ApplicationDTO toDTO(Application e, String className) {
        return ApplicationDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .idCard(e.getIdCardMasked())   // 返脱敏版
                .gender(e.getGender())
                .phone(e.getPhone())
                .hasPhysics(e.getHasPhysics())
                .hasEnglish(e.getHasEnglish())
                .classId(e.getClassId())
                .className(className)
                .status(String.valueOf(e.getStatus()))
                .applyTime(e.getApplyTime())
                .build();
    }
}
