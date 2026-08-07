# 特色班报名系统 · 架构整改计划（Phase 3 + Phase 4）

> 前置：Phase 1/2 计划见 `docs/superpowers/plans/2026-08-05-architecture-cleanup.md`。
> 本文件只在 `refactor/architecture-cleanup` 分支执行，改一处验证一处。
> **验证铁律**：每个 Phase 全部任务完成后，必须调用 `/verification-before-completion` 技能，实际跑命令验证（`npm run build` / `mvnw.cmd compile` / curl / 浏览器冒烟），不能光说"做完了"。
> **删除铁律**：Task 26 涉及删文件，执行时必须列清单、等主人二次确认（"是的，我确定删除"）后再删。

---

## 本文件范围

| Phase | 目标 | 涉及 |
|-------|------|------|
| **Phase 3** | 前端治理：Pinia 统一认证状态、组件拆分瘦身、重复逻辑去重、删死代码、统一 API 层 | Task 17 ~ 27 |
| **Phase 4** | 技术债清理 + 测试补强 | Task 28 ~ 31 |

---

# Phase 3：前端治理

> 原则：**适度拆分**（主人拍板）——只拆收益明显的：① 重复 2 次以上的窗口宽度/弹窗自适应逻辑抽组合式函数；② 超过 40 行的内嵌弹窗抽独立组件；③ 其余保持不动。
> 每个 Task 改完先在浏览器手动冒烟一次（`npm run dev`），再做下一个。

## Task 17：引入 Pinia + 学生认证 store

### ① 装依赖

```bash
cd ***REMOVED***RegistrationQuestionnaire/enroll-web && npm i pinia
```

### ② 新建 `enroll-web/src/stores/auth.js`

```js
/**
 * stores/auth.js · 学生端认证状态（Pinia store）
 *
 * 为什么用 Pinia 而不是"localStorage + window.dispatchEvent('login_changed')"：
 *   1. 单一数据源：token / phone / isLoggedIn 只存 store 一份，组件不再各自读 localStorage
 *   2. 响应式传播：login()/logout() 之后，所有引用该 store 的组件自动更新视图，
 *      不需要再手动派发 login_changed 事件 + 逐个 addEventListener
 *   3. 持久化：token 同时写 localStorage，刷新页面后 state 从 localStorage 恢复
 *
 * 现实类比：localStorage 像"写在纸上的会员卡号"，Pinia 像"会员系统"——
 * 登录 = 在系统里登记（store 响应式更新所有页面）；纸只是系统重启后找回记录用。
 */
import { defineStore } from 'pinia'

const TOKEN_KEY = 'student_token'
const PHONE_KEY = 'student_phone'

export const useAuthStore = defineStore('auth', {
  // 初始化时从 localStorage 恢复（刷新页面不丢登录态）
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    phone: localStorage.getItem(PHONE_KEY) || '',
  }),

  getters: {
    // 有 token 即视为已登录
    isLoggedIn: (state) => !!state.token,
  },

  actions: {
    /** 登录成功：写 store（响应式）+ 写 localStorage（持久化） */
    login(token, phone) {
      this.token = token
      this.phone = phone || ''
      localStorage.setItem(TOKEN_KEY, token)
      if (phone) localStorage.setItem(PHONE_KEY, phone)
    },
    /** 退出登录：清 store + 清 localStorage */
    logout() {
      this.token = ''
      this.phone = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PHONE_KEY)
    },
  },
})

/** 跨标签页同步：另一标签页登录/退出后，本标签页 store 自动更新（在 main.js 调一次即可） */
export function syncAuthAcrossTabs() {
  window.addEventListener('storage', (e) => {
    if (e.key !== TOKEN_KEY && e.key !== PHONE_KEY) return
    const auth = useAuthStore()
    if (e.key === TOKEN_KEY) auth.token = e.newValue || ''
    if (e.key === PHONE_KEY) auth.phone = e.newValue || ''
  })
}
```

> 为什么保留 localStorage 而不只用内存：刷新页面 / 跨标签页都需要持久化，Pinia 不内置持久化，localStorage 是最轻方案。

### ③ 修改 `enroll-web/src/main.js` — 注册 Pinia

```js
import { createPinia } from 'pinia'
import { syncAuthAcrossTabs } from './stores/auth.js'
// ...
const app = createApp(App)
app.use(createPinia())                    // Pinia 必须先于组件使用 store
app.use(router)
app.use(ElementPlus, { locale: zhCn })
syncAuthAcrossTabs()                      // 跨标签页登录态同步（只注册一次）
app.mount('#app')
```

- [ ] `npm run dev` 后控制台无 Pinia 报错（"no active Pinia instance" 之类）
- [ ] `git add enroll-web/src/stores/auth.js enroll-web/src/main.js enroll-web/package.json enroll-web/package-lock.json && git commit -m "feat(front): 引入 Pinia + 学生认证 store（Task 17）"`

---

## Task 18：api.js 统一（token 自动注入 + 去 mock）

> 现状问题：① `request()` 不自动带 JWT，每个接口手动塞 header；② `fetchClasses()` 有 mock 兜底（后端挂了显示假数据，掩盖真实错误）；③ `fetchMyApplications()` 是废弃接口却还在；④ `fetchMyApplicationsMe()` 手动读 localStorage + 手写 fetch。

### 修改 `enroll-web/src/utils/api.js`

**① `request()` 自动注入 token**（从 Pinia store 读，而不是散落的 localStorage）：

```js
import { useAuthStore } from '../stores/auth.js'

async function request(url, options = {}) {
  // useAuthStore 必须在 Pinia 激活后调用（api.js 只在组件生命周期内被调用，此时已激活）
  const auth = useAuthStore()
  const headers = {
    'Content-Type': 'application/json',
    ...(auth.token ? { Authorization: `Bearer ${auth.token}` } : {}),
    ...(options.headers || {}),
  }
  const res = await fetch(url, { ...options, headers })
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}: ${res.statusText}`)
    err.status = res.status
    throw err
  }
  return res.json()
}
```

**② `fetchClasses()` 去掉 mock 兜底**：删掉 try/catch 里的假数据数组，直接 `const res = await request('/api/classes'); return res.data || []`。后端挂了就该抛错、页面显示"加载失败"，而不是给人看假班级。

**③ 删除废弃接口 `fetchMyApplications()`**（整段删，已无调用方；注释里也说明了已废弃）。

**④ `fetchMyApplicationsMe()` 改走 `request()`**（自动带 token，不再手动读 localStorage）：

```js
/** JWT 版我的报名（token 由 request() 自动注入） */
export const fetchMyApplicationsMe = async () => {
  const res = await request('/api/applications/me')
  return res.code === 200 ? (res.data || []) : []
}
```

> ⚠️ 同步改掉第 114 行过时注释（写的是"改用 httpOnly Cookie"，实际代码一直用的 Bearer header），避免误导。

- [ ] 登录后浏览器 Network 面板看到 `GET /api/applications/me` 自动带 `Authorization: Bearer xxx`
- [ ] 停掉后端 → 首页显示"班级数据加载失败"，不再出现假班级
- [ ] `git add enroll-web/src/utils/api.js && git commit -m "refactor(front): api.js 统一 token 注入 + 去 mock（Task 18）"`

---

## Task 19：路由守卫

> 现状：任何页面都能直接访问，未登录也能打开"我的报名"。
> ⚠️ **只守卫 `/my-applications`，不守卫 `/form`**：FormPage 的登录发生在提交表单的过程中（Step 1 手机号+身份证），加了硬守卫会阻断"先填表后登录"的正常流程。

### 修改 `enroll-web/src/router/index.js`

```js
import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

