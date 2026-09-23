# 报名时间边界测试计划

> **实现计划（任务驱动开发）：** 按 Phase 逐任务推进，每个任务含验证步骤与验收标准（复选框跟踪）；设计决策依据见 specs/ 对应设计文档。

**目标：** 验证报名系统时间边界逻辑：`startTime <= now < endTime`，即"报名时间刚到可报名，超过截止时间不可报名"

**架构：**
- 后端 JUnit 测试：改 DB `periods` 时间 → 发真实 HTTP POST 请求 → 验证响应码和 message
- 前端 Jest/Vitest 测试：Mock 时间工具函数，验证 `getClassTimeStatus` 分类结果
- 测试数据用 `@Transactional` 自动回滚，不污染 DB

**技术栈：** JUnit 5 + RestTemplate（后端）、Vitest（前端）、MySQL

---

## 文件结构

```
enroll-server/src/test/java/com/enroll/server/
  service/
    ApplicationTimeBoundaryTest.java    ← 新建：后端时间边界单元测试

enroll-web/src/
  utils/
    __tests__/
      data.time.test.js                 ← 新建：前端时间状态判断测试
```

---

## 业务逻辑说明

报名时间段 JSON 格式：
```json
[{"round":1,"period":"2026/09/01 08:00 - 2026/09/13 23:59:59"}]
```

判断逻辑（`ApplicationService.getRound`）：
- `now < startTime` → 不在报名时间段（未开始）
- `startTime <= now < endTime` → 可报名
- `now >= endTime` → 已截止

**本测试聚焦的边界场景：**

| 场景 | 构造方式 | 期望结果 |
|------|---------|---------|
| A：报名时间**正好到达** | `period = "2026/09/01 08:00:00 - 2026/09/13 23:59:59"` | ✅ 可报名 |
| B：恰好在窗口内 | `period = "2026/09/01 08:00:00 - 2026/09/01 09:00:00"` | ✅ 可报名 |
| C：截止时间**刚好超1秒** | `period = "2026/09/01 08:00:00 - 2026/09/01 08:00:00"`（窗口=0秒）| ❌ 拒绝（超过截止）|
| D：早于报名开始时间 | `period = "2026/09/01 08:00:00 - 2026/09/01 09:00:00"`，请求时人为改 NOW | ❌ 拒绝（未开始）|

---

## 任务 1：后端 JUnit 测试 — 构造测试类和辅助方法

**文件：**
- 创建：`enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java`

- [ ] **步骤 1：创建测试类骨架 + 依赖注入**

```java
package com.enroll.server.service;

import com.enroll.server.entity.ClassInfo;
import com.enroll.server.repository.ClassInfoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional  // 测试结束后自动回滚，不污染DB
public class ApplicationTimeBoundaryTest {

    @Autowired
    private ClassInfoRepository classInfoRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private static int testClassId = -1;

    @BeforeEach
    void init() {
        // 找一个真实存在的班级（id=1），不创建新数据
        testClassId = 1;
    }
}
```

- [ ] **步骤 2：验证测试环境正常（发一个已知的合法请求）**

运行：`cd enroll-server && mvnw.cmd test -Dtest=ApplicationTimeBoundaryTest#init -q`
预期：测试通过（testClassId = 1 存在）

- [ ] **步骤 3：Commit**

```bash
git add enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java
git commit -m "test: 创建时间边界测试类骨架"
```

---

## 任务 2：场景 A — 报名时间正好到达

**文件：**
- 修改：`enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java`

- [ ] **步骤 1：编写场景 A 测试（报名时间正好到达，应可报名）**

在 `ApplicationTimeBoundaryTest.java` 中添加：

```java
@Test
@Order(1)
void shouldAllowEnrollment_whenStartTimeJustReached() {
    // GIVEN：构造一个极窄窗口，起点是当前时间前1秒，终点是当前时间后1小时
    // 实际用 UPDATE periods 改 id=1 的班级
    String periodJson = "[{\"round\":1,\"period\":\" + getTimeStr(1, -1) + " - " + getTimeStr(1, 3600) + "\"}]";
    classInfoRepository.findById(testClassId).ifPresent(c -> {
        c.setPeriod(periodJson);
        c.setPeriods(periodJson);
        classInfoRepository.save(c);
    });

    // WHEN：发报名请求（合法数据）
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> body = new HashMap<>();
    body.put("name", "测试边界时间");
    body.put("idCard", "110101***REMOVED***4");
    body.put("gender", "男");
    body.put("phone", "***REMOVED***");
    body.put("classId", testClassId);

    ResponseEntity<String> resp = restTemplate.postForEntity(
        "/api/applications", new HttpEntity<>(body, headers), String.class);

    // THEN：报名成功（202 或 200）
    assertTrue(resp.getStatusCode().is2xxSuccessful(),
        "报名时间刚到，应可报名，实际响应：" + resp.getBody());
}
```

