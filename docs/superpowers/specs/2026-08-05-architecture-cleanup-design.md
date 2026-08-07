# 特色班报名系统 · 架构整改设计文档

> 日期：2026-08-05
> 状态：已批准
> 分支策略：从 main 创建 `refactor/architecture-cleanup` 分支，逐 Phase 推进
> 验证铁律：每个 Phase 完成后必须调用 `/verification-before-completion` 技能，实际跑命令验证，不能光说"做完了"

---

## 一、背景

本项目在开发过程中"走一步写一步"，缺乏前期架构设计，导致 13 个架构级问题积累。本次整改目标是系统性修复这些问题，使代码具备可维护性。

### 13 个架构问题清单

**后端（6 个）**：
1. 胖 Controller（AdminController 335行15方法管4领域、SyncController 295行直接操作5 Repository+EntityManager）
2. 无 Request DTO（全部 Map<String, Object> 接收参数）
3. 事务在 Controller（@Transactional 写在 Controller 方法上）
4. 重复逻辑散落（JWT解析×2、IP提取×2、须知获取×2、日期解析×2）
5. 单机锁假分布式（ConcurrentHashMap + synchronized 防超报）
6. Repository 方法名爆炸（16+ 超长方法名）

**前端（5 个）**：
1. 无状态管理（4 组件各自读 localStorage + window.dispatchEvent）
2. 组件臃肿（7/11 组件超 300 行，最大 937 行）
3. 重复逻辑遍地（parsePeriodsArray×2、formatTime×2、onLogout×2、windowWidth×3、弹窗×2）
4. 无路由守卫（/my-applications 靠组件内部检查 auth）
5. API 层不统一（MyApplications 直接 fetch() 绕过 api.js）

**跨前后端（2 个）**：
1. 无接口契约层（后端无 Request DTO + 前端无 API 类型定义）
2. 仓库卫生差（构建产物、H2 数据库、截图、日志提交到 Git）

---

## 二、整改决策

| 决策点 | 选择 |
|--------|------|
| 整改节奏 | 逐 Phase 推进，每 Phase 做完验证再进下一个 |
| 安全策略 | Git 分支副本整改，原项目不动 |
| Request DTO | 全量加（每个 POST 接口建 Request DTO + @Valid） |
| Pinia | 这次加 |
| 组件拆分 | 适度拆分（只拆最明显的，不强制 200 行以下） |
| 死代码 | 全删 |

---

## 三、Phase 1：安全止血 + 仓库卫生（1 天）

### 3.1 .gitignore 补全

在项目根 `.gitignore` 追加：

```
# 运行时产物
*.log
enroll-server/data/
enroll-server/src/main/resources/static/

# 临时文件
tmp/
create_class.json
```

### 3.2 AuthController fail-open → fail-closed

将 `@Value` 注解的 fallback 默认值移除，环境变量未设时启动报错：

```java
// 改前（危险）
@Value("${admin.username:***REMOVED***}")
private String adminUsername;

// 改后（安全）
@Value("${admin.username}")
private String adminUsername;
```

涉及字段：
- `admin.username`
- `admin.password`
- `jwt.secret`

### 3.3 idCardRaw 权限控制

`ApplicationService.toDTO()` 方法加 `includeRaw` 参数：

- AdminController 调用时传 `true`（管理员需要看完整身份证号）
- ApplicationController 调用时传 `false`（学生端只返回 idCardMasked）

### 3.4 清理已提交的敏感文件

```bash
git rm --cached enroll-server/data/enroll_db.mv.db
git rm --cached -r enroll-server/src/main/resources/static/
git rm --cached -r tmp/
git rm --cached *.log
```

### Phase 1 验证清单

- [ ] `mvnw.cmd spring-boot:run` 启动成功（环境变量已设时）
- [ ] 不设环境变量时启动报错（fail-closed）
- [ ] 学生端 API 不返回 idCardRaw
- [ ] 管理员 API 返回 idCardRaw
- [ ] `git status` 不再追踪 .log / data/ / static/ / tmp/

