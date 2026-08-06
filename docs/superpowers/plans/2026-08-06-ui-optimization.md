# 前端 UI 优化（色条/去重/动画/消息提示）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 修复 MyApplications 卡片色条突出、咨询方式去重、移动端卡片入场动画、退出登录提示、全局按钮操作消息提示补全

**架构：** 5 个独立任务，每个任务修改 1-2 个文件，互不依赖可并行。动画使用 motion 库 Vue 版，仅移动端生效。

**技术栈：** Vue 3 + Element Plus + Vite + motion（新增）

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `enroll-web/package.json` | 修改 | 新增 motion 依赖 |
| `enroll-web/src/views/MyApplications.vue` | 修改 | 任务1：色条不突出 |
| `enroll-web/src/views/HomePage.vue` | 修改 | 任务2：咨询方式去重 |
| `enroll-web/src/components/ClassCard.vue` | 修改 | 任务3：移动端入场动画 |
| `enroll-web/src/components/AppHeader.vue` | 修改 | 任务4：退出登录提示 |
| `enroll-web/src/components/FilterBar.vue` | 修改 | 任务5：重置按钮提示 |
| `enroll-web/src/views/FormPage.vue` | 修改 | 任务5：班级介绍弹窗确认提示 |

---

### 任务 1：MyApplications 卡片色条两侧不突出

**文件：**
- 修改：`enroll-web/src/views/MyApplications.vue:455-461`

**现状：** `.record-card::before` 用 `margin: -16px -16px 0 -16px` 让色条超出卡片 padding，导致两侧突出。

**目标：** 色条完全贴齐卡片两侧，不突出。

- [ ] **步骤 1：修改色条 CSS**

将 `.record-card::before` 的 `margin` 从负值改为 `0`，让色条宽度等于卡片内容区宽度。同时去掉 `border-radius`（色条贴齐两侧后圆角不自然）。

```css
/* 修改前 */
.record-card::before {
  content: '';
  display: block;
  height: 3px;
  border-radius: 3px 3px 0 0;
  margin: -16px -16px 0 -16px;
}

/* 修改后 */
.record-card::before {
  content: '';
  display: block;
  height: 3px;
  border-radius: 0;
  margin: 0;
}
```

- [ ] **步骤 2：移动端同步调整**

移动端 `.record-card` 的 padding 是 14px，色条 `margin: 0` 已经不受 padding 影响，无需额外调整。确认移动端样式无冲突。

- [ ] **步骤 3：浏览器验证**

1. 打开 http://localhost:5173/my-applications
2. 确认卡片顶部色条两侧与卡片边缘齐平，不突出
3. 切换移动端视口（375px），确认同样齐平

- [ ] **步骤 4：Commit**

```bash
git add enroll-web/src/views/MyApplications.vue
git commit -m "fix: MyApplications卡片顶部色条两侧不再突出(margin归零)"
```

---

### 任务 2：咨询方式弹窗班级名称去重

**文件：**
- 修改：`enroll-web/src/views/HomePage.vue:203-205`

**现状：** `classesWithGroupInfo` computed 只过滤 `groupInfo` 非空，同名班级会重复显示。

**目标：** 同名班级只显示第一条（不合并 groupInfo 内容）。

- [ ] **步骤 1：修改 classesWithGroupInfo computed**

```javascript
// 修改前
const classesWithGroupInfo = computed(() =>
  classes.value.filter(c => c.groupInfo && c.groupInfo.trim() !== '')
)

// 修改后：按班级名称去重，同名只保留第一条
const classesWithGroupInfo = computed(() => {
  const seen = new Set()
  return classes.value.filter(c => {
    if (!c.groupInfo || c.groupInfo.trim() === '') return false
    if (seen.has(c.name)) return false
    seen.add(c.name)
    return true
  })
})
```

- [ ] **步骤 2：浏览器验证**

1. 打开首页，点击"咨询方式"
2. 确认同名班级（如两个"成电联合培养班"）只显示一条
3. 确认不同名班级正常显示

