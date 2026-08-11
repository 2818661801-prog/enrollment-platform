package com.enroll.server.service;

import com.enroll.server.dto.ApplicationDTO;
import com.enroll.server.entity.Application;
import com.enroll.server.entity.ClassInfo;
import com.enroll.server.entity.ClassRound;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.repository.ApplicationRepository;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * ApplicationService 纯单元测试（Mockito 隔离，不依赖 MySQL）
 *
 * 覆盖范围：
 *   - submit() 字段格式校验（姓名/手机号/身份证格式 + 校验码）
 *   - submit() 重复校验（身份证全局/手机号全局/同班重复）
 *   - submit() 名额校验
 *   - withdraw() 状态守卫
 *   - batchAdmit/batchReject 状态守卫
 *   - parseFlag() 边界
 *   - statusLabel() 映射
 *   - toDTO() idCardRaw 权限控制
 *   - 身份证脱敏
 */
@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock private ApplicationRepository appRepo;
    @Mock private ClassInfoRepository classRepo;
    @Mock private ClassRoundRepository roundRepo;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ApplicationService service;

    // ==================== 公共 fixture ====================

    private ClassInfo buildClass(int id, int quota, int enrolled) {
        ClassInfo cls = new ClassInfo();
        cls.setId(id);
        cls.setName("测试班级");
        cls.setQuota(quota);
        cls.setEnrolled(enrolled);
        cls.setIsDeleted(0);
        return cls;
    }

    private Map<String, Object> validForm() {
        Map<String, Object> form = new HashMap<>();
        form.put("name", "张三丰");
        form.put("idCard", "110101***REMOVED***7");  // 合法校验码
        form.put("phone", "***REMOVED***");
        form.put("gender", "男");
        form.put("classId", 1);
        form.put("hasPhysics", "否");
        form.put("hasEnglish", "否");
        form.put("noticeAgreed", true);
        return form;
    }

    // ==================== submit() 字段格式校验 ====================

    @Nested
    @DisplayName("submit() 字段格式校验")
    class SubmitValidation {

        @Test
        @DisplayName("姓名：2个中文字符 → 通过")
        void name_2chinese_pass() {
            Map<String, Object> form = validForm();
            form.put("name", "李四");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(1);
            when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.submit(form));
        }

        @Test
        @DisplayName("姓名：1个字符 → 拒绝")
        void name_1char_reject() {
            Map<String, Object> form = validForm();
            form.put("name", "张");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
            assertTrue(ex.getMessage().contains("姓名"));
        }

        @Test
        @DisplayName("姓名：含数字 → 拒绝")
        void name_withDigit_reject() {
            Map<String, Object> form = validForm();
            form.put("name", "张三1");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            assertThrows(BusinessException.class, () -> service.submit(form));
        }

        @Test
        @DisplayName("姓名：null → 拒绝")
        void name_null_reject() {
            Map<String, Object> form = validForm();
            form.put("name", null);
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            assertThrows(BusinessException.class, () -> service.submit(form));
        }

        @Test
        @DisplayName("手机号：非1开头 → 拒绝")
        void phone_wrongPrefix_reject() {
            Map<String, Object> form = validForm();
            form.put("phone", "23900001111");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            assertThrows(BusinessException.class, () -> service.submit(form));
        }

        @Test
        @DisplayName("手机号：10位 → 拒绝")
        void phone_10digits_reject() {
            Map<String, Object> form = validForm();
            form.put("phone", "1390000111");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            assertThrows(BusinessException.class, () -> service.submit(form));
        }

        @Test
        @DisplayName("身份证：17位 → 拒绝（格式校验先于校验码）")
        void idCard_17digits_reject() {
            Map<String, Object> form = validForm();
            form.put("idCard", "110101***REMOVED***");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            assertThrows(BusinessException.class, () -> service.submit(form));
        }

        @Test
        @DisplayName("身份证：校验码错误 → 拒绝")
        void idCard_badChecksum_reject() {
            Map<String, Object> form = validForm();
            form.put("idCard", "110101***REMOVED***3");  // 末位改错
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
            assertTrue(ex.getMessage().contains("校验码"));
        }

        @Test
        @DisplayName("身份证：末位X大写 → 通过格式+校验码")
        void idCard_upperX_pass() {
            Map<String, Object> form = validForm();
            form.put("idCard", "110101***REMOVED***X");  // 合法X结尾
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(1);
            when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.submit(form));
        }
    }

    // ==================== submit() 重复校验 ====================

    @Nested
    @DisplayName("submit() 重复校验")
    class SubmitDuplicate {

        @Test
        @DisplayName("身份证全局重复（已报名）→ 拒绝，提示已报班级名")
        void idCardGlobalDup_reject() {
            Map<String, Object> form = validForm();
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));

            Application existing = new Application();
            existing.setClassId(2);
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(eq("110101***REMOVED***7"), anyList(), eq(0)))
                    .thenReturn(List.of(existing));
            when(classRepo.findById(2)).thenReturn(Optional.of(buildClass(2, 10, 1)));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
            assertTrue(ex.getMessage().contains("测试班级"));
        }

        @Test
        @DisplayName("手机号全局重复 → 拒绝")
        void phoneGlobalDup_reject() {
            Map<String, Object> form = validForm();
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());

            Application existing = new Application();
            existing.setClassId(2);
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(eq("***REMOVED***"), anyList(), eq(0)))
                    .thenReturn(List.of(existing));
            when(classRepo.findById(2)).thenReturn(Optional.of(buildClass(2, 10, 1)));

            assertThrows(BusinessException.class, () -> service.submit(form));
        }

        @Test
        @DisplayName("同班重复（已报名）→ 拒绝")
        void sameClassDup_reject() {
            Map<String, Object> form = validForm();
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());

            Application existing = new Application();
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(eq("110101***REMOVED***7"), eq(1), anyList(), eq(0)))
                    .thenReturn(List.of(existing));

            assertThrows(BusinessException.class, () -> service.submit(form));
        }
    }

    // ==================== submit() 名额校验 ====================

    @Nested
    @DisplayName("submit() 名额校验")
    class SubmitQuota {

        @Test
        @DisplayName("名额已满 → 拒绝")
        void classFull_reject() {
            Map<String, Object> form = validForm();
            ClassInfo cls = buildClass(1, 10, 10);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(0);  // 名额已满

            BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
            assertEquals(com.enroll.server.dto.ResultCode.CLASS_FULL, ex.getResultCode());
        }

        @Test
        @DisplayName("班级不存在 → 拒绝")
        void classNotFound_reject() {
            Map<String, Object> form = validForm();
            form.put("classId", 999);  // 匹配 mock 的 findById(999)
            when(classRepo.findById(999)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
            assertEquals(com.enroll.server.dto.ResultCode.CLASS_NOT_FOUND, ex.getResultCode());
        }

        @Test
        @DisplayName("不在报名时间内 → 拒绝")
        void notInTime_reject() {
            Map<String, Object> form = validForm();
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(null);  // 无有效轮次

            BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(form));
            assertTrue(ex.getMessage().contains("报名时间"));
        }
    }

    // ==================== submit() 身份证脱敏 ====================

    @Nested
    @DisplayName("submit() 身份证脱敏")
    class SubmitMasking {

        @Test
        @DisplayName("18位身份证中间8位脱敏为********")
        void idCardMasked_8stars() {
            Map<String, Object> form = validForm();
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(1);
            when(appRepo.save(any())).thenAnswer(inv -> {
                Application app = inv.getArgument(0);
                assertEquals("110101********1237", app.getIdCardMasked());
                return app;
            });

            service.submit(form);
        }
    }

    // ==================== withdraw() 状态守卫 ====================

    @Nested
    @DisplayName("withdraw() 状态守卫")
    class WithdrawGuard {

        private Application buildApp(int status) {
            Application app = new Application();
            app.setId(1);
            app.setStatus(status);
            app.setClassId(1);
            app.setRound(1);
            return app;
        }

        @Test
        @DisplayName("已报名(status=1) → 允许撤回")
        void applied_allow() {
            Application app = buildApp(ApplicationService.STATUS_APPLIED);
            when(appRepo.findById(1)).thenReturn(Optional.of(app));
            when(roundRepo.countExpired(1, 1)).thenReturn(0);

            assertDoesNotThrow(() -> service.withdraw(1));
            verify(classRepo).decrementEnrolled(1);
        }

        @Test
        @DisplayName("已撤回(status=2) → 拒绝，提示'已撤回'")
        void withdrawn_reject() {
            Application app = buildApp(ApplicationService.STATUS_WITHDRAWN);
            when(appRepo.findById(1)).thenReturn(Optional.of(app));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.withdraw(1));
            assertTrue(ex.getMessage().contains("已撤回"));
        }

        @Test
        @DisplayName("已录取(status=3) → 拒绝，提示'已录取'")
        void enrolled_reject() {
            Application app = buildApp(ApplicationService.STATUS_ENROLLED);
            when(appRepo.findById(1)).thenReturn(Optional.of(app));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.withdraw(1));
            assertTrue(ex.getMessage().contains("已录取"));
        }

        @Test
        @DisplayName("未录取(status=4) → 拒绝，提示'未录取'")
        void rejected_reject() {
            Application app = buildApp(ApplicationService.STATUS_REJECTED);
            when(appRepo.findById(1)).thenReturn(Optional.of(app));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.withdraw(1));
            assertTrue(ex.getMessage().contains("未录取"));
        }

        @Test
        @DisplayName("报名已截止 → 拒绝撤回")
        void expired_reject() {
            Application app = buildApp(ApplicationService.STATUS_APPLIED);
            when(appRepo.findById(1)).thenReturn(Optional.of(app));
            when(roundRepo.countExpired(1, 1)).thenReturn(1);  // 已过期

            BusinessException ex = assertThrows(BusinessException.class, () -> service.withdraw(1));
            assertTrue(ex.getMessage().contains("截止"));
        }
    }

    // ==================== batchAdmit / batchReject 状态守卫 ====================

    @Nested
    @DisplayName("batchAdmit/batchReject 状态守卫")
    class BatchStatusGuard {

        @Test
        @DisplayName("batchAdmit：含非审核中记录 → 拒绝，提示状态")
        void admit_nonApplied_reject() {
            Application applied = new Application();
            applied.setId(1);
            applied.setStatus(ApplicationService.STATUS_APPLIED);

            Application withdrawn = new Application();
            withdrawn.setId(2);
            withdrawn.setStatus(ApplicationService.STATUS_WITHDRAWN);

            when(appRepo.findAllById(List.of(1, 2))).thenReturn(List.of(applied, withdrawn));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.batchAdmit(List.of(1, 2)));
            assertTrue(ex.getMessage().contains("已撤回"));
        }

        @Test
        @DisplayName("batchReject：含非审核中记录 → 拒绝")
        void reject_nonApplied_reject() {
            Application enrolled = new Application();
            enrolled.setId(1);
            enrolled.setStatus(ApplicationService.STATUS_ENROLLED);
            enrolled.setClassId(1);

            when(appRepo.findAllById(List.of(1))).thenReturn(List.of(enrolled));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.batchReject(List.of(1)));
            assertTrue(ex.getMessage().contains("已录取"));
        }

        @Test
        @DisplayName("batchReject：审核中记录 → 释放名额")
        void reject_applied_decrementQuota() {
            Application app = new Application();
            app.setId(1);
            app.setStatus(ApplicationService.STATUS_APPLIED);
            app.setClassId(1);

            when(appRepo.findAllById(List.of(1))).thenReturn(List.of(app));

            service.batchReject(List.of(1));
            verify(classRepo).decrementEnrolled(1);
        }
    }

    // ==================== parseFlag() 边界 ====================

    @Nested
    @DisplayName("parseFlag() 边界值")
    class ParseFlag {

        @Test
        @DisplayName("null → 0")
        void null_returns0() {
            // parseFlag 是 private，通过 submit 间接测试
            Map<String, Object> form = validForm();
            form.put("noticeAgreed", null);
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(1);
            when(appRepo.save(any())).thenAnswer(inv -> {
                Application app = inv.getArgument(0);
                assertEquals(0, app.getNoticeAgreed());
                return app;
            });
            service.submit(form);
        }

        @Test
        @DisplayName("Boolean true → 1")
        void booleanTrue_returns1() {
            Map<String, Object> form = validForm();
            form.put("noticeAgreed", Boolean.TRUE);
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(1);
            when(appRepo.save(any())).thenAnswer(inv -> {
                Application app = inv.getArgument(0);
                assertEquals(1, app.getNoticeAgreed());
                return app;
            });
            service.submit(form);
        }

        @Test
        @DisplayName("字符串 '1' → 1")
        void string1_returns1() {
            Map<String, Object> form = validForm();
            form.put("noticeAgreed", "1");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(1);
            when(appRepo.save(any())).thenAnswer(inv -> {
                Application app = inv.getArgument(0);
                assertEquals(1, app.getNoticeAgreed());
                return app;
            });
            service.submit(form);
        }

        @Test
        @DisplayName("字符串 '0' → 0")
        void string0_returns0() {
            Map<String, Object> form = validForm();
            form.put("noticeAgreed", "0");
            ClassInfo cls = buildClass(1, 10, 0);
            when(classRepo.findById(1)).thenReturn(Optional.of(cls));
            when(roundRepo.findCurrentRound(1)).thenReturn(buildRound(1));
            when(appRepo.findByIdCardAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByPhoneAndStatusInAndIsDeleted(anyString(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(anyString(), anyInt(), anyList(), anyInt())).thenReturn(Collections.emptyList());
            when(classRepo.incrementEnrolledIfQuotaAvailable(1)).thenReturn(1);
            when(appRepo.save(any())).thenAnswer(inv -> {
                Application app = inv.getArgument(0);
                assertEquals(0, app.getNoticeAgreed());
                return app;
            });
            service.submit(form);
        }
    }

    // ==================== toDTO() idCardRaw 权限控制 ====================

    @Nested
    @DisplayName("toDTO() idCardRaw 权限控制")
    class ToDTOPermission {

        private Application buildApplication() {
            Application app = new Application();
            app.setId(1);
            app.setName("张三");
            app.setIdCard("110101***REMOVED***7");
            app.setIdCardMasked("110101********1237");
            app.setGender("男");
            app.setPhone("***REMOVED***");
            app.setHasPhysics("否");
            app.setHasEnglish("否");
            app.setClassId(1);
            app.setStatus(1);
            app.setRound(1);
            app.setIsDeleted(0);
            return app;
        }

        @Test
        @DisplayName("includeRaw=false → idCardRaw 为 null（学生端）")
        void studentView_noRaw() {
            ApplicationDTO dto = service.toDTO(buildApplication(), "测试班", null, null, false);
            assertNull(dto.getIdCardRaw());
            assertEquals("110101********1237", dto.getIdCard());
        }

        @Test
        @DisplayName("includeRaw=true → idCardRaw 有值（管理员端）")
        void adminView_hasRaw() {
            ApplicationDTO dto = service.toDTO(buildApplication(), "测试班", null, null, true);
            assertEquals("110101***REMOVED***7", dto.getIdCardRaw());
            assertEquals("110101********1237", dto.getIdCard());
        }
    }

    // ==================== 辅助方法 ====================

    /** 构造一个有效的 ClassRound（让 determineCurrentRound 返回指定轮次） */
    private ClassRound buildRound(int roundNum) {
        ClassRound r = new ClassRound();
        r.setClassId(1);
        r.setRoundNum(roundNum);
        return r;
    }
}