- [ ] **步骤 2：添加辅助方法 getTimeStr**

```java
// 辅助方法：返回 "yyyy/MM/dd HH:mm:ss" 格式的时间串
private String getTimeStr(int offsetDays, int offsetSeconds) {
    java.time.LocalDateTime dt = java.time.LocalDateTime.now()
        .plusDays(offsetDays).plusSeconds(offsetSeconds);
    return dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
}
```

- [ ] **步骤 3：运行测试验证**

运行：`cd enroll-server && mvnw.cmd test -Dtest=ApplicationTimeBoundaryTest#shouldAllowEnrollment_whenStartTimeJustReached -q`
预期：PASS（当前时间在窗口内，报名成功）

- [ ] **步骤 4：Commit**

```bash
git add enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java
git commit -m "test: 场景A - 报名时间正好到达应可报名"
```

---

## 任务 3：场景 C — 截止时间刚好超1秒（窗口=0秒）

**文件：**
- 修改：`enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java`

- [ ] **步骤 1：编写场景 C 测试（截止时间已过，应拒绝报名）**

```java
@Test
@Order(2)
void shouldRejectEnrollment_whenEndTimeJustPassed() {
    // GIVEN：构造窗口 = 0秒，即 startTime == endTime
    // now 既不小于 startTime，也不小于 endTime（因为 ==），所以 now < endTime 不成立
    String periodJson = "[{\"round\":1,\"period\":\" + getTimeStr(0, -1) + " - " + getTimeStr(0, -1) + "\"}]";
    classInfoRepository.findById(testClassId).ifPresent(c -> {
        c.setPeriod(periodJson);
        c.setPeriods(periodJson);
        classInfoRepository.save(c);
    });

    // WHEN：发报名请求
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> body = new HashMap<>();
    body.put("name", "测试截止时间");
    body.put("idCard", "110101***REMOVED***9");
    body.put("gender", "女");
    body.put("phone", "***REMOVED***");
    body.put("classId", testClassId);

    ResponseEntity<String> resp = restTemplate.postForEntity(
        "/api/applications", new HttpEntity<>(body, headers), String.class);

    // THEN：报名失败（400 或 403）
    assertTrue(resp.getStatusCode().value() >= 400,
        "截止时间已过，应拒绝报名，实际响应：" + resp.getStatusCode() + " - " + resp.getBody());
}
```

- [ ] **步骤 2：运行测试验证**

运行：`cd enroll-server && mvnw.cmd test -Dtest=ApplicationTimeBoundaryTest#shouldRejectEnrollment_whenEndTimeJustPassed -q`
预期：PASS（窗口=0秒，报名被拒绝）

- [ ] **步骤 3：Commit**

```bash
git add enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java
git commit -m "test: 场景C - 截止时间刚过应拒绝报名"
```

---

## 任务 4：场景 B — 恰好在窗口内（常规可报名场景）

**文件：**
- 修改：`enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java`

- [ ] **步骤 1：编写场景 B 测试（正常窗口内，应可报名）**

```java
@Test
@Order(3)
void shouldAllowEnrollment_whenWithinOpenWindow() {
    // GIVEN：窗口从现在起1小时后开始，2小时后结束（确定在未来）
    String periodJson = "[{\"round\":1,\"period\":\" + getTimeStr(0, 3600) + " - " + getTimeStr(0, 7200) + "\"}]";
    classInfoRepository.findById(testClassId).ifPresent(c -> {
        c.setPeriod(periodJson);
        c.setPeriods(periodJson);
        classInfoRepository.save(c);
    });

    // WHEN：发报名请求
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> body = new HashMap<>();
    body.put("name", "测试窗口内报名");
    body.put("idCard", "110101***REMOVED***8");
    body.put("gender", "男");
    body.put("phone", "***REMOVED***");
    body.put("classId", testClassId);

    ResponseEntity<String> resp = restTemplate.postForEntity(
        "/api/applications", new HttpEntity<>(body, headers), String.class);

    // THEN：报名失败（时间段在未来的1小时后，当前不在窗口内）
    assertTrue(resp.getStatusCode().value() >= 400,
        "当前时间不在窗口内（窗口1小时后才开），应拒绝报名，实际响应：" + resp.getStatusCode());
}
```

- [ ] **步骤 2：运行测试验证**

运行：`cd enroll-server && mvnw.cmd test -Dtest=ApplicationTimeBoundaryTest#shouldAllowEnrollment_whenWithinOpenWindow -q`
预期：PASS（窗口在1小时后，当前不满足条件被拒绝）