---

## 四、Phase 2：后端分层归位（3-4 天）

### 4.1 新建 Request DTO（全量）

新建 `dto/request/` 包，为每个 POST 接口建 Request DTO：

| Request DTO | 替代接口 | 关键校验 |
|-------------|---------|---------|
| `ApplicationSubmitRequest` | ApplicationController.submit | @NotBlank name, @Pattern idCard, @Pattern phone, @NotNull classId |
| `ApplicationUpdateRequest` | ApplicationController.update | 同上，可选字段 |
| `AdminLoginRequest` | AuthController.login | @NotBlank username, @NotBlank password |
| `SmsLoginRequest` | AuthController.loginSms | @NotBlank phone, @NotBlank code |
| `SmsSendRequest` | AuthController.sendCode | @NotBlank phone |
| `ClassCreateRequest` | AdminController.createClass | @NotBlank name, quota, periods, categoryIds |
| `ClassUpdateRequest` | AdminController.updateClass | @NotNull id, name, quota, periods |
| `PeriodUpdateRequest` | AdminController.updatePeriod | @NotNull classId, periods |
| `QuotaUpdateRequest` | AdminController.updateQuota | @NotNull classId, @NotNull quota |
| `NoticeUpdateRequest` | AdminController.updateNotice | title, conditions, notices |
| `BatchAdmitRequest` | AdminController.batchAdmit | @NotEmpty ids |
| `BatchRejectRequest` | AdminController.batchReject | @NotEmpty ids |
| `BatchDeleteRequest` | AdminController.batchDelete | @NotEmpty ids |
| `ClearClassRequest` | AdminController.clearClass | @NotNull classId |
| `CategoryCreateRequest` | AdminCategoryController.create | @NotBlank name |
| `CategoryUpdateRequest` | AdminCategoryController.update | @NotNull id, @NotBlank name |
| `CategoryDeleteRequest` | AdminCategoryController.delete | @NotNull id |
| `SyncClassesRequest` | SyncController.syncClasses | @NotEmpty dataList |
| `SyncCategoriesRequest` | SyncController.syncCategories | @NotEmpty dataList |
| `SyncConfigRequest` | SyncController.syncConfig | title, conditions, notices |

Controller 方法签名改为：
```java
public R submit(@RequestBody @Valid ApplicationSubmitRequest req)
```

### 4.2 AdminController 拆分

| 新 Controller | 职责 | 方法 | 预估行数 |
|--------------|------|------|---------|
| `AdminClassController` | 班级 CRUD + 配额 + 时间段 | listClasses, createClass, updateClass, updatePeriod, updateQuota, deleteClass | ~120 |
| `AdminApplicationController` | 报名查询 + 批量操作 + 清空 | listAllApplications, getApplication, batchAdmit, batchReject, batchDelete, clearClass | ~130 |
| `AdminNoticeController` | 报名须知读写 | getNotice, updateNotice | ~40 |

> **注意**：`addClassCategory` / `deleteClassCategory` 已在独立的 `AdminCategoryController` 中，不归入上述任何 Controller。

路径不变，只是代码物理位置拆开。`AdminCategoryController` 已独立，不动。

### 4.3 SyncController 业务下沉 → SyncService

- 新建 `SyncService`，把 `syncClasses()`、`syncCategories()`、`syncConfig()` 的业务逻辑全部搬过去
- SyncController 只做：接收 Request → 调 SyncService → 返回结果
- `@Transactional` 从 Controller 移到 Service
- `ObjectMapper` 改用 Spring 注入的 Bean
- `parseDt()` 和 ClassService 的 `parseDateTime()` 合并到 `DateUtils`

### 4.4 Controller 里的 Repository 注入全部移除

| Controller | 移除的 Repository | 改走 Service |
|-----------|------------------|-------------|
| AuthController | ApplicationRepository | → AuthService |
| PublicConfigController | SysConfigRepository | → 新建 SysConfigService |
| AdminClassController | SysConfigRepository, ClassCategoryRepository | → SysConfigService, ClassService |
| AdminApplicationController | ApplicationRepository | → ApplicationService |

