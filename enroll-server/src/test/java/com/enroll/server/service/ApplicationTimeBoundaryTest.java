package com.enroll.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// 不加 @Transactional，手动管理事务，确保每步操作独立可见
public class ApplicationTimeBoundaryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int TEST_CLASS_ID = 1;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // 辅助方法：返回 "yyyy/MM/dd HH:mm:ss" 格式的时间串
    private String getTimeStr(int offsetDays, int offsetSeconds) {
        LocalDateTime dt = LocalDateTime.now().plusDays(offsetDays).plusSeconds(offsetSeconds);
        return dt.format(DTF);
    }

    // 辅助方法：返回 LocalDateTime
    private LocalDateTime ldt(int offsetDays, int offsetSeconds) {
        return LocalDateTime.now().plusDays(offsetDays).plusSeconds(offsetSeconds);
    }

    // 辅助方法：从响应 body 中解析 code 字段
    private int getCode(ResponseEntity<String> resp) {
        try {
            JsonNode node = objectMapper.readTree(resp.getBody());
            return node.has("code") ? node.get("code").asInt() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    // 辅助方法：用 native SQL UPDATE 修改 class_rounds 表（REQUIRES_NEW事务），确保数据立即落盘
    private void updateRoundInNewTransaction(int classId, int roundNum,
            LocalDateTime periodStart, LocalDateTime periodEnd) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                    "UPDATE ssc_class_rounds SET period_start = ?, period_end = ? " +
                    "WHERE class_id = ? AND round_num = ?")
                    .setParameter(1, periodStart)
                    .setParameter(2, periodEnd)
                    .setParameter(3, classId)
                    .setParameter(4, roundNum)
                    .executeUpdate();
        });
    }

    // 辅助方法：用 native SQL UPDATE 修改 ssc_classes 表（REQUIRES_NEW事务）
    private void updateClassInNewTransaction(int classId, String period) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                    "UPDATE ssc_classes SET period = ? WHERE id = ?")
                    .setParameter(1, period)
                    .setParameter(2, classId)
                    .executeUpdate();
        });
    }

    // ========== 场景A：窗口刚开放（起点刚过，当前时间在窗口内）==========
    @Test
    @Order(1)
    void shouldAllowEnrollment_whenStartTimeJustReached() {
        // GIVEN：窗口 now-30s ~ now+3600s，起点刚过（now >= start），终点在未来1小时
        LocalDateTime start = ldt(0, -30);
        LocalDateTime end = ldt(0, 3600);

        updateClassInNewTransaction(TEST_CLASS_ID, getTimeStr(0, -30) + " - " + getTimeStr(0, 3600));
        updateRoundInNewTransaction(TEST_CLASS_ID, 1, start, end);

        // WHEN：发报名请求（合法数据）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", "测试边界时间");
        body.put("idCard", "110101***REMOVED***4");
        body.put("gender", "男");
        body.put("phone", "***REMOVED***");
        body.put("classId", TEST_CLASS_ID);
        body.put("hasPhysics", "否");

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/applications", new HttpEntity<>(body, headers), String.class);

        // THEN：报名成功（HTTP 2xx 且业务 code = 200）
        assertTrue(resp.getStatusCode().is2xxSuccessful() && getCode(resp) == 200,
                "报名时间刚到，应可报名（code=200），实际响应：" + resp.getStatusCode() + " code=" + getCode(resp) + " body=" + resp.getBody());
    }

    // ========== 场景C：窗口已关闭（now > endTime，严格成立）==========
    @Test
    @Order(2)
    void shouldRejectEnrollment_whenEndTimeJustPassed() {
        // GIVEN：窗口已过，start = now-2s，end = now-1s → now > endTime 严格成立
        LocalDateTime start = ldt(0, -2);
        LocalDateTime end = ldt(0, -1);

        updateClassInNewTransaction(TEST_CLASS_ID, getTimeStr(0, -2) + " - " + getTimeStr(0, -1));
        updateRoundInNewTransaction(TEST_CLASS_ID, 1, start, end);

        // WHEN：发报名请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", "测试截止时间");
        body.put("idCard", "110101***REMOVED***9");
        body.put("gender", "女");
        body.put("phone", "***REMOVED***");
        body.put("classId", TEST_CLASS_ID);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/applications", new HttpEntity<>(body, headers), String.class);

        // THEN：报名失败（HTTP 2xx 且业务 code = 4005）
        assertTrue(resp.getStatusCode().is2xxSuccessful() && getCode(resp) == 4005,
                "截止时间已过，应拒绝报名（code=4005），实际响应：" + resp.getStatusCode() + " code=" + getCode(resp) + " body=" + resp.getBody());
    }

    // ========== 场景B：窗口在未来（当前时间不在窗口内，1小时后才开）==========
    @Test
    @Order(3)
    void shouldRejectEnrollment_whenWindowInFuture() {
        // GIVEN：窗口从现在起1小时后开始，2小时后结束（确定在未来）
        LocalDateTime start = ldt(0, 3600);
        LocalDateTime end = ldt(0, 7200);

        updateClassInNewTransaction(TEST_CLASS_ID, getTimeStr(0, 3600) + " - " + getTimeStr(0, 7200));
        updateRoundInNewTransaction(TEST_CLASS_ID, 1, start, end);

        // WHEN：发报名请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", "测试窗口在未来");
        body.put("idCard", "110101***REMOVED***8");
        body.put("gender", "男");
        body.put("phone", "***REMOVED***");
        body.put("classId", TEST_CLASS_ID);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/applications", new HttpEntity<>(body, headers), String.class);

        // THEN：报名失败（HTTP 2xx 且业务 code = 4005）
        assertTrue(resp.getStatusCode().is2xxSuccessful() && getCode(resp) == 4005,
                "当前时间不在窗口内（窗口1小时后才开），应拒绝报名（code=4005），实际响应：" + resp.getStatusCode() + " code=" + getCode(resp) + " body=" + resp.getBody());
    }
}
