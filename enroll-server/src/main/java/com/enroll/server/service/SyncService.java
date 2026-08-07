package com.enroll.server.service;

import com.enroll.server.dto.ResultCode;
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
import com.enroll.server.util.DateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 低代码平台数据同步 Service
 *
 * 从 SyncController 搬过来的业务逻辑：
 *   ① @Transactional 从 Controller 移到 Service（事务属于 Service 层职责）
 *   ② ObjectMapper 改为 Spring 注入（不再 new，复用容器单例）
 *   ③ 重复的日期解析逻辑合并到 DateUtils.parseDateTime()
 *   ④ EntityManager 保留（truncate 清表需要）
 *
 * Controller 只负责"收 Body → 调 Service → 返回响应"，业务逻辑全在这里。
 */
@Service
@Transactional(readOnly = true)
public class SyncService {

    private final ClassInfoRepository classRepo;
    private final ClassRoundRepository roundRepo;
    private final ClassCategoryRepository classCatRepo;
    private final CategoryRepository categoryRepo;
    private final SysConfigRepository sysConfigRepo;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public SyncService(ClassInfoRepository classRepo,
                       ClassRoundRepository roundRepo,
                       ClassCategoryRepository classCatRepo,
                       CategoryRepository categoryRepo,
                       SysConfigRepository sysConfigRepo,
                       ObjectMapper objectMapper) {
        this.classRepo = classRepo;
        this.roundRepo = roundRepo;
        this.classCatRepo = classCatRepo;
        this.categoryRepo = categoryRepo;
        this.sysConfigRepo = sysConfigRepo;
        this.objectMapper = objectMapper;
    }

    /** 清空表（禁用外键检查后 truncate） */
    private void truncateWithForeignKeyDisabled(String table) {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE " + table).executeUpdate();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
    }

    /** 从 item 的 classRounds 字段计算 period 字符串（取第一轮） */
    @SuppressWarnings("unchecked")
    private String computePeriod(Map<String, Object> item) {
        Object roundsObj = item.get("classRounds");
        if (roundsObj == null) return null;
        List<Map> rounds = (roundsObj instanceof List) ? (List<Map>) roundsObj : List.of();
        if (rounds.isEmpty()) return null;
        Map first = rounds.get(0);
        Object ps = first.get("periodStart");
        Object pe = first.get("periodEnd");
        if (ps == null || pe == null) return null;
        LocalDateTime start = DateUtils.parseDateTime(ps.toString(), true);
        LocalDateTime end = DateUtils.parseDateTime(pe.toString(), false);
        if (start == null || end == null) return null;
        return start.format(DateUtils.ISO_DATETIME_FMT) + " - " + end.format(DateUtils.ISO_DATETIME_FMT);
    }

    @Transactional
    public Map<String, Object> syncClasses(List<Map<String, Object>> dataList) {
        truncateWithForeignKeyDisabled("ssc_class_category");
        truncateWithForeignKeyDisabled("ssc_class_rounds");
        truncateWithForeignKeyDisabled("ssc_classes");

        int inserted = 0;
        for (Map<String, Object> item : dataList) {
            String name = (String) item.get("name");
            if (name == null || name.isBlank()) continue;

            String period = computePeriod(item);
            if (period == null || period.isBlank()) period = "待定";

            ClassInfo cls = new ClassInfo();
            cls.setName(name);
            cls.setQuota(item.get("quota") == null ? 0 : (Integer) item.get("quota"));
            cls.setDescription((String) item.get("description"));
            cls.setIsDeleted(0);
            cls.setEnrolled(0);
            cls.setSource((String) item.getOrDefault("source", "sync"));
            cls.setGroupInfo((String) item.get("groupInfo"));
            cls.setPeriod(period);
            Object innerIdVal = item.get("innerId");
            if (innerIdVal != null) cls.setOuterId((Integer) innerIdVal);
            ClassInfo saved = classRepo.save(cls);

            Object roundsObj = item.get("classRounds");
            if (roundsObj instanceof List) {
                for (Map<String, Object> r : (List<Map<String, Object>>) roundsObj) {
                    ClassRound cr = new ClassRound();
                    cr.setClassId(saved.getId());
                    cr.setRoundNum(r.get("roundNum") == null ? 1 : (Integer) r.get("roundNum"));
                    cr.setPeriodStart(DateUtils.parseDateTime(String.valueOf(r.get("periodStart")), true));
                    cr.setPeriodEnd(DateUtils.parseDateTime(String.valueOf(r.get("periodEnd")), false));
                    roundRepo.save(cr);
                }
            }

            Object namesObj = item.get("categoryNames");
            if (namesObj instanceof List) {
                for (String catName : (List<String>) namesObj) {
                    if (catName == null || catName.isBlank()) continue;
                    final String n = catName.trim();
                    categoryRepo.findByName(n).ifPresent(cat -> {
                        ClassCategory cc = new ClassCategory();
                        cc.setClassId(saved.getId());
                        cc.setCategoryId(cat.getId());
                        classCatRepo.save(cc);
                    });
                }
            }
            inserted++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", ResultCode.SUCCESS.getCode());
        response.put("message", "同步成功");
        response.put("data", null);
        return response;
    }

    @Transactional
    public Map<String, Object> syncCategories(List<Map<String, Object>> dataList) {
        truncateWithForeignKeyDisabled("ssc_class_category");
        truncateWithForeignKeyDisabled("ssc_categories");

        int inserted = 0;
        for (Map<String, Object> item : dataList) {
            String name = (String) item.get("name");
            if (name == null || name.isBlank()) continue;
            Category cat = new Category();
            cat.setName(name.trim());
            categoryRepo.save(cat);
            inserted++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", ResultCode.SUCCESS.getCode());
        response.put("message", "同步成功");
        response.put("data", null);
        return response;
    }

    @Transactional
    public Map<String, Object> syncConfig(List<Map<String, Object>> dataList) {
        for (Map<String, Object> item : dataList) {
            SysConfig cfg = sysConfigRepo.findById(1).orElse(new SysConfig());
            cfg.setTitle((String) item.get("title"));
            cfg.setConditions((String) item.get("conditions"));
            cfg.setNotices((String) item.get("notices"));
            cfg.setUpdatedBy((String) item.getOrDefault("updatedBy", "sync"));
            cfg.setUpdatedAt(LocalDateTime.now());
            sysConfigRepo.save(cfg);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", ResultCode.SUCCESS.getCode());
        response.put("message", "同步成功");
        response.put("data", null);
        return response;
    }
}