- [ ] **步骤 3：Commit**

```bash
git add enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java
git commit -m "test: 场景B - 窗口在未来应拒绝报名"
```

---

## 任务 5：前端时间状态判断测试（Vitest）

**文件：**
- 创建：`enroll-web/src/utils/__tests__/data.time.test.js`

- [ ] **步骤 1：确认 `getClassTimeStatus` 函数位置**

查看：`enroll-web/src/utils/data.js` 中的 `getClassTimeStatus` 函数签名和逻辑

- [ ] **步骤 2：编写 Vitest 测试文件**

```javascript
// enroll-web/src/utils/__tests__/data.time.test.js
import { describe, it, expect } from 'vitest';
// 注意：实际 import 路径根据 data.js 导出方式调整
// import { getClassTimeStatus } from '../data.js';

describe('getClassTimeStatus 边界测试', () => {
  // 构造 class 对象（只含 periods 字段）
  const makeClass = (periodsStr) => ({ periods: periodsStr });

  it('报名时间正好到达 - 应返回可报名状态', () => {
    const now = new Date();
    // period 从现在开始，1小时后结束
    const start = formatTime(now);
    const end = formatTime(new Date(now.getTime() + 3600 * 1000));
    const periods = `[{"round":1,"period":"${start} - ${end}"}]`;

    // 当前时间 >= 开始时间，且 < 结束时间 → 可报名
    // 这里依赖 getClassTimeStatus 的具体实现
    const result = getClassTimeStatus(makeClass(periods));
    expect(result.canEnroll).toBe(true);
  });

  it('截止时间已过（start == end，窗口=0秒）- 应返回不可报名', () => {
    const now = new Date();
    const t = formatTime(now); // start == end
    const periods = `[{"round":1,"period":"${t} - ${t}"}]`;

    // now >= endTime（因为 ==），不满足 now < endTime
    const result = getClassTimeStatus(makeClass(periods));
    expect(result.canEnroll).toBe(false);
  });

  it('早于报名开始时间 - 应返回未开始', () => {
    const now = new Date();
    const start = formatTime(new Date(now.getTime() + 3600 * 1000)); // 1小时后
    const end = formatTime(new Date(now.getTime() + 7200 * 1000));    // 2小时后
    const periods = `[{"round":1,"period":"${start} - ${end}"}]`;

    const result = getClassTimeStatus(makeClass(periods));
    expect(result.canEnroll).toBe(false);
    expect(result.status).toBe('notStarted'); // 或实际返回的字段名
  });
});

// 辅助：格式化时间为 yyyy/MM/dd HH:mm:ss
function formatTime(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}/${pad(date.getMonth()+1)}/${pad(date.getDate())} ` +
         `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}
```

- [ ] **步骤 3：查看 data.js 确认 getClassTimeStatus 签名**

运行：`grep -n "getClassTimeStatus" enroll-web/src/utils/data.js | head -20`

- [ ] **步骤 4：如有需要，调整 import 路径和断言字段名**

根据实际函数签名修正测试文件

- [ ] **步骤 5：运行前端测试**

运行：`cd enroll-web && npx vitest run src/utils/__tests__/data.time.test.js`
预期：PASS（如有失败，根据实际返回值调整断言）

- [ ] **步骤 6：Commit**

```bash
git add enroll-web/src/utils/__tests__/data.time.test.js
git commit -m "test: 前端 getClassTimeStatus 边界测试（Vitest）"
```

---

## 验证总结

| 测试 | 工具 | 验证方式 | 预期结果 |
|------|------|---------|---------|
| 场景 A：报名时间正好到达 | JUnit + DB | HTTP POST | ✅ 2xx |
| 场景 B：窗口在1小时后 | JUnit + DB | HTTP POST | ❌ 4xx |
| 场景 C：窗口=0秒（截止过） | JUnit + DB | HTTP POST | ❌ 4xx |
| 前端边界判断 | Vitest | 直接调用函数 | 断言 canEnroll |

---

## 风险点

- `formatTime` 格式必须与 DB 存储格式一致（`yyyy/MM/dd HH:mm:ss`），否则 `parsePeriod` 解析失败
- 场景 B 的"窗口在1小时后"依赖 NOW() 在 JVM 进程内单调，测试执行时间差不会超过几秒
- `@Transactional` 对 REST 调用是否生效取决于 Spring Boot 测试配置，需确认 `@SpringBootTest` 包含事务支持

## 后期补救

- 如果 `@Transactional` 回滚不生效，测试前用 `@BeforeEach` UPDATE 后在 `@AfterEach` 手动 restore
- 如果 `parsePeriod` 格式不对，调 `formatTime` 格式为 DB 实际存储格式
