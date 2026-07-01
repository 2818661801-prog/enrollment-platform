<!--
  HomePage.vue · 报名首页（移动端适配 + 进入时弹出报名须知）
  问卷式报名：学生扫码进入 → 弹窗须知 → 选班 → 填表 → 提交
-->
<template>
  <div class="home-page">
    <!-- ===== Banner：放在 home-body 外面，方便移动端用 order 置顶 ===== -->
    <div class="banner">
      <div class="banner-content">
        <h1>2026 特色班报名通道已开启</h1>
        <p>杭州电子科技大学信息工程学院 · {{ classes.length }} 个特色班级供你选择</p>
        <el-button type="primary" class="notice-link" @click="showNotice = true">
          <img src="@/assets/images/checklist.svg" class="icon-svg" />查看报名须知
        </el-button>
      </div>
    </div>

    <div class="home-body">
      <!-- ===== 主区 ===== -->
      <main class="home-main">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-select
            v-model="searchKeyword"
            placeholder="搜索班级名称..."
            filterable
            clearable
            size="large"
            style="width:320px"
          >
            <el-option
              v-for="c in classes"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
          <el-button size="large" @click="searchKeyword=null">重置</el-button>
          <el-button text type="primary" @click="router.push('/my-applications')" style="margin-left:auto">
            我的报名 →
          </el-button>
        </div>

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

      <!-- ===== 右侧时间轴 ===== -->
      <aside class="home-timeline">
        <h4 class="timeline-title"><img src="@/assets/images/calendar(1).svg" class="icon-svg" />报名时间轴</h4>
        <div class="timeline">
          <div v-for="(item, i) in timelineItems" :key="i" class="timeline-item">
            <div class="timeline-left">
              <div class="timeline-name" :title="item.name">
                <span v-if="item.roundLabel" class="round-badge">{{ item.roundLabel }}</span>
                {{ item.name }}
              </div>
              <div class="timeline-period">{{ item.period }}</div>
            </div>
            <el-tag :type="item.tagType" size="small" class="timeline-tag">{{ item.tagText }}</el-tag>
          </div>
        </div>
      </aside>
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
          <el-table :data="classes" border size="small">
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
import { getClassTimeStatus } from '../utils/data.js'
import { fetchClasses, fetchNotice } from '../utils/api.js'
import AppFooter from '../components/AppFooter.vue'
import ClassCard from '../components/ClassCard.vue'

const router = useRouter()
const searchKeyword = ref(null)
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