**新建 `SysConfigService`**：
- `getNotice()` — 从 PublicConfigController 和 AdminController 的重复代码合并
- `updateNotice()` — 从 AdminController 搬过来

### 4.5 @Transactional 归位到 Service

从 Controller 方法上移除所有 `@Transactional`，确保写操作的事务边界在 Service 层。

### 4.6 JWT 解析逻辑统一

提取到 `JwtUtil.getUserIdFromToken(String token)` 方法，ApplicationController 的 `/my` 和 `/me` 各自只调一行。

### Phase 2 验证清单

- [ ] 所有 POST 接口用 Request DTO 接收参数
- [ ] 传空必填字段返回 400 校验错误
- [ ] AdminController 拆为 3 个 Controller，路径不变
- [ ] SyncController 只调 SyncService，不直接操作 Repository
- [ ] 所有 Controller 不再直接注入 Repository
- [ ] @Transactional 只出现在 Service 方法上
- [ ] JWT 解析只调 JwtUtil 一行
- [ ] `mvnw.cmd spring-boot:run` 启动成功
- [ ] curl 测试报名提交成功
- [ ] curl 测试管理员登录成功

---

## 五、Phase 3：前端治理（3-4 天）

### 5.1 引入 Pinia + Auth Store

**新建**：`src/stores/auth.js`

```js
// state
token, phone, isAdmin

// actions
login(token, phone)    → 更新 state + localStorage
logout()               → 清空 state + localStorage
checkAuth()            → 启动时从 localStorage 恢复

// getters
isLoggedIn
```

**改造 4 个组件**：
- AppHeader → `useAuthStore().isLoggedIn`
- HomePage → 同上
- StudentLogin → `authStore.login(token, phone)`
- MyApplications → `authStore.logout()`

**删除**：所有 `window.dispatchEvent('login_changed')` 和对应的 `addEventListener`

### 5.2 路由守卫

```js
router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.path === '/my-applications' && !auth.isLoggedIn) {
    next('/student-login')
  } else {
    next()
  }
})
```

### 5.3 组件适度拆分

| 原组件 | 拆出 | 预估行数变化 |
|--------|------|-------------|
| HomePage.vue (937) | → NoticeDialog.vue + ClassTable.vue | 937 → ~500 + 150 + 120 |
| ClassCard.vue (745) | → ClassDescDialog.vue（共用） | 745 → ~400 + 200 |
| FormPage.vue (779) | → 复用 ClassDescDialog.vue | 779 → ~550 |
| MyApplications.vue (648) | → EditDialog.vue | 648 → ~450 + 100 |

**不拆的**：AppFooter（665 行主要是 CSS）、HeroBanner（545 行轮播逻辑合理）、FilterBar（501 行可接受）

### 5.4 重复逻辑统一

| 重复内容 | 统一到 | 涉及组件 |
|---------|--------|---------|
| parsePeriodsArray | utils/data.js | HomePage, FilterBar |
| formatTime（本地版）| 删除本地版，用 data.js | MyApplications |
| onLogout | stores/auth.js 的 logout() | AppHeader, MyApplications |
| windowWidth resize | composables/useWindowWidth.js | HomePage, ClassCard, FormPage |
| 直接 fetch() | utils/api.js | MyApplications |
| statusLabel/statusTagType | utils/data.js | MyApplications, ClassCard |

### 5.5 死代码删除

| 删除项 | 位置 |
|--------|------|
| ApiResponse.java | dto/ |
| useApplication.js | composables/ |
| PieChart.vue | components/ |
| TrendChart.vue | components/ |
| playwright 依赖 | package.json |
| mammoth 依赖 | package.json |
| xlsx 依赖 | package.json |
| data.js 中的 mock 数据导出 | utils/data.js |

### 5.6 API 层统一

