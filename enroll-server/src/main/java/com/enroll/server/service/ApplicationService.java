package com.enroll.server.service;

import com.enroll.server.dto.ApplicationDTO;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.entity.Application;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.repository.ApplicationRepository;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
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

    /** 状态常量 */
    public static final int STATUS_NONE      = 0;
    public static final int STATUS_APPLIED   = 1;
    public static final int STATUS_WITHDRAWN = 2;
    public static final int STATUS_ENROLLED  = 3;
    public static final int STATUS_REJECTED  = 4;

    private final ApplicationRepository appRepo;
    private final ClassInfoRepository classRepo;
    private final ClassRoundRepository roundRepo;

    public ApplicationService(ApplicationRepository appRepo,
                               ClassInfoRepository classRepo,
                               ClassRoundRepository roundRepo) {
        this.appRepo = appRepo;
        this.classRepo = classRepo;
        this.roundRepo = roundRepo;
    }

    // ==================== 提交报名（写） ====================

    @Transactional
    public ApplicationDTO submit(Map<String, Object> form) {
        try {
        String idCard   = (String) form.get("idCard");
        String phone    = (String) form.get("phone");
        Integer classId = (Integer) form.get("classId");

        // 1) 校验班级存在
        ClassInfo cls = classRepo.findById(classId)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));

        // 2) 判断当前轮次，不在报名期内拒绝
        int currentRound = determineCurrentRound(cls);
        if (currentRound == 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该班级当前不在报名时间内");
        }

        // 3) 身份证全局唯一：已报名（审核中）或已录取（永久锁定）不可再报
        List<Application> idCardDup = appRepo.findByIdCardAndStatusIn(
                idCard, List.of(STATUS_APPLIED, STATUS_ENROLLED));
        if (!idCardDup.isEmpty()) {
            Application existing = idCardDup.get(0);
            String className = classRepo.findById(existing.getClassId())
                    .map(ClassInfo::getName)
                    .orElse("未知班级");
            throw new BusinessException(ResultCode.DUPLICATE_APPLICATION,
                    "该身份证持有者已报名【" + className + "】");
        }

        // 3.5) 手机号全局唯一：同一手机号只能报名一个班（核心防重：手机号=JWT subject=用户唯一标识）
        List<Application> phoneDup = appRepo.findByPhoneAndStatusIn(
                phone, List.of(STATUS_APPLIED, STATUS_ENROLLED));
        if (!phoneDup.isEmpty()) {
            Application existing = phoneDup.get(0);
            String className = classRepo.findById(existing.getClassId())
                    .map(ClassInfo::getName)
                    .orElse("未知班级");
            throw new BusinessException(ResultCode.DUPLICATE_APPLICATION,
                    "该手机号已报名【" + className + "】");
        }

        // 4) 本轮防重复（同一身份证+同一班级，防止同一人报两次同一班）
        List<Application> roundDup = appRepo.findByIdCardAndClassIdAndStatusIn(
                idCard, classId, List.of(STATUS_APPLIED, STATUS_ENROLLED, STATUS_REJECTED));
        if (!roundDup.isEmpty()) {
            throw new BusinessException(ResultCode.DUPLICATE_APPLICATION);
        }

        // 6) 名额校验（⚠️ S16 修复：名额校验 + enrolled+1 合并为原子 UPDATE）
        int affected = classRepo.incrementEnrolledIfQuotaAvailable(classId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.CLASS_FULL);
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
        // log.info("报名成功: id={}, name={}, classId={}, round={}", saved.getId(), saved.getName(), classId, currentRound);
        return toDTO(saved, cls.getName());
        } catch (Exception e) {
            // log.error("submit 异常: form={}", form, e);
            throw e;
        }
    }

    // ==================== 二轮判断逻辑（改查 class_rounds 表） ====================

    /**
     * 根据当前时间查 class_rounds 表，判断当前是第几轮
     * @return 当前在报名时间内的那一轮，0 表示当前不在任何报名时间内
     */
    private int determineCurrentRound(ClassInfo cls) {
        LocalDateTime now = LocalDateTime.now();
        // 用 Repository 自定义 SQL 查询当前有效轮次
        var current = roundRepo.findCurrentRound(cls.getId(), now);
        if (current != null) {
            return current.getRoundNum();
        }
        // 不在任何一轮内，返回0，由 submit() 的 if (currentRound == 0) 拦截
        return 0;
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

        // log.info("撤回报名: id={}, name={}", id, app.getName());
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
        // log.info("修改报名: id={}, name={}", id, app.getName());
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

    // ==================== 低代码平台同步 ====================

    /**
     * 全量同步报名记录（低代码平台调 sync/applications 时调用）
     * 主键：idCard + classId（一个学生一个班只有一条报名记录）
     *
     * @param dataList  低代码平台传来的报名数据列表
     * @return 同步结果统计 {total, inserted, updated, deleted}
     */
    @Transactional
    public Map<String, Object> syncFromLowCode(List<Map> dataList) {
        List<Application> allA = appRepo.findAll();

        // 用 idCard+classId 做 a 端 map
        Map<String, Application> aMap = new java.util.HashMap<>();
        for (Application a : allA) {
            aMap.put(a.getIdCard() + "|" + a.getClassId(), a);
        }

        java.util.Set<String> bKeys = new java.util.HashSet<>();
        java.util.Set<String> toDelete = new java.util.HashSet<>(aMap.keySet());

        int inserted = 0, updated = 0, deleted = 0;

        for (Map<String, Object> item : dataList) {
            String idCard = (String) item.get("idCard");
            Integer classId = (Integer) item.get("classId");
            if (idCard == null || idCard.isBlank() || classId == null) continue;

            String key = idCard + "|" + classId;
            bKeys.add(key);
            toDelete.remove(key);

            Application existing = aMap.get(key);
            if (existing == null) {
                // 新增
                Application app = new Application();
                app.setName((String) item.get("name"));
                app.setIdCard(idCard);
                app.setIdCardMasked(idCard.replaceAll("(?<=^.{6}).{8}(?=.{4}$)", "********"));
                app.setGender((String) item.get("gender"));
                app.setPhone((String) item.get("phone"));
                app.setHasPhysics((String) item.getOrDefault("hasPhysics", "否"));
                app.setHasEnglish((String) item.getOrDefault("hasEnglish", "否"));
                app.setAppliedCategory((String) item.get("appliedCategory"));
                app.setClassId(classId);
                app.setStatus((Integer) item.getOrDefault("status", 1));
                app.setNoticeAgreed(parseFlag(item.get("noticeAgreed")));
                app.setApplyTime(LocalDateTime.now());
                app.setRound((Integer) item.getOrDefault("round", 1));
                appRepo.save(app);
                inserted++;
            } else {
                // 更新（只更新传来的字段）
                if (item.containsKey("name"))           existing.setName((String) item.get("name"));
                if (item.containsKey("gender"))         existing.setGender((String) item.get("gender"));
                if (item.containsKey("phone"))          existing.setPhone((String) item.get("phone"));
                if (item.containsKey("hasPhysics"))     existing.setHasPhysics((String) item.get("hasPhysics"));
                if (item.containsKey("hasEnglish"))    existing.setHasEnglish((String) item.get("hasEnglish"));
                if (item.containsKey("appliedCategory")) existing.setAppliedCategory((String) item.get("appliedCategory"));
                if (item.containsKey("status"))         existing.setStatus((Integer) item.get("status"));
                if (item.containsKey("noticeAgreed"))   existing.setNoticeAgreed(parseFlag(item.get("noticeAgreed")));
                if (item.containsKey("auditComment"))  existing.setAuditComment((String) item.get("auditComment"));
                appRepo.save(existing);
                updated++;
            }
        }

        // a有、b无 → 真正删除
        for (String keyToDelete : toDelete) {
            Application toRemove = aMap.get(keyToDelete);
            if (toRemove != null) {
                appRepo.delete(toRemove);
                deleted++;
            }
        }

        return java.util.Map.of(
            "total", dataList.size(),
            "inserted", inserted,
            "updated", updated,
            "deleted", deleted
        );
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
                .classPeriods(null) // 兼容旧字段，报名记录接口不再返回班级轮次 JSON
                .round(e.getRound())
                .build();
    }
}