const displayClasses = computed(() => {
  if (!searchKeyword.value) return classes.value
  return classes.value.filter(c => c.id === searchKeyword.value)
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

const timelineItems = computed(() => {
  const result = []
  // 按班级 id 排序（保证同一班级的两轮相邻）
  const sorted = [...classes.value].sort((a, b) => a.id - b.id)

  for (const c of sorted) {
    const periods = parsePeriodsArray(c.periods, c.period)

    if (periods.length > 1 || isChengDian(c)) {
      // 成电班（多轮）：每轮一行
      for (const p of periods) {
        const { status, label } = getClassTimeStatus({ period: p.period })
        const roundLabel = p.round === 2 ? '第二轮' : '第一轮'
        result.push({
          name: c.name,
          period: p.period,
          roundLabel,
          active: status === 'open',
          tagType: status === 'open' ? 'success' : status === 'not_started' ? 'warning' : 'info',
          tagText: label,
        })
      }
    } else {
      // 普通班：一行
      const { status, label } = getClassTimeStatus(c)
      result.push({
        name: c.name,
        period: c.period,
        roundLabel: null,
        active: status === 'open',
        tagType: status === 'open' ? 'success' : status === 'not_started' ? 'warning' : 'info',
        tagText: label,
      })
    }
  }
  return result
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  /* padding-top: 0; */  /* header 改为普通流，banner 自然紧贴下方 */
}
/* Banner：独立元素，桌面端作为 home-page 顶部最大宽度块 */
.banner {
  max-width: 1400px;
  width: calc(100% - 68px);
  margin: 20px auto 0;
  padding: 0 0 0 0;
}
.home-body {
  display: flex;
  gap: 32px;
  max-width: 1400px;
  margin: 20px auto 0;
  padding: 0 60px 0 24px;
  flex: 1;
  width: 100%;
}
/* SVG 图标通用样式 */
.icon-svg {
  display: inline-block;
  width: 1.2em;
  height: 1.2em;
  vertical-align: middle;
  margin-right: 4px;
}

/* ===== 主区 ===== */
.home-main {
  flex: 1;
  min-width: 0;
}
.banner {
  min-height: 160px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #337eff 0%, #6366f1 100%);
  display: flex;
  align-items: center;
  padding: 28px 40px;
  margin-bottom: 0;
}
.banner-content h1 {
  color: #fff;
  font-size: 26px;
  margin-bottom: 8px;
}
.banner-content p {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-bottom: 16px;
}
.notice-link {
  background: rgba(255,255,255,0.2) !important;
  border-color: rgba(255,255,255,0.5) !important;
  color: #fff !important;
  margin-left: 12px;
  font-size: 13px;
}
.notice-link:hover {
  background: rgba(255,255,255,0.35) !important;
  border-color: #fff !important;
}
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* ===== 右侧时间轴 ===== */
.home-timeline {
  width: 260px;
  flex-shrink: 0;
  margin-left: 15px;
}
.timeline-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}
.timeline {
  position: relative;
  padding-left: 16px;
  border-left: 2px solid var(--divider);
  max-height: 420px;      /* 约7-8行，超出滚动 */
  overflow-y: auto;        /* 垂直滚动条 */
  scrollbar-width: thin;   /* Firefox 细滚动条 */
}
/* 滚动条样式（Chrome/Safari） */
.timeline::-webkit-scrollbar { width: 4px; }
.timeline::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 2px; }

/* 成电班第几轮标签 */
.round-badge {
  display: inline-block;
  font-size: 10px;
  color: #fff;
  background: #e6a23c;
  border-radius: 3px;
  padding: 0 4px;
  margin-right: 4px;
  vertical-align: middle;
  line-height: 16px;
}
/* 两行布局：上行班级名，下行报名时间，右侧状态标签（桌面端） */
.timeline-item {
  position: relative;
  margin-bottom: 16px;
  margin-left: -23px;
  padding-left: 23px;
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto;
  row-gap: 4px;
}
.timeline-dot {
  position: absolute;
  left: -6px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #c0c4cc;
  border: 2px solid #fff;
}
.timeline-dot.active {
  background: var(--success);
}
.timeline-left {
  grid-column: 1;
  grid-row: 1 / 3;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.timeline-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  white-space: normal;
  overflow: visible;
  text-overflow: unset;
  max-width: 160px;
  word-break: break-all;
}
.timeline-period {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
}
.timeline-tag {
  grid-column: 2;
  grid-row: 1 / 3;
  flex-shrink: 0;
  align-self: center;
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
  .home-timeline { width: 200px; }
  .card-grid { grid-template-columns: repeat(2, 1fr); }
  .banner { min-height: 160px; padding: 24px 28px; }
  .banner-content h1 { font-size: 22px; }
  .banner-content p { font-size: 13px; }
  .timeline-name { font-size: 11px; }
  .timeline-period { font-size: 10px; }
}

/* ==================== 响应式：手机 ==================== */
@media (max-width: 768px) {
  .home-page { padding-top: 0; }  /* header 已改为普通流，不需要占位 */
  /* Banner 已经在 home-page 顶部，自然排第一 */
  .banner {
    min-height: auto;
    padding: 12px 16px;
    margin: 12px 12px 0;
    width: auto;
  }
  .home-body {
    flex-direction: column;
    gap: 12px;
    margin-top: 12px;
    padding: 0 12px;
  }
  /* 移动端顺序：时间轴(-3) → 卡片(-1) */
  .home-timeline {
    width: 100%;
    margin-top: 0;
    margin-left: 0;
    order: -3;
  }
  .home-main {
    order: -1;
  }
  .banner-content h1 { font-size: 18px; }
  .banner-content p { font-size: 12px; margin-bottom: 12px; }
  .card-grid { grid-template-columns: 1fr; gap: 12px; }
  .notice-link { display: block; margin-left: 0; margin-top: 8px; }

  .timeline {
    display: flex;
    flex-wrap: nowrap;       /* 不换行，横向滚动 */
    gap: 8px;
    padding-left: 5px;
    border-left: none;
    border-top: 2px solid var(--divider);
    padding-top: 15px;
    padding-bottom: 15px;
    overflow-x: auto;         /* 水平滚动条 */
    -webkit-overflow-scrolling: touch;
  }
  /* 移动端：横向滚动，每列 = 班级名(上) + 时间(下) + 标签 */
  .timeline-item {
    flex-shrink: 0;
    width: 130px;            /* 每列固定宽度，避免挤在一起 */
    display: grid;
    grid-template-rows: auto auto auto;  /* 三行：班级名、时间、标签 */
    grid-template-columns: 1fr;
    row-gap: 2px;
    margin-left: 0;
    margin-bottom: 0;
    padding-left: 0;
    background: #f8fafc;
    border-radius: 6px;
    padding: 6px 4px;
  }
  .timeline-dot { display: none; }
  .timeline-left {
    width: 100%;
    grid-column: 1;
    grid-row: 1;     /* 第一行：班级名 */
  }
  .timeline-name {
    font-size: 10px;
    max-width: 100%;
    white-space: normal;
    overflow: visible;
    text-overflow: unset;
    text-align: center;
  }
  .timeline-period {
    font-size: 9px;
    grid-row: 2;     /* 第二行：时间 */
    text-align: center;
  }
  .timeline-tag {
    grid-column: 1;
    grid-row: 3;     /* 第三行：标签 */
    justify-self: center;
    margin-top: 0;
  }

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
