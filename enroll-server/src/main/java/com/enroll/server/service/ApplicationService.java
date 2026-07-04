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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报名业务层（核心）
 *
 * 状态值（status）：
 *   0 = 未报名（登录后无记录）
 *   1 = 已报名（学生提交，待审核）
 *   2 = 已撤回（学生主动撤回）
 *   3 = 已录取（管理员录取，永久锁定）
 *   4 = 未录取（管理员驳回）
 *
 * 业务规则：
 *   1. 全局唯一报名：同学生只能有一条 status=1（已报名）的记录
 *   2. 校验名额：班级 enrolled >= quota 则拒绝
 *   3. 提交后 enrolled +1
 *   4. 撤回后 enrolled -1
 *   5. 身份证脱敏：中间 8 位 → ********
 *
 * 事务边界：
 *   - 写方法（submit/withdraw）@Transactional，失败自动回滚
 *   - 读方法（findMy）继承类级别 readOnly = true
 *
 * ===== 测试开关 ===== 临时开放报名，测完改回 false
 */
@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    // ===== 测试开关（生产必须为 false） =====
    private static final boolean TEST_MODE = false;

    /** 状态常量 */
    public static final int STATUS_NONE      = 0;
    public static final int STATUS_APPLIED   = 1;
    public static final int STATUS_WITHDRAWN = 2;
    public static final int STATUS_ENROLLED  = 3;
    public static final int STATUS_REJECTED  = 4;

    private final ApplicationRepository appRepo;
    private final ClassInfoRepository classRepo;

    public ApplicationService(ApplicationRepository appRepo, ClassInfoRepository classRepo) {
        this.appRepo = appRepo;
        this.classRepo = classRepo;
    }

    // ==================== 提交报名（写） ====================

    @Transactional
    public ApplicationDTO submit(Map<String, Object> form) {
        try {
        String idCard  = (String) form.get("idCard");
        Integer classId = (Integer) form.get("classId");

        // 1) 校验班级存在
        ClassInfo cls = classRepo.findById(classId)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));

        // 2) 测试模式：跳过时间校验
        int currentRound = 1;
        if (!TEST_MODE) {
            currentRound = determineCurrentRound(cls);
            if (currentRound == 0) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "该班级当前不在报名时间内");
            }
        }

        // 3) 全局唯一报名：已报名（审核中）或已录取（永久锁定）不可再报，未录取可以重新报
        List<Application> globalDup = appRepo.findByIdCardAndStatusIn(
                idCard, List.of(STATUS_APPLIED, STATUS_ENROLLED));
        if (!globalDup.isEmpty()) {
            throw new BusinessException(ResultCode.DUPLICATE_APPLICATION, "您已报名其他特色班，不可重复报名");
        }

        // 4) 本轮防重复
        List<Application> roundDup = appRepo.findByIdCardAndClassIdAndStatusIn(
                idCard, classId, List.of(STATUS_APPLIED, STATUS_ENROLLED, STATUS_REJECTED));
        if (!roundDup.isEmpty()) {
            throw new BusinessException(ResultCode.DUPLICATE_APPLICATION);
        }

        // 6) 名额校验（⚠️ S16 修复：名额校验 + enrolled+1 合并为原子 UPDATE）
        if (!TEST_MODE) {
            int affected = classRepo.incrementEnrolledIfQuotaAvailable(classId);
            if (affected == 0) {
                throw new BusinessException(ResultCode.CLASS_FULL);
            }
        }

        // 7) 构造报名记录
        Application app = new Application();
        app.setName((String) form.get("name"));
        app.setIdCard(idCard);
        app.setIdCardMasked(idCard == null || idCard.length() != 18 ? idCard : idCard.replaceAll("(?<=^.{6}).{8}(?=.{4}$)", "********"));
        app.setGender((String) form.get("gender"));
        app.setPhone((String) form.get("phone"));
        app.setHasPhysics((String) form.get("hasPhysics"));
        app.setHasEnglish((String) form.getOrDefault("hasEnglish", "否"));
        app.setAppliedCategory((String) form.get("appliedCategory"));
        app.setClassId(classId);
        app.setStatus(STATUS_APPLIED);
        Object agreed = form.get("noticeAgreed");
        app.setNoticeAgreed(parseFlag(agreed));
        app.setApplyTime(LocalDateTime.now());
        app.setRound(currentRound);
        Application saved = appRepo.save(app);

        // S16 修复：enrolled+1 已由上面的原子 UPDATE 完成，无需再 save classRepo
        log.info("报名成功: id={}, name={}, classId={}, round={}", saved.getId(), saved.getName(), classId, currentRound);
        return toDTO(saved, cls.getName());
        } catch (Exception e) {
            log.error("submit 异常: form={}", form, e);
            throw e;
        }
    }

    // ==================== 二轮判断逻辑 ====================

    private int determineCurrentRound(ClassInfo cls) {
        String periodsJson = cls.getPeriods();
        if (periodsJson == null || periodsJson.isBlank()) {
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
            return 0;
        } catch (Exception e) {
            log.warn("periods JSON 解析失败: {}", periodsJson, e);
            return isInPeriod(cls.getPeriod()) ? 1 : 0;
        }
    }

    private boolean isInPeriod(String period) {
        if (period == null || period.isBlank()) return false;
        try {
            String[] parts = period.split(" - ");
            if (parts.length != 2) return false;
            // 去掉时间后缀（"2026/09/16 23:59" → "2026/09/16"），再解析日期
            String startStr = parts[0].trim().split(" ")[0];
            String endStr   = parts[1].trim().split(" ")[0];
            java.time.format.DateTimeFormatter FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd");
            java.time.LocalDate start = java.time.LocalDate.parse(startStr, FMT);
            java.time.LocalDate end   = java.time.LocalDate.parse(endStr, FMT);
            java.time.LocalDate now   = java.time.LocalDate.now();
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            log.warn("时间段解析失败: period={}", period);
            return false;
        }
    }


    // ==================== 撤回报名（软删除，写） ====================

    @Transactional
    public void withdraw(Integer id) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.APPLICATION_NOT_FOUND));

        if (app.getStatus() == STATUS_WITHDRAWN) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该报名已撤回，请勿重复操作");
        }
        if (app.getStatus() == STATUS_ENROLLED) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "已录取的报名无法撤回");
        }

        app.setStatus(STATUS_WITHDRAWN);
        appRepo.save(app);

        // S16 修复：enrolled-1 改为原子 UPDATE，防止并发超卖回升
        classRepo.decrementEnrolled(app.getClassId());

        log.info("撤回报名: id={}, name={}", id, app.getName());
    }

    // ==================== 我的报名（读） ====================

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

    public List<ApplicationDTO> findMyByPhone(String phone) {
        return appRepo.findByPhoneAndStatusIn(phone, List.of(STATUS_APPLIED, STATUS_ENROLLED, STATUS_REJECTED)).stream()
                .map(app -> {
                    String className = classRepo.findById(app.getClassId())
                            .map(ClassInfo::getName)
                            .orElse("未知班级");
                    return toDTO(app, className);
                })
                .collect(Collectors.toList());
    }

    // ==================== 修改报名（写） ====================

    @Transactional
    public void updateApp(Integer id, Map<String, Object> body) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.APPLICATION_NOT_FOUND));
        if (app.getStatus() != STATUS_APPLIED) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "只能修改已报名的记录");
        }
        if (body.containsKey("name"))        app.setName((String) body.get("name"));
        if (body.containsKey("phone"))       app.setPhone((String) body.get("phone"));
        if (body.containsKey("hasPhysics"))  app.setHasPhysics((String) body.get("hasPhysics"));
        if (body.containsKey("hasEnglish")) app.setHasEnglish((String) body.get("hasEnglish"));
        appRepo.save(app);
        log.info("修改报名: id={}, name={}", id, app.getName());
    }

    // ==================== 管理端方法 ====================

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

    @Transactional
    public void batchUpdateStatus(List<Integer> ids, int status) {
        appRepo.batchUpdateStatus(ids, status);
    }

    @Transactional
    public void clearClass(Integer classId) {
        appRepo.findByClassId(classId).forEach(app -> {
            app.setStatus(STATUS_WITHDRAWN);
            appRepo.save(app);
        });
    }

    @Transactional
    public void batchAdmit(List<Integer> ids) {
        appRepo.batchUpdateStatus(ids, STATUS_ENROLLED);
    }

    @Transactional
    public void batchAdmit(List<Integer> ids, String auditComment) {
        appRepo.batchUpdateStatusAndComment(ids, STATUS_ENROLLED, auditComment);
    }

    @Transactional
    public void batchReject(List<Integer> ids) {
        appRepo.batchUpdateStatus(ids, STATUS_REJECTED);
    }

    @Transactional
    public void batchReject(List<Integer> ids, String auditComment) {
        appRepo.batchUpdateStatusAndComment(ids, STATUS_REJECTED, auditComment);
    }

    // ==================== 内部工具 ====================

    private Integer parseFlag(Object val) {
        if (val == null) return 0;
        if (val instanceof Boolean) return ((Boolean) val) ? 1 : 0;
        String s = String.valueOf(val);
        return "true".equalsIgnoreCase(s) || "1".equals(s) ? 1 : 0;
    }

    public ApplicationDTO toDTO(Application e, String className) {
        return ApplicationDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .idCard(e.getIdCardMasked())
                .gender(e.getGender())
                .phone(e.getPhone())
                .hasPhysics(e.getHasPhysics())
                .hasEnglish(e.getHasEnglish())
                .classId(e.getClassId())
                .className(className)
                .appliedCategory(e.getAppliedCategory())
                .status(String.valueOf(e.getStatus()))
                .applyTime(e.getApplyTime())
                .auditComment(e.getAuditComment())
                .classPeriods(classRepo.findById(e.getClassId()).map(ClassInfo::getPeriods).orElse(null))
                .round(e.getRound())
                .build();
    }
}
