package com.enroll.server.service;

import com.enroll.server.dto.ResultCode;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.entity.ClassRound;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.repository.ApplicationRepository;
import com.enroll.server.repository.ClassCategoryRepository;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 2026-09-13 班级类别校验修复验证测试
 *
 * 验证场景：
 *   1. 班级未设置类别 → 不传 appliedCategory 也能通过
 *   2. 班级有类别但未传 appliedCategory → 拦截
 *   3. 班级有类别但 appliedCategory 为空字符串 → 拦截
 *   4. 班级有类别但 appliedCategory 无效 → 拦截
 *   5. 班级有类别且 appliedCategory 有效 → 通过
 */
@ExtendWith(MockitoExtension.class)
public class CategoryValidationTest {

    @Mock private ApplicationRepository appRepo;
    @Mock private ClassInfoRepository classRepo;
    @Mock private ClassRoundRepository roundRepo;
    @Mock private ClassCategoryRepository classCatRepo;

    @InjectMocks
    private ApplicationService service;

    private static final String ID_CARD = "110101***REMOVED***7"; // 合法校验码
    private static final int CLASS_ID = 1;

    private ClassInfo buildClass() {
        ClassInfo cls = new ClassInfo();
        cls.setId(CLASS_ID);
        cls.setName("测试班级");
        cls.setQuota(10);
        cls.setEnrolled(0);
        return cls;
    }

    private ClassRound buildRound() {
        ClassRound r = new ClassRound();
        r.setClassId(CLASS_ID);
        r.setRoundNum(1);
        return r;
    }

    private Map<String, Object> baseForm() {
        Map<String, Object> form = new HashMap<>();
        form.put("name", "张三丰");
        form.put("idCard", ID_CARD);
        form.put("phone", "***REMOVED***");
        form.put("gender", "男");
        form.put("classId", CLASS_ID);
        form.put("hasPhysics", "否");
        form.put("hasEnglish", "否");
        form.put("noticeAgreed", true);
        return form;
    }

    private void stubClassWithCategories(List<String> categories) {
        when(classRepo.findById(CLASS_ID)).thenReturn(Optional.of(buildClass()));
        when(roundRepo.findCurrentRound(CLASS_ID)).thenReturn(buildRound());
        when(classCatRepo.findCategoryNamesByClassId(CLASS_ID)).thenReturn(categories);
        // 通过校验后的后续依赖
        lenient().when(appRepo.findByIdCardAndStatusInAndIsDeleted(eq(ID_CARD), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());
        lenient().when(appRepo.findByPhoneAndStatusInAndIsDeleted(eq("***REMOVED***"), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());
        lenient().when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(eq(ID_CARD), eq(CLASS_ID), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());
        lenient().when(classRepo.incrementEnrolledIfQuotaAvailable(CLASS_ID)).thenReturn(1);
        lenient().when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("班级未设置类别 → 不传 appliedCategory 通过")
    void noCategoryRequired_pass() {
        Map<String, Object> form = baseForm();
        stubClassWithCategories(Collections.emptyList());

        assertDoesNotThrow(() -> service.submit(form));
    }

    @Test
    @DisplayName("班级有类别但未传 appliedCategory → 拦截")
    void categoryMissing_reject() {
        Map<String, Object> form = baseForm();
        stubClassWithCategories(List.of("杭电班", "成电班"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
        assertEquals(ResultCode.PARAM_INVALID, ex.getResultCode());
        assertEquals("请选择班级类别", ex.getMessage());
    }

    @Test
    @DisplayName("班级有类别但 appliedCategory 为空字符串 → 拦截")
    void categoryEmpty_reject() {
        Map<String, Object> form = baseForm();
        form.put("appliedCategory", "  ");
        stubClassWithCategories(List.of("杭电班", "成电班"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
        assertEquals(ResultCode.PARAM_INVALID, ex.getResultCode());
        assertEquals("请选择班级类别", ex.getMessage());
    }

    @Test
    @DisplayName("班级有类别但 appliedCategory 无效 → 拦截")
    void categoryInvalid_reject() {
        Map<String, Object> form = baseForm();
        form.put("appliedCategory", "不存在班");
        stubClassWithCategories(List.of("杭电班", "成电班"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
        assertEquals(ResultCode.PARAM_INVALID, ex.getResultCode());
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("班级类别无效"));
    }

    @Test
    @DisplayName("班级有类别且 appliedCategory 有效 → 通过")
    void categoryValid_pass() {
        Map<String, Object> form = baseForm();
        form.put("appliedCategory", "杭电班");
        stubClassWithCategories(List.of("杭电班", "成电班"));

        assertDoesNotThrow(() -> service.submit(form));
    }
}
