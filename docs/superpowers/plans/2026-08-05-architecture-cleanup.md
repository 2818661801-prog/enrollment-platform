# 特色班报名系统 · 架构整改实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 系统性修复 13 个架构级问题，使代码具备可维护性

**架构：** 4 Phase 串行推进——安全止血 → 后端分层归位 → 前端治理 → 技术债清理。在 `refactor/architecture-cleanup` 分支上执行，每个 Phase 完成后必须调用 `/verification-before-completion` 验证。

**技术栈：** Spring Boot 3.2.7 + JPA + MySQL / Vue 3 + Element Plus + Pinia + Vite

**设计文档：** `docs/superpowers/specs/2026-08-05-architecture-cleanup-design.md`

---

## 文件结构总览

### 后端新建文件

| 文件 | 职责 |
|------|------|
| `dto/request/ApplicationSubmitRequest.java` | 报名提交请求 DTO |
| `dto/request/ApplicationUpdateRequest.java` | 报名修改请求 DTO |
| `dto/request/AdminLoginRequest.java` | 管理员登录请求 DTO |
| `dto/request/SmsLoginRequest.java` | 短信登录请求 DTO |
| `dto/request/SmsSendRequest.java` | 发送验证码请求 DTO |
| `dto/request/ClassCreateRequest.java` | 新建班级请求 DTO |
| `dto/request/ClassUpdateRequest.java` | 更新班级请求 DTO |
| `dto/request/PeriodUpdateRequest.java` | 修改时间段请求 DTO |
| `dto/request/QuotaUpdateRequest.java` | 修改配额请求 DTO |
| `dto/request/NoticeUpdateRequest.java` | 修改须知请求 DTO |
| `dto/request/BatchAdmitRequest.java` | 批量录取请求 DTO |
| `dto/request/BatchRejectRequest.java` | 批量驳回请求 DTO |
| `dto/request/BatchDeleteRequest.java` | 批量删除请求 DTO |
| `dto/request/ClearClassRequest.java` | 清空班级请求 DTO |
| `dto/request/CategoryCreateRequest.java` | 新增类别请求 DTO |
| `dto/request/CategoryUpdateRequest.java` | 修改类别请求 DTO |
| `dto/request/CategoryDeleteRequest.java` | 删除类别请求 DTO |
| `dto/request/SyncClassesRequest.java` | 同步班级请求 DTO |
| `dto/request/SyncCategoriesRequest.java` | 同步类别请求 DTO |
| `dto/request/SyncConfigRequest.java` | 同步配置请求 DTO |
| `dto/request/WithdrawRequest.java` | 撤回报名请求 DTO |
| `dto/request/UpdateAppRequest.java` | 修改报名请求 DTO |
| `dto/request/ClassCategoryRequest.java` | 班级类别关联请求 DTO |
| `dto/request/ClassCategoryDeleteRequest.java` | 删除班级类别关联请求 DTO |
| `controller/AdminClassController.java` | 管理端班级 CRUD |
| `controller/AdminApplicationController.java` | 管理端报名管理 |
| `controller/AdminNoticeController.java` | 管理端报名须知 |
| `service/SyncService.java` | 同步业务逻辑 |
| `service/SysConfigService.java` | 系统配置业务逻辑 |
| `util/DateUtils.java` | 日期解析工具（合并 ClassService.parseDateTime + SyncController.parseDt） |

### 后端修改文件

| 文件 | 改动 |
|------|------|
| `controller/AuthController.java` | 移除 ApplicationRepository 注入，改用 Request DTO，fail-closed |
| `controller/AdminController.java` | 删除（拆分为 3 个新 Controller） |
| `controller/SyncController.java` | 瘦身：只调 SyncService |
| `controller/ApplicationController.java` | 改用 Request DTO，JWT 解析统一 |
| `controller/PublicConfigController.java` | 移除 SysConfigRepository，改用 SysConfigService |
| `controller/AdminCategoryController.java` | 改用 Request DTO |
| `controller/TimeController.java` | getClientIp 委托 RequestUtils |
| `service/ApplicationService.java` | toDTO 加 includeRaw 参数，ObjectMapper 改注入 |
| `service/AuthService.java` | 新增 findMyByPhone 方法，getClientIp 委托 RequestUtils |
| `service/ClassService.java` | parseDateTime 改调 DateUtils |
| `security/JwtUtil.java` | 新增 getUserIdFromToken 方法 |
| `entity/Category.java` | 删除 @ManyToMany classes 字段 |
| `exception/GlobalExceptionHandler.java` | catch-all 返回固定消息 |
| `application.yml` | fail-closed（移除 fallback 默认值） |
| `application-prod.yml` | 补充安全配置覆盖 |
| `.gitignore` | 追加运行时产物 |

### 后端删除文件

| 文件 | 原因 |
|------|------|
| `dto/ApiResponse.java` | 死代码 |

### 前端新建文件

| 文件 | 职责 |
|------|------|
| `stores/auth.js` | Pinia auth store |
| `composables/useWindowWidth.js` | 窗口宽度响应式 composable |
| `components/NoticeDialog.vue` | 报名须知弹窗（从 HomePage 拆出） |
| `components/ClassTable.vue` | 报名详情表格（从 HomePage 拆出） |
| `components/ClassDescDialog.vue` | 班级介绍弹窗（从 ClassCard + FormPage 共用） |
| `components/EditDialog.vue` | 编辑报名弹窗（从 MyApplications 拆出） |

### 前端修改文件