- [ ] **步骤 3：Commit**

```bash
git add enroll-web/src/views/HomePage.vue
git commit -m "fix: 咨询方式弹窗同名班级去重(只显示第一条)"
```

---

### 任务 3：移动端班级卡片入场动画

**文件：**
- 修改：`enroll-web/package.json`（安装 motion）
- 修改：`enroll-web/src/components/ClassCard.vue`（包裹 Motion 组件）
- 修改：`enroll-web/src/views/HomePage.vue`（传递动画 index）

**动画效果：** 从下方轻微滑入 + 淡入（`translateY: 20px → 0` + `opacity: 0 → 1`），duration 0.3s，stagger delay 0.05s/张。仅移动端生效。

- [ ] **步骤 1：安装 motion 库**

```bash
cd ***REMOVED***RegistrationQuestionnaire/enroll-web
npm install motion
```

- [ ] **步骤 2：验证 motion 安装成功**

```bash
cd ***REMOVED***RegistrationQuestionnaire/enroll-web
node -e "const m = require('motion'); console.log('motion OK', Object.keys(m).slice(0,5))"
```

预期输出：`motion OK [ ... ]`（不报错即可）

- [ ] **步骤 3：在 ClassCard.vue 中引入 Motion 组件**

在 `<script setup>` 顶部添加 import：

```javascript
import { Motion } from 'motion/vue'
```

- [ ] **步骤 4：用 Motion 包裹卡片内容**

将 `<article class="class-card">` 内部内容用 `<Motion>` 包裹。动画仅在移动端生效，通过 `isMobile` ref 控制。

在 `<script setup>` 中添加：

```javascript
// 移动端检测（用于动画条件渲染）
const isMobile = ref(window.innerWidth <= 768)
function onResizeCard() { isMobile.value = window.innerWidth <= 768 }
onMounted(() => window.addEventListener('resize', onResizeCard))
// 注意：onUnmounted 已有 resize 监听器清理，需合并
```

修改模板，用 `<Motion>` 包裹 `.class-card`：

```html
<!-- 修改前 -->
<article
  class="class-card"
  :class="{ 'is-disabled': !timeStatus.canApply, 'is-single': !isMultiRound, 'is-multi': isMultiRound }"
  @click="onClick"
>
  <!-- ... 内容不变 ... -->
</article>

<!-- 修改后 -->
<Motion
  v-if="isMobile"
  :initial="{ opacity: 0, y: 20 }"
  :animate="{ opacity: 1, y: 0 }"
  :transition="{ duration: 0.3, delay: animDelay }"
>
  <article
    class="class-card"
    :class="{ 'is-disabled': !timeStatus.canApply, 'is-single': !isMultiRound, 'is-multi': isMultiRound }"
    @click="onClick"
  >
    <!-- ... 内容不变 ... -->
  </article>
</Motion>
<article
  v-else
  class="class-card"
  :class="{ 'is-disabled': !timeStatus.canApply, 'is-single': !isMultiRound, 'is-multi': isMultiRound }"
  @click="onClick"
>
  <!-- ... 内容不变 ... -->
</article>
```

**问题：** `v-if/v-else` 会导致内容重复。更好的方案是用 CSS 媒体查询控制动画，避免模板重复。

**改进方案：** 不用 `<Motion>` 组件包裹，改用 `useMotion` composable 或纯 CSS `@keyframes` + `animation-delay` CSS 变量。

**最终方案（纯 CSS + CSS 变量，零模板改动）：**

在 HomePage.vue 的 `.card-grid` 中给每个 ClassCard 传入 stagger index，ClassCard 用 CSS `@keyframes` 实现动画。

- [ ] **步骤 3（替换）：在 HomePage.vue 传递 stagger index**

修改 HomePage.vue 模板中 ClassCard 调用，添加 `style` 绑定传递动画延迟：

