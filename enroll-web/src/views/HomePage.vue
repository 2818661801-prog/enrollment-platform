<!--
  HomePage.vue · 报名首页（V2.0 · Banner 轮播 + 筛选栏）
  流程：学生扫码进入 → 弹窗须知 → 选班 → 填表 → 提交
-->
<template>
  <div class="home-page">
    <!-- ===== Banner + 帮助提示横条（放在同一容器，保证左右对齐） ===== -->
    <div class="banner-hint-container">
      <div class="banner-fullscreen-wrapper">
        <HeroBanner
          :server-year="serverYear"
          :classes-count="classes.length"
          :classes="classes"
          @cta-click="showNotice = true"
          @consult-click="showGroupInfo = true"
        />
      </div>

      <!-- ===== 帮助提示横条 ===== -->
      <div v-if="contactInfo" class="hint-bar-wrapper">
        <div class="help-hint-bar">
          <InfoFilled class="hint-icon" />
          <span>{{ contactInfo }}</span>
        </div>
      </div>
    </div>

    <div class="home-body">
      <!-- ===== 主区 ===== -->
      <main class="home-main">
        <!-- 筛选栏：自定义下拉 + 重置 + 登录 + 我的报名 -->
        <FilterBar
          v-model="searchKeyword"
          :selectedRound="selectedRound"
          :selectedTimeStatus="selectedTimeStatus"
          :classes="classes"
          :is-logged-in="isLoggedIn"
          @update:selectedRound="selectedRound = $event"
          @update:selectedTimeStatus="selectedTimeStatus = $event"
          @reset="searchKeyword = null"
          @logout="onLogout"
        />

        <!-- 卡片 -->
        <el-empty v-if="flatCards.length === 0" description="暂无匹配班级" />
        <div v-else class="card-grid">
          <ClassCard
            v-for="c in flatCards"
            :key="c._uid"
            :class-info="c"
            :is-applied="appliedClassIds[c.id]"
            :is-admitted="admittedClassIds[c.id]"
            :is-logged-in="isLoggedIn"
            @select="goFormDirect"
          />
        </div>
      </main>
    </div>

    <!-- ===== 报名须知弹窗（进入时自动弹出） ===== -->
    <el-dialog
      v-model="showNotice"
      :title="noticeData.title || '杭州电子科技大学 2026 级特色班报名须知'"
      width="720px"
      :close-on-click-modal="true"
      :destroy-on-close="false"
      class="notice-dialog"
    >
      <template #header>
        <span class="notice-dialog-title">{{ noticeData.title || '杭州电子科技大学 2026 级特色班报名须知' }}</span>
        <button class="notice-dialog-close" @click="showNotice = false" type="button" aria-label="关闭">
          <el-icon :size="18"><Close /></el-icon>
        </button>
      </template>
      <!-- 一、报名条件（从 sys_config 动态加载） -->
      <section class="nd-section">
        <h3>一、报名条件</h3>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
          <template #title>以下条件需 <strong>全部满足</strong> 方可报名</template>
        </el-alert>
        <ul v-if="noticeData.conditions && noticeData.conditions.length">
          <li v-for="(c, i) in noticeData.conditions" :key="i" v-html="c" />
        </ul>
        <ul v-else>
          <li>加载报名条件失败，请刷新页面</li>
        </ul>
      </section>

      <!-- 二、班级介绍 -->
      <section class="nd-section">
        <h3>二、特色班简介</h3>
        <div class="nd-table-wrap">
          <el-table :data="flatTableData" border size="small">
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

      <!-- 三、注意事项（从 sys_config 动态加载） -->
      <section class="nd-section">
        <h3>三、注意事项</h3>
        <ul v-if="noticeData.notices && noticeData.notices.length">
          <li v-for="(n, i) in noticeData.notices" :key="i" v-html="n" />
        </ul>
        <ul v-else>
          <li>暂无注意事项</li>
        </ul>
      </section>

      <template #footer>
        <div class="notice-footer">
          <el-button type="primary" size="large" @click="onAgreeNotice">我已知晓并同意</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 咨询方式弹窗 ===== -->
    <el-dialog
      v-model="showGroupInfo"
      title="咨询方式"
      width="500px"
      :close-on-click-modal="true"
      :destroy-on-close="false"
      class="group-info-dialog"
    >
      <template #header>
        <span class="group-info-dialog-title">咨询方式</span>
        <button class="group-info-dialog-close" @click="showGroupInfo = false" type="button" aria-label="关闭">
          <el-icon :size="18"><Close /></el-icon>
        </button>
      </template>
      <div class="nd-section" v-if="classesWithGroupInfo.length > 0">
        <div
          v-for="cls in classesWithGroupInfo"
          :key="cls.id"
          class="group-info-card"
        >
          <div class="group-info-card-header">{{ cls.name }}</div>
          <div class="group-info-card-body">{{ cls.groupInfo }}</div>
        </div>
      </div>
      <div v-else class="group-info-empty">
        <span>暂无信息</span>
      </div>
    </el-dialog>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, InfoFilled } from '@element-plus/icons-vue'