| 文件 | 改动 |
|------|------|
| `views/HomePage.vue` | 瘦身：拆出 NoticeDialog + ClassTable，用 auth store |
| `components/ClassCard.vue` | 拆出 ClassDescDialog，用 useWindowWidth |
| `views/FormPage.vue` | 复用 ClassDescDialog，用 useWindowWidth |
| `views/MyApplications.vue` | 拆出 EditDialog，用 auth store + api.js |
| `components/AppHeader.vue` | 用 auth store 替代 localStorage |
| `views/StudentLogin.vue` | 用 auth store |
| `router/index.js` | 加 beforeEach 路由守卫 |
| `utils/api.js` | request() 自动注入 token，删除 mock fallback |
| `utils/data.js` | 统一 parsePeriodsArray，删除 mock 数据导出 |
| `main.js` | 注册 Pinia |
| `package.json` | 添加 pinia，删除 playwright/mammoth/xlsx |

### 前端删除文件

| 文件 | 原因 |
|------|------|
| `composables/useApplication.js` | 死代码 |
| `components/PieChart.vue` | mock 数据，未接真实 API |
| `components/TrendChart.vue` | mock 数据，未接真实 API |

---

## Phase 1：安全止血 + 仓库卫生

### 任务 1：切换到整改分支 + .gitignore 补全

**文件：**
- 修改：`.gitignore`

- [ ] **步骤 1：切换分支**

```bash
cd 
git checkout refactor/architecture-cleanup
```

- [ ] **步骤 2：在 .gitignore 末尾追加**

```
# 运行时产物
*.log
enroll-server/data/
enroll-server/src/main/resources/static/

# 临时文件
tmp/
create_class.json
```

- [ ] **步骤 3：从 Git 追踪中移除已提交的运行时文件**

```bash
git rm --cached backend.log backend-err.log frontend.log frontend-err.log 2>/dev/null; true
git rm --cached -r enroll-server/src/main/resources/static/ 2>/dev/null; true
git rm --cached -r tmp/ 2>/dev/null; true
git rm --cached enroll-web/tmp/ 2>/dev/null; true
git rm --cached create_class.json 2>/dev/null; true
```

- [ ] **步骤 4：Commit**

```bash
git add .gitignore
git add -u
git commit -m "chore: .gitignore补全 + 移除运行时产物追踪"
```

---

### 任务 2：AuthController fail-open → fail-closed

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/controller/AuthController.java`
- 修改：`enroll-server/src/main/resources/application.yml`

- [ ] **步骤 1：修改 AuthController.java，移除硬编码 fallback**

将第 38-41 行的 `System.getenv` + fallback 改为 `@Value` 注入（无默认值 = 启动时必须设环境变量）：

```java
// 删除这两行：
// private static final String ADMIN_USER = System.getenv("AUTH_ADMIN_USER") != null
//     ? System.getenv("AUTH_ADMIN_USER") : "***REMOVED***";
// private static final String ADMIN_PWD  = System.getenv("AUTH_ADMIN_PASSWORD") != null
//     ? System.getenv("AUTH_ADMIN_PASSWORD") : "***REMOVED***";

// 新增字段注入（无 fallback = fail-closed）
@org.springframework.beans.factory.annotation.Value("${admin.username}")
private String adminUsername;

@org.springframework.beans.factory.annotation.Value("${admin.password}")
private String adminPassword;
```

同时修改 `login()` 方法中的引用：
```java
// 改前
if (!ADMIN_USER.equals(username) || !ADMIN_PWD.equals(password))
// 改后
if (!adminUsername.equals(username) || !adminPassword.equals(password))
```

- [ ] **步骤 2：在 application.yml 中添加 admin 配置项**

在 `jwt:` 之前添加：

```yaml
admin:
  username: ${ADMIN_USERNAME:***REMOVED***}
  password: ${ADMIN_PASSWORD:***REMOVED***}
```

> 注意：开发环境保留 fallback（方便本地调试），生产环境通过堡垒机环境变量覆盖。这与 JWT secret 的处理方式一致。

- [ ] **步骤 3：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/AuthController.java enroll-server/src/main/resources/application.yml
git commit -m "fix: AuthController改@Value注入，开发环境保留fallback，生产走环境变量"
```

---

### 任务 3：idCardRaw 权限控制

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/service/ApplicationService.java`
- 修改：`enroll-server/src/main/java/com/enroll/server/dto/ApplicationDTO.java`

- [ ] **步骤 1：ApplicationDTO.java 添加 includeRaw 字段控制**

在 ApplicationDTO 的 Builder 链中，将 `idCardRaw` 的设置改为受控：

```java
// 在 ApplicationDTO 中添加一个字段
private Boolean includeRaw;  // 是否包含原始身份证号

// 修改 builder 的 idCardRaw 设置逻辑（在 toDTO 调用处控制）
```

- [ ] **步骤 2：ApplicationService.toDTO() 加 includeRaw 参数**

修改 3 个 toDTO 重载方法：

```java
// 改前
public ApplicationDTO toDTO(Application e, String className) {
    return toDTO(e, className, null);
}
public ApplicationDTO toDTO(Application e, String className, Integer innerId) {
    return toDTO(e, className, innerId, null);
}
public ApplicationDTO toDTO(Application e, String className, Integer innerId, String classPeriods) {
    return ApplicationDTO.builder()
            ...
            .idCardRaw(e.getIdCard())  // ← 无条件暴露
            ...
            .build();
}