```html
<!-- 修改前 -->
<ClassCard
  v-for="c in flatCards"
  :key="c._uid"
  :class-info="c"
  :is-applied="appliedClassIds[c.id]"
  :is-admitted="admittedClassIds[c.id]"
  :is-logged-in="isLoggedIn"
  @select="goFormDirect"
/>

<!-- 修改后 -->
<ClassCard
  v-for="(c, idx) in flatCards"
  :key="c._uid"
  :class-info="c"
  :is-applied="appliedClassIds[c.id]"
  :is-admitted="admittedClassIds[c.id]"
  :is-logged-in="isLoggedIn"
  :style="{ '--anim-delay': `${idx * 0.05}s` }"
  @select="goFormDirect"
/>
```

- [ ] **步骤 4（替换）：在 ClassCard.vue 添加 CSS 动画**

在 ClassCard.vue `<style scoped>` 末尾（移动端适配区块内）添加入场动画：

```css
/* ==================== 移动端入场动画 ==================== */
@media (max-width: 768px) {
  .class-card {
    animation: cardSlideIn 0.3s ease-out both;
    animation-delay: var(--anim-delay, 0s);
  }
}

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

**注意：** `@keyframes` 必须放在 `<style>` 非 scoped 块中（Vue scoped 会给 keyframes 名加哈希前缀导致失效），或者放在已有的非 scoped `<style>` 块中。

ClassCard.vue 已有两个 `<style>` 块（一个非 scoped 用于弹窗样式，一个 scoped 用于卡片样式）。将 `@keyframes` 放在非 scoped 块中，`.class-card` 动画声明放在 scoped 块的移动端区块中。

- [ ] **步骤 5：浏览器验证**

1. 打开首页，切换到移动端视口（375px）
2. 刷新页面，确认卡片依次从下方滑入+淡入
3. 切换到 PC 视口（1200px），确认无动画
4. 筛选班级后，确认新出现的卡片也有动画

- [ ] **步骤 6：Commit**

```bash
git add enroll-web/src/views/HomePage.vue enroll-web/src/components/ClassCard.vue
git commit -m "feat: 移动端班级卡片入场动画(滑入+淡入,stagger延迟)"
```

---

### 任务 4：AppHeader 退出登录加消息提示

**文件：**
- 修改：`enroll-web/src/components/AppHeader.vue:108-115`

**现状：** `onLogout()` 确认退出后只 dispatch 事件，无 ElMessage 提示。MyApplications.vue 的 `onLogout()` 已有 `ElMessage.success('已退出登录')`。

- [ ] **步骤 1：在 onLogout 的 .then() 中添加 ElMessage**

```javascript
// 修改前
.then(() => {
  localStorage.removeItem('student_token')
  localStorage.removeItem('student_phone')
  isLoggedIn.value = false
  window.dispatchEvent(new Event('login_changed'))
  window.dispatchEvent(new Event('application_changed'))
}).catch(() => {})