- `api.js` 的 `request()` 函数加自动 token 注入（从 Pinia auth store 读取，不直接读 localStorage）
- MyApplications 的直接 `fetch()` 改用 `api.js`
- 删除 `data.js` 中 `fetchClasses` 的 mock fallback

### Phase 3 验证清单

- [ ] `npm run dev` 启动成功
- [ ] 学生登录 → 首页显示登录状态 → 退出登录状态消失
- [ ] 未登录访问 /my-applications → 跳转到 /student-login
- [ ] 首页班级列表正常显示
- [ ] 报名表单提交成功
- [ ] 我的报名页面正常显示
- [ ] 班级介绍弹窗正常弹出（ClassCard 和 FormPage 共用）
- [ ] `npm run build` 构建成功

---

## 六、Phase 4：技术债清理（2-3 天）

### 6.1 后端技术债

| 项目 | 改法 |
|------|------|
| ObjectMapper 手动 new | SyncController + ApplicationService 改用 Spring 注入的 ObjectMapper Bean |
| TimeController 内存限流器 | cleanExpired() 加 @Scheduled(fixedRate = 60000) 定时清理 |
| Category 实体死映射 | 删除 @ManyToMany classes 字段 |
| AdminController O(n) 找班级名 | 改为 classInfoRepository.findById(classId).getName() |
| Repository 方法名过长 | ApplicationRepository 引入 JpaSpecificationExecutor |
| getClientIp() 重复 | 统一到 RequestUtils.getClientIp() |
| 全局日志 OFF | application-prod.yml 改 root: WARN, com.enroll.server: INFO |
| ddl-auto: update | application-prod.yml 改 validate |
| GlobalExceptionHandler 泄露内部信息 | catch-all Exception 返回固定消息 |

### 6.2 前端技术债

| 项目 | 改法 |
|------|------|
| ::v-deep() 废弃语法 | FilterBar 改 :deep() |
| 非 scoped style 块 | 改用 Element Plus CSS 变量覆盖 |
| !important 泛滥 | 逐步替换为更具体选择器或 :deep() |
| 内联 style 属性 | FilterBar el-select 改 class |
| isChengDian 硬编码 classId | 改为后端字段（ClassDTO 加 isMultiRound） |
| 轮询无退避 | setInterval 改 visibilitychange + 指数退避 |

### 6.3 补测试

| 测试类型 | 范围 |
|---------|------|
| Service 单元测试 | ApplicationService.submit()、ClassService.createClass()、SyncService.syncClasses() |
| Request DTO 校验测试 | 每个 DTO 的 @Valid 注解是否生效 |
| 前端 composable 测试 | useWindowWidth、useAuthStore |
| 前端 utils 测试 | api.js 的 request() 测试 |

### Phase 4 验证清单

- [ ] 所有 Service 单元测试通过
- [ ] Request DTO 校验测试通过
- [ ] 前端 composable 测试通过
- [ ] `mvnw.cmd spring-boot:run` 启动成功
- [ ] `npm run dev` 启动成功
- [ ] 全流程冒烟测试通过

---

## 七、风险点

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| Phase 2 改动量大，可能引入 bug | 🟠 高 | 每个 Controller 拆分后立即 curl 测试 |
| Request DTO 全量加可能漏字段 | 🟡 中 | 对照现有 Map 的 key 逐一检查 |
| Pinia 引入后 localStorage 迁移 | 🟡 中 | auth store 的 checkAuth() 从 localStorage 恢复，兼容旧数据 |
| 组件拆分可能破坏 props/events 传递 | 🟡 中 | 拆分后逐页面刷新验证 |
| SyncController 下沉可能影响低代码平台对接 | 🟠 高 | 路径不变，只改内部实现，低代码平台无感知 |

## 八、不在本次整改范围

- 单机锁改分布式锁（需要 Redis 分布式锁方案，单独设计）
- 前端 API 类型定义（TypeScript 迁移，成本太高）
- 身份证号加密存储（需要加解密方案设计，影响查询性能）
