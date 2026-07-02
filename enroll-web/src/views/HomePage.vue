<!--
  HomePage.vue · 报名首页（V2.0 · Banner 轮播 + 筛选栏）
  流程：学生扫码进入 → 弹窗须知 → 选班 → 填表 → 提交
-->
<template>
  <div class="home-page">
    <div class="home-body">
      <!-- ===== Banner 轮播区（与主体内容共享 1200px 容器 + 边距） ===== -->
      <HeroBanner
        :classes-count="classes.length"
        :classes="classes"
        @cta-click="showNotice = true"
      />

      <!-- ===== 主区 ===== -->
      <main class="home-main">
        <!-- 筛选栏：自定义下拉 + 重置 + 登录 + 我的报名 -->
        <FilterBar
          v-model="searchKeyword"
          :classes="classes"
          :is-logged-in="isLoggedIn"
          @reset="searchKeyword = null"
        />

        <!-- 卡片 -->
        <el-empty v-if="displayClasses.length === 0" description="暂无匹配班级" />
        <div v-else class="card-grid">
          <ClassCard
            v-for="c in displayClasses"
            :key="c.id"
            :class-info="c"
            @select="goFormDirect"
          />
        </div>
      </main>
    </div>

    <!-- ===== 报名须知弹窗（进入时自动弹出） ===== -->
    <el-dialog
      v-model="showNotice"
      title="杭州电子科技大学 2026 级特色班报名须知"
      width="720px"
      :close-on-click-modal="true"
      :destroy-on-close="false"
      show-close
      class="notice-dialog"
    >
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
          <el-table :data="sortedClassesByPeriod" border size="small">
            <el-table-column label="班级名称" align="center" width="260">
              <template #default="{ row }">
                <span class="class-name-full">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column label="报名时间" align="center" min-width="180">
              <template #default="{ row }">
                <span class="period-text">{{ row.period }}</span>
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
          <el-tag type="success" size="large">✓ 已阅读并同意报名须知</el-tag>
        </div>
      </template>
    </el-dialog>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getClassTimeStatus, parsePeriod } from '../utils/data.js'
import { fetchClasses, fetchNotice } from '../utils/api.js'
import AppFooter from '../components/AppFooter.vue'
import ClassCard from '../components/ClassCard.vue'
import HeroBanner from '../components/HeroBanner.vue'
import FilterBar from '../components/FilterBar.vue'

const router = useRouter()
const searchKeyword = ref(null)
// 是否已登录（学生端 JWT）
const isLoggedIn = ref(false)
// 班级列表：从后端 API 拿
const classes = ref([])
const loading = ref(false)
const loadError = ref('')

// 报名须知弹窗
const showNotice = ref(false)
const noticeData = ref({ conditions: [], notices: [] })

// 仅首次进入弹窗（关闭浏览器标签后重开才再弹）
// 为什么用 sessionStorage：关闭标签即清除，localStorage 会永久记着
let pollTimer = null

onMounted(async () => {
  // 检测学生端登录态（有 student_token 视为已登录）
  isLoggedIn.value = !!localStorage.getItem('student_token')

  if (!sessionStorage.getItem('notice_shown')) {
    showNotice.value = true
    sessionStorage.setItem('notice_shown', '1')
  }
  await loadData()
  // 每 30 秒轮询，管理员操作后返回首页能自动看到最新数据
  pollTimer = setInterval(loadData, 30_000)
})

onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })

