# [手机号身份证直填报名] 实现计划

> **目标：** 学生点报名卡片后，无需等验证码，直接填手机号+身份证进表单；输入后立即调后端查重（手机号全局唯一/身份证全局唯一/同班级唯一），三类冲突分别提示。

> **架构：** 前端表单页去掉 Step 1 验证码页，改为单页；顶部手机号+身份证输入 → 失焦调 `GET /api/applications/check` → 有冲突则锁定表单、无冲突则正常填表提交。后端新增 `ApplicationService.checkDuplicate()` 复用现有三重复检逻辑。

> **技术栈：** Spring Boot（JPA）/ Vue 3 + Element Plus / Fetch

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `ApplicationController.java` | 新增 `GET /api/applications/check?phone=&idCard=&classId=` |
| `ApplicationService.java` | 新增 `checkDuplicate(phone, idCard, classId)` 方法 |
| `api.js` | 新增 `checkDuplicateAPI(phone, idCard, classId)` |
| `FormPage.vue` | 去掉 Step 1，改单页；顶部加手机号+身份证输入框；失焦调 check |
| `data.js` | `initialForm()` 加 `phone` 字段 |

---

## 任务 1：后端新增查重接口

**文件：**
- 修改：`enroll-server/src/main/java/com/enroll/server/controller/ApplicationController.java`
- 修改：`enroll-server/src/main/java/com/enroll/server/service/ApplicationService.java`

### 步骤

- [ ] **步骤 1：在 `ApplicationService.java` 新增 `checkDuplicate()` 方法**

在 `ApplicationService.java` 类的末尾（现有方法之后）新增：

```java
/**
 * 查重接口（给表单页实时查询用）
 * 返回结构：
 *   { hasPhoneConflict: bool, phoneClassName: string,
 *     hasIdCardConflict: bool, idCardClassName: string,
 *     hasSameClassConflict: bool }
 * 三种冲突分别提示，不抛异常。
 */
public Map<String, Object> checkDuplicate(String phone, String idCard, Integer classId) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("hasPhoneConflict", false);
    result.put("phoneClassName", null);
    result.put("hasIdCardConflict", false);
    result.put("idCardClassName", null);
    result.put("hasSameClassConflict", false);

    // 1) 手机号全局唯一检查
    List<Application> phoneDup = appRepo.findByPhoneAndStatusInAndIsDeleted(
            phone, List.of(STATUS_APPLIED, STATUS_ENROLLED), 0);
    if (!phoneDup.isEmpty()) {
        Application existing = phoneDup.get(0);
        String className = classRepo.findById(existing.getClassId())
                .map(ClassInfo::getName).orElse("未知班级");
        result.put("hasPhoneConflict", true);
        result.put("phoneClassName", className);
    }

    // 2) 身份证全局唯一检查
    List<Application> idCardDup = appRepo.findByIdCardAndStatusInAndIsDeleted(
            idCard, List.of(STATUS_APPLIED, STATUS_ENROLLED), 0);
    if (!idCardDup.isEmpty()) {
        Application existing = idCardDup.get(0);
        String className = classRepo.findById(existing.getClassId())
                .map(ClassInfo::getName).orElse("未知班级");
        result.put("hasIdCardConflict", true);
        result.put("idCardClassName", className);
    }

    // 3) 同班级防重（同一身份证+同一班级）
    List<Application> sameClassDup = appRepo.findByIdCardAndClassIdAndStatusInAndIsDeleted(
            idCard, classId, List.of(STATUS_APPLIED, STATUS_ENROLLED, STATUS_REJECTED), 0);
    if (!sameClassDup.isEmpty()) {
        result.put("hasSameClassConflict", true);
    }

    return result;
}
```

- [ ] **步骤 2：在 `ApplicationController.java` 新增 check 接口**

在 `@GetMapping("/my")` 方法后面新增：

