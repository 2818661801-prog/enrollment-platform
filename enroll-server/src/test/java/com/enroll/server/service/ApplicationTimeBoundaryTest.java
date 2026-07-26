package com.enroll.server.service;

import com.enroll.server.entity.ClassInfo;
import com.enroll.server.entity.ClassRound;
import com.enroll.server.repository.ClassInfoRepository;
import com.enroll.server.repository.ClassRoundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional  // 测试结束后自动回滚，不污染DB
public class ApplicationTimeBoundaryTest {

    @Autowired
    private ClassInfoRepository classInfoRepository;

    @Autowired
    private ClassRoundRepository roundRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

    // ========== 场景A：窗口刚开放（起点刚到，当前时间在窗口内）==========
    @Test
    @Order(1)
    void shouldAllowEnrollment_whenStartTimeJustReached() {
        // GIVEN：构造一个极窄窗口，起点是当前时间前1秒，终点是当前时间后1小时
        LocalDateTime start = ldt(0, -1);
        LocalDateTime end = ldt(0, 3600);

        // 查 id=1 的班级，确保存在
        ClassInfo cls = classInfoRepository.findById(TEST_CLASS_ID)
                .orElseThrow(() -> new RuntimeException("测试班级不存在，id=1"));
        cls.setPeriod(getTimeStr(0, -1) + " - " + getTimeStr(0, 3600));
        classInfoRepository.save(cls);

        // 更新/创建 class_rounds 表的 round=1
        List<ClassRound> existing = roundRepository.findByClassIdOrderByRoundNum(TEST_CLASS_ID);
        ClassRound round;
        if (existing.isEmpty()) {
            round = new ClassRound();
            round.setClassId(TEST_CLASS_ID);
            round.setRoundNum(1);
        } else {
            round = existing.get(0);
        }
        round.setPeriodStart(start);
        round.setPeriodEnd(end);
        round.setCreatedAt(LocalDateTime.now());
        roundRepository.save(round);

        // WHEN：发报名请求（合法数据）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "测试边界时间");
        body.put("idCard", "110101***REMOVED***4");
        body.put("gender", "男");
        body.put("phone", "***REMOVED***");
        body.put("classId", TEST_CLASS_ID);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/applications", new HttpEntity<>(body, headers), String.class);

        // THEN：报名成功（202 或 200）
        assertTrue(resp.getStatusCode().is2xxSuccessful(),
                "报名时间刚到，应可报名，实际响应：" + resp.getStatusCode() + " - " + resp.getBody());
    }

    // ========== 场景C：窗口刚关闭（起点=终点，0秒窗口，等于已过）==========
    @Test
    @Order(2)
    void shouldRejectEnrollment_whenEndTimeJustPassed() {
        // GIVEN：构造窗口 = 0秒，即 startTime == endTime
        LocalDateTime moment = ldt(0, -1);

        ClassInfo cls = classInfoRepository.findById(TEST_CLASS_ID)
                .orElseThrow(() -> new RuntimeException("测试班级不存在，id=1"));
        cls.setPeriod(getTimeStr(0, -1) + " - " + getTimeStr(0, -1));
        classInfoRepository.save(cls);

        List<ClassRound> existing = roundRepository.findByClassIdOrderByRoundNum(TEST_CLASS_ID);
        ClassRound round;
        if (existing.isEmpty()) {
            round = new ClassRound();
            round.setClassId(TEST_CLASS_ID);
            round.setRoundNum(1);
        } else {
            round = existing.get(0);
        }
        round.setPeriodStart(moment);
        round.setPeriodEnd(moment);
        round.setCreatedAt(LocalDateTime.now());
        roundRepository.save(round);

        // WHEN：发报名请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "测试截止时间");
        body.put("idCard", "110101***REMOVED***9");
        body.put("gender", "女");
        body.put("phone", "***REMOVED***");
        body.put("classId", TEST_CLASS_ID);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/applications", new HttpEntity<>(body, headers), String.class);

        // THEN：报名失败（code != 200）
        assertNotEquals(200, getCode(resp),
                "截止时间已过，应拒绝报名，实际响应：" + resp.getStatusCode() + " - " + resp.getBody());
    }

    // ========== 场景B：窗口在未来（当前时间不在窗口内，1小时后才开）==========
    @Test
    @Order(3)
    void shouldRejectEnrollment_whenWindowInFuture() {
        // GIVEN：窗口从现在起1小时后开始，2小时后结束（确定在未来）
        LocalDateTime start = ldt(0, 3600);
        LocalDateTime end = ldt(0, 7200);

        ClassInfo cls = classInfoRepository.findById(TEST_CLASS_ID)
                .orElseThrow(() -> new RuntimeException("测试班级不存在，id=1"));
        cls.setPeriod(getTimeStr(0, 3600) + " - " + getTimeStr(0, 7200));
        classInfoRepository.save(cls);

        List<ClassRound> existing = roundRepository.findByClassIdOrderByRoundNum(TEST_CLASS_ID);
        ClassRound round;
        if (existing.isEmpty()) {
            round = new ClassRound();
            round.setClassId(TEST_CLASS_ID);
            round.setRoundNum(1);
        } else {
            round = existing.get(0);
        }
        round.setPeriodStart(start);
        round.setPeriodEnd(end);
        round.setCreatedAt(LocalDateTime.now());
        roundRepository.save(round);

        // WHEN：发报名请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "测试窗口在未来");
        body.put("idCard", "110101***REMOVED***8");
        body.put("gender", "男");
        body.put("phone", "***REMOVED***");
        body.put("classId", TEST_CLASS_ID);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/applications", new HttpEntity<>(body, headers), String.class);

        // THEN：报名失败（当前时间不在窗口内，窗口1小时后才开）
        assertNotEquals(200, getCode(resp),
                "当前时间不在窗口内（窗口1小时后才开），应拒绝报名，实际响应：" + resp.getStatusCode() + " - " + resp.getBody());
    }
}