// 改后
public ApplicationDTO toDTO(Application e, String className) {
    return toDTO(e, className, null, null, false);
}
public ApplicationDTO toDTO(Application e, String className, Integer innerId) {
    return toDTO(e, className, innerId, null, false);
}
public ApplicationDTO toDTO(Application e, String className, Integer innerId, String classPeriods) {
    return toDTO(e, className, innerId, classPeriods, false);
}
public ApplicationDTO toDTO(Application e, String className, Integer innerId, String classPeriods, boolean includeRaw) {
    return ApplicationDTO.builder()
            .id(e.getId())
            .name(e.getName())
            .idCard(e.getIdCardMasked())
            .idCardRaw(includeRaw ? e.getIdCard() : null)  // ← 受控
            .gender(e.getGender())
            .phone(e.getPhone())
            .hasPhysics(e.getHasPhysics())
            .hasEnglish(e.getHasEnglish())
            .classId(e.getClassId())
            .className(className)
            .appliedCategory(e.getAppliedCategory())
            .status(String.valueOf(e.getStatus()))
            .applyTime(e.getApplyTime())
            .auditComment(e.getAuditComment())
            .classPeriods(classPeriods)
            .round(e.getRound())
            .isDeleted(e.getIsDeleted())
            .innerId(innerId)
            .enrollmentYear(e.getEnrollmentYear())
            .build();
}
```

- [ ] **步骤 3：管理员接口调用 toDTO 时传 includeRaw=true**

在后续 Phase 2 拆分 AdminApplicationController 时，调用 `toDTO(app, className, null, null, true)`。
当前 Phase 1 先改 ApplicationService，确保学生端默认不暴露。

- [ ] **步骤 4：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/service/ApplicationService.java enroll-server/src/main/java/com/enroll/server/dto/ApplicationDTO.java
git commit -m "fix: toDTO加includeRaw参数，学生端默认不返回idCardRaw"
```

---

### 任务 4：删除死代码 ApiResponse.java

**文件：**
- 删除：`enroll-server/src/main/java/com/enroll/server/dto/ApiResponse.java`

- [ ] **步骤 1：确认无引用**

```bash
cd 
grep -r "ApiResponse" enroll-server/src --include="*.java" -l
```

预期：只有 ApiResponse.java 自身。如果有其他文件引用，先处理引用。

- [ ] **步骤 2：删除文件**

```bash
git rm enroll-server/src/main/java/com/enroll/server/dto/ApiResponse.java
```

- [ ] **步骤 3：Commit**

```bash
git commit -m "chore: 删除死代码ApiResponse.java"
```

---

### 任务 5：Phase 1 验证

- [ ] **步骤 1：启动后端**

```bash
cd enroll-server
mvnw.cmd spring-boot:run -DskipTests
```

预期：启动成功（开发环境有 fallback 值）

- [ ] **步骤 2：测试学生端 API 不返回 idCardRaw**

```bash
curl -s http://localhost:8081/api/applications/me -H "Authorization: Bearer <student_token>" | python -m json.tool
```

预期：响应中 `idCardRaw` 为 `null`

- [ ] **步骤 3：调用 /verification-before-completion**

---

## Phase 2：后端分层归位

### 任务 6：新建 Request DTO（学生端接口）

**文件：**
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/ApplicationSubmitRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/ApplicationUpdateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/WithdrawRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/IdRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/AdminLoginRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/SmsLoginRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/SmsSendRequest.java`

- [ ] **步骤 1：创建 ApplicationSubmitRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApplicationSubmitRequest {
    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^[一-龥]{2,10}$", message = "姓名格式不正确（2-10个中文）")
    private String name;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String gender;
    private String hasPhysics;
    private String hasEnglish;
    private String appliedCategory;

    @NotNull(message = "班级ID不能为空")
    private Integer classId;

    private Object noticeAgreed;
}
```

- [ ] **步骤 2：创建 ApplicationUpdateRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationUpdateRequest {
    @NotNull(message = "报名ID不能为空")
    private Integer id;
    private String name;
    private String phone;
    private String idCard;
    private String gender;
    private String hasPhysics;
    private String hasEnglish;
}
```

- [ ] **步骤 3：创建 WithdrawRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WithdrawRequest {
    @NotNull(message = "报名ID不能为空")
    private Integer id;
}
```

- [ ] **步骤 4：创建 IdRequest.java（通用 ID 请求 DTO，复用于 deleteClass 等）**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IdRequest {
    @NotNull(message = "ID不能为空")
    private Integer id;
}
```

- [ ] **步骤 5：创建 AdminLoginRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginRequest {
    @NotBlank(message = "账号不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **步骤 6：创建 SmsLoginRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsLoginRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

- [ ] **步骤 7：创建 SmsSendRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsSendRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

- [ ] **步骤 8：确认 pom.xml 有 lombok 和 validation 依赖**

```bash
grep -E "lombok|validation" enroll-server/pom.xml
```

如果没有 `spring-boot-starter-validation`，需要添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

- [ ] **步骤 9：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/dto/request/
git add enroll-server/pom.xml  # 如果改了
git commit -m "feat: 新建学生端Request DTO（ApplicationSubmit/Update/Withdraw/AdminLogin/SmsLogin/SmsSend）"
```

---

### 任务 7：新建 Request DTO（管理端接口）

**文件：**
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/ClassCreateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/ClassUpdateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/PeriodUpdateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/QuotaUpdateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/NoticeUpdateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/BatchAdmitRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/BatchRejectRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/BatchDeleteRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/ClearClassRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/CategoryCreateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/CategoryUpdateRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/CategoryDeleteRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/ClassCategoryRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/ClassCategoryDeleteRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/SyncClassesRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/SyncCategoriesRequest.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/dto/request/SyncConfigRequest.java`

- [ ] **步骤 1：创建 ClassCreateRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ClassCreateRequest {
    @NotBlank(message = "班级名称不能为空")
    private String name;
    private Integer quota;
    private String description;
    private List<Map<String, Object>> classRounds;
    private List<String> categoryNames;
}
```