import { getClassTimeStatus, parsePeriod, syncServerTime, formatTime } from '../utils/data.js'
import { fetchClasses, fetchNotice, fetchMyApplicationsMe, fetchServerYear } from '../utils/api.js'
import AppFooter from '../components/AppFooter.vue'
import ClassCard from '../components/ClassCard.vue'
import HeroBanner from '../components/HeroBanner.vue'
import FilterBar from '../components/FilterBar.vue'

const router = useRouter()
const searchKeyword = ref(null)
const selectedRound = ref(null)  // null=全部轮次，数字=只看第N轮
const selectedTimeStatus = ref(null)  // null=全部状态，open/not_started/closed
// 是否已登录（学生端 JWT）
const isLoggedIn = ref(false)
// 服务器年份（用于 HeroBanner 动态标题，获取失败则用本地）
const serverYear = ref(new Date().getFullYear())
// 班级列表：从后端 API 拿
const classes = ref([])
const loading = ref(false)
const loadError = ref('')

// 报名须知弹窗
const showNotice = ref(false)
const noticeData = ref({ conditions: [], notices: [] })

// 咨询方式弹窗
const showGroupInfo = ref(false)
const contactInfo = ref('')

// 有咨询信息的班级
const classesWithGroupInfo = computed(() =>
  classes.value.filter(c => c.groupInfo && c.groupInfo.trim() !== '')
)

// 已报名的班级 ID（status=1/4，不含撤回）
const appliedClassIds = reactive({})
// 已录取的班级 ID（status=3，单独显示黄色"已录取"）
const admittedClassIds = reactive({})

// 仅首次进入弹窗（关闭浏览器标签后重开才再弹）
// 为什么用 sessionStorage：关闭标签即清除，localStorage 会永久记着
let pollTimer = null
// storage 事件处理器（需存引用才能在 unmount 时正确移除）
const onStorageChange = (e) => { if (e.key === 'application_changed') loadData() }
const onAppChanged = () => loadData()
const onLoginChanged = () => loadData()

onMounted(async () => {
  // 检测学生端登录态（有 student_token 视为已登录）
  isLoggedIn.value = !!localStorage.getItem('student_token')

  // 先同步服务器时间（解决浏览器本地时间可被篡改的问题）
  try {
    await syncServerTime()
  } catch (e) {
    console.warn('[HomePage] 服务器时间同步失败，使用本地时间：', e)
  }

  // 再加载数据（loadData 会填充 appliedClassIds）
  await loadData()

  // 读取 contactInfo（loadData 里已调用 fetchNotice，noticeData 已有值）
  if (noticeData.value.contactInfo) {
    contactInfo.value = noticeData.value.contactInfo
  }

  // 报名须知弹窗判断（loadData 后执行，因为要用 appliedClassIds）：
  // 1. 已登录 + 有报名记录 → 不弹（已报过，肯定看过）
  // 2. 已登录 + 无报名记录 + localStorage 有同意记录 → 不弹（曾经同意过）
  // 3. 已登录 + 无报名记录 + localStorage 无记录 → 弹
  // 4. 未登录 → 弹（每次都弹）
  const hasAgreed = localStorage.getItem('student_notice_agreed')
  const hasApps = Object.keys(appliedClassIds).length > 0
  if (isLoggedIn.value && hasApps) {
    // 情况1：不弹
  } else if (!isLoggedIn.value || !hasAgreed) {
    // 情况3、4：弹
    showNotice.value = true
    sessionStorage.setItem('notice_shown', '1')
  }

  // 每 30 秒轮询，管理员操作后返回首页能自动看到最新数据
  pollTimer = setInterval(loadData, 30_000)

  // 监听其他页面报名变化，刷新已报名状态（storage 事件跨标签页，自定义事件同标签页）
  window.addEventListener('storage', onStorageChange)
  window.addEventListener('application_changed', onAppChanged)
  window.addEventListener('login_changed', onLoginChanged)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  window.removeEventListener('storage', onStorageChange)
  window.removeEventListener('application_changed', onAppChanged)
  window.removeEventListener('login_changed', onLoginChanged)
})