// ... routes 定义不变 ...

const router = createRouter({ history: createWebHashHistory(), routes, scrollBehavior: /* 不变 */ })

// 全局前置守卫：未登录访问"我的报名" → 跳登录页，登录后回到原页面
router.beforeEach((to) => {
  if (to.path === '/my-applications') {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      // query 里的 redirect 让 StudentLogin 登录成功后跳回来
      return { path: '/student-login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

export default router
```

- [ ] 未登录访问 `#/my-applications` → 跳到登录页
- [ ] 登录后访问 `#/my-applications` → 正常进入
- [ ] 未登录访问 `#/form/1` → 正常进入（不拦截）
- [ ] `git add enroll-web/src/router/index.js && git commit -m "feat(front): 路由守卫（未登录拦截我的报名）（Task 19）"`

---

## Task 20：useWindowWidth 组合式函数

> 现状：HomePage / ClassCard / FormPage 各自复制了"windowWidth ref + onResize + addEventListener"三件套。

### 新建 `enroll-web/src/composables/useWindowWidth.js`

```js
/**
 * composables/useWindowWidth.js · 响应式窗口宽度
 *
 * 为什么抽这个：多个组件各自写了三件套（windowWidth ref + onResize + resize 监听），
 * 复制粘贴 N 次。抽成组合式函数，一处实现，多处复用。
 *
 * 用法：
 *   const windowWidth = useWindowWidth()   // 响应式数字，窗口 resize 时自动更新
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useWindowWidth() {
  const windowWidth = ref(window.innerWidth)
  function onResize() {
    windowWidth.value = window.innerWidth
  }
  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))
  return windowWidth
}
```

### 修改 `HomePage.vue` / `ClassCard.vue`

把 `const windowWidth = ref(window.innerWidth)` + `function onResize()` + `onMounted(() => addEventListener('resize', ...))` + `onUnmounted(() => removeEventListener('resize', ...))` 这四处，替换为一行：

```js
import { useWindowWidth } from '../composables/useWindowWidth.js'
const windowWidth = useWindowWidth()
```

（注意：HomePage 的 `onUnmounted` 里还有其他监听器，保留其余部分，只删 resize 相关；ClassCard 同理，保留 IntersectionObserver 逻辑。）

- [ ] 拖动窗口宽度变化 → 弹窗宽度正确响应
- [ ] `git add enroll-web/src/composables/useWindowWidth.js enroll-web/src/views/HomePage.vue enroll-web/src/components/ClassCard.vue && git commit -m "refactor(front): 抽取 useWindowWidth 去重 resize 三件套（Task 20）"`

---

## Task 21：useDialogAdapt 弹窗自适应

> 现状：HomePage 里 notice-dialog 和 group-info-dialog 两个 `watch` 弹窗打开逻辑 90% 重复（移动端固定宽度 + body 弹性滚动 / PC 端 max-width）。抽成一个组合式函数。

### 新建 `enroll-web/src/composables/useDialogAdapt.js`

```js
/**
 * composables/useDialogAdapt.js · Element Plus 弹窗移动端/PC 尺寸自适应
 *
 * 为什么抽这个：HomePage 两个弹窗 watch 逻辑重复 90%（都是拿 dialog DOM →
 * 移动端固定宽高 + body 滚动 → PC 端 max-width + body 滚动）。
 * 抽成组合式函数后，每个弹窗组件一行调用。
 *
 * 用法：
 *   useDialogAdapt('notice-dialog', () => props.modelValue, { maxWidthPc: 720, withFooter: true })
 */
import { watch } from 'vue'

export function useDialogAdapt(
  dialogClass,
  isOpen,                 // 传 getter：() => props.modelValue 或 ref
  { maxWidthPc = 720, withFooter = true, maxHeightMobile = '88vh' } = {}
) {
  watch(isOpen, async (val) => {
    if (!val) return
    // Element Plus Teleport 到 body，等几帧确保 DOM 渲染完成
    await new Promise((resolve) => setTimeout(resolve, 200))
    const dialog = document.querySelector(`.${dialogClass}`)
    if (!dialog) return

    const isMobile = window.innerWidth < 768

    if (isMobile) {
      // 移动端：固定宽度 + 顶部留 5vh 间距 + body 弹性滚动
      dialog.style.setProperty('width', '95vw', 'important')
      dialog.style.maxWidth = '355px'
      dialog.style.maxHeight = maxHeightMobile
      dialog.style.setProperty('top', 'auto', 'important')
      dialog.style.setProperty('transform', 'none', 'important')
      const overlayDialog = document.querySelector('.el-overlay-dialog')
      if (overlayDialog) {
        overlayDialog.style.setProperty('align-items', 'flex-start', 'important')
        overlayDialog.style.setProperty('justify-content', 'center', 'important')
        overlayDialog.style.setProperty('display', 'flex', 'important')
        overlayDialog.style.setProperty('padding-top', '5vh', 'important')
      }
      fitBody(dialog, maxHeightMobile, withFooter)
    } else {
      // PC 端：max-width + body 内部滚动
      dialog.style.maxWidth = `${maxWidthPc}px`
      fitBody(dialog, '85vh', withFooter)
    }
  })
}

/** 计算 body 可用高度 = 弹窗总高 - header - footer - buffer */
async function fitBody(dialog, totalHeight, withFooter) {
  await new Promise((resolve) => setTimeout(resolve, 50))
  const headerH = dialog.querySelector('.el-dialog__header')?.getBoundingClientRect().height ?? 53
  const footerH = withFooter
    ? (dialog.querySelector('.el-dialog__footer')?.getBoundingClientRect().height ?? 49)
    : 0
  const total = parseInt(totalHeight, 10) * window.innerHeight / 100
  const maxBodyH = Math.floor(total - headerH - footerH - 8)
  const body = dialog.querySelector('.el-dialog__body')
  if (body) {
    body.style.setProperty('max-height', maxBodyH + 'px', 'important')
    body.style.overflowY = 'auto'
  }
}
```

- [ ] 依赖 Task 22 的弹窗组件一起验证

---

## Task 22：抽取 NoticeDialog + GroupInfoDialog（HomePage 瘦身）

> 现状：HomePage 模板 + 脚本里嵌了两个大弹窗（各 60+ 行模板 + 两个重复的 watch 自适应），挤在一起很难维护。

### ① 新建 `enroll-web/src/components/NoticeDialog.vue`

```vue
<!--
  NoticeDialog.vue · 报名须知弹窗
  从 HomePage 抽出：标题 + 报名条件 + 特色班简介表格 + 注意事项 + 同意按钮
  自带移动端/PC 自适应（useDialogAdapt），HomePage 不再需要两个重复 watch
-->
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="title"
    width="720px"
    :close-on-click-modal="true"
    :destroy-on-close="false"
    :lock-scroll="windowWidth > 768"
    class="notice-dialog"
  >
    <template #header>
      <span class="notice-dialog-title">{{ title }}</span>
      <button class="notice-dialog-close" @click="emit('update:modelValue', false)" type="button" aria-label="关闭">
        <el-icon :size="18"><Close /></el-icon>
      </button>
    </template>

    <!-- 一、报名条件（从 sys_config 动态加载） -->
    <section class="nd-section">
      <h3>一、报名条件</h3>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
        <template #title>以下条件需 <strong>全部满足</strong> 方可报名</template>
      </el-alert>
      <ul v-if="conditions.length">
        <li v-for="(c, i) in conditions" :key="i" v-html="c" />
      </ul>
      <ul v-else>
        <li>加载报名条件失败，请刷新页面</li>
      </ul>
    </section>

    <!-- 二、班级介绍（每个班 × 每轮 = 一行，多轮班多轮时间都能展示） -->
    <section class="nd-section">
      <h3>二、特色班简介</h3>
      <div class="nd-table-wrap">
        <el-table :data="tableData" border size="small">
          <el-table-column label="班级名称" align="center" width="260">
            <template #default="{ row }">
              <span class="class-name-cell">{{ row._name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="报名时间" align="center" min-width="180">
            <template #default="{ row }">
              <el-tooltip :content="`报名时间：${row._period}`" placement="top">
                <span class="period-text">{{ row._period }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 三、注意事项 -->
    <section class="nd-section">
      <h3>三、注意事项</h3>
      <ul v-if="notices.length">
        <li v-for="(n, i) in notices" :key="i" v-html="n" />
      </ul>
      <ul v-else>
        <li>暂无注意事项</li>
      </ul>
    </section>

    <template #footer>
      <div class="notice-footer">
        <el-button type="primary" size="large" @click="emit('agree')">我已知晓并同意</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { Close } from '@element-plus/icons-vue'
import { useWindowWidth } from '../composables/useWindowWidth.js'
import { useDialogAdapt } from '../composables/useDialogAdapt.js'

// 声明 props：open=开关，title=标题，conditions=条件数组，notices=须知数组，tableData=班级表格数据
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '杭州电子科技大学 2026 级特色班报名须知' },
  conditions: { type: Array, default: () => [] },
  notices: { type: Array, default: () => [] },
  tableData: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'agree'])

// lock-scroll 需要响应式窗口宽度
const windowWidth = useWindowWidth()
// 弹窗尺寸自适应（原来 HomePage 里 40 行重复 watch，现在一行）
useDialogAdapt('notice-dialog', () => props.modelValue, { maxWidthPc: 720 })
</script>

<!-- 样式迁移：把 HomePage.vue 里 .notice-dialog / .nd-section / .notice-dialog-title /
     .notice-dialog-close / .class-name-cell / .period-text / .notice-footer 相关样式
     原样搬到这里（<style scoped> 块）。类名不能改——JS 自适应靠 .notice-dialog 查 DOM。 -->
```

### ② 新建 `enroll-web/src/components/GroupInfoDialog.vue`

```vue
<!--
  GroupInfoDialog.vue · 咨询方式弹窗
  从 HomePage 抽出：按班级展示咨询群信息，无信息时显示"暂无信息"
-->
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="咨询方式"
    width="500px"
    :close-on-click-modal="true"
    :destroy-on-close="false"
    :lock-scroll="windowWidth > 768"
    class="group-info-dialog"
  >
    <template #header>
      <span class="group-info-dialog-title">咨询方式</span>
      <button class="group-info-dialog-close" @click="emit('update:modelValue', false)" type="button" aria-label="关闭">
        <el-icon :size="18"><Close /></el-icon>
      </button>
    </template>

    <div class="nd-section" v-if="classes.length > 0">
      <div v-for="cls in classes" :key="cls.id" class="group-info-card">
        <div class="group-info-card-header">{{ cls.name }}</div>
        <div class="group-info-card-body">{{ cls.groupInfo }}</div>
      </div>
    </div>
    <div v-else class="group-info-empty">
      <span>暂无信息</span>
    </div>
  </el-dialog>
</template>

<script setup>
import { Close } from '@element-plus/icons-vue'
import { useWindowWidth } from '../composables/useWindowWidth.js'
import { useDialogAdapt } from '../composables/useDialogAdapt.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 已按班级去重的有咨询信息数组 [{ id, name, groupInfo }]（HomePage 的 classesWithGroupInfo 传入）
  classes: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue'])

const windowWidth = useWindowWidth()
// 该弹窗无 footer，withFooter=false（body 高度计算不含 footer）
useDialogAdapt('group-info-dialog', () => props.modelValue, { maxWidthPc: 500, withFooter: false })
</script>

<!-- 样式迁移：把 HomePage.vue 里 .group-info-dialog / .group-info-dialog-title /
     .group-info-dialog-close / .group-info-card / .group-info-empty 样式原样搬到这里。 -->
```

### ③ 修改 `HomePage.vue`

- 模板：删除两个内嵌 `<el-dialog>` 大块，换成：

```vue
<NoticeDialog
  v-model="showNotice"
  :title="noticeData.title || '杭州电子科技大学 2026 级特色班报名须知'"
  :conditions="noticeData.conditions"
  :notices="noticeData.notices"
  :table-data="flatTableData"
  @agree="onAgreeNotice"
/>
<GroupInfoDialog v-model="showGroupInfo" :classes="classesWithGroupInfo" />
```

- 脚本：
  - 删除 `watch(showNotice, ...)` 和 `watch(showGroupInfo, ...)` 两个重复块（自适应逻辑已进组件）
  - 删除 `windowWidth` ref + `onResize` + resize 监听（Task 20 已换成 `useWindowWidth()`）
  - import 加 `NoticeDialog` / `GroupInfoDialog`；`Close` 图标 import 删掉（已移入组件），`InfoFilled` 保留（提示横条还在用）
- 样式：把上述两个弹窗相关样式从 HomePage 的 `<style scoped>` 中**剪走**（搬到对应组件），类名不变

- [ ] 首页进入时须知弹窗照常弹出，移动端宽度/滚动正常
- [ ] 点 Banner 咨询按钮 → 咨询弹窗正常
- [ ] `git add enroll-web/src/components/NoticeDialog.vue enroll-web/src/components/GroupInfoDialog.vue enroll-web/src/views/HomePage.vue && git commit -m "refactor(front): 抽取 NoticeDialog/GroupInfoDialog 瘦身 HomePage（Task 22）"`

---

## Task 23：抽取 ClassDescDialog（ClassCard 瘦身）

> 现状：ClassCard 内嵌班级介绍弹窗 38 行模板 + 220 行弹窗样式，跟卡片本身混在一起。抽成独立组件，弹窗样式（非 scoped，Teleport 必需）也跟过去，ClassCard 只剩卡片本体。

### ① 新建 `enroll-web/src/components/ClassDescDialog.vue`

```vue
<!--
  ClassDescDialog.vue · 班级介绍弹窗
  从 ClassCard 抽出：只负责展示 description + 响应式宽度（PC 500px / 移动 calc(100vw-40px)）
  ⚠️ 为什么非 scoped 样式：el-dialog 被 Teleport 到 body，scoped 属性在 body 下匹配不到，
  必须用全局选择器覆盖尺寸。这是本弹窗唯一"丑"，但必要。
-->
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    class="desc-dialog"
    :width="dialogWidth"
    :show-close="false"
    :lock-scroll="windowWidth > 768"
    append-to-body
    destroy-on-close
  >
    <!-- 自定义头部：图标 + 标题 + 红关闭按钮 -->
    <div class="custom-header">
      <div class="header-content">
        <img :src="descIcon" alt="" class="custom-icon" />
        <div class="header-text">
          <div class="header-title" :title="title">{{ title }}</div>
        </div>
      </div>
      <button class="custom-close-btn" @click="emit('update:modelValue', false)">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </div>

    <div class="dialog-inner">
      <span class="desc-tag">班级介绍</span>
      <div class="desc-dialog-body">
        <p class="desc-dialog-text">{{ description }}</p>
      </div>
      <div class="desc-dialog-footer">
        <button class="desc-dialog-confirm-btn" @click="emit('update:modelValue', false)">我知道了</button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import descIcon from '../assets/images/description.svg'
import { useWindowWidth } from '../composables/useWindowWidth.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },        // 卡片标题（多轮班含"第X轮报名"）
  description: { type: String, default: '' },  // 班级介绍文本
})
const emit = defineEmits(['update:modelValue'])

const windowWidth = useWindowWidth()
// 响应式宽度：PC 500px，移动端 calc(100vw-40px)
// Element Plus 用 inline style 控制 width，CSS 选择器在 Teleport+scoped 下易失效，JS 控 prop 最可靠
const dialogWidth = computed(() => {
  return windowWidth.value <= 768 ? 'calc(100vw - 40px)' : '500px'
})
</script>

<!-- 样式迁移：把 ClassCard.vue 里非 scoped 块（.desc-dialog 相关）+ scoped 块里
     .custom-header / .header-content / .custom-icon / .header-text / .header-title /
     .custom-close-btn / .dialog-inner / .desc-tag / .desc-dialog-body / .desc-dialog-text /
     .desc-dialog-footer / .desc-dialog-confirm-btn 及对应 @media 规则，原样搬到这里。 -->
```

### ② 修改 `ClassCard.vue`

- 模板：删除内嵌 `<el-dialog>` 大块（71-108 行），在卡片末尾加：

```vue
<ClassDescDialog
  v-model="showDesc"
  :title="cardTitle"
  :description="classInfo.description"
/>
```

- 脚本：
  - 删掉 `dialogVisible`、`dialogWidth`、`windowWidth` ref、`onResize`、resize 监听、`descIcon` import
  - 加 `const showDesc = ref(false)`；`showDescription()` 改为 `showDesc.value = true`
  - import 加 `ClassDescDialog`
- 样式：搬走弹窗样式后 ClassCard 从 798 行降到约 450 行

- [ ] 点卡片"介绍"按钮 → 弹窗正常，移动端宽度正确
- [ ] `git add enroll-web/src/components/ClassDescDialog.vue enroll-web/src/components/ClassCard.vue && git commit -m "refactor(front): 抽取 ClassDescDialog 瘦身 ClassCard（Task 23）"`

---

## Task 24：抽取 EditDialog（MyApplications 瘦身 + 身份证不可改）

> ⚠️ **关键安全修复**：Phase 1 起学生端接口不再返回 `idCardRaw`（只返回脱敏 `idCard`）。
> 旧代码 `editForm.idCard = row.idCardRaw || row.idCard` 会拿到**脱敏值**，提交时后端
> `ApplicationService.updateApp` 第 294-298 行 `setIdCard(newIdCard)` 会把**真实身份证覆盖成脱敏值 = 数据损坏**。
>
> 方案：学生端**身份证提交后不可修改**（它是报名记录的身份凭证）。弹窗里身份证只读展示脱敏值，提示"修改请联系管理员"。同时后端兜底——`updateApp` 不再接受学生请求体里的 idCard 字段。

### ① 新建 `enroll-web/src/components/EditDialog.vue`

```vue
<!--
  EditDialog.vue · 修改报名信息弹窗
  从 MyApplications 抽出。
  ⚠️ 身份证号不可修改：Phase 1 起学生端不返回 idCardRaw，回填脱敏值提交会覆盖真实身份证（数据损坏）。
  因此：idCard 只读展示脱敏值，改身份证请联系管理员（低代码平台 admin 接口可改）。
-->
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    width="90%"
    max-width="440px"
    destroy-on-close
    :show-close="false"
    class="edit-dialog"
  >
    <div class="edit-dialog-bar" />
    <div class="edit-dialog-head">
      <img :src="editToolsIcon" alt="" class="edit-dialog-icon" />
      <span class="edit-dialog-title">修改报名信息</span>
    </div>

    <div class="edit-dialog-body">
      <div class="edit-field">
        <label class="edit-label">姓名</label>
        <el-input v-model="form.name" maxlength="10" placeholder="请输入姓名" class="edit-input" />
      </div>
      <div class="edit-field">
        <label class="edit-label">身份证号</label>
        <!-- 只读展示脱敏值，不可修改（改身份证请联系管理员） -->
        <el-input :model-value="form.idCardMasked" disabled class="edit-input" placeholder="——" />
        <div class="edit-hint">身份证号作为身份凭证不可修改，如有错误请联系管理员</div>
      </div>
      <div class="edit-field">
        <label class="edit-label">性别</label>
        <el-radio-group v-model="form.gender" class="edit-radio-group">
          <el-radio-button value="男">男</el-radio-button>
          <el-radio-button value="女">女</el-radio-button>
        </el-radio-group>
      </div>
      <div class="edit-field">
        <label class="edit-label">选考物理</label>
        <el-radio-group v-model="form.hasPhysics" class="edit-radio-group">
          <el-radio-button value="是">是</el-radio-button>
          <el-radio-button value="否">否</el-radio-button>
        </el-radio-group>
      </div>
      <div class="edit-field">
        <label class="edit-label">选考英语</label>
        <el-radio-group v-model="form.hasEnglish" class="edit-radio-group">
          <el-radio-button value="是">是</el-radio-button>
          <el-radio-button value="否">否</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="edit-dialog-footer">
      <el-button class="edit-cancel-btn" @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="loading" class="edit-save-btn" @click="emit('submit')">保存修改</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import editToolsIcon from '../assets/images/edit-tools.svg'

// form 由父组件传入的响应式对象（editForm），直接改其字段即可（沿用原 MyApplications 的用法）
defineProps({
  modelValue: { type: Boolean, default: false },
  form: { type: Object, required: true },     // { name, gender, hasPhysics, hasEnglish, idCardMasked }
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'submit'])
</script>

<!-- 样式迁移：把 MyApplications.vue 里 .edit-dialog* 全部样式原样搬到这里的 <style scoped>，类名不变。 -->
```

### ② 修改 `MyApplications.vue`

- 模板：删除内嵌编辑弹窗（105-155 行），替换为：

```vue
<EditDialog
  v-model="editDialogVisible"
  :form="editForm"
  :loading="editLoading"
  @submit="onEditSubmit"
/>
```

- 脚本：
  - `editForm` 里 `idCard` 改为 `idCardMasked`（只读展示），去掉 `idCard: ''`：

    ```js
    const editForm = reactive({ name: '', idCardMasked: '', gender: '', hasPhysics: '', hasEnglish: '' })
    ```

  - `onEdit(row)` 不再读 `idCardRaw`（学生端接口已不返回）：

    ```js
    function onEdit(row) {
      editingId.value = row.id
      editForm.name = row.name
      editForm.idCardMasked = row.idCard          // 后端返回的脱敏值，只读展示
      editForm.gender = row.gender || ''
      editForm.hasPhysics = row.hasPhysics || '否'
      editForm.hasEnglish = row.hasEnglish || '否'
      editDialogVisible.value = true
    }
    ```

  - `onEditSubmit()` 删掉身份证校验，提交体不含 idCard：

    ```js
    async function onEditSubmit() {
      if (editLoading.value) return
      const name = editForm.name.trim()
      if (!name) { ElMessage.warning('姓名不能为空'); return }
      if (name.length < 2) { ElMessage.warning('姓名至少2个字符'); return }
      editLoading.value = true
      try {
        // 不传 idCard：后端忽略 idCard，身份证不可篡改
        await updateApplicationAPI(editingId.value, {
          name, gender: editForm.gender, hasPhysics: editForm.hasPhysics, hasEnglish: editForm.hasEnglish,
        })
        ElMessage.success('修改成功')
        editDialogVisible.value = false
        await fetchMyRecords()
      } catch (e) {
        ElMessage.error(e.message || '修改失败')
      } finally {
        editLoading.value = false
      }
    }
    ```

  - `editToolsIcon` import 删掉（图标已进 EditDialog 组件）

### ③ 后端兜底：修改 `enroll-server/src/main/java/com/enroll/server/service/ApplicationService.java`

`updateApp` 里删掉 idCard 写入块（约 294-302 行），只保留 name/gender/hasPhysics/hasEnglish 更新：

```java
// 身份证是报名记录的身份凭证，学生端接口不允许修改（防脱敏值覆盖真实值）
// body.containsKey("idCard") 时直接忽略，不写入
if (body.containsKey("name"))   app.setName((String) body.get("name"));
if (body.containsKey("gender")) app.setGender((String) body.get("gender"));
// ... 保留 hasPhysics / hasEnglish 更新 ...
// ❌ 删除：if (body.containsKey("idCard")) { app.setIdCard(...); app.setIdCardMasked(...) }
```

> 说明：低代码平台/admin 如需改身份证，走专门的 admin 接口，不在学生接口职责内。

- [ ] 后端 `mvnw.cmd compile` 通过
- [ ] 修改报名 → 只改姓名/性别，保存成功；DB 里 idCard 保持原值（查 `SELECT idCard FROM applications`）
- [ ] 手工 curl 带 idCard 字段调 `/api/applications/update` → DB 身份证不变（兜底生效）
- [ ] `git add -A && git commit -m "fix(front+back): 抽取 EditDialog + 身份证提交后不可改（防脱敏覆盖）（Task 24）"`

---

## Task 25：登录态统一走 Pinia（AppHeader/StudentLogin/FormPage/usePhoneCode/MyApplications/HomePage）

> 现状：登录态 = localStorage 读写 + `window.dispatchEvent('login_changed')` + 每个页面各自 `addEventListener`，散落 6 处。统一后：所有组件读 `auth.isLoggedIn` / `auth.token`，登录退出只调 `auth.login()` / `auth.logout()`。

逐个文件修改（每个改完顺手 `npm run dev` 冒烟）：

### ① `enroll-web/src/components/AppHeader.vue`

- 删掉 `localStorage.getItem('student_token')` 判断、`login_changed` / `storage` 监听、`onLogout` 里的 dispatchEvent
- 替换为：

```js
import { useAuthStore } from '../stores/auth.js'
const auth = useAuthStore()
const isLoggedIn = computed(() => auth.isLoggedIn)   // 响应式，登录/退出自动更新
function onLogout() {
  auth.logout()                                      // 只调 store，不再手动派发事件
  // 原来的提示 + 跳转逻辑保留
}
```

> 跨标签页退出：Task 17 的 `syncAuthAcrossTabs()`（main.js 已注册）监听 student_token 变化自动更新 store，所以这里不用再写 storage 监听。

### ② `enroll-web/src/views/StudentLogin.vue`

- `onLogin` 里 `localStorage.setItem('student_token', ...)` + `dispatchEvent('login_changed')` 换成：

```js
const auth = useAuthStore()
auth.login(token, phone)          // phone 有就传，没有传 undefined
```

### ③ `enroll-web/src/composables/usePhoneCode.js`

- `onLogin` 成功回调里同样换成 `auth.login(token, phone)`（FormPage 和 StudentLogin 都复用它，一次改全）

### ④ `enroll-web/src/views/FormPage.vue`

- `const token = localStorage.getItem('student_token')`（308 行）→ `const token = auth.token`
- 335 行未登录判断 `!localStorage.getItem('student_token')` → `!auth.isLoggedIn`
- 330 行 `addEventListener('login_changed', onLoginChanged)` 删除——store 响应式已自动处理

### ⑤ `enroll-web/src/views/MyApplications.vue`

- `onLogout`：`localStorage.removeItem` 两行 + `dispatchEvent` 换成 `auth.logout()`
- `onLoginChanged` 监听删掉（store 响应式）；`fetchMyRecords` 里 `localStorage.getItem('student_token')` → `auth.token`
- 401 处理里 `localStorage.removeItem` 两行 → `auth.logout()`

### ⑥ `enroll-web/src/views/HomePage.vue`

- `const isLoggedIn = ref(false)` → `const auth = useAuthStore()` + `const isLoggedIn = computed(() => auth.isLoggedIn)`
- `onMounted` 里 `isLoggedIn.value = !!localStorage.getItem('student_token')` → 删（getter 自动算）
- `loadData` 里 `localStorage.getItem('student_token')` → `auth.token`
- `onStorageChange` 里 student_token 清空分支删掉（Task 17 已全局同步）；`login_changed` 监听删掉；`onLogout` → `auth.logout()`

- [ ] 登录 → 刷新页面登录态保持；退出 → 所有页面按钮/标签即时更新（不刷新）
- [ ] 开两个标签页，一个退出 → 另一个"我的报名"按钮变"登录"（跨标签同步生效）
- [ ] `git add -A && git commit -m "refactor(front): 登录态统一走 Pinia store（Task 25）"`

---

## Task 26：删除死代码

> ⚠️ **删除铁律**：以下 3 个文件已用 Grep 确认**全项目无 import 引用**，但删之前仍要列清单、等主人二次确认（"是的，我确定删除"）后再删。

| 文件 | 现状 | 证据 |
|------|------|------|
| `enroll-web/src/composables/useApplication.js` | 无任何组件 import | Grep 全 `src` 仅自引用 |
| `enroll-web/src/components/PieChart.vue` | 引用 data.js 里已不存在的 `classes/generateMockApplications/classColors`，import 即报错 | Grep 无引用方 |
| `enroll-web/src/components/TrendChart.vue` | 引用已不存在的 `trendData`，同理 | Grep 无引用方 |

### 修改 `enroll-web/package.json` — 去掉 3 个未用依赖

`mammoth` / `playwright` / `xlsx` 在 `src` 里无任何 `import`（已 Grep 确认），从 dependencies 移除，然后 `npm i` 更新 lockfile。

- [ ] 主人确认删除后执行删除
- [ ] `npm run build` 通过（PieChart/TrendChart 报错消失）
- [ ] `git add -A && git commit -m "chore(front): 删除死代码组件 + 清理未用依赖（Task 26）"`

---

## Task 27：Phase 3 验证

```bash
cd ***REMOVED***RegistrationQuestionnaire/enroll-web && npm run build    # 编译通过
cd ***REMOVED***RegistrationQuestionnaire/enroll-server && mvnw.cmd compile
```

调用 `/verification-before-completion` 技能，按技能清单实测，重点回归：
- 学生端完整流程：首页 → 须知弹窗 → 选班 → 填表（未登录）→ 登录 → 提交 → 我的报名 → 撤回
- 编辑报名：只改姓名成功、身份证灰色不可改、DB 身份证不变
- 移动端（375px 宽）：两个弹窗、编辑弹窗、介绍弹窗尺寸正常
- 跨标签页退出同步

通过后：确认工作区干净，提交均已在 `refactor/architecture-cleanup` 分支。

---

# Phase 4：技术债清理 + 测试补强

> 目标：清理长期积累的"能用但丑"技术债，补齐测试防回归。每个子任务独立小步走。

## Task 28：后端技术债

### 28.1 ObjectMapper 统一注入（去 `new`）

| 位置 | 现状 | 改法 |
|------|------|------|
| `ClassService.java:35` | `private static final ObjectMapper MAPPER = new ObjectMapper()` | 注入 Spring 自动配置的 bean：构造器注入 `ObjectMapper`，删静态字段 |
| `ApplicationService.java:249` | `ObjectMapper om = new ObjectMapper()` | 同上，用注入的 bean |
| `SyncController.java:63` | 构造器里 `this.objectMapper = new ObjectMapper()` | 改用 Spring 注入的 bean |

> 为什么：`new ObjectMapper()` 每处各建一个实例，配置（如时间格式）不统一；Spring Boot 自动配置的 bean 全局一份，可统一加 Jackson 配置。

### 28.2 TimeController 内存泄漏 + @Scheduled

`TimeController.cleanExpired()` 从未被调用（`RATE_LIMIT` 这个 `static ConcurrentHashMap` 只增不删，IP 一多就泄漏）。

改法（推荐改动最小方案）：
- 启动类加 `@EnableScheduling`（若还没有），方法加：

```java
/** 每 60 秒清理过期 IP，防止内存泄漏（原来是死代码，从未被调用） */
@Scheduled(fixedRate = 60_000)
public void cleanExpired() { /* 已有实现不动 */ }
```

- 若不想引入定时任务，**直接删掉这个死方法 + 把 RATE_LIMIT 换成 Caffeine 过期缓存**（需加依赖）——不推荐（要加依赖）。

### 28.3 Category 死代码删除

`Category.java:24-30` 的 `@ManyToMany classes` 字段：`ssc_class_category` 中间表 + 双向关联，当前业务**根本没用到**（类别只是标签字符串，班级-类别关系不走这个表）。

改法：删掉 `classes` 字段 + `getClasses/setClasses`。⚠️ 删前确认 `ssc_class_category` 表在 DB 里也无业务使用（`SELECT COUNT(*) FROM ssc_class_category` 为 0 或确认确实不用），避免误删有数据的表。

### 28.4 内存过滤改 SQL / 缓存

| 位置 | 现状 | 改法 |
|------|------|------|
| `ClassService.java:54-55` | `classRepo.findAll().stream().filter(isDeleted==0)` | `classRepo.findByIsDeleted(0)`（仓库加这个方法，其他 Service 已有同款） |
| `AdminController.java:96-97` | 批量操作里每行 `listAllForAdmin().stream().filter(id 匹配)` → O(n²) | 循环外先 `Map<Integer, ClassInfo>` 一次性构建，循环里 `map.get(app.getClassId())` O(1) |
| `CategoryService.java:30-31` | `findAll().stream().filter(...)` | 仓库加 `findByName` / `findById` 等精确查询 |

> 现实类比：`findAll()` 再在内存里 filter 像"把整本电话簿抄下来再一页页翻"，直接 SQL WHERE 像"直接按名字查"——数据量大了前者明显慢，还会拖垮 JVM 内存。

### 28.5 getClientIp 去重

`TimeController.getClientIp(HttpServletRequest)` 和 `AuthService.getClientIp()` 各写了一份（逻辑相同：X-Forwarded-For → X-Real-IP → RemoteAddr → 逗号截断）。

改法：新建 `enroll-server/src/main/java/com/enroll/server/util/IpUtil.java`：

```java
package com.enroll.server.util;

import jakarta.servlet.http.HttpServletRequest;

/** 客户端 IP 提取工具（统一处理反向代理头），避免两处重复实现 */
public final class IpUtil {
    private IpUtil() {}

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
```

两个 Controller/Service 改调 `IpUtil.getClientIp(request)`，删私有方法。

### 28.6 日志级别恢复

`application.yml` 现在 `root: OFF` + `com.enroll.server: OFF`——**所有日志全关**，出 bug 无迹可查（当初为清屏图安静）。

改法：dev 环境至少开 `com.enroll.server: INFO`（业务日志）、`root: WARN`；Hibernate SQL 可保持 OFF（看 SQL 用 `show-sql: true` 临时开）。

### 28.7 GlobalExceptionHandler 兜底文案

`handleAll(Exception ex)` 直接返回 `ex.getMessage()`——把底层异常原文（可能含 SQL/路径/类名）泄露给前端，还暴露了实现细节。

改法：兜底统一返回固定文案，堆栈只打日志：

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleAll(Exception ex) {
    // log.error("系统异常: {}", ex.getMessage(), ex);  ← 日志记录详情
    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.fail(ResultCode.SYSTEM_ERROR, "服务器开小差了，请稍后重试"));
}
```

### 28.8 生产配置补充（application-prod.yml）

现状：prod 只覆盖了 port / ddl-auto / 日志，`sms.test-mode` 没覆盖（继承 dev 的 `true`，生产会跳过真实短信）。

改法：`application-prod.yml` 加 `sms.test-mode: false`。数据库密码等敏感项**不改**——主人已明确：生产部署在堡垒机，yml 由堡垒机环境变量注入。

- [ ] `mvnw.cmd compile` 通过
- [ ] 后端重启后冒烟：首页 / 报名 / 时间接口 / 验证码登录正常
- [ ] `git add -A && git commit -m "refactor(back): 后端技术债清理（ObjectMapper/Scheduled/IP去重/日志/异常文案/prod配置）（Task 28）"`

---

## Task 29：前端技术债

### 29.1 `::v-deep` → `:deep()`

`::v-deep` 是 Vue 2 语法（现已弃用，Vite 下会告警）。全局搜 `enroll-web/src` 下 `::v-deep`，逐个替换为 `:deep(.xxx)`（`:deep` 前面要有选择器，如 `.form .el-input :deep(.xxx)`）。⚠️ 每处替换后浏览器看对应样式是否仍生效（穿透选择器最容易静默失效）。

### 29.2 `!important` 收敛

全局搜 `!important`，逐个判断：是"覆盖 Element Plus Teleport 样式"（弹窗/遮罩，必须保留）还是"自己的组件样式打架"（可以去掉，改为更精确的选择器）。目标：非弹窗场景尽量清零，弹窗场景保留并加注释说明"为什么必须"。

### 29.3 `isChengDian` 硬编码 classId 移除

`HomePage.vue:535-537`：`return cls.id === 2 || cls.id === 3 || name.includes('成电联合培养')`——把班级 ID 硬编码进前端，DB 换班就失效。

改法：`ClassInfo` 已有 `classRounds` 数组，**用"是否多轮班"判断**（`Array.isArray(classRounds) && classRounds.length > 1`）替代 ID 判断；函数改名 `isMultiRoundClass` 并让调用处统一走这个判断。若别处（FormPage 等）也有同类硬编码，一并搜 `id === 2` / `id === 3` / `成电` 清理。

### 29.4 首页轮询退避

`HomePage.vue:270` `pollTimer = setInterval(loadData, 30_000)` 固定 30 秒轮询。后端挂了会每 30 秒报一次错刷屏。

改法：失败时退避（指数），成功恢复固定间隔：

```js
let pollDelay = 30_000              // 正常间隔 30s
const MAX_POLL_DELAY = 5 * 60_000   // 最大退避 5min
async function pollLoad() {
  const ok = await loadData()       // loadData 改造成返回成功与否
  pollDelay = ok ? 30_000 : Math.min(pollDelay * 2, MAX_POLL_DELAY)
  clearInterval(pollTimer)
  pollTimer = setInterval(pollLoad, pollDelay)
}
```

- [ ] `npm run build` 通过，无 `::v-deep` 弃用告警
- [ ] 每处样式改完浏览器冒烟（穿透选择器最容易坏）
- [ ] `git add -A && git commit -m "refactor(front): 前端技术债清理（deep语法/important/硬编码/轮询退避）（Task 29）"`

---

## Task 30：测试补强

> 现有测试参考：`enroll-server/src/test/java/com/enroll/server/service/ApplicationTimeBoundaryTest.java`（JUnit）、前端 `validate.js` 的 Vitest。新测试沿用同样风格，**只测纯逻辑，不测 IO/网络**。

### 30.1 后端 Service 单测

- `ClassService` 轮次解析：输入 `classRounds` 数组 → 输出轮次/时间字符串，覆盖：空数组、单轮、多轮、异常时间格式
- `ApplicationService` 身份证校验码：`validateIdCardChecksum` 合法/非法末位（已有部分覆盖，补边界：末位 X 大小写）

### 30.2 DTO @Valid 校验测试

Phase 1/2 加了 Request DTO + `@Valid`。补一个校验测试：用 `jakarta.validation.Validator`（Bean Validation）构造非法请求体（缺字段 / 超长 / 格式错），断言校验错误列表非空、错误消息包含期望字段。参考写法：

```java
private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

@Test
void 姓名必填() {
    ClassRequestDTO dto = new ClassRequestDTO();
    dto.setName("");  // 空
    Set<ConstraintViolation<ClassRequestDTO>> violations = validator.validate(dto);
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
}
```

### 30.3 前端 composable 测试

- `data.js` 纯函数（`parsePeriod` / `formatTime` / `getClassTimeStatus` 时间边界）——这些是业务核心，优先测
- `useDialogAdapt`：依赖真实 DOM，测试需 `jsdom` 环境——Vitest 默认 node 环境，给该文件加 `// @vitest-environment jsdom` 文件级注释，测"打开后 body 被设置 max-height"；若 jsdom 测不稳，退而只测纯逻辑部分

### 30.4 api.js 测试

`request()` 用 `vi.stubGlobal('fetch', ...)` mock fetch，断言：① 无 token 不带 Authorization；② 有 token 自动带 `Bearer`；③ 非 2xx 抛错。

```js
// @vitest-environment jsdom   // ⚠️ store 读写 localStorage，node 环境没有，必须 jsdom
// api.test.js
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../stores/auth.js'

describe('request token 注入', () => {
  beforeEach(() => {
    // 注册一个隔离的 pinia，避免污染全局
    setActivePinia(createPinia())
  })
  afterEach(() => vi.unstubAllGlobals())

  it('有 token 时自动带 Authorization', async () => {
    const auth = useAuthStore()
    auth.login('test-token', '***REMOVED***')
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true, json: () => Promise.resolve({ code: 200, data: [] }),
    })
    vi.stubGlobal('fetch', mockFetch)
    const { fetchMyApplicationsMe } = await import('../utils/api.js')
    await fetchMyApplicationsMe()
    const [url, opts] = mockFetch.mock.calls[0]
    expect(opts.headers.Authorization).toBe('Bearer test-token')
  })
})
```

> ⚠️ 前端测试需要先把 `pinia` 测试桩搭好（`setActivePinia` + `createPinia`），否则 `useAuthStore()` 报 "no active Pinia"。

- [ ] `mvnw.cmd test` 全绿
- [ ] `npm run test`（vitest run）全绿
- [ ] `git add -A && git commit -m "test: 补强后端Service/DTO校验 + 前端composable/api测试（Task 30）"`

---

## Task 31：全量回归 + 收尾

### 回归清单（对照项目 CLAUDE.md §10 联调清单 + 日常功能）

- [ ] 首页：班级列表 / 排序 / 多轮展开 / 筛选 / 搜索去重 / 帮助横条 / Banner
- [ ] 报名：填表 / 实时查重 / 提交 / 验证码登录 / 手机号+身份证直填
- [ ] 我的报名：列表 / 修改（身份证只读）/ 撤回（截止时间判断）
- [ ] 管理端（dev）：登录 / 班级 CRUD / 类别 CRUD / 报名查询 / 批量录取（配额检查）
- [ ] 时间接口 `/api/time` 限流（1 秒 1 次）
- [ ] 移动端 375px 全流程走一遍

### 收尾

1. 调用 `/verification-before-completion` 技能，按清单逐项实测，日志原样贴给主人
2. `git status` 确认工作区干净，全部提交在 `refactor/architecture-cleanup` 分支
3. 把整改过程中踩的坑沉淀到工作台 `02-全局踩坑日志.md`，可复用流程进 `03-全局SOP.md`（主动问主人要不要）
4. 汇总《整改前后对照》给主人：
   - 后端：Controller 瘦身比例 / Service 职责 / DTO 校验 / 异常统一
   - 前端：Pinia 替换 localStorage 事件 / 组件拆分前后行数 / API 统一
   - 技术债：哪些清了、哪些**有意保留**（yml 硬编码密码→主人已明确堡垒机兜底，标注保留原因）

---

## ⚠️ 风险点 & 后期补救

| 风险 | 等级 | 说明 & 补救 |
|------|------|------------|
| Task 24 身份证不可改后，学生无法自行纠正身份证 typo | 🟡 中 | 改为"联系管理员/低代码平台用 admin 接口改"；若学校需要自助改，后期加"重新输入身份证+校验匹配旧值"方案（verify-by-reentry） |
| Task 22/23 弹窗样式迁移时类名错位 | 🟡 中 | 类名已固定在组件内，迁移只搬 CSS 块；每步浏览器冒烟兜底 |
| Task 25 移除 login_changed 事件后某页面漏改 | 🟡 中 | `npm run dev` 全局搜 `login_changed` / `dispatchEvent` 残留；Phase 3 验证清单覆盖 |
| Task 26 删依赖后 lockfile 不一致 | 🟢 低 | `npm i` 更新 lockfile；`npm run build` 兜底 |
| 29.1 `:deep` 替换静默失效 | 🟡 中 | 每处替换后浏览器看样式；优先改风险低的，弹窗样式可暂缓 |
| 30 测试补强依赖 Phase 1/2 重构后的方法签名 | 🟡 中 | 测试在 Phase 4 写，签名以重构后代码为准（计划里已注明方法名是"重构后"） |

> 🔧 后期补救总原则：任何"为了上线先不做"的事，都要在代码里留 TODO 注释 + 在本节登记，不靠脑子记。

---

## 附：Phase 3/4 文件清单

### 新建
| 文件 | Task |
|------|------|
| `enroll-web/src/stores/auth.js` | 17 |
| `enroll-web/src/composables/useWindowWidth.js` | 20 |
| `enroll-web/src/composables/useDialogAdapt.js` | 21 |
| `enroll-web/src/components/NoticeDialog.vue` | 22 |
| `enroll-web/src/components/GroupInfoDialog.vue` | 22 |
| `enroll-web/src/components/ClassDescDialog.vue` | 23 |
| `enroll-web/src/components/EditDialog.vue` | 24 |
| `enroll-server/src/main/java/com/enroll/server/util/IpUtil.java` | 28.5 |

### 修改
| 文件 | Task |
|------|------|
| `enroll-web/src/main.js` | 17 |
| `enroll-web/src/router/index.js` | 19 |
| `enroll-web/src/utils/api.js` | 18 |
| `enroll-web/src/views/HomePage.vue` | 20/22/25/29 |
| `enroll-web/src/components/ClassCard.vue` | 20/23 |
| `enroll-web/src/views/MyApplications.vue` | 24/25 |
| `enroll-web/src/views/StudentLogin.vue` | 25 |
| `enroll-web/src/views/FormPage.vue` | 25 |
| `enroll-web/src/components/AppHeader.vue` | 25 |
| `enroll-web/src/composables/usePhoneCode.js` | 25 |
| `enroll-web/package.json` | 17/26 |
| `enroll-server/.../service/ApplicationService.java` | 24/28.1/28.4 |
| `enroll-server/.../service/ClassService.java` | 28.1/28.4 |
| `enroll-server/.../controller/SyncController.java` | 28.1 |
| `enroll-server/.../controller/TimeController.java` | 28.2/28.5 |
| `enroll-server/.../entity/Category.java` | 28.3 |
| `enroll-server/.../exception/GlobalExceptionHandler.java` | 28.7 |
| `enroll-server/.../controller/AdminController.java` | 28.4 |
| `enroll-server/.../service/AuthService.java` | 28.5 |
| `enroll-server/src/main/resources/application.yml` | 28.6 |
| `enroll-server/src/main/resources/application-prod.yml` | 28.8 |
| `enroll-server/.../service/CategoryService.java` | 28.4 |

### 删除（需主人二次确认）
| 文件 | Task |
|------|------|
| `enroll-web/src/composables/useApplication.js` | 26 |
| `enroll-web/src/components/PieChart.vue` | 26 |
| `enroll-web/src/components/TrendChart.vue` | 26 |
| `enroll-web/package.json` 的 `mammoth`/`playwright`/`xlsx` 依赖 | 26 |