async function loadData() {
  try {
    const [clsRes, noticeRes] = await Promise.all([fetchClasses(), fetchNotice()])
    classes.value = clsRes
    noticeData.value = noticeRes
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
 * 班级列表按报名开始时间升序
 * 为什么排序：主人担心班级乱了，报名须知表格应按时间顺序展示
 * 用 parsePeriod 解析 period 字符串 → start Date → 升序
 */
const sortedClassesByPeriod = computed(() => {
  return [...classes.value].sort((a, b) => {
    try {
      const aStart = parsePeriod(a.period).start.getTime()
      const bStart = parsePeriod(b.period).start.getTime()
      return aStart - bStart
    } catch {
      return 0  // 解析失败保持原序
    }
  })
})

async function goFormDirect(classId) {
  const c = classes.value.find(c => c.id === classId)
  if (!c) return
  const { canApply, label } = getClassTimeStatus(c)
  if (!canApply) {
    ElMessage.warning(`「${c.name}」${label}`)
    return
  }
  showNotice.value = false
  await nextTick()
  router.push(`/form/${classId}`)
}

/**
 * 判断是否为成电班（多轮班级，id=2,3 或 name 含"成电联合培养"）
 */
function isChengDian(cls) {
  return cls.id === 2 || cls.id === 3 || (cls.name && cls.name.includes('成电联合培养'))
}

/**
 * 解析 periods JSON 字符串为数组
 * 若解析失败或为空，返回单元素数组（用 period 字段兜底）
 */
function parsePeriodsArray(periodsJson, fallbackPeriod) {
  if (!periodsJson || periodsJson.trim() === '') {
    return [{ round: 1, period: fallbackPeriod || '' }]
  }
  try {
    const arr = JSON.parse(periodsJson)
    if (Array.isArray(arr) && arr.length > 0) {
      return arr
    }
  } catch {}
  return [{ round: 1, period: fallbackPeriod || '' }]
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  /* padding-top: 0; */  /* header 改为普通流，banner 自然紧贴下方 */
}
/* ===== 主体容器 ===== */
.home-page {
  /* 整体页面背景 */
  background: #f5f6fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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
}

/* ===== Banner 下边距（web 端，移动端不动） ===== */
:deep(.hero-banner) {
  margin-bottom: 24px;
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
  .home-body {
    padding: 0 12px 24px;
    margin-top: 12px;
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
</style>

<!-- 报名须知弹窗样式（必须非 scoped，因为 el-dialog 被 teleport 到 body） -->
<style>
/* ===== 通用 ===== */
/* 表格 td/th 居中 */
.notice-dialog .el-table td,
.notice-dialog .el-table th {
  text-align: center !important;
  vertical-align: middle !important;
}
.notice-dialog .el-table .cell {
  text-align: center !important;
  justify-content: center !important;
  display: flex !important;
  align-items: center !important;
}
.notice-dialog .el-table .el-tag {
  margin: 0 auto !important;
  display: inline-flex !important;
  justify-content: center !important;
}
/* 班级全称：允许换行、居中 */
.notice-dialog .class-name-full {
  display: inline-block !important;
  white-space: normal !important;
  word-break: break-all !important;
  text-align: center !important;
  width: 100% !important;
  font-size: 12px !important;
  line-height: 1.35 !important;
}
.notice-dialog .period-text {
  display: inline-block !important;
  text-align: center !important;
  width: 100% !important;
  font-size: 11px !important;
}

/* ===== 桌面端：关闭按钮红色醒目 ===== */
.notice-dialog .el-dialog {
  border-radius: 12px;
}
.notice-dialog .el-dialog__headerbtn {
  top: 14px !important;
  right: 14px !important;
  width: 36px !important;
  height: 36px !important;
  border-radius: 50% !important;
  background: #ef4444 !important;
  transition: background 0.15s ease !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-shadow: 0 2px 6px rgba(239,68,68,0.35) !important;
}
.notice-dialog .el-dialog__headerbtn:hover {
  background: #dc2626 !important;
}
.notice-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #fff !important;
  font-size: 16px !important;
  line-height: 1 !important;
}
/* 桌面端 footer */
.notice-dialog .el-dialog__footer {
  padding: 12px 16px !important;
  flex-shrink: 0 !important;
  border-top: 1px solid #e2e8f0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: flex-end !important;
}
.notice-dialog .notice-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
}
.notice-dialog .notice-footer .el-tag {
  font-size: 13px;
}

/* ===== 移动端（max-width: 768px）===== */
@media (max-width: 768px) {
  /* el-overlay-dialog 是控制 dialog 位置的容器，对齐到顶部 */
  .notice-dialog.el-overlay-dialog {
    display: flex !important;
    align-items: flex-start !important;
    justify-content: center !important;
  }
  /* .el-dialog 是实际弹窗 — 强制覆盖 inline style width */
  .notice-dialog.el-dialog {
    width: 95vw !important;
    max-width: 355px !important;
    min-width: unset !important;
    max-height: 90vh !important;
    margin: 0 auto !important;
    border-radius: 12px !important;
    display: flex !important;
    flex-direction: column !important;
    position: relative !important;
    left: auto !important;
    top: auto !important;
    transform: none !important;
  }
  /* 头部 */
  .notice-dialog .el-dialog__header {
    padding: 8px 12px 6px !important;
    min-height: unset !important;
    position: relative !important;
    flex-shrink: 0 !important;
  }
  .notice-dialog .el-dialog__title {
    font-size: 14px !important;
    line-height: 1.4 !important;
    /* 标题超长允许换行，不截断 */
    white-space: normal !important;
    overflow: visible !important;
    text-overflow: unset !important;
    word-break: break-word !important;
    display: block !important;
    padding-right: 48px !important;  /* 给关闭按钮留空间 */
  }
  /* 关闭按钮：固定在弹窗右上角，36×36px 红色圆形 */
  .notice-dialog .el-dialog__headerbtn {
    position: absolute !important;
    top: 6px !important;
    right: 6px !important;
    width: 36px !important;
    height: 36px !important;
    border-radius: 50% !important;
    background: #ef4444 !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    box-shadow: 0 2px 8px rgba(239,68,68,0.4) !important;
    z-index: 10 !important;
  }
  .notice-dialog .el-dialog__headerbtn:hover {
    background: #dc2626 !important;
  }
  .notice-dialog .el-dialog__headerbtn .el-dialog__close {
    color: #fff !important;
    font-size: 16px !important;
    line-height: 1 !important;
  }
  /* body */
  .notice-dialog .el-dialog__body {
    padding: 6px 10px !important;
    overflow-y: auto !important;
    flex: none !important;
    max-height: calc(85vh - 102px) !important;
  }
  /* footer */
  .notice-dialog .el-dialog__footer {
    padding: 8px 12px !important;
    flex-shrink: 0 !important;
    border-top: 1px solid #e2e8f0 !important;
    min-height: 44px !important;
    display: flex !important;
    align-items: center !important;
    justify-content: flex-end !important;
    align-self: flex-end !important;
  }
  /* 表格 */
  .notice-dialog .el-table {
    width: 100% !important;
    overflow-x: auto !important;
    display: block !important;
  }
  .notice-dialog .el-table__body-wrapper {
    overflow-x: auto !important;
    display: block !important;
  }
  .notice-dialog .el-table th .cell {
    font-size: 10px !important;
    font-weight: 600 !important;
    padding: 4px 3px !important;
    line-height: 1.3 !important;
    white-space: normal !important;
    word-break: break-all !important;
  }
  .notice-dialog .el-table td .cell {
    padding: 5px 3px !important;
    font-size: 11px !important;
  }
  /* 各 section */
  .notice-dialog .nd-section {
    margin-bottom: 12px !important;
  }
  .notice-dialog .nd-section h3 {
    font-size: 13px !important;
    margin-bottom: 6px !important;
  }
  .notice-dialog .nd-section ul {
    font-size: 11px !important;
    line-height: 1.7 !important;
    padding-left: 14px !important;
  }
  .notice-dialog .el-alert {
    font-size: 11px !important;
  }
  /* 底部已同意标签 */
  .notice-dialog .notice-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    width: 100%;
  }
  .notice-dialog .notice-footer .el-tag {
    font-size: 12px;
  }
}
</style>
