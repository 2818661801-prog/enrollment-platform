package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.entity.Category;
import com.enroll.server.entity.ClassCategory;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.entity.ClassRound;
import com.enroll.server.entity.SysConfig;
import com.enroll.server.repository.CategoryRepository;
import com.enroll.server.repository.ClassCategoryRepository;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
import com.enroll.server.repository.SysConfigRepository;
import com.enroll.server.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 低代码平台数据同步 API
 *
 * 同步按钮流程：
 *   ① b查a  GET /api/admin/classes  → 低代码平台了解 enroll_db 现状
 *   ② 在b里做增删改                  → 低代码平台操作自己的内网 DB
 *   ③ b查b                          → 低代码平台拿到 b 的完整数据
 *   ④ 增删a  POST /api/admin/sync/{table}  → 全量同步到 enroll_db
 *
 * 同步逻辑：
 *   b有、a无  → 新增
 *   b有、a有  → 更新
 *   b有、isDeleted=1 → 软删（更新 is_deleted=1）
 *   a有、b无  → 真正删除
 */
@RestController
@RequestMapping("/api/admin/sync")
public class SyncController {

    private final ClassInfoRepository classRepo;
    private final ClassRoundRepository roundRepo;
    private final ClassCategoryRepository classCatRepo;
    private final CategoryRepository categoryRepo;
    private final SysConfigRepository sysConfigRepo;
    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;

