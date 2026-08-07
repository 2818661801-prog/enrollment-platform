package com.enroll.server.service;

import com.enroll.server.dto.ClassDTO;
import com.enroll.server.dto.ClassRoundDTO;
import com.enroll.server.dto.R;
import com.enroll.server.entity.ClassCategory;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.entity.ClassRound;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
import com.enroll.server.repository.ClassCategoryRepository;
import com.enroll.server.repository.CategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班级业务层
 *
 * 班级类别（中间表 ssc_class_category）：
 *   一个班级可属多个类别，通过中间表实现 N:N 关联
 *   查询/写入均通过 ClassCategoryRepository
 */
@Service
@Transactional(readOnly = true)
public class ClassService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ClassInfoRepository classRepo;
    private final ClassRoundRepository roundRepo;
    private final ClassCategoryRepository classCatRepo;
    private final CategoryRepository categoryRepo;

    public ClassService(ClassInfoRepository classRepo,
                        ClassRoundRepository roundRepo,
                        ClassCategoryRepository classCatRepo,
                        CategoryRepository categoryRepo) {
        this.classRepo = classRepo;
        this.roundRepo = roundRepo;
        this.classCatRepo = classCatRepo;
        this.categoryRepo = categoryRepo;
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
     * @param body {name, classRounds([{roundNum, periodStart, periodEnd}]), quota, description, categoryNames:[name1,name2]}
     */
    @Transactional
    public ClassDTO createClass(java.util.Map<String, Object> body) {
        Integer quota = (Integer) body.getOrDefault("quota", 0);
        validateQuota(quota);

        ClassInfo cls = new ClassInfo();
        cls.setName((String) body.get("name"));
        cls.setQuota(quota);
        cls.setEnrolled(0);
        cls.setDescription((String) body.get("description"));
        cls.setIsDeleted(0);
        cls.setSource("admin");

        // classRounds：前端传 classRounds List，写 class_rounds 表
        Object roundsObj = body.get("classRounds");
        if (roundsObj == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "报名轮次不能为空");
        }
        List<ClassRound> savedRounds = saveRounds(null, roundsObj);
        if (!savedRounds.isEmpty()) {
            cls.setPeriod(formatPeriod(savedRounds.get(0)));
        }
        ClassInfo saved = classRepo.save(cls);

        // 回填 class_id 后写轮次表
        for (ClassRound r : savedRounds) {
            r.setClassId(saved.getId());
        }
        roundRepo.saveAll(savedRounds);

        // 写中间表类别
        saveCategories(saved.getId(), body.get("categoryNames"));

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
        if (body.containsKey("quota")) {
            Integer q = (Integer) body.get("quota");
            validateQuota(q);
            cls.setQuota(q);
        }
        if (body.containsKey("description")) cls.setDescription((String) body.get("description"));
        if (body.containsKey("isDeleted"))  cls.setIsDeleted((Integer) body.get("isDeleted"));
        if (body.containsKey("groupInfo"))   cls.setGroupInfo((String) body.get("groupInfo"));

        // classRounds 单独处理：先删旧轮次，再插新轮次
        if (body.containsKey("classRounds")) {
            roundRepo.deleteByClassId(id);
            Object roundsObj = body.get("classRounds");
            List<ClassRound> savedRounds = saveRounds(id, roundsObj);
            if (!savedRounds.isEmpty()) {
                cls.setPeriod(formatPeriod(savedRounds.get(0)));
                roundRepo.saveAll(savedRounds);
            }
        }

        // 更新中间表类别（无论是否传入 categoryNames，都重新写入）
        if (body.containsKey("categoryNames")) {
            saveCategories(id, body.get("categoryNames"));
        }

        ClassInfo saved = classRepo.save(cls);
        return toDTO(saved);
    }

    /** 修改报名时间段（兼容旧接口，废弃，由轮次表取代） */
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
        validateQuota(quota);
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
    }

    // ==================== 内部工具方法 ====================

    /** P1-7: quota 边界校验 */
    private void validateQuota(Integer quota) {
        if (quota != -1 && quota < 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "名额必须为 -1（不限）或正整数");
        }
    }

    // ==================== Entity → DTO（查中间表组装轮次+类别） ====================

    private ClassDTO toDTO(ClassInfo e) {
        List<String> cats = classCatRepo.findCategoryNamesByClassId(e.getId());
        List<ClassRoundDTO> rounds = findRoundsDTO(e.getId());
        return ClassDTO.builder()
                .id(e.getId())
                .name(e.getName())
                .period(e.getPeriod())
                .classRounds(rounds)
                .quota(e.getQuota())
                .enrolled(e.getEnrolled())
                .description(e.getDescription())
                .isDeleted(e.getIsDeleted() != null ? e.getIsDeleted() : 0)
                .categories(cats)
                .source(e.getSource() != null ? e.getSource() : "admin")
                .groupInfo(e.getGroupInfo())
                .build();
    }

    /**
     * 查某班级所有轮次，转 DTO 列表
     */
    private List<ClassRoundDTO> findRoundsDTO(Integer classId) {
        return roundRepo.findByClassIdOrderByRoundNum(classId).stream()
                .map(r -> ClassRoundDTO.builder()
                        .roundNum(r.getRoundNum())
                        .periodStart(r.getPeriodStart())
                        .periodEnd(r.getPeriodEnd())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 解析前端传来的 classRounds 列表，写入 class_rounds 表（先删后插，幂等）
     */
    @SuppressWarnings("unchecked")
    private List<ClassRound> saveRounds(Integer classId, Object roundsObj) {
        List<Map<String, Object>> rounds;
        if (roundsObj instanceof String) {
            try {
                rounds = MAPPER.readValue((String) roundsObj, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "classRounds 格式错误");
            }
        } else if (roundsObj instanceof List) {
            rounds = (List<Map<String, Object>>) roundsObj;
        } else {
            throw new BusinessException(ResultCode.PARAM_INVALID, "classRounds 格式错误");
        }

        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        // 先删旧轮次（幂等保障）
        if (classId != null) {
            roundRepo.deleteByClassId(classId);
        }

        return rounds.stream().map(item -> {
            ClassRound r = new ClassRound();
            r.setClassId(classId);
            r.setRoundNum((Integer) item.get("roundNum"));
            Object ps = item.get("periodStart");
            if (ps != null) {
                r.setPeriodStart(parseDateTime(String.valueOf(ps), dateTimeFmt, dateFmt, true));
            }
            Object pe = item.get("periodEnd");
            if (pe != null) {
                r.setPeriodEnd(parseDateTime(String.valueOf(pe), dateTimeFmt, dateFmt, false));
            }
            return r;
        }).collect(Collectors.toList());
    }

    /**
     * 解析日期时间字符串，支持 "2026/09/01 08:00" 和 "2026/09/01" 两种格式
     */
    private java.time.LocalDateTime parseDateTime(String s, DateTimeFormatter dateTimeFmt,
                                                   DateTimeFormatter dateFmt, boolean isStart) {
        try {
            if (s.contains(":")) {
                return java.time.LocalDateTime.parse(s, dateTimeFmt);
            } else {
                java.time.LocalDate d = java.time.LocalDate.parse(s, dateFmt);
                return isStart ? d.atStartOfDay() : d.atTime(23, 59);
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "日期格式错误: " + s);
        }
    }

    /**
     * 把 ClassRound 格式化为 "2026/09/01 08:00 - 2026/09/13 23:59" 字符串
     */
    private String formatPeriod(ClassRound r) {
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        String start = r.getPeriodStart().format(dtFmt);
        String end = r.getPeriodEnd().format(dtFmt);
        return start + " - " + end;
    }

    /**
     * 写入班级-类别中间表
     * @param classId 班级ID
     * @param categoryNamesObj 前端传来的类别名称数组（List<String> 或 JSON 字符串）
     */
    @SuppressWarnings("unchecked")
    private void saveCategories(Integer classId, Object categoryNamesObj) {
        // 先删旧关联
        classCatRepo.deleteByClassId(classId);
        if (categoryNamesObj == null) return;

        List<String> names;
        if (categoryNamesObj instanceof String) {
            try {
                names = MAPPER.readValue((String) categoryNamesObj, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return;
            }
        } else if (categoryNamesObj instanceof List) {
            names = (List<String>) categoryNamesObj;
        } else {
            return;
        }

        // 按名称查 id，写入中间表
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            categoryRepo.findByName(name.trim()).ifPresent(cat -> {
                ClassCategory cc = new ClassCategory();
                cc.setClassId(classId);
                cc.setCategoryId(cat.getId());
                classCatRepo.save(cc);
            });
        }
    }

    // ==================== 班级-类别关联（从 AdminController 搬入） ====================

    /** 获取全部班级-类别关联（从 AdminController 搬入） */
    public List<ClassCategory> listClassCategories() {
        return classCatRepo.findAll();
    }

    /** 新增班级-类别关联（从 AdminController 搬入，含查重） */
    public Map<String, Object> addClassCategory(Integer classId, Integer categoryId) {
        if (classId == null || categoryId == null) {
            return R.fail(ResultCode.PARAM_INVALID, "classId 和 categoryId 不能为空");
        }
        List<ClassCategory> existing = classCatRepo.findByClassId(classId);
        boolean alreadyExists = existing.stream()
                .anyMatch(cc -> cc.getCategoryId().equals(categoryId));
        if (alreadyExists) {
            return R.ok("关联已存在，无需重复创建", null);
        }
        ClassCategory cc = new ClassCategory();
        cc.setClassId(classId);
        cc.setCategoryId(categoryId);
        cc.setCreatedAt(java.time.LocalDateTime.now());
        classCatRepo.save(cc);
        return R.ok("关联创建成功", null);
    }

    /** 删除班级-类别关联（从 AdminController 搬入） */
    public Map<String, Object> deleteClassCategory(Integer id) {
        if (id == null) {
            return R.fail(ResultCode.PARAM_INVALID, "id 不能为空");
        }
        classCatRepo.deleteById(id);
        return R.ok("关联已删除", null);
    }
}