```java
/**
 * 报名查重接口（GET，查询不写数据库）
 * 前端表单页输入手机号+身份证后失焦调用
 * GET /api/applications/check?phone=&idCard=&classId=
 */
@GetMapping("/check")
public Map<String, Object> checkDuplicate(
        @RequestParam String phone,
        @RequestParam String idCard,
        @RequestParam Integer classId) {
    if (phone == null || idCard == null || classId == null) {
        return R.fail(ResultCode.PARAM_INVALID, "参数不完整");
    }
    return R.ok(applicationService.checkDuplicate(phone, idCard, classId));
}
```

- [ ] **步骤 3：验证后端接口**

```bash
curl -s "http://localhost:8081/api/applications/check?phone=***REMOVED***&idCard=110101***REMOVED***4&classId=1"
```

预期返回：
```json
{"code":200,"data":{"hasPhoneConflict":false,"phoneClassName":null,"hasIdCardConflict":false,"idCardClassName":null,"hasSameClassConflict":false}}
```

- [ ] **步骤 4：Commit**

```bash
git add enroll-server/src/main/java/com/enroll/server/controller/ApplicationController.java \
        enroll-server/src/main/java/com/enroll/server/service/ApplicationService.java
git commit -m "feat: 新增 GET /api/applications/check 报名查重接口"
```

---

## 任务 2：前端新增 API 调用

**文件：**
- 修改：`enroll-web/src/utils/api.js`

### 步骤

- [ ] **步骤 1：在 `api.js` 末尾新增 checkDuplicateAPI**

在 `fetchNotice` 函数后面新增：

```javascript
/**
 * 报名查重接口（GET）
 * @param {string} phone - 手机号
 * @param {string} idCard - 身份证号
 * @param {number} classId - 班级ID
 * @returns {Promise<{hasPhoneConflict, phoneClassName, hasIdCardConflict, idCardClassName, hasSameClassConflict}>}
 */
export const checkDuplicateAPI = async (phone, idCard, classId) => {
  const res = await request(
    `/api/applications/check?phone=${encodeURIComponent(phone)}&idCard=${encodeURIComponent(idCard)}&classId=${classId}`
  )
  return res.code === 200 ? res.data : null
}
```

- [ ] **步骤 2：Commit**

```bash
git add enroll-web/src/utils/api.js
git commit -m "feat: 新增 checkDuplicateAPI 查重接口封装"
```

---

## 任务 3：表单页重构（单页 + 顶部手机号+身份证）

**文件：**
- 修改：`enroll-web/src/views/FormPage.vue`
- 修改：`enroll-web/src/utils/data.js`

### 步骤

- [ ] **步骤 1：修改 `data.js` 的 `initialForm()` — 加 phone 字段**

`initialForm()` 改为：

```javascript
export const initialForm = () => ({
  name: '',           // 姓名
  phone: '',          // 手机号（新增，之前靠 token 存储）
  idCard: '',         // 身份证号
  gender: '',         // 性别
  hasPhysics: '',     // 是否选考物理
  hasEnglish: '',     // 是否选考英语
  classId: null,      // 所选班级 ID
  appliedCategory: '', // 班级类别
})
```

- [ ] **步骤 2：修改 FormPage.vue — 去掉 Step 1，改为单页**

改动要点：
1. 删除步骤指示器（`.step-indicator`）
2. 删除 Step 1 的 `el-card`（手机验证码页）
3. 在 `form-card` 顶部、班级信息行之前，插入**手机号+身份证输入行**（并排，移动端堆叠）
4. 新增冲突提示 `el-alert`
5. 去掉 `usePhoneCode` composable 引用
6. 新增 `formLocked`、`duplicateAlert`、`phoneError`、`idCardError` 响应式状态
7. `onIdentityBlur()` 函数：手机号/身份证失焦时调 `checkDuplicateAPI`，有冲突则锁定表单
8. `goBack()` 简化为直接返回首页
9. `submit` 按钮加 `:disabled="formLocked || submitting"`

关键代码段：