    public SyncController(ClassInfoRepository classRepo,
                          ClassRoundRepository roundRepo,
                          ClassCategoryRepository classCatRepo,
                          CategoryRepository categoryRepo,
                          SysConfigRepository sysConfigRepo,
                          ApplicationService applicationService) {
        this.classRepo = classRepo;
        this.roundRepo = roundRepo;
        this.classCatRepo = classCatRepo;
        this.categoryRepo = categoryRepo;
        this.sysConfigRepo = sysConfigRepo;
        this.applicationService = applicationService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 班级同步 ====================

    /**
     * 全量同步班级
     * 主键：name
     * Body: { "data": [{ "name": "...", "period": "...", "classRounds": [...], "quota": 50, "isDeleted": 0 }] }
     */
    @Transactional
    @PostMapping("/classes")
    public Map<String, Object> syncClasses(@RequestBody Map<String, Object> body) {
        List<Map> dataList = extractList(body, "data");
        int inserted = 0, updated = 0, deleted = 0, softDeleted = 0;

        // ① 把a全部数据查出来，用name做key
        List<ClassInfo> allA = classRepo.findAll();
        Map<String, ClassInfo> aMap = new HashMap<>();
        for (ClassInfo c : allA) {
            aMap.put(c.getName(), c);
        }
        Set<String> bNames = new HashSet<>();
        Set<String> toDelete = new HashSet<>(aMap.keySet()); // 初始：a所有name

        // ② 遍历b的数据，upsert到a
        for (Map<String, Object> item : dataList) {
            String name = (String) item.get("name");
            if (name == null || name.isBlank()) continue;
            bNames.add(name);
            toDelete.remove(name); // b有的，标记为不删除

            ClassInfo existing = aMap.get(name);
            if (existing == null) {
                // 新增：先保存 class 拿到真实 ID，再写 rounds + 中间表
                ClassInfo cls = new ClassInfo();
                updateClassFromMap(cls, item);
                cls.setIsDeleted(0);
                cls.setEnrolled(0);
                cls.setSource("sync");
                cls.setPeriod(computePeriod(item));
                ClassInfo saved = classRepo.save(cls);
                upsertClassRounds(saved.getId(), item);
                upsertClassCategories(saved.getId(), item);
                inserted++;
            } else {
                // 更新：先删旧 rounds/中间表，再更新 class，再插新 rounds/中间表
                upsertClassRounds(existing.getId(), item);
                upsertClassCategories(existing.getId(), item);
                updateClassFromMap(existing, item);
                existing.setPeriod(computePeriod(item));
                classRepo.save(existing);
                updated++;
                if ("1".equals(String.valueOf(item.get("isDeleted")))) {
                    softDeleted++;
                }
            }
        }

        // ③ a有、b无 → 真正删除（同时删 class_rounds）
        for (String nameToDelete : toDelete) {
            ClassInfo toRemove = aMap.get(nameToDelete);
            if (toRemove != null) {
                roundRepo.deleteByClassId(toRemove.getId());
                classCatRepo.deleteByClassId(toRemove.getId());
                classRepo.delete(toRemove);
                deleted++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", dataList.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("deleted", deleted);
        result.put("softDeleted", softDeleted);
        return R.ok("同步成功", result);
    }

    /**
     * 从 item 的 classRounds 字段计算 period 字符串（取第一轮）
     */
    private String computePeriod(Map<String, Object> item) {
        Object roundsObj = item.get("classRounds");
        if (roundsObj == null) return null;
        List<Map> rounds;
        if (roundsObj instanceof String) {
            try {
                rounds = objectMapper.readValue((String) roundsObj,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map>>() {});
            } catch (Exception e) { return null; }
        } else if (roundsObj instanceof List) {
            rounds = (List<Map>) roundsObj;
        } else { return null; }
        if (rounds.isEmpty()) return null;
        Map first = rounds.get(0);
        Object ps = first.get("periodStart");
        Object pe = first.get("periodEnd");
        if (ps == null || pe == null) return null;
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        try {
            String start = LocalDateTime.parse(ps.toString(), dtFmt).format(dtFmt);
            String end = LocalDateTime.parse(pe.toString(), dtFmt).format(dtFmt);
            return start + " - " + end;
        } catch (Exception e) {
            try {
                String start = java.time.LocalDate.parse(ps.toString(), dFmt).format(dFmt);
                String end = java.time.LocalDate.parse(pe.toString(), dFmt).format(dFmt);
                return start + " - " + end;
            } catch (Exception ex) { return null; }
        }
    }

    /**
     * 同步轮次：先删旧轮次，再插新轮次
     */
    @SuppressWarnings("unchecked")
    private void upsertClassRounds(Integer classId, Map<String, Object> item) {
        roundRepo.deleteByClassId(classId);
        Object roundsObj = item.get("classRounds");
        if (roundsObj == null) return;
        List<Map> rounds;
        if (roundsObj instanceof String) {
            try {
                rounds = objectMapper.readValue((String) roundsObj,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map>>() {});
            } catch (Exception e) { return; }
        } else if (roundsObj instanceof List) {
            rounds = (List<Map>) roundsObj;
        } else { return; }
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        for (Map<String, Object> r : rounds) {
            ClassRound cr = new ClassRound();
            cr.setClassId(classId);
            cr.setRoundNum((Integer) r.get("roundNum"));
            cr.setPeriodStart(parseDt(r.get("periodStart"), dtFmt, dFmt, true));
            cr.setPeriodEnd(parseDt(r.get("periodEnd"), dtFmt, dFmt, false));
            roundRepo.save(cr);
        }
    }

    /**
     * 同步班级-类别中间表：先删旧关联，再按名称查 id 写新关联
     */
    @SuppressWarnings("unchecked")
    private void upsertClassCategories(Integer classId, Map<String, Object> item) {
        classCatRepo.deleteByClassId(classId);
        Object namesObj = item.get("categoryNames");
        if (namesObj == null) return;
        List<String> names;
        if (namesObj instanceof String) {
            try {
                names = objectMapper.readValue((String) namesObj, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            } catch (Exception e) { return; }
        } else if (namesObj instanceof List) {
            names = (List<String>) namesObj;
        } else { return; }
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            final String n = name.trim();
            categoryRepo.findByName(n).ifPresent(cat -> {
                ClassCategory cc = new ClassCategory();
                cc.setClassId(classId);
                cc.setCategoryId(cat.getId());
                classCatRepo.save(cc);
            });
        }
    }

    private java.time.LocalDateTime parseDt(Object val, DateTimeFormatter dtFmt,
                                             DateTimeFormatter dFmt, boolean isStart) {
        if (val == null) return null;
        String s = val.toString();
        try { return java.time.LocalDateTime.parse(s, dtFmt); }
        catch (Exception e) {
            java.time.LocalDate d = java.time.LocalDate.parse(s, dFmt);
            return isStart ? d.atStartOfDay() : d.atTime(23, 59);
        }
    }

    // ==================== 类别同步 ====================

    /**
     * 全量同步类别（先清再插）
     * Body: { "data": [{ "name": "经管类" }] }
     */
    @PostMapping("/categories")
    @Transactional
    public Map<String, Object> syncCategories(@RequestBody Map<String, Object> body) {
        List<Map> dataList = extractList(body, "data");

        // ① 清空 categories 和 class_category（外键约束先清中间表）
        classCatRepo.deleteAll();
        categoryRepo.deleteAll();

        // ② 全量插入
        int inserted = 0;
        for (Map<String, Object> item : dataList) {
            String name = (String) item.get("name");
            if (name == null || name.isBlank()) continue;
            name = name.trim();
            Category cat = new Category();
            cat.setName(name);
            categoryRepo.save(cat);
            inserted++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", dataList.size());
        result.put("inserted", inserted);
        return R.ok("同步成功", result);
    }

    // ==================== 报名记录同步 ====================

    /**
     * 全量同步报名记录
     * 主键：idCard + classId（一个学生一个班只有一条报名记录）
     * Body: { "data": [{ "idCard": "...", "classId": 1, "name": "...", "status": 1 }] }
     */
    @PostMapping("/applications")
    public Map<String, Object> syncApplications(@RequestBody Map<String, Object> body) {
        return R.ok("同步成功", applicationService.syncFromLowCode(extractList(body, "data")));
    }

    // ==================== 系统配置同步 ====================

    /**
     * 全量同步系统配置
     * 新结构（v2.2）：sys_config 只有一条记录（id=1）
     * Body: { "data": [{ "title": "...", "conditions": "..\n..", "notices": "..\n..", "updatedBy": "sync" }] }
     */
    @PostMapping("/config")
    @Transactional
    public Map<String, Object> syncConfig(@RequestBody Map<String, Object> body) {
        List<Map> dataList = extractList(body, "data");
        int inserted = 0, updated = 0;

        for (Map<String, Object> item : dataList) {
            SysConfig cfg = sysConfigRepo.findById(1).orElse(new SysConfig());
            cfg.setTitle((String) item.get("title"));
            cfg.setConditions((String) item.get("conditions"));
            cfg.setNotices((String) item.get("notices"));
            cfg.setUpdatedBy((String) item.getOrDefault("updatedBy", "sync"));
            cfg.setUpdatedAt(LocalDateTime.now());
            sysConfigRepo.save(cfg);
            // id=1 记录只可能 updated（不存在时 inserted）
            inserted++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", dataList.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        return R.ok("同步成功", result);
    }

    // ==================== 内部工具 ====================

    /** 从body里提取List<Map>，兼容空值 */
    @SuppressWarnings("unchecked")
    private List<Map> extractList(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) return List.of();
        if (val instanceof List) return (List<Map>) val;
        return List.of();
    }

    /** 把Map里的字段映射到ClassInfo（只更新传来的字段，periods 已废弃由 classRounds 替代） */
    private void updateClassFromMap(ClassInfo cls, Map<String, Object> map) {
        if (map.containsKey("name"))        cls.setName((String) map.get("name"));
        if (map.containsKey("period"))      cls.setPeriod((String) map.get("period"));
        if (map.containsKey("quota"))       cls.setQuota((Integer) map.get("quota"));
        if (map.containsKey("description")) cls.setDescription((String) map.get("description"));
        if (map.containsKey("isDeleted"))  cls.setIsDeleted((Integer) map.get("isDeleted"));
        if (map.containsKey("source"))      cls.setSource((String) map.get("source"));
        else cls.setSource("sync"); // 默认 sync
    }
}
