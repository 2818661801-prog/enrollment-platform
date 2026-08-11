package com.enroll.server.service;

import com.enroll.server.dto.ClassDTO;
import com.enroll.server.entity.ClassCategory;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.entity.ClassRound;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.repository.ClassCategoryRepository;
import com.enroll.server.repository.CategoryRepository;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ClassService 纯单元测试（Mockito 隔离，不依赖 MySQL）
 *
 * 覆盖范围：
 *   - createClass() quota 校验 + classRounds 解析
 *   - updateClass() 字段更新 + quota 校验
 *   - updateQuota() 边界
 *   - deleteClass() 软删除
 *   - addClassCategory() 查重
 *   - toDTO() 类别/轮次组装
 */
@ExtendWith(MockitoExtension.class)
public class ClassServiceTest {

    @Mock private ObjectMapper objectMapper;
    @Mock private ClassInfoRepository classRepo;
    @Mock private ClassRoundRepository roundRepo;
    @Mock private ClassCategoryRepository classCatRepo;
    @Mock private CategoryRepository categoryRepo;

    @InjectMocks
    private ClassService service;

    // ==================== createClass() ====================

    @Nested
    @DisplayName("createClass() quota 校验")
    class CreateClassQuota {

        @Test
        @DisplayName("quota=0 → 通过（0 名额虽无意义但不报错，validateQuota 只拦截负数非-1）")
        void quota0_pass() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "测试班");
            body.put("quota", 0);
            body.put("classRounds", List.of(
                    Map.of("roundNum", 1, "periodStart", "2026/09/01 08:00", "periodEnd", "2026/09/13 23:59")
            ));
            when(classRepo.save(any())).thenAnswer(inv -> {
                ClassInfo cls = inv.getArgument(0);
                cls.setId(1);
                return cls;
            });
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.createClass(body));
        }

        @Test
        @DisplayName("quota=-5 → 拒绝（负数非 -1）")
        void quotaNegative5_reject() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "测试班");
            body.put("quota", -5);
            body.put("classRounds", List.of());

            assertThrows(BusinessException.class, () -> service.createClass(body));
        }

        @Test
        @DisplayName("quota=-1 → 通过（不限名额）")
        void quotaMinus1_pass() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "不限名额班");
            body.put("quota", -1);
            body.put("classRounds", List.of(
                    Map.of("roundNum", 1, "periodStart", "2026/09/01 08:00", "periodEnd", "2026/09/13 23:59")
            ));
            when(classRepo.save(any())).thenAnswer(inv -> {
                ClassInfo cls = inv.getArgument(0);
                cls.setId(1);
                return cls;
            });
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.createClass(body));
        }

        @Test
        @DisplayName("quota=50 → 通过")
        void quota50_pass() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "50人班");
            body.put("quota", 50);
            body.put("classRounds", List.of(
                    Map.of("roundNum", 1, "periodStart", "2026/09/01 08:00", "periodEnd", "2026/09/13 23:59")
            ));
            when(classRepo.save(any())).thenAnswer(inv -> {
                ClassInfo cls = inv.getArgument(0);
                cls.setId(1);
                return cls;
            });
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.createClass(body));
        }

        @Test
        @DisplayName("classRounds 为 null → 拒绝")
        void classRoundsNull_reject() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "测试班");
            body.put("quota", 10);
            body.put("classRounds", null);

            BusinessException ex = assertThrows(BusinessException.class, () -> service.createClass(body));
            assertTrue(ex.getMessage().contains("轮次"));
        }
    }

    // ==================== createClass() classRounds 解析 ====================

    @Nested
    @DisplayName("createClass() classRounds 解析")
    class CreateClassRounds {

        @Test
        @DisplayName("List 格式 classRounds → 正确解析日期时间")
        void listFormat_parseOk() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "两轮班");
            body.put("quota", 30);
            body.put("classRounds", List.of(
                    Map.of("roundNum", 1, "periodStart", "2026/09/01 08:00", "periodEnd", "2026/09/13 23:59"),
                    Map.of("roundNum", 2, "periodStart", "2026/09/15 08:00", "periodEnd", "2026/09/16 23:59")
            ));
            when(classRepo.save(any())).thenAnswer(inv -> {
                ClassInfo cls = inv.getArgument(0);
                cls.setId(1);
                return cls;
            });
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());

            ClassDTO dto = service.createClass(body);
            assertNotNull(dto);
            // period 应由第一轮格式化
            assertNotNull(dto.getPeriod());
            assertTrue(dto.getPeriod().contains("2026/09/01"));
        }

        @Test
        @DisplayName("仅日期格式（无时分）→ 开始 00:00，结束 23:59")
        void dateOnly_defaultTime() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "日期班");
            body.put("quota", 20);
            body.put("classRounds", List.of(
                    Map.of("roundNum", 1, "periodStart", "2026/09/01", "periodEnd", "2026/09/13")
            ));
            when(classRepo.save(any())).thenAnswer(inv -> {
                ClassInfo cls = inv.getArgument(0);
                cls.setId(1);
                return cls;
            });
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());

            ClassDTO dto = service.createClass(body);
            assertTrue(dto.getPeriod().contains("00:00"));
            assertTrue(dto.getPeriod().contains("23:59"));
        }

        @Test
        @DisplayName("非法日期格式 → 拒绝")
        void invalidDate_reject() {
            Map<String, Object> body = new HashMap<>();
            body.put("name", "非法日期班");
            body.put("quota", 20);
            body.put("classRounds", List.of(
                    Map.of("roundNum", 1, "periodStart", "not-a-date", "periodEnd", "2026/09/13")
            ));
            // classRepo.save 不需要 mock — 异常在 saveRounds 解析日期时抛出，不会到 save

            assertThrows(BusinessException.class, () -> service.createClass(body));
        }
    }

    // ==================== updateClass() ====================

    @Nested
    @DisplayName("updateClass() 字段更新")
    class UpdateClass {

        private ClassInfo existingClass() {
            ClassInfo cls = new ClassInfo();
            cls.setId(1);
            cls.setName("旧名称");
            cls.setQuota(10);
            cls.setEnrolled(5);
            cls.setIsDeleted(0);
            cls.setPeriod("2026/09/01 08:00 - 2026/09/13 23:59");
            return cls;
        }

        @Test
        @DisplayName("更新名称 → 成功")
        void updateName_ok() {
            ClassInfo cls = existingClass();
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(classRepo.save(any())).thenReturn(cls);
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());

            Map<String, Object> body = Map.of("name", "新名称");
            ClassDTO dto = service.updateClass(1, body);
            assertEquals("新名称", dto.getName());
        }

        @Test
        @DisplayName("更新 quota 为非法值 → 拒绝")
        void updateQuotaInvalid_reject() {
            ClassInfo cls = existingClass();
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));

            Map<String, Object> body = Map.of("quota", -5);
            assertThrows(BusinessException.class, () -> service.updateClass(1, body));
        }

        @Test
        @DisplayName("软删除 → isDeleted=1")
        void softDelete_ok() {
            ClassInfo cls = existingClass();
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(classRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.deleteClass(1);
            assertEquals(1, cls.getIsDeleted());
        }

        @Test
        @DisplayName("班级不存在 → 拒绝")
        void notFound_reject() {
            when(classRepo.findById(999)).thenReturn(Optional.empty());
            assertThrows(BusinessException.class, () -> service.updateClass(999, Map.of("name", "x")));
        }
    }

    // ==================== updateQuota() ====================

    @Nested
    @DisplayName("updateQuota() 边界")
    class UpdateQuota {

        @Test
        @DisplayName("quota=-1 → 通过")
        void minus1_pass() {
            ClassInfo cls = new ClassInfo();
            cls.setId(1);
            cls.setQuota(10);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(classRepo.save(any())).thenReturn(cls);
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.updateQuota(1, -1));
        }

        @Test
        @DisplayName("quota=-2 → 拒绝")
        void minus2_reject() {
            assertThrows(BusinessException.class, () -> service.updateQuota(1, -2));
        }
    }

    // ==================== addClassCategory() 查重 ====================

    @Nested
    @DisplayName("addClassCategory() 查重")
    class AddClassCategory {

        @Test
        @DisplayName("null 参数 → 返回失败")
        void nullParams_fail() {
            var result = service.addClassCategory(null, 1);
            assertTrue(result.get("code").toString().contains("4005") ||
                       result.toString().contains("不能为空"));
        }

        @Test
        @DisplayName("已存在关联 → 返回'已存在'提示")
        void alreadyExists_ok() {
            ClassCategory existing = new ClassCategory();
            existing.setClassId(1);
            existing.setCategoryId(2);
            when(classCatRepo.findByClassId(1)).thenReturn(List.of(existing));

            var result = service.addClassCategory(1, 2);
            // 应返回成功但提示"已存在"
            assertTrue(result.toString().contains("已存在") || result.toString().contains("200"));
        }

        @Test
        @DisplayName("新关联 → 创建成功")
        void newAssoc_created() {
            when(classCatRepo.findByClassId(1)).thenReturn(Collections.emptyList());
            when(classCatRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.addClassCategory(1, 2);
            assertTrue(result.toString().contains("成功") || result.toString().contains("200"));
        }
    }

    // ==================== toDTO() 类别/轮次组装 ====================

    @Nested
    @DisplayName("toDTO() 类别/轮次组装")
    class ToDTO {

        @Test
        @DisplayName("source 为 null → 默认 'admin'")
        void sourceNull_defaultAdmin() {
            ClassInfo cls = new ClassInfo();
            cls.setId(1);
            cls.setName("测试班");
            cls.setSource(null);
            cls.setIsDeleted(0);
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(List.of("杭电班"));
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());

            // 间接通过 listAllForAdmin 测试 toDTO
            when(classRepo.findAll()).thenReturn(List.of(cls));
            List<ClassDTO> dtos = service.listAllForAdmin();
            assertEquals("admin", dtos.get(0).getSource());
            assertEquals(List.of("杭电班"), dtos.get(0).getCategories());
        }

        @Test
        @DisplayName("isDeleted 为 null → 默认 0")
        void isDeletedNull_default0() {
            ClassInfo cls = new ClassInfo();
            cls.setId(1);
            cls.setName("测试班");
            cls.setIsDeleted(null);
            when(classCatRepo.findCategoryNamesByClassId(1)).thenReturn(Collections.emptyList());
            when(roundRepo.findByClassIdOrderByRoundNum(1)).thenReturn(Collections.emptyList());

            when(classRepo.findAll()).thenReturn(List.of(cls));
            List<ClassDTO> dtos = service.listAllForAdmin();
            assertEquals(0, dtos.get(0).getIsDeleted());
        }
    }
}