// 修改后
.then(() => {
  localStorage.removeItem('student_token')
  localStorage.removeItem('student_phone')
  isLoggedIn.value = false
  window.dispatchEvent(new Event('login_changed'))
  window.dispatchEvent(new Event('application_changed'))
  ElMessage.success('已退出登录')
}).catch(() => {})
```

- [ ] **步骤 2：浏览器验证**

1. 登录后点击右上角"退出登录"
2. 确认弹窗点"确定"后出现绿色"已退出登录"提示
3. 确认按钮状态从"退出登录"变为"登录"

- [ ] **步骤 3：Commit**

```bash
git add enroll-web/src/components/AppHeader.vue
git commit -m "fix: AppHeader退出登录添加ElMessage提示"
```

---

### 任务 5：全局按钮操作消息提示补全

**文件：**
- 修改：`enroll-web/src/components/FilterBar.vue:223-228`（重置按钮）
- 修改：`enroll-web/src/components/ClassCard.vue:104`（介绍弹窗确认按钮）
- 修改：`enroll-web/src/views/FormPage.vue:160`（班级介绍弹窗确认按钮）

**现状扫描：**

| 按钮 | 文件 | 当前提示 | 需补提示 |
|------|------|----------|----------|
| FilterBar 重置 | FilterBar.vue:223 | 无 | `ElMessage.info('已重置筛选')` |
| ClassCard 介绍弹窗"我知道了" | ClassCard.vue:104 | 无 | `ElMessage.success('已关闭')` ← 不合适，关闭弹窗不需要提示 |
| FormPage 班级介绍弹窗"我知道了" | FormPage.vue:160 | 无 | 同上，关闭弹窗不需要提示 |
| FormPage 返回按钮 | FormPage.vue:15 | 无 | 不需要（导航行为，非操作反馈） |
| MyApplications 返回按钮 | MyApplications.vue:15 | 无 | 不需要（导航行为，非操作反馈） |
| StudentLogin "去报名首页" | StudentLogin.vue:62 | 无 | 不需要（导航行为） |
| HomePage "我已知晓并同意" | HomePage.vue:124 | 无 | `ElMessage.success('已确认报名须知')` |

**结论：** 只需补 2 个提示——FilterBar 重置 + HomePage 须知确认。弹窗关闭和页面导航不需要消息提示（用户已有视觉反馈）。

- [ ] **步骤 1：FilterBar 重置按钮添加提示**

在 `onReset()` 函数末尾添加 ElMessage：

```javascript
// 修改前
function onReset() {
  emit('update:modelValue', null)
  emit('update:selectedRound', null)
  emit('update:selectedTimeStatus', null)
  emit('reset')
}

// 修改后
function onReset() {
  emit('update:modelValue', null)
  emit('update:selectedRound', null)
  emit('update:selectedTimeStatus', null)
  emit('reset')
  ElMessage.info('已重置筛选')
}
```

- [ ] **步骤 2：HomePage 须知确认按钮添加提示**

在 `onAgreeNotice()` 函数中添加 ElMessage：

```javascript
// 修改前
function onAgreeNotice() {
  localStorage.setItem('student_notice_agreed', String(Date.now()))
  showNotice.value = false
}

// 修改后
function onAgreeNotice() {
  localStorage.setItem('student_notice_agreed', String(Date.now()))
  showNotice.value = false
  ElMessage.success('已确认报名须知')
}
```

- [ ] **步骤 3：浏览器验证**

1. 首页筛选栏点"重置"→ 确认出现蓝色 info 提示"已重置筛选"
2. 首页报名须知弹窗点"我已知晓并同意"→ 确认出现绿色 success 提示"已确认报名须知"

- [ ] **步骤 4：Commit**

```bash
git add enroll-web/src/components/FilterBar.vue enroll-web/src/views/HomePage.vue
git commit -m "feat: FilterBar重置+须知确认添加消息提示"
```

---

## 自检

### 1. 规格覆盖度

| 需求 | 对应任务 | ✅ |
|------|----------|-----|
| 卡片色条不突出 | 任务1 | ✅ |
| 咨询方式去重 | 任务2 | ✅ |
| 移动端卡片动画 | 任务3 | ✅ |
| 退出登录提示 | 任务4 | ✅ |
| 全局按钮提示补全 | 任务5 | ✅ |

### 2. 占位符扫描

无 TODO/TBD/待定。所有步骤含完整代码。

### 3. 类型一致性

- `classesWithGroupInfo` 返回类型不变（`Array<ClassInfo>`），只是过滤逻辑变了
- `--anim-delay` CSS 变量在 HomePage 传入、ClassCard 消费，命名一致
- `ElMessage` 已在各文件 import，无需新增 import（FilterBar.vue 已有 `import { ElMessage } from 'element-plus'`，HomePage.vue 同理）

### 4. 遗漏检查

- motion 库安装后未使用（最终方案改用纯 CSS），可跳过安装步骤
- ClassCard.vue 的 `@keyframes` 需放在非 scoped style 块——已在步骤4中说明