- [ ] **步骤 2：创建 ClassUpdateRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ClassUpdateRequest {
    @NotNull(message = "班级ID不能为空")
    private Integer id;
    private String name;
    private Integer quota;
    private String description;
    private Integer isDeleted;
    private String groupInfo;
    private List<Map<String, Object>> classRounds;
    private List<String> categoryNames;
}
```

- [ ] **步骤 3：创建 PeriodUpdateRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PeriodUpdateRequest {
    @NotNull(message = "班级ID不能为空")
    private Integer id;
    private String period;
}
```

- [ ] **步骤 4：创建 QuotaUpdateRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuotaUpdateRequest {
    @NotNull(message = "班级ID不能为空")
    private Integer id;
    @NotNull(message = "配额不能为空")
    private Integer quota;
}
```

- [ ] **步骤 5：创建 NoticeUpdateRequest.java**

```java
package com.enroll.server.dto.request;

import lombok.Data;

@Data
public class NoticeUpdateRequest {
    private String title;
    private String conditions;
    private String notices;
    private String contactInfo;
    private String updatedBy;
}
```

- [ ] **步骤 6：创建 BatchAdmitRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BatchAdmitRequest {
    @NotEmpty(message = "ids不能为空")
    private List<Integer> ids;
    private String auditComment;
}
```

- [ ] **步骤 7：创建 BatchRejectRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BatchRejectRequest {
    @NotEmpty(message = "ids不能为空")
    private List<Integer> ids;
    private String auditComment;
}
```

- [ ] **步骤 8：创建 BatchDeleteRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class BatchDeleteRequest {
    @NotEmpty(message = "ids不能为空")
    private List<Integer> ids;
}
```

- [ ] **步骤 9：创建 ClearClassRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClearClassRequest {
    @NotNull(message = "班级ID不能为空")
    private Integer classId;
}
```

- [ ] **步骤 10：创建 CategoryCreateRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotBlank(message = "类别名称不能为空")
    private String name;
}
```

- [ ] **步骤 11：创建 CategoryUpdateRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryUpdateRequest {
    @NotNull(message = "类别ID不能为空")
    private Integer id;
    @NotBlank(message = "类别名称不能为空")
    private String name;
}
```

- [ ] **步骤 12：创建 CategoryDeleteRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryDeleteRequest {
    @NotNull(message = "类别ID不能为空")
    private Integer id;
}
```

- [ ] **步骤 13：创建 ClassCategoryRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassCategoryRequest {
    @NotNull(message = "班级ID不能为空")
    private Integer classId;
    @NotNull(message = "类别ID不能为空")
    private Integer categoryId;
}
```

- [ ] **步骤 14：创建 ClassCategoryDeleteRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassCategoryDeleteRequest {
    @NotNull(message = "关联ID不能为空")
    private Integer id;
}
```

- [ ] **步骤 15：创建 SyncClassesRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SyncClassesRequest {
    @NotEmpty(message = "data不能为空")
    private List<Map<String, Object>> data;
}
```

- [ ] **步骤 16：创建 SyncCategoriesRequest.java**

```java
package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SyncCategoriesRequest {
    @NotEmpty(message = "data不能为空")
    private List<Map<String, Object>> data;
}
```

- [ ] **步骤 17：创建 SyncConfigRequest.java**

```java
package com.enroll.server.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SyncConfigRequest {
    private List<Map<String, Object>> data;
}
```

- [ ] **步骤 18：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/dto/request/
git commit -m "feat: 新建管理端Request DTO（Class/Notice/Batch/Category/Sync全套）"
```

---

### 任务 8：新建 SysConfigService + DateUtils

**文件：**
- 创建：`enroll-server/src/main/java/com/enroll/server/service/SysConfigService.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/util/DateUtils.java`

- [ ] **步骤 1：创建 SysConfigService.java**

```java
package com.enroll.server.service;

import com.enroll.server.entity.SysConfig;
import com.enroll.server.repository.SysConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SysConfigService {

    private final SysConfigRepository sysConfigRepo;

    public SysConfigService(SysConfigRepository sysConfigRepo) {
        this.sysConfigRepo = sysConfigRepo;
    }

    /** 获取报名须知（合并 PublicConfigController + AdminController 的重复代码） */
    public Map<String, String> getNotice() {
        return sysConfigRepo.findById(1)
                .map(cfg -> Map.of(
                    "title", cfg.getTitle() != null ? cfg.getTitle() : ",
                    "conditions", cfg.getConditions() != null ? cfg.getConditions() : ",
                    "notices", cfg.getNotices() != null ? cfg.getNotices() : ",
                    "contactInfo", cfg.getContactInfo() != null ? cfg.getContactInfo() : "
                ))
                .orElse(Map.of("title", ", "conditions", ", "notices", ", "contactInfo", "));
    }

    /** 更新报名须知 */
    @Transactional
    public void updateNotice(String title, String conditions, String notices, String contactInfo, String updatedBy) {
        SysConfig cfg = sysConfigRepo.findById(1).orElse(new SysConfig());
        cfg.setTitle(title);
        cfg.setConditions(conditions);
        cfg.setNotices(notices);
        cfg.setContactInfo(contactInfo);
        cfg.setUpdatedBy(updatedBy);
        cfg.setUpdatedAt(LocalDateTime.now());
        sysConfigRepo.save(cfg);
    }
}
```

- [ ] **步骤 2：创建 DateUtils.java**

