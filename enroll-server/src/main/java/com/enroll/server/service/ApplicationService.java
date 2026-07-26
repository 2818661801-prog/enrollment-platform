package com.enroll.server.service;

import com.enroll.server.dto.ApplicationDTO;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.entity.Application;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.entity.ClassRound;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.repository.ApplicationRepository;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    /** 并发提交锁池：按身份证号加锁，防止同一人在两个 tab 同时提交绕过互斥校验 */
    private final ConcurrentHashMap<String, Object> submitLocks = new ConcurrentHashMap<>();

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
        String name     = (String) form.get("name");
        Integer classId = (Integer) form.get("classId");

        // 1) 校验班级存在（只读，无需加锁）
        ClassInfo cls = classRepo.findById(classId)
                .orElseThrow(() -> new BusinessException(ResultCode.CLASS_NOT_FOUND));

        // 2) 判断当前轮次，不在报名期内拒绝（只读，无需加锁）
        int currentRound = determineCurrentRound(cls);
        if (currentRound == 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该班级当前不在报名时间内");
        }

        // 3~7) 互斥校验 + 写入：按身份证号加锁，防止并发绕过（Bug #2 修复）
        //       同一身份证的并发请求串行执行，杜绝"两次都查到无记录→两条 INSERT"的竞态
        Object lock = submitLocks.computeIfAbsent(idCard, k -> new Object());
        synchronized (lock) {
            try {
            // ===== 字段格式校验（防止绕过前端直接调API） =====
            if (name == null || !name.matches("^[一-龥]{2,10}$")) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "姓名格式不正确（2-10个中文）");
            }
            if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "手机号格式不正确");
            }
            if (idCard == null || !idCard.matches("^\\d{17}[\\dXx]$")) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "身份证号格式不正确");
            }
            if (!validateIdCardChecksum(idCard)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "身份证号校验码不正确");
            }

            // 3) 身份证全局唯一：已报名（审核中）或已录取（永久锁定）不可再报（只查未删除）
            List<Application> idCardDup = appRepo.findByIdCardAndStatusInAndIsDeleted(
                    idCard, List.of(STATUS_APPLIED, STATUS_ENROLLED), 0);
            if (!idCardDup.isEmpty()) {
                Application existing = idCardDup.get(0);
                String className = classRepo.findById(existing.getClassId())
                        .map(ClassInfo::getName)
                        .orElse("未知班级");
                throw new BusinessException(ResultCode.DUPLICATE_APPLICATION,
                        "该身份证持有者已报名【" + className + "】");
            }

            // 3.5) 手机号全局唯一：同一手机号只能报名一个班
            List<Application> phoneDup = appRepo.findByPhoneAndStatusInAndIsDeleted(
                    phone, List.of(STATUS_APPLIED, STATUS_ENROLLED), 0);
            if (!phoneDup.isEmpty()) {
                Application existing = phoneDup.get(0);
                String className = classRepo.findById(existing.getClassId())
                        .map(ClassInfo::getName)
                        .orElse("未知班级");
                throw new BusinessException(ResultCode.DUPLICATE_APPLICATION,
                        "该手机号已报名【" + className + "】");
            }

            // 4) 本轮防重复（同一身份证+同一班级，防止同一人报两次同一班，只查未删除）
            List<Application> roundDup = appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(
                    idCard, classId, List.of(STATUS_APPLIED, STATUS_ENROLLED, STATUS_REJECTED), 0);
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
            app.setApplyTime(LocalDateTime.now().withNano(0));  // 截断到秒，避免纳秒→MySQL DATETIME 四舍五入导致时间不一致
            app.setRound(currentRound);
            // enrollmentYear：后端自动取当前年份（2026/2027）
            app.setEnrollmentYear(java.time.LocalDate.now().getYear());
            Application saved = appRepo.save(app);

            // S16 修复：enrolled+1 已由上面的原子 UPDATE 完成，无需再 save classRepo
            return toDTO(saved, cls.getName());
            } finally {
                // 锁用完后从池中移除，防止内存泄漏
                submitLocks.remove(idCard);
            }
            }
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
        // 用 Repository 自定义 SQL（内部用 MySQL NOW()）查询当前有效轮次
        var current = roundRepo.findCurrentRound(cls.getId());
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

        // 只有"已报名"(status=1)才允许撤回，其他状态一律拒绝
        if (app.getStatus() != STATUS_APPLIED) {
            String msg = switch (app.getStatus()) {
                case STATUS_WITHDRAWN -> "该报名已撤回，请勿重复操作";
                case STATUS_ENROLLED  -> "已录取的报名无法撤回";
                case STATUS_REJECTED  -> "未录取的报名无需撤回";
                default               -> "当前状态不允许撤回";
            };
            throw new BusinessException(ResultCode.PARAM_INVALID, msg);
        }

        // 校验截止时间：超过该班级该轮次的报名截止时间不允许撤回（用 MySQL NOW() 消除时钟差）
        int expired = roundRepo.countExpired(app.getClassId(), app.getRound());
        if (expired > 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "报名已截止，无法撤回");
        }

        app.setStatus(STATUS_WITHDRAWN);
        appRepo.save(app);

        // S16 修复：enrolled-1 改为原子 UPDATE，防止并发超卖回升
        classRepo.decrementEnrolled(app.getClassId());

        // log.info("撤回报名: id={}, name={}", id, app.getName());
    }

    // ==================== 我的报名（读） ====================

    public List<ApplicationDTO> findMy(String idCard) {
        return appRepo.findByIdCardAndStatusAndIsDeleted(idCard, STATUS_APPLIED, 0).stream()
                .map(app -> {
                    String className = classRepo.findById(app.getClassId())
                            .map(ClassInfo::getName)
                            .orElse("未知班级");
                    return toDTO(app, className);
                })
                .collect(Collectors.toList());
    }

    public List<ApplicationDTO> findMyByPhone(String phone) {
        ObjectMapper om = new ObjectMapper();
        return appRepo.findByPhoneAndStatusInAndIsDeleted(phone, List.of(STATUS_APPLIED, STATUS_ENROLLED, STATUS_REJECTED), 0).stream()
                .map(app -> {
                    ClassInfo cls = classRepo.findById(app.getClassId()).orElse(null);
                    String className = cls != null ? cls.getName() : "未知班级";
                    // 查轮次列表，构建 classPeriods JSON
                    List<ClassRound> rounds = roundRepo.findByClassIdOrderByRoundNum(app.getClassId());
                    String classPeriods;
                    if (rounds == null || rounds.isEmpty()) {
                        classPeriods = "[]";
                    } else {
                        try {
                            List<Map<String, Object>> list = rounds.stream()
                                    .map(r -> {
                                        Map<String, Object> m = new java.util.HashMap<>();
                                        m.put("round", r.getRoundNum());
                                        m.put("period", r.getPeriodStart() + " - " + r.getPeriodEnd());
                                        return m;
                                    })
                                    .collect(Collectors.toList());
                            classPeriods = om.writeValueAsString(list);
                        } catch (Exception e) {
                            classPeriods = "[]";
                        }
                    }
                    return toDTO(app, className, null, classPeriods);
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
        if (body.containsKey("hasEnglish"))  app.setHasEnglish((String) body.get("hasEnglish"));
        // Bug #1 修复：补充 idCard 和 gender 的更新处理
        if (body.containsKey("gender"))      app.setGender((String) body.get("gender"));
        if (body.containsKey("idCard")) {
            String newIdCard = (String) body.get("idCard");
            app.setIdCard(newIdCard);
            // Bug #6 修复：修改身份证号时同步更新脱敏字段
            app.setIdCardMasked(newIdCard == null || newIdCard.length() != 18 ? newIdCard
                    : newIdCard.replaceAll("(?<=^.{6}).{8}(?=.{4}$)", "********"));
        }
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
        appRepo.findByClassIdAndIsDeleted(classId, 0).forEach(app -> {
            // Bug #3 修复：清空时释放已报名状态占用的名额（status=1 才占用配额）
            if (app.getStatus() == STATUS_APPLIED) {
                classRepo.decrementEnrolled(app.getClassId());
            }
            app.setStatus(STATUS_WITHDRAWN);
            appRepo.save(app);
        });
    }

    @Transactional
    public void batchAdmit(List<Integer> ids) {
        batchAdmit(ids, "");
    }

    @Transactional
    public void batchAdmit(List<Integer> ids, String auditComment) {
        // Bug #5 修复：只允许录取"审核中(status=1)"的记录，防止覆盖已撤回/已录取/已驳回
        List<Application> apps = appRepo.findAllById(ids);
        for (Application app : apps) {
            if (app.getStatus() != STATUS_APPLIED) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "只能录取审核中的记录（id=" + app.getId() + " 当前状态为" + statusLabel(app.getStatus()) + "）");
            }
        }
        appRepo.batchUpdateStatusAndComment(ids, STATUS_ENROLLED, auditComment);
    }

    @Transactional
    public void batchReject(List<Integer> ids) {
        batchReject(ids, "");
    }

    @Transactional
    public void batchReject(List<Integer> ids, String auditComment) {
        // Bug #4 修复：驳回时释放名额 + Bug #5 修复：只允许驳回"审核中"的记录
        List<Application> apps = appRepo.findAllById(ids);
        for (Application app : apps) {
            if (app.getStatus() != STATUS_APPLIED) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "只能驳回审核中的记录（id=" + app.getId() + " 当前状态为" + statusLabel(app.getStatus()) + "）");
            }
            // 释放每个被驳回记录占用的名额
            classRepo.decrementEnrolled(app.getClassId());
        }
        appRepo.batchUpdateStatusAndComment(ids, STATUS_REJECTED, auditComment);
    }

    // ==================== 低代码平台同步 ====================

    /**
     * 全量查询报名记录（不分页，给低代码平台同步用）
     * 2026-07-10 新增：解决分页响应解析问题
     */
    public List<ApplicationDTO> findAllForSync() {
        return appRepo.findByIsDeleted(0).stream()
                .map(app -> {
                    // 查班级名和内网班级ID（outer_id = innerId，用于跨系统 id 映射）
                    ClassInfo cls = classRepo.findById(app.getClassId()).orElse(null);
                    String className = cls != null ? cls.getName() : "未知班级";
                    Integer innerId = cls != null ? cls.getOuterId() : null;
                    return toDTO(app, className, innerId);
                })
                .collect(Collectors.toList());
    }

    /**
     * 全量同步报名记录（低代码平台调 sync/applications 时调用）
     * 主键：idCard + classId（一个学生一个班只有一条报名记录）
     *
     * @param dataList  低代码平台传来的报名数据列表
     * @return 同步结果统计 {total, inserted, updated, deleted}
     */
    @Transactional
    public Map<String, Object> syncFromLowCode(List<Map> dataList) {
        List<Application> allA = appRepo.findByIsDeleted(0);

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
                app.setApplyTime(LocalDateTime.now().withNano(0));  // 截断到秒
                app.setRound((Integer) item.getOrDefault("round", 1));
                app.setEnrollmentYear((Integer) item.getOrDefault("enrollmentYear", java.time.LocalDate.now().getYear()));
                app.setIsDeleted(0);  // 默认未删除
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

        // a有、b无 → 软删除（is_deleted=1）
        for (String keyToDelete : toDelete) {
            Application toRemove = aMap.get(keyToDelete);
            if (toRemove != null) {
                toRemove.setIsDeleted(1);
                appRepo.save(toRemove);
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

    /**
     * 查重接口（给表单页实时查询用）
     * 返回结构：
     *   { hasPhoneConflict: bool, phoneClassName: string,
     *     hasIdCardConflict: bool, idCardClassName: string,
     *     hasSameClassConflict: bool }
     * 三种冲突分别提示，不抛异常。
     */
    public java.util.Map<String, Object> checkDuplicate(String phone, String idCard, Integer classId) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("hasPhoneConflict", false);
        result.put("phoneClassName", null);
        result.put("hasIdCardConflict", false);
        result.put("idCardClassName", null);
        result.put("hasSameClassConflict", false);

        // 1) 手机号全局唯一检查
        java.util.List<Application> phoneDup = appRepo.findByPhoneAndStatusInAndIsDeleted(
                phone, java.util.List.of(STATUS_APPLIED, STATUS_ENROLLED), 0);
        if (!phoneDup.isEmpty()) {
            Application existing = phoneDup.get(0);
            String className = classRepo.findById(existing.getClassId())
                    .map(ClassInfo::getName).orElse("未知班级");
            result.put("hasPhoneConflict", true);
            result.put("phoneClassName", className);
        }

        // 2) 身份证全局唯一检查
        java.util.List<Application> idCardDup = appRepo.findByIdCardAndStatusInAndIsDeleted(
                idCard, java.util.List.of(STATUS_APPLIED, STATUS_ENROLLED), 0);
        if (!idCardDup.isEmpty()) {
            Application existing = idCardDup.get(0);
            String className = classRepo.findById(existing.getClassId())
                    .map(ClassInfo::getName).orElse("未知班级");
            result.put("hasIdCardConflict", true);
            result.put("idCardClassName", className);
        }

        // 3) 同班级防重（同一身份证+同一班级）
        java.util.List<Application> sameClassDup = appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(
                idCard, classId, java.util.List.of(STATUS_APPLIED, STATUS_ENROLLED, STATUS_REJECTED), 0);
        if (!sameClassDup.isEmpty()) {
            result.put("hasSameClassConflict", true);
        }

        return result;
    }

    // ==================== 内部工具 ====================

    private Integer parseFlag(Object val) {
        if (val == null) return 0;
        if (val instanceof Boolean) return ((Boolean) val) ? 1 : 0;
        String s = String.valueOf(val);
        return "true".equalsIgnoreCase(s) || "1".equals(s) ? 1 : 0;
    }

    /** 状态值的可读文字（供 batchAdmit / batchReject 错误提示用） */
    private String statusLabel(int status) {
        return switch (status) {
            case STATUS_APPLIED   -> "审核中";
            case STATUS_WITHDRAWN -> "已撤回";
            case STATUS_ENROLLED  -> "已录取";
            case STATUS_REJECTED  -> "未录取";
            default               -> "未知(" + status + ")";
        };
    }

    public ApplicationDTO toDTO(Application e, String className) {
        return toDTO(e, className, null);
    }

    public ApplicationDTO toDTO(Application e, String className, Integer innerId) {
        return toDTO(e, className, innerId, null);
    }

    public ApplicationDTO toDTO(Application e, String className, Integer innerId, String classPeriods) {
        return ApplicationDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .idCard(e.getIdCardMasked())
                .idCardRaw(e.getIdCard())
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
                .classPeriods(classPeriods)
                .round(e.getRound())
                .isDeleted(e.getIsDeleted())
                .innerId(innerId)
                .enrollmentYear(e.getEnrollmentYear())
                .build();
    }

    // ===== 字段格式校验 =====

    /** 身份证校验码验证（GB 11643-1999） */
    private boolean validateIdCardChecksum(String idCard) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] codes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * weights[i];
        }
        return Character.toUpperCase(idCard.charAt(17)) == codes[sum % 11];
    }
}