async function loadData() {
  try {
    const [clsRes, noticeRes] = await Promise.all([fetchClasses(), fetchNotice()])
    classes.value = clsRes
    noticeData.value = {
      ...noticeRes,
      conditions: (noticeRes.conditions || '').split('\n').filter(l => l.trim()),
      notices:    (noticeRes.notices    || '').split('\n').filter(l => l.trim()),
    }
    // 无论登录/退出，先清空旧数据（退出登录后 token 没了，if 被跳过导致残留）
    Object.keys(appliedClassIds).forEach(k => delete appliedClassIds[k])
    Object.keys(admittedClassIds).forEach(k => delete admittedClassIds[k])
    // 已登录时加载我的报名记录，标记已报名的班级（只要有记录就不让再报）
    if (localStorage.getItem('student_token')) {
      const myApps = await fetchMyApplicationsMe()
      myApps.forEach(a => {
        const key = String(a.classId)
        if (a.status === '1' || a.status === '3') appliedClassIds[key] = true
        if (a.status === '3') admittedClassIds[key] = true
      })
    }
  } catch (err) {
    loadError.value = '班级数据加载失败，请检查后端是否启动'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

// 监听弹窗打开 → 设备自适应设宽度和高度
watch(showNotice, async (val) => {
  if (val) {
    // Element Plus Teleport 到 body，等几帧确保渲染
    await new Promise(resolve => setTimeout(resolve, 200))
    const dialog = document.querySelector('.notice-dialog')
    if (!dialog) return

    const isMobile = window.innerWidth < 768

    if (isMobile) {
      // ===== 移动端：dialog 固定宽度，body 弹性滚动 =====
      dialog.style.setProperty('width', '95vw', 'important')
      dialog.style.maxWidth = '355px'
      dialog.style.maxHeight = '88vh'
      dialog.style.setProperty('top', 'auto', 'important')
      dialog.style.setProperty('transform', 'none', 'important')
      // overlay-dialog 顶部留 5vh 间距
      const overlayDialog = document.querySelector('.el-overlay-dialog')
      if (overlayDialog) {
        overlayDialog.style.setProperty('align-items', 'flex-start', 'important')
        overlayDialog.style.setProperty('justify-content', 'center', 'important')
        overlayDialog.style.setProperty('display', 'flex', 'important')
        overlayDialog.style.setProperty('padding-top', '5vh', 'important')
      }
      // body：固定 max-height = 剩余空间，overflow-y auto
      await new Promise(resolve => setTimeout(resolve, 50))
      const headerH = dialog.querySelector('.el-dialog__header')?.getBoundingClientRect().height ?? 53
      const footerH = dialog.querySelector('.el-dialog__footer')?.getBoundingClientRect().height ?? 49
      const maxBodyH = Math.floor(88 * window.innerHeight / 100 - headerH - footerH - 8)
      const body = dialog.querySelector('.el-dialog__body')
      if (body) {
        body.style.setProperty('max-height', maxBodyH + 'px', 'important')
        body.style.overflowY = 'auto'
      }
    } else {
      // ===== PC 端：居中，max-width 720px，body 内部滚动 =====
      dialog.style.maxWidth = '720px'
      // body 固定 max-height = 85vh - header - footer - buffer
      await new Promise(resolve => setTimeout(resolve, 50))
      const headerH = dialog.querySelector('.el-dialog__header')?.getBoundingClientRect().height ?? 60
      const footerH = dialog.querySelector('.el-dialog__footer')?.getBoundingClientRect().height ?? 49
      const maxBodyH = Math.floor(85 * window.innerHeight / 100 - headerH - footerH - 8)
      const body = dialog.querySelector('.el-dialog__body')
      if (body) {
        body.style.setProperty('max-height', maxBodyH + 'px', 'important')
        body.style.overflowY = 'auto'
      }
    }
  }
})

// 监听咨询方式弹窗打开 → 屏幕适配（与 notice-dialog 相同逻辑）
watch(showGroupInfo, async (val) => {
  if (val) {
    await new Promise(resolve => setTimeout(resolve, 200))
    const dialog = document.querySelector('.group-info-dialog')
    if (!dialog) return

    const isMobile = window.innerWidth < 768

    if (isMobile) {
      dialog.style.setProperty('width', '95vw', 'important')
      dialog.style.maxWidth = '355px'
      dialog.style.maxHeight = '88vh'
      dialog.style.setProperty('top', 'auto', 'important')
      dialog.style.setProperty('transform', 'none', 'important')
      const overlayDialog = document.querySelector('.el-overlay-dialog')
      if (overlayDialog) {
        overlayDialog.style.setProperty('align-items', 'flex-start', 'important')
        overlayDialog.style.setProperty('justify-content', 'center', 'important')
        overlayDialog.style.setProperty('display', 'flex', 'important')
        overlayDialog.style.setProperty('padding-top', '5vh', 'important')
      }
      await new Promise(resolve => setTimeout(resolve, 50))
      const headerH = dialog.querySelector('.el-dialog__header')?.getBoundingClientRect().height ?? 53
      const maxBodyH = Math.floor(88 * window.innerHeight / 100 - headerH - 8)
      const body = dialog.querySelector('.el-dialog__body')
      if (body) {
        body.style.setProperty('max-height', maxBodyH + 'px', 'important')
        body.style.overflowY = 'auto'
      }
    } else {
      dialog.style.maxWidth = '500px'
      await new Promise(resolve => setTimeout(resolve, 50))
      const headerH = dialog.querySelector('.el-dialog__header')?.getBoundingClientRect().height ?? 60
      const maxBodyH = Math.floor(85 * window.innerHeight / 100 - headerH - 8)
      const body = dialog.querySelector('.el-dialog__body')
      if (body) {
        body.style.setProperty('max-height', maxBodyH + 'px', 'important')
        body.style.overflowY = 'auto'
      }
    }
  }
})

/**
 * 卡片列表：先按 searchKeyword 过滤，再按报名开始时间升序
 * 为什么排序：主人要求"按时间最早排前面"
 */
const displayClasses = computed(() => {
  // 1. 过滤（保留原逻辑）
  const filtered = searchKeyword.value
    ? classes.value.filter(c => c.id === searchKeyword.value)
    : classes.value

  // 2. 按报名开始时间升序排序
  return [...filtered].sort((a, b) => {
    try {
      const aStart = parsePeriod(a.period).start.getTime()
      const bStart = parsePeriod(b.period).start.getTime()
      return aStart - bStart
    } catch {
      return 0  // 解析失败保持原序
    }
  })
})

/**
 * 扁平卡片列表：每个班 × 每轮 = 一张卡片
 * 成电班（id=2,3）显示两张（第1轮 + 第2轮），其他班显示一张
 * _uid 格式："classId-round"，保证 key 唯一
 */
const flatCards = computed(() => {
  let result = []
  for (const cls of displayClasses.value) {
    const rounds = parsePeriodsArray(cls.classRounds, cls.period)
    for (const r of rounds) {
      result.push({ ...cls, _round: r.round, _period: r.period, _uid: `${cls.id}-${r.round}` })
    }
  }
  // 轮次筛选：selectedRound 为 null 时显示全部
  if (selectedRound.value != null) {
    result = result.filter(c => c._round === selectedRound.value)
  }
  // 时间状态筛选：selectedTimeStatus 为 null 时显示全部
  if (selectedTimeStatus.value != null) {
    result = result.filter(c => {
      const s = getClassTimeStatus({ period: c._period }).status
      return s === selectedTimeStatus.value
    })
  }
  // 排序：可报名/已报名/已录取 > 未开始 > 已截止，同组内按报名开始时间升序
  return result.sort((a, b) => {
    try {
      const aStatus = getClassTimeStatus({ period: a._period })
      const bStatus = getClassTimeStatus({ period: b._period })
      const aOpen = aStatus.canApply || appliedClassIds[a.id] || admittedClassIds[a.id]
      const bOpen = bStatus.canApply || appliedClassIds[b.id] || admittedClassIds[b.id]
      // 优先级：可报名(0) > 未开始(1) > 已截止(2)
      const priorityA = aOpen ? 0 : aStatus.status === 'not_started' ? 1 : 2
      const priorityB = bOpen ? 0 : bStatus.status === 'not_started' ? 1 : 2
      if (priorityA !== priorityB) return priorityA - priorityB
      const aStart = parsePeriod(a._period).start.getTime()
      const bStart = parsePeriod(b._period).start.getTime()
      return aStart - bStart
    } catch {
      return 0
    }
  })
})

/**
 * 表格数据源：每个班 × 每轮 = 一行（与 flatCards 逻辑一致，保证多轮班的多轮时间都能展示）
 * 按报名开始时间升序排列
 */
const flatTableData = computed(() => {
  const result = []
  for (const cls of classes.value) {
    const rounds = parsePeriodsArray(cls.classRounds, cls.period)
    for (const r of rounds) {
      // 多轮班班级名加"（第X轮报名）"后缀，与 ClassCard.cardTitle 逻辑一致
      const name = rounds.length > 1 ? `${cls.name}（第${r.round}轮报名）` : cls.name
      result.push({ ...cls, _name: name, _period: r.period, _round: r.round })
    }
  }
  // 按报名开始时间升序
  return result.sort((a, b) => {
    try {
      const aStart = parsePeriod(a._period).start.getTime()
      const bStart = parsePeriod(b._period).start.getTime()
      return aStart - bStart
    } catch {
      return 0
    }
  })
})

function onLogout() {
  isLoggedIn.value = false
  Object.keys(appliedClassIds).forEach(k => delete appliedClassIds[k])
  Object.keys(admittedClassIds).forEach(k => delete admittedClassIds[k])
  ElMessage.success('已退出登录')
}

function onAgreeNotice() {
  // 记录同意时间戳，后续报名提交时后端写入 notice_agreed=1
  localStorage.setItem('student_notice_agreed', String(Date.now()))
  showNotice.value = false
}

async function goFormDirect({ id, period }) {
  const c = classes.value.find(c => c.id === id)
  if (!c) return
  // 用该轮的实际 period 判断，而非 cls.period（第一轮的）
  const { canApply, label } = getClassTimeStatus({ period })
  if (!canApply) {
    ElMessage.warning(`「${c.name}」${label}`)
    return
  }
  showNotice.value = false
  await nextTick()
  router.push(`/form/${id}?period=${encodeURIComponent(period)}`)
}

/**
 * 判断是否为成电班（多轮班级，id=2,3 或 name 含"成电联合培养"）
 */
function isChengDian(cls) {
  return cls.id === 2 || cls.id === 3 || (cls.name && cls.name.includes('成电联合培养'))
}

/**
 * 从 classRounds 数组解析出轮次信息（替代旧的 periods JSON 解析）
 * classRounds 格式：[{roundNum: 1, periodStart: "2026/09/01 08:00", periodEnd: "2026/09/13 23:59"}]
 * 返回格式：[{round: 1, period: "2026/09/01 08:00 - 2026/09/13 23:59"}]
 */
function parsePeriodsArray(classRounds, fallbackPeriod) {
  if (!classRounds || !Array.isArray(classRounds) || classRounds.length === 0) {
    return [{ round: 1, period: fallbackPeriod || '' }]
  }
  return classRounds.map(r => ({
    round: r.roundNum,
    period: `${formatTime(r.periodStart)} - ${formatTime(r.periodEnd)}`
  }))
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding-top: 56px;   /* fixed header 高度 56px，防止内容被遮挡 */
}
/* ===== 主体容器 ===== */
.home-page {
  /* 整体页面背景 */
  background: #f5f6fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== Banner + 帮助提示横条统一容器 ===== */
.banner-hint-container {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 0 16px;
  box-sizing: border-box;
  flex-shrink: 0;
  margin-top: 16px;          /* 和 header 的间距 */
  display: flex;
  flex-direction: column;    /* banner 在上，横条在下 */
}

/* ===== Banner wrapper ===== */
.banner-fullscreen-wrapper {
  width: 100%;
  overflow: hidden;
  flex-shrink: 0;
}
/* hero-banner 宽度 = wrapper（1200px），完全对齐下方模块 */
:deep(.hero-banner) {
  width: 100%;
  border-radius: 14px;       /* 恢复圆角（wrapper 的 overflow:hidden 裁圆角） */
}

/* ===== 帮助提示横条 ===== */
.hint-bar-wrapper {
  width: 100%;
  margin-top: 12px;          /* 与 banner 的间距 */
}
.help-hint-bar {
  width: 100%;
  padding: 6px 20px;
  background: #ffffff;
  border: 2px solid #c7d2fe;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  color: #0369a1;
  line-height: 1.4;
  box-sizing: border-box;
}
.help-hint-bar .hint-icon {
  width: 14px;
  height: 14px;
  color: #0284c7;
  flex-shrink: 0;
  line-height: 1;
}
.help-hint-bar strong {
  color: #0369a1;
  font-weight: 700;
}

/* Banner + 主体内容统一包在 home-body 里，最大宽度 1200px 居中 */
.home-body {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 16px 16px 32px;             /* 上下左右都留 16px 间距 */
  flex: 1;
}

/* ===== 主区 ===== */
.home-main {
  flex: 1;
  min-width: 0;
}

/* ===== 卡片网格 ===== */
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 16px;
  align-items: stretch;            /* 同行卡片等高 */
}

/* ===== 响应式：手机 ===== */
@media (max-width: 768px) {
  .banner-hint-container {
    margin-top: 12px;
    padding: 8px 12px;
  }
  .hint-bar-wrapper {
    margin-top: 10px;
  }
  .help-hint-bar {
    padding: 8px 12px;
    font-size: 12px;
    border-radius: 6px;
    justify-content: flex-start;
    flex-wrap: nowrap;
    gap: 6px;
  }
  .help-hint-bar .hint-icon {
    width: 13px;
    height: 13px;
  }
}

/* ===== 报名须知弹窗样式 ===== */
.nd-section {
  margin-bottom: 20px;
}
.nd-section h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid var(--brand-primary);
}
.nd-section ul {
  padding-left: 20px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 2;
}
.nd-section ul li {
  margin-bottom: 2px;
}
.nd-section ul li strong {
  color: var(--text-primary);
}
.nd-table-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

/* ==================== 响应式：平板 ==================== */
@media (max-width: 1100px) {
  .card-grid { grid-template-columns: repeat(2, 1fr); }
}

/* ==================== 响应式：手机 ==================== */
@media (max-width: 768px) {
  .home-page {
    padding-top: 52px;   /* 移动端 header 52px */
  }
  :deep(.hero-banner) {
    border-radius: 10px;
  }
  .home-body {
    padding: 0 12px 24px;
    margin-top: 0;
  }
  .card-grid { grid-template-columns: 1fr; gap: 12px; }

  .nd-section h3 {
    font-size: 14px;
  }
  .nd-section ul {
    font-size: 12px;
    line-height: 1.8;
    padding-left: 14px;
  }
}

/* ===== 咨询方式弹窗样式 ===== */
.group-info-dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.group-info-dialog-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  color: #909399;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}
.group-info-dialog-close:hover {
  color: #409eff;
  background: #f0f9ff;
}

.group-info-card {
  margin-bottom: 16px;
}
.group-info-card:last-child {
  margin-bottom: 0;
}
.group-info-card-header {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid var(--brand-primary);
}
.group-info-card-body {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
  padding-left: 11px;
}
.group-info-empty {
  text-align: center;
  color: #909399;
  font-size: 14px;
  padding: 32px 0;
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  .group-info-card-header {
    font-size: 14px;
  }
  .group-info-card-body {
    font-size: 12px;
  }
}
</style>

<!-- 报名须知弹窗样式（必须非 scoped，因为 el-dialog 被 teleport 到 body） -->
<style>
/* ===== 通用样式优化 ===== */
.notice-dialog .el-table {
  font-size: 13px;
}
.notice-dialog .el-table td,
.notice-dialog .el-table th {
  padding: 8px 0 !important;
}

/* 核心修复：班级名称自动换行 */
.notice-dialog .class-name-cell {
  display: inline-block;
  white-space: normal !important;
  word-break: break-word !important;
  line-height: 1.4;
  text-align: center;
  color: #303133;
}

/* 报名时间样式 */
.notice-dialog .period-text {
  color: #606266;
  font-size: 12px;
  white-space: normal;
  word-break: break-word;
}

/* 标题样式微调 */
.notice-dialog .nd-section h3 {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  border-left: 4px solid #409eff;
  padding-left: 8px;
  line-height: 1.2;
}

/* 底部按钮区 */
.notice-dialog .notice-footer {
  display: flex;
  justify-content: center;
  width: 100%;
}

/* ===== 自定义头部 + 关闭按钮 ===== */
/* 隐藏 el-dialog 原生头部（用自定义 header slot 替代） */
.notice-dialog .el-dialog__header {
  padding: 14px 16px 10px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
}
.notice-dialog .el-dialog__headerbtn {
  display: none !important;  /* 隐藏原生关闭按钮 */
}
.notice-dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.notice-dialog-close {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.08);
  color: #606266;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, transform 0.15s;
}
.notice-dialog-close:hover {
  background: rgba(0, 0, 0, 0.2);
  color: #303133;
  transform: scale(1.1);
}
.notice-dialog-close:active {
  transform: scale(0.92);
}