```java
package com.enroll.server.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private DateUtils() {}

    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static final DateTimeFormatter ISO_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 解析日期时间字符串，支持多种格式
     * @param s 日期字符串
     * @param isStart true=纯日期补00:00，false=纯日期补23:59:59
     */
    public static LocalDateTime parseDateTime(String s, boolean isStart) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        // 尝试 yyyy/MM/dd HH:mm
        try { return LocalDateTime.parse(s, DATETIME_FMT); } catch (Exception ignored) {}
        // 尝试 yyyy-MM-dd HH:mm:ss
        try { return LocalDateTime.parse(s, ISO_DATETIME_FMT); } catch (Exception ignored) {}
        // 尝试 yyyy/MM/dd
        try {
            LocalDate d = LocalDate.parse(s, DATE_FMT);
            return isStart ? d.atStartOfDay() : d.atTime(23, 59, 59);
        } catch (Exception ignored) {}
        // 尝试 yyyy-MM-dd
        try {
            LocalDate d = LocalDate.parse(s, ISO_DATE_FMT);
            return isStart ? d.atStartOfDay() : d.atTime(23, 59, 59);
        } catch (Exception ignored) {}
        return null;
    }
}
```

- [ ] **步骤 3：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/service/SysConfigService.java enroll-server/src/main/java/com/enroll/server/util/DateUtils.java
git commit -m "feat: 新建SysConfigService + DateUtils（合并重复逻辑）"
```

---

### 任务 9：JwtUtil 新增 getUserIdFromToken 方法

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/security/JwtUtil.java`

- [ ] **步骤 1：在 JwtUtil.java 的 parse() 方法后添加**

```java
/**
 * 从 Authorization header 解析学生手机号
 * @param authHeader "Bearer xxx" 格式的 header
 * @return 手机号，解析失败返回 null
 */
public String getStudentPhoneFromAuthHeader(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
    try {
        Claims claims = parse(authHeader.substring(7));
        String role = String.valueOf(claims.get("role"));
        if (!"student".equals(role)) return null;
        return claims.getSubject();
    } catch (Exception e) {
        return null;
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/security/JwtUtil.java
git commit -m "feat: JwtUtil新增getStudentPhoneFromAuthHeader方法"
```

---

### 任务 10：新建 SyncService（SyncController 业务下沉）

**文件：**
- 创建：`enroll-server/src/main/java/com/enroll/server/service/SyncService.java`

- [ ] **步骤 1：创建 SyncService.java**

将 SyncController 中 syncClasses()、syncCategories()、syncConfig() 的业务逻辑搬过来，同时：
- `@Transactional` 移到 Service 方法
- `ObjectMapper` 改用 Spring 注入
- `parseDt()` 改调 `DateUtils.parseDateTime()`
- `EntityManager` 保留（truncate 操作需要）

```java
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
```

