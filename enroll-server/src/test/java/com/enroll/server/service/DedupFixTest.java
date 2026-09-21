package com.enroll.server.service;

import com.enroll.server.entity.Application;
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
 * 2026-08-23 防重逻辑修复验证测试
 *
 * 验证三个关键场景：
 *   1. 被驳回(4)后报其他班 → 应该通过（全局唯一不查 status=4）
 *   2. 被驳回(4)后报同一个班 → 应该拦截（同班级防重查 status=4）
 *   3. 已录取(3)后报任何班 → 应该拦截（全局唯一查 status=3）
 */
@ExtendWith(MockitoExtension.class)
public class DedupFixTest {

    @Mock private ApplicationRepository appRepo;
    @Mock private ClassInfoRepository classRepo;
    @Mock private ClassRoundRepository roundRepo;
    @Mock private ClassCategoryRepository classCatRepo;

    @InjectMocks
    private ApplicationService service;

    private static final String ID_CARD = "110101***REMOVED***7"; // 合法校验码

    private ClassInfo buildClass(int id) {
        ClassInfo cls = new ClassInfo();
        cls.setId(id);
        cls.setName("班级" + id);
        cls.setQuota(10);
        cls.setEnrolled(0);
        return cls;
    }

    private ClassRound buildRound(int roundNum) {
        ClassRound r = new ClassRound();
        r.setClassId(1);
        r.setRoundNum(roundNum);
        return r;
    }

    private Map<String, Object> validForm(int classId) {
        Map<String, Object> form = new HashMap<>();
        form.put("name", "张三丰");
        form.put("idCard", ID_CARD);
        form.put("phone", "***REMOVED***");
        form.put("gender", "男");
        form.put("classId", classId);
        form.put("hasPhysics", "否");
        form.put("hasEnglish", "否");
        form.put("noticeAgreed", true);
        return form;
    }

    private Application rejectedApp(int classId) {
        Application a = new Application();
        a.setClassId(classId);
        a.setStatus(ApplicationService.STATUS_REJECTED); // status=4 被驳回
        return a;
    }

    private void stubPassThrough(int classId) {
        when(classRepo.findById(classId)).thenReturn(Optional.of(buildClass(classId)));
        when(roundRepo.findCurrentRound(classId)).thenReturn(buildRound(1));
        // 默认测试班级未设置类别，避免 appliedCategory 校验干扰原有防重测试
        when(classCatRepo.findCategoryNamesByClassId(classId)).thenReturn(Collections.emptyList());
        // 拦截场景会提前抛异常，下面两个 stub 不会被调用，用 lenient() 避免 UnnecessaryStubbing
        lenient().when(classRepo.incrementEnrolledIfQuotaAvailable(classId)).thenReturn(1);
        lenient().when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("被驳回(4)后报其他班 → 通过（全局唯一不查4）")
    void rejectedThenApplyOtherClass_pass() {
        Map<String, Object> form = validForm(1); // 报班级1
        stubPassThrough(1);

        // 全局唯一(身份证/手机号)都查不到（被驳回的4不在1,3里，所以这里返回空）
        when(appRepo.findByIdCardAndStatusInAndIsDeleted(eq(ID_CARD), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(appRepo.findByPhoneAndStatusInAndIsDeleted(eq("***REMOVED***"), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());
        // 同班级防重也查不到（班级1没有历史记录）
        when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(eq(ID_CARD), eq(1), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.submit(form));
    }

    @Test
    @DisplayName("被驳回(4)后报同一个班 → 拦截（同班级防重查4）")
    void rejectedThenApplySameClass_reject() {
        Map<String, Object> form = validForm(1); // 报班级1
        stubPassThrough(1);

        when(appRepo.findByIdCardAndStatusInAndIsDeleted(eq(ID_CARD), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(appRepo.findByPhoneAndStatusInAndIsDeleted(eq("***REMOVED***"), anyList(), eq(0)))
                .thenReturn(Collections.emptyList());
        // 同班级防重：查到被驳回(4)的记录 → 应拦截
        when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(eq(ID_CARD), eq(1), anyList(), eq(0)))
                .thenReturn(List.of(rejectedApp(1)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
        assertEquals(com.enroll.server.dto.ResultCode.DUPLICATE_APPLICATION, ex.getResultCode());
    }

    @Test
    @DisplayName("已录取(3)后报任何班 → 拦截（全局唯一查3）")
    void enrolledThenApplyAnyClass_reject() {
        Map<String, Object> form = validForm(2); // 报班级2
        stubPassThrough(2);

        // 身份证全局唯一：查到已录取(3)的记录 → 应拦截
        Application enrolled = new Application();
        enrolled.setClassId(1);
        enrolled.setStatus(ApplicationService.STATUS_ENROLLED); // status=3 已录取
        when(appRepo.findByIdCardAndStatusInAndIsDeleted(eq(ID_CARD), anyList(), eq(0)))
                .thenReturn(List.of(enrolled));
        when(classRepo.findById(1)).thenReturn(Optional.of(buildClass(1)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
        assertEquals(com.enroll.server.dto.ResultCode.DUPLICATE_APPLICATION, ex.getResultCode());
    }
}