/* ===== 移动端适配 (Max-width: 768px) ===== */
@media (max-width: 768px) {
  .notice-dialog.el-dialog {
    width: 96% !important;
    max-width: 400px !important;
    margin-top: 5vh !important;
    max-height: 85vh !important;
    display: flex !important;
    flex-direction: column !important;
  }

  .notice-dialog .el-dialog__body {
    padding: 12px 15px !important;
    overflow-y: auto !important;
    flex: 1;
  }

  .notice-dialog .el-table th > .cell,
  .notice-dialog .el-table td > .cell {
    font-size: 12px !important;
    padding: 0 4px !important;
  }

  .notice-dialog .class-name-cell {
    font-size: 12px !important;
  }

  .notice-dialog .nd-section h3 {
    font-size: 14px !important;
  }

  .notice-dialog .el-alert__title {
    font-size: 12px !important;
  }
}

/* ===== 咨询方式弹窗非 scoped 样式 ===== */
.group-info-dialog .el-dialog__header {
  padding: 14px 16px 10px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
}
.group-info-dialog .el-dialog__headerbtn {
  display: none !important;
}
.group-info-dialog .el-dialog__body {
  padding: 12px 16px !important;
  overflow-y: auto !important;
}
.group-info-dialog.el-dialog {
  max-width: 500px !important;
  width: 90% !important;
}

/* ===== 移动端咨询方式弹窗 ===== */
@media (max-width: 768px) {
  .group-info-dialog.el-dialog {
    width: 96% !important;
    max-width: 355px !important;
    margin-top: 5vh !important;
    max-height: 85vh !important;
    display: flex !important;
    flex-direction: column !important;
  }
  .group-info-dialog .el-dialog__body {
    padding: 12px 15px !important;
  }
}
</style>