- [ ] **步骤 2：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/service/SyncService.java
git commit -m "feat: 新建SyncService（SyncController业务下沉）"
```

---

### 任务 11：拆分 AdminController → 3 个新 Controller

**文件：**
- 创建：`enroll-server/src/main/java/com/enroll/server/controller/AdminClassController.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/controller/AdminApplicationController.java`
- 创建：`enroll-server/src/main/java/com/enroll/server/controller/AdminNoticeController.java`
- 删除：`enroll-server/src/main/java/com/enroll/server/controller/AdminController.java`

- [ ] **步骤 1：创建 AdminClassController.java**

从 AdminController 搬出班级相关方法（listClasses, createClass, updateClass, updatePeriod, updateQuota, deleteClass, listClassCategories, addClassCategory, deleteClassCategory），改用 Request DTO + 走 Service：

```java
package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.request.*;
import com.enroll.server.service.ClassService;
import com.enroll.server.service.SysConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminClassController {

    private final ClassService classService;
    private final SysConfigService sysConfigService;

    public AdminClassController(ClassService classService, SysConfigService sysConfigService) {
        this.classService = classService;
        this.sysConfigService = sysConfigService;
    }

    @GetMapping("/classes")
    public Map<String, Object> listClasses() {
        return R.ok(classService.listAllForAdmin());
    }

    @PostMapping("/classes")
    public Map<String, Object> createClass(@RequestBody @Valid ClassCreateRequest req) {
        // ClassCreateRequest → Map 转换（ClassService.createClass 仍接受 Map，后续可改）
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", req.getName());
        body.put("quota", req.getQuota());
        body.put("description", req.getDescription());
        body.put("classRounds", req.getClassRounds());
        body.put("categoryNames", req.getCategoryNames());
        return R.ok("创建成功", classService.createClass(body));
    }

    @PostMapping("/classes/update")
    public Map<String, Object> updateClass(@RequestBody @Valid ClassUpdateRequest req) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("id", req.getId());
        if (req.getName() != null) body.put("name", req.getName());
        if (req.getQuota() != null) body.put("quota", req.getQuota());
        if (req.getDescription() != null) body.put("description", req.getDescription());
        if (req.getIsDeleted() != null) body.put("isDeleted", req.getIsDeleted());
        if (req.getGroupInfo() != null) body.put("groupInfo", req.getGroupInfo());
        if (req.getClassRounds() != null) body.put("classRounds", req.getClassRounds());
        if (req.getCategoryNames() != null) body.put("categoryNames", req.getCategoryNames());
        return R.ok("更新成功", classService.updateClass(req.getId(), body));
    }

    @PostMapping("/classes/update-period")
    public Map<String, Object> updatePeriod(@RequestBody @Valid PeriodUpdateRequest req) {
        return R.ok("修改成功", classService.updatePeriod(req.getId(), req.getPeriod()));
    }

    @PostMapping("/classes/update-quota")
    public Map<String, Object> updateQuota(@RequestBody @Valid QuotaUpdateRequest req) {
        return R.ok("修改成功", classService.updateQuota(req.getId(), req.getQuota()));
    }

    @PostMapping("/classes/delete")
    public Map<String, Object> deleteClass(@RequestBody @Valid IdRequest req) {
        classService.deleteClass(req.getId());
        return R.ok("已删除", null);
    }

    @GetMapping("/classCategory")
    public Map<String, Object> listClassCategories() {
        return R.ok(classService.listClassCategories());
    }

    @PostMapping("/classCategory")
    public Map<String, Object> addClassCategory(@RequestBody @Valid ClassCategoryRequest req) {
        return classService.addClassCategory(req.getClassId(), req.getCategoryId());
    }

    @PostMapping("/classCategory/delete")
    public Map<String, Object> deleteClassCategory(@RequestBody @Valid ClassCategoryDeleteRequest req) {
        return classService.deleteClassCategory(req.getId());
    }
}
```

> 注意：`listClassCategories`、`addClassCategory`、`deleteClassCategory` 原来在 AdminController 中直接操作 ClassCategoryRepository，现在需要搬入 ClassService。见任务 12。

- [ ] **步骤 2：创建 AdminApplicationController.java**

```java
package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.request.*;
import com.enroll.server.service.ApplicationService;
import com.enroll.server.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApplicationController {

    private final ApplicationService applicationService;
    private final ClassService classService;

    public AdminApplicationController(ApplicationService applicationService, ClassService classService) {
        this.applicationService = applicationService;
        this.classService = classService;
    }

    @GetMapping("/applications/all")
    public Map<String, Object> listAllApplications() {
        return R.ok(applicationService.findAllForSync());
    }

    @GetMapping("/applications/{id}")
    public Map<String, Object> getApplication(@PathVariable Integer id) {
        return applicationService.findByIdForAdmin(id);
    }

    @PostMapping("/applications/admit")
    public Map<String, Object> batchAdmit(@RequestBody @Valid BatchAdmitRequest req) {
        if (req.getIds().isEmpty()) return R.ok("没有需要录取的记录", null);
        applicationService.batchAdmit(req.getIds(), req.getAuditComment());
        return R.ok("已录取 " + req.getIds().size() + " 名学生", null);
    }

    @PostMapping("/applications/reject")
    public Map<String, Object> batchReject(@RequestBody @Valid BatchRejectRequest req) {
        if (req.getIds().isEmpty()) return R.ok("没有需要驳回的记录", null);
        applicationService.batchReject(req.getIds(), req.getAuditComment());
        return R.ok("已设置 " + req.getIds().size() + " 名学生为未录取", null);
    }

    @PostMapping("/applications/delete")
    public Map<String, Object> batchDelete(@RequestBody @Valid BatchDeleteRequest req) {
        if (req.getIds().isEmpty()) return R.ok("没有需要删除的记录", null);
        applicationService.batchUpdateStatus(req.getIds(), ApplicationService.STATUS_WITHDRAWN);
        return R.ok("已删除 " + req.getIds().size() + " 条记录", null);
    }

    @PostMapping("/applications/clear")
    public Map<String, Object> clearClass(@RequestBody @Valid ClearClassRequest req) {
        applicationService.clearClass(req.getClassId());
        return R.ok("已清空该班所有报名记录", null);
    }
}
```

- [ ] **步骤 3：创建 AdminNoticeController.java**

```java
package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.request.NoticeUpdateRequest;
import com.enroll.server.service.SysConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminNoticeController {

    private final SysConfigService sysConfigService;

    public AdminNoticeController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @GetMapping("/notice")
    public Map<String, Object> getNotice() {
        return R.ok(sysConfigService.getNotice());
    }

    @PostMapping("/notice/update")
    public Map<String, Object> updateNotice(@RequestBody @Valid NoticeUpdateRequest req) {
        sysConfigService.updateNotice(
            req.getTitle(), req.getConditions(), req.getNotices(),
            req.getContactInfo(), req.getUpdatedBy());
        return R.ok("保存成功", null);
    }
}
```

- [ ] **步骤 4：删除 AdminController.java**

```bash
git rm enroll-server/src/main/java/com/enroll/server/controller/AdminController.java
```

- [ ] **步骤 5：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/
git commit -m "refactor: AdminController拆分为AdminClassController+AdminApplicationController+AdminNoticeController"
```

---

### 任务 12：ClassService 补充搬入的方法

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/service/ClassService.java`

- [ ] **步骤 1：在 ClassService 中添加从 AdminController 搬来的方法**

```java
// 在 ClassService 末尾添加：

/** 获取全部班级-类别关联（从 AdminController 搬入） */
public java.util.List<com.enroll.server.entity.ClassCategory> listClassCategories() {
    return classCatRepo.findAll();
}