**template 新增（身份输入行，在 `class-desc-row` 之前）：**
```html
<!-- ===== 手机号 + 身份证输入行 ===== -->
<el-row :gutter="16" class="form-row identity-row">
  <el-col :xs="24" :sm="12">
    <el-form-item label="手机号" prop="phone" :error="phoneError">
      <el-input
        v-model="form.phone"
        placeholder="请输入11位手机号"
        maxlength="11"
        @blur="onIdentityBlur"
        :disabled="formLocked"
      >
        <template #prefix>
          <span class="phone-prefix">+86</span>
        </template>
      </el-input>
    </el-form-item>
  </el-col>
  <el-col :xs="24" :sm="12">
    <el-form-item label="身份证号" prop="idCard" :error="idCardError">
      <el-input
        v-model="form.idCard"
        placeholder="请输入18位身份证号"
        maxlength="18"
        :suffix-icon="idCardValid ? SuccessFilled : undefined"
        @blur="onIdentityBlur"
        :disabled="formLocked"
      />
    </el-form-item>
  </el-col>
</el-row>

<!-- 冲突提示 -->
<el-alert
  v-if="duplicateAlert"
  :title="duplicateAlert"
  type="error"
  show-icon
  :closable="false"
  style="margin-bottom: 12px;"
/>
```

**script setup 修改：**
- 删除 `usePhoneCode` 引入
- 删除 `step`, `phoneCode` 相关所有代码
- 新增 `formLocked = ref(false)`, `duplicateAlert = ref('')`, `phoneError = ref('')`, `idCardError = ref('')`
- 新增 `onIdentityBlur` 函数：

```javascript
async function onIdentityBlur() {
  const ph = form.phone.trim()
  const ic = form.idCard.trim()
  if (!ph || !/^1[3-9]\d{9}$/.test(ph)) return
  if (!ic || ic.length !== 18) return
  try {
    const res = await checkDuplicateAPI(ph, ic, form.classId)
    if (!res) return
    const msgs = []
    if (res.hasPhoneConflict) msgs.push(`该手机号已报名【${res.phoneClassName}】`)
    if (res.hasIdCardConflict) msgs.push(`该身份证持有者已报名【${res.idCardClassName}】`)
    if (res.hasSameClassConflict) msgs.push('您已报名此班级，不能重复报名')
    if (msgs.length > 0) {
      duplicateAlert.value = msgs.join('；')
      formLocked.value = true
    } else {
      duplicateAlert.value = ''
      formLocked.value = false
    }
  } catch {
    duplicateAlert.value = ''
    formLocked.value = false
  }
}
```

- `submit-btn` 加 `:disabled="formLocked || submitting"`
- `goBack()` 改为 `router.push('/home')`

- [ ] **步骤 3：添加 CSS**

```css
.identity-row { margin-bottom: 8px; }
.phone-prefix { font-size: 14px; color: #94a3b8; padding: 0 4px; }
@media (max-width: 768px) {
  .identity-row .el-col { margin-bottom: 0; }
}
```

- [ ] **步骤 4：Commit**

```bash
git add enroll-web/src/utils/data.js enroll-web/src/views/FormPage.vue
git commit -m "refactor: 表单页去掉Step1验证码，改为手机号+身份证直填+实时查重"
```

---

## 任务 4：全流程验证

### 步骤

- [ ] **步骤 1：构建前端**

```powershell
cd ***REMOVED***RegistrationQuestionnaire/enroll-web && npm run build
```

- [ ] **步骤 2：重启后端**

```powershell
netstat -ano | findstr 8081
# 找到 PID 后
taskkill /F /PID <PID>
cd ***REMOVED***RegistrationQuestionnaire/enroll-server; mvnw.cmd spring-boot:run -DskipTests
```

- [ ] **步骤 3：测试场景**

1. **无冲突路径**：进表单 → 填手机号+身份证（无已报名记录）→ 失焦无提示 → 正常填表 → 提交成功
2. **手机号冲突**：用已报过名的手机号输入 → 提示"该手机号已报名【X班】" → 表单锁定
3. **身份证冲突**：用已报过名的身份证输入 → 提示"该身份证持有者已报名【X班】" → 表单锁定
4. **同班重复**：同一手机号+身份证报过此班后再次进入 → 提示"您已报名此班级，不能重复报名"
5. **移动端排版**：Chrome DevTools 切换到 mobile 视图，验证手机号+身份证两行纵向排列