/** 新增班级-类别关联（从 AdminController 搬入，含查重） */
public Map<String, Object> addClassCategory(Integer classId, Integer categoryId) {
    if (classId == null || categoryId == null) {
        return R.fail(ResultCode.PARAM_INVALID, "classId 和 categoryId 不能为空");
    }
    java.util.List<com.enroll.server.entity.ClassCategory> existing = classCatRepo.findByClassId(classId);
    boolean alreadyExists = existing.stream()
            .anyMatch(cc -> cc.getCategoryId().equals(categoryId));
    if (alreadyExists) {
        return R.ok("关联已存在，无需重复创建", null);
    }
    com.enroll.server.entity.ClassCategory cc = new com.enroll.server.entity.ClassCategory();
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
```

需要在 ClassService 顶部添加 import：
```java
import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import java.util.Map;
```

- [ ] **步骤 2：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/service/ClassService.java
git commit -m "refactor: ClassService补充addClassCategory/deleteClassCategory/listClassCategories方法"
```

---

### 任务 13：ApplicationService 补充 findByIdForAdmin 方法

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/service/ApplicationService.java`

- [ ] **步骤 1：在 ApplicationService 中添加 findByIdForAdmin**

```java
/** 管理端查单条报名详情（含 idCardRaw，从 AdminController 搬入） */
public Map<String, Object> findByIdForAdmin(Integer id) {
    return appRepo.findById(id)
            .map(app -> {
                String className = classRepo.findById(app.getClassId())
                        .map(ClassInfo::getName)
                        .orElse(null);
                return R.ok(toDTO(app, className, null, null, true));
            })
            .orElse(R.fail(ResultCode.PARAM_INVALID, "报名记录不存在: id=" + id));
}
```

需要在 ApplicationService 顶部添加 import：
```java
import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
```

- [ ] **步骤 2：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/service/ApplicationService.java
git commit -m "refactor: ApplicationService补充findByIdForAdmin方法（含idCardRaw）"
```

---

### 任务 14：瘦化 SyncController

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/controller/SyncController.java`

- [ ] **步骤 1：重写 SyncController，只调 SyncService**

```java
package com.enroll.server.controller;

import com.enroll.server.dto.request.SyncCategoriesRequest;
import com.enroll.server.dto.request.SyncClassesRequest;
import com.enroll.server.dto.request.SyncConfigRequest;
import com.enroll.server.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/classes")
    public Map<String, Object> syncClasses(@RequestBody @Valid SyncClassesRequest req) {
        return syncService.syncClasses(req.getData());
    }

    @PostMapping("/categories")
    public Map<String, Object> syncCategories(@RequestBody @Valid SyncCategoriesRequest req) {
        return syncService.syncCategories(req.getData());
    }

    @PostMapping("/config")
    public Map<String, Object> syncConfig(@RequestBody @Valid SyncConfigRequest req) {
        return syncService.syncConfig(req.getData());
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/SyncController.java
git commit -m "refactor: SyncController瘦身，业务逻辑下沉到SyncService"
```

---

### 任务 15：改造 AuthController + PublicConfigController + ApplicationController + AdminCategoryController

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/controller/AuthController.java`
- 修改：`enroll-server/src/main/java/com/enroll/server/controller/PublicConfigController.java`
- 修改：`enroll-server/src/main/java/com/enroll/server/controller/ApplicationController.java`
- 修改：`enroll-server/src/main/java/com/enroll/server/controller/AdminCategoryController.java`

- [ ] **步骤 1：AuthController 改用 Request DTO + 移除 ApplicationRepository**

```java
package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.request.AdminLoginRequest;
import com.enroll.server.dto.request.SmsLoginRequest;
import com.enroll.server.dto.request.SmsSendRequest;
import com.enroll.server.service.AuthService;
import com.enroll.server.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    public AuthController(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid AdminLoginRequest req, HttpServletResponse response) {
        if (!adminUsername.equals(req.getUsername()) || !adminPassword.equals(req.getPassword())) {
            return R.fail(ResultCode.PARAM_INVALID, "账号或密码错误");
        }
        String token = jwtUtil.generateAdmin(req.getUsername());
        Cookie cookie = new Cookie("admin_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(86400);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
        return R.ok("登录成功", Map.of("token", token, "username", req.getUsername()));
    }

    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody @Valid SmsSendRequest req) {
        authService.sendCode(req.getPhone());
        return R.ok("验证码已发送", null);
    }

    @PostMapping("/login/sms")
    public Map<String, Object> loginSms(@RequestBody @Valid SmsLoginRequest req, HttpServletResponse response) {
        String token = authService.verifyCodeAndLogin(req.getPhone(), req.getCode());
        Cookie cookie = new Cookie("student_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(86400);
        response.addCookie(cookie);
        // 查报名状态走 AuthService
        return authService.getLoginResponse(req.getPhone(), token);
    }
}
```

- [ ] **步骤 2：AuthService 新增 getLoginResponse 方法**

在 AuthService.java 中添加：

```java
/** 登录后查报名状态，构造响应（从 AuthController 搬入） */
public Map<String, Object> getLoginResponse(String phone, String token) {
    List<com.enroll.server.entity.Application> apps = appRepo.findByPhoneAndStatusIn(phone, List.of(1, 2, 3, 4));
    if (apps.isEmpty()) {
        return R.ok("登录成功", Map.of("token", token, "phone", phone,
                "hasRegistration", false, "status", 0));
    }
    com.enroll.server.entity.Application app = apps.get(0);
    return R.ok("登录成功", Map.of(
            "token", token,
            "phone", phone,
            "hasRegistration", true,
            "status", app.getStatus(),
            "classId", app.getClassId(),
            "applyTime", app.getApplyTime() != null ? app.getApplyTime().toString() : null
    ));
}
```

需要在 AuthService 中注入 ApplicationRepository：
```java
private final com.enroll.server.repository.ApplicationRepository appRepo;

public AuthService(StringRedisTemplate redis, JwtUtil jwtUtil,
                   com.enroll.server.repository.ApplicationRepository appRepo) {
    this.redis = redis;
    this.jwtUtil = jwtUtil;
    this.appRepo = appRepo;
}
```

- [ ] **步骤 3：PublicConfigController 改用 SysConfigService**

```java
package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.service.SysConfigService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    private final SysConfigService sysConfigService;

    public PublicConfigController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @GetMapping("/notice")
    public Map<String, Object> getNotice() {
        return R.ok(sysConfigService.getNotice());
    }
}
```

- [ ] **步骤 4：ApplicationController 改用 Request DTO + JWT 统一**

```java
package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.request.ApplicationSubmitRequest;
import com.enroll.server.dto.request.WithdrawRequest;
import com.enroll.server.dto.request.UpdateAppRequest;
import com.enroll.server.security.JwtUtil;
import com.enroll.server.service.ApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final JwtUtil jwtUtil;

    public ApplicationController(ApplicationService applicationService, JwtUtil jwtUtil) {
        this.applicationService = applicationService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public Map<String, Object> submit(@RequestBody @Valid ApplicationSubmitRequest req) {
        // Request DTO → Map 转换（ApplicationService.submit 仍接受 Map，后续可改）
        java.util.Map<String, Object> form = new java.util.HashMap<>();
        form.put("name", req.getName());
        form.put("idCard", req.getIdCard());
        form.put("phone", req.getPhone());
        form.put("gender", req.getGender());
        form.put("hasPhysics", req.getHasPhysics());
        form.put("hasEnglish", req.getHasEnglish());
        form.put("appliedCategory", req.getAppliedCategory());
        form.put("classId", req.getClassId());
        form.put("noticeAgreed", req.getNoticeAgreed());
        return R.ok("提交成功", applicationService.submit(form));
    }

    @GetMapping("/my")
    public Map<String, Object> myApplications(HttpServletRequest request) {
        String phone = jwtUtil.getStudentPhoneFromAuthHeader(request.getHeader("Authorization"));
        if (phone == null) return R.fail(com.enroll.server.dto.ResultCode.PARAM_INVALID, "请先登录");
        return R.ok(applicationService.findMyByPhone(phone));
    }

    @PostMapping("/withdraw")
    public Map<String, Object> withdraw(@RequestBody @Valid WithdrawRequest req) {
        applicationService.withdraw(req.getId());
        return R.ok("已撤回", null);
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody @Valid UpdateAppRequest req) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        if (req.getName() != null) body.put("name", req.getName());
        if (req.getPhone() != null) body.put("phone", req.getPhone());
        if (req.getIdCard() != null) body.put("idCard", req.getIdCard());
        if (req.getGender() != null) body.put("gender", req.getGender());
        if (req.getHasPhysics() != null) body.put("hasPhysics", req.getHasPhysics());
        if (req.getHasEnglish() != null) body.put("hasEnglish", req.getHasEnglish());
        applicationService.updateApp(req.getId(), body);
        return R.ok("修改成功", null);
    }

    @GetMapping("/me")
    public Map<String, Object> myApplicationsMe(HttpServletRequest request) {
        String phone = jwtUtil.getStudentPhoneFromAuthHeader(request.getHeader("Authorization"));
        if (phone == null) return R.fail(com.enroll.server.dto.ResultCode.PARAM_INVALID, "缺少有效的登录凭证，请重新登录");
        return R.ok(applicationService.findMyByPhone(phone));
    }

    @GetMapping("/check")
    public Map<String, Object> checkDuplicate(
            @RequestParam String phone,
            @RequestParam String idCard,
            @RequestParam Integer classId) {
        return R.ok(applicationService.checkDuplicate(phone, idCard, classId));
    }
}
```

- [ ] **步骤 5：AdminCategoryController 改用 Request DTO**

```java
package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.request.CategoryCreateRequest;
import com.enroll.server.dto.request.CategoryDeleteRequest;
import com.enroll.server.dto.request.CategoryUpdateRequest;
import com.enroll.server.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Map<String, Object> list() {
        return R.ok(categoryService.list());
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody @Valid CategoryCreateRequest req) {
        return R.ok(categoryService.create(req.getName()));
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody @Valid CategoryUpdateRequest req) {
        return R.ok(categoryService.update(req.getId(), req.getName()));
    }

    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestBody @Valid CategoryDeleteRequest req) {
        categoryService.delete(req.getId());
        return R.ok(null);
    }
}
```

- [ ] **步骤 6：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/AuthController.java enroll-server/src/main/java/com/enroll/server/controller/PublicConfigController.java enroll-server/src/main/java/com/enroll/server/controller/ApplicationController.java enroll-server/src/main/java/com/enroll/server/controller/AdminCategoryController.java enroll-server/src/main/java/com/enroll/server/service/AuthService.java
git commit -m "refactor: Controller改用RequestDTO+移除Repository直接注入+JWT解析统一"
```

---

### 任务 16：Phase 2 验证

- [ ] **步骤 1：编译检查**

```bash
cd enroll-server
mvnw.cmd compile -DskipTests
```

预期：BUILD SUCCESS

- [ ] **步骤 2：启动后端**

```bash
mvnw.cmd spring-boot:run -DskipTests
```

预期：启动成功

- [ ] **步骤 3：curl 测试报名提交**

```bash
curl -s -X POST "http://localhost:8081/api/applications" -H "Content-Type: application/json" --data-raw '{"name":"测试","idCard":"110101***REMOVED***4","gender":"男","phone":"***REMOVED***","classId":1}'
```

预期：返回成功

- [ ] **步骤 4：curl 测试管理员登录**

```bash
curl -s -X POST "http://localhost:8081/api/auth/login" -H "Content-Type: application/json" --data-raw '{"username":"***REMOVED***","password":"***REMOVED***"}'
```

预期：返回 token

- [ ] **步骤 5：curl 测试空必填字段校验**

```bash
curl -s -X POST "http://localhost:8081/api/applications" -H "Content-Type: application/json" --data-raw '{}'
```

预期：返回 400 校验错误

- [ ] **步骤 6：调用 /verification-before-completion**

---

Phase 3 和 Phase 4 的计划见 `docs/superpowers/plans/2026-08-05-architecture-cleanup-phase3-4.md`
