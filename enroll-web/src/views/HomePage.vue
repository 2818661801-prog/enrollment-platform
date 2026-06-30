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
        <p>杭州电子科技大学信息工程学院 · 7 个特色班级任你选择</p>
        <el-button link type="primary" class="notice-link" @click="showNotice = true">
          📋 查看报名须知
        </el-button>
      </div>
    </div>

    <div class="home-body">
      <!-- ===== 左侧边栏：分类筛选 ===== -->
      <aside class="home-sidebar">
        <el-menu default-active="all" class="filter-menu" @select="onFilterSelect">
          <el-menu-item index="all">
            <el-icon><Grid /></el-icon>
            <span>全部班级（{{ classes.length }}）</span>
          </el-menu-item>
          <el-menu-item index="理工类">
            <el-icon><Cpu /></el-icon>
            <span>理工类（{{ 理工Count }}）</span>
          </el-menu-item>
          <el-menu-item index="经管类">
            <el-icon><TrendCharts /></el-icon>
            <span>经管类（{{ 经管Count }}）</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- ===== 主区（只放卡片）===== -->
      <main class="home-main">
        <el-empty v-if="filteredClasses.length === 0" description="暂无匹配班级" />
        <div v-else class="card-grid">
          <ClassCard
            v-for="c in filteredClasses"
            :key="c.id"
            :class-info="c"
            @select="goFormDirect"
          />
        </div>
      </main>

      <!-- ===== 右侧时间轴 ===== -->
      <aside class="home-timeline">
        <h4 class="timeline-title">📅 报名时间轴</h4>
        <div class="timeline">
          <div v-for="(item, i) in timelineItems" :key="i" class="timeline-item">
            <div class="timeline-left">
              <div class="timeline-name" :title="classes[i]?.name">{{ item.name }}</div>
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
      title="📋 杭州电子科技大学 2026 级特色班报名须知"
      width="720px"
      :close-on-click-modal="true"
      :destroy-on-close="false"
      show-close
      class="notice-dialog"
    >
      <!-- 一、报名条件 -->
      <section class="nd-section">
        <h3>一、报名条件</h3>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
          <template #title>以下条件需 <strong>全部满足</strong> 方可报名</template>
        </el-alert>
        <ul>
          <li>杭州电子科技大学 2026 级 <strong>全日制本科新生</strong>（已取得学籍）</li>
          <li>思想品德良好，无违纪记录</li>
          <li>部分班级要求 <strong>选考物理</strong>（见下方班级介绍）</li>
          <li>每位学生 <strong>限报 1 个</strong> 特色班</li>
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
            <el-table-column prop="category" label="类别" width="120" align="center" />
            <el-table-column label="高考科目包含\n物理/理综" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.needPhysics ? 'warning' : 'info'" size="small">
                  {{ row.needPhysics ? '需要' : '不需要' }}
                </el-tag>
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

      <!-- 三、注意事项 -->
      <section class="nd-section">
        <h3>三、注意事项</h3>
        <ul>
          <li>⏰ 请在报名时间内提交，逾期不予补报</li>
          <li>📝 信息提交后无法修改，请仔细核对</li>
          <li>🆔 身份证号将用于学籍比对，请如实填写</li>
          <li>📞 如有疑问，请联系招生办：<strong>0571-58619116</strong></li>
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
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Grid, Cpu, TrendCharts } from '@element-plus/icons-vue'
import { getClassTimeStatus } from '../utils/data.js'
import { fetchClasses } from '../utils/api.js'
import AppFooter from '../components/AppFooter.vue'
import ClassCard from '../components/ClassCard.vue'

const router = useRouter()
const filterKey = ref('all')
// 班级列表：从后端 API 拿
const classes = ref([])
const loading = ref(false)
const loadError = ref('')

// 报名须知弹窗
const showNotice = ref(false)

// 仅首次进入弹窗（关闭浏览器标签后重开才再弹）
// 为什么用 sessionStorage：关闭标签即清除，localStorage 会永久记着
onMounted(async () => {
  if (!sessionStorage.getItem('notice_shown')) {
    showNotice.value = true
    sessionStorage.setItem('notice_shown', '1')
  }
  // 从后端拉取班级列表
  loading.value = true
  try {
    classes.value = await fetchClasses()
  } catch (err) {
    loadError.value = '班级数据加载失败，请检查后端是否启动'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
})
// 监听弹窗打开 → 等 DOM 渲染完 → 强制设移动端宽度和高度
watch(showNotice, async (val) => {
  if (val) {
    // Element Plus Teleport 到 body，等几帧确保渲染
    await new Promise(resolve => setTimeout(resolve, 200))
    // notice-dialog 和 el-dialog 是同一元素（同一 DOM 节点有两个 class）
    const dialog = document.querySelector('.notice-dialog')
    if (dialog) {
      dialog.style.setProperty('width', '95vw', 'important')
      dialog.style.maxWidth = '355px'
      dialog.style.maxHeight = '85vh'
      // 强制 top=0，Element Plus 用 transform 居中，top:0 让它从顶部开始
      dialog.style.setProperty('top', '0px', 'important')
      dialog.style.setProperty('transform', 'none', 'important')
    }
  }
})

const 理工Count = computed(() => classes.value.filter(c => c.category === '理工类').length)
const 经管Count = computed(() => classes.value.filter(c => c.category === '经管类').length)

const filteredClasses = computed(() => {
  if (filterKey.value === 'all') return classes.value
  return classes.value.filter(c => c.category === filterKey.value)
})

function onFilterSelect(key) {
  filterKey.value = key
}

function goFormDirect(classId) {
  const c = classes.value.find(c => c.id === classId)
  if (!c) return
  const { canApply, label } = getClassTimeStatus(c)
  if (!canApply) {
    ElMessage.warning(`「${c.name}」${label}`)
    return
  }
  showNotice.value = false
  router.push(`/form/${classId}`)
}

const timelineItems = computed(() =>
  classes.value.map(c => {
    const { status, label } = getClassTimeStatus(c)
    return {
      name: c.name,  // 直接用真实名称（含...是名称本身字符，不是省略）
      period: c.period,
      active: status === 'open',
      tagType: status === 'open' ? 'success' : status === 'not_started' ? 'warning' : 'info',
      tagText: label,
    }
  })
)
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding-top: 36px;  /* 头部高度占位（减小白空） */
}
/* Banner：独立元素，桌面端作为 home-page 顶部最大宽度块 */
.banner {
  max-width: 1400px;
  width: calc(100% - 68px);
  margin: 4px auto 0;
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

/* ===== 左侧边栏 ===== */
.home-sidebar {
  width: 200px;
  flex-shrink: 0;
}
.filter-menu {
  border-radius: var(--radius-md);
  border: 1px solid var(--divider);
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
  color: rgba(255,255,255,0.75);
  margin-left: 12px;
  font-size: 13px;
}
.notice-link:hover {
  color: #fff;
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
  margin-left: 15px;               /* 整体左移 15px */
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
}
/* 两行布局：上行班级名，下行报名时间，右侧状态标签（桌面端） */
.timeline-item {
  position: relative;
  margin-bottom: 16px;
  margin-left: -23px;
  padding-left: 23px;
  display: grid;
  grid-template-columns: 1fr auto;   /* 左：班级名+时间（两行） | 右：标签 */
  grid-template-rows: auto auto;       /* 上：班级名 | 下：时间 */
  row-gap: 4px;                        /* 上下行间距 */
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
  grid-row: 1 / 3;   /* 占据上下两行，作为两行内容的容器 */
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
  /* 两行布局：班级名完整显示，换行不截断 */
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
  grid-row: 1 / 3;   /* 右侧标签占据两行 */
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
  .home-page { padding-top: 52px; }  /* 移动端头部高度（index.html） */
  /* Banner 已经在 home-page 顶部，自然排第一 */
  .banner {
    min-height: auto;
    padding: 20px 16px;
    margin: 0 12px 0;
    width: auto;
  }
  .home-body {
    flex-direction: column;
    gap: 12px;
    margin-top: 12px;
    padding: 0 12px;
  }
  /* 移动端顺序：时间轴(-3) → 筛选(-2) → 卡片(-1) */
  .home-timeline {
    width: 100%;
    margin-top: 0;
    margin-left: 0;           /* 左对齐，与 banner 平齐 */
    order: -3;
  }
  /* 筛选：放卡片左边（卡片左侧的横向滚动条） */
  .home-sidebar {
    width: 100%;
    order: -2;
  }
  .home-main {
    order: -1;
  }
  .filter-menu :deep(.el-menu) {
    display: flex;
    overflow-x: auto;
    white-space: nowrap;
    border-bottom: 1px solid var(--divider);
  }
  .filter-menu :deep(.el-menu-item) {
    flex-shrink: 0;
    min-width: auto;
    padding: 0 14px;
    font-size: 13px;
  }
  .banner-content h1 { font-size: 18px; }
  .banner-content p { font-size: 12px; margin-bottom: 12px; }
  .card-grid { grid-template-columns: 1fr; gap: 12px; }
  .notice-link { display: block; margin-left: 0; margin-top: 8px; }

  .timeline {
    display: flex;
    flex-wrap: nowrap;       /* 不换行，横向滚动 */
    gap: 8px;
    padding-left: 5px;        /* 左移5px，和左侧边缘留5px间距 */
    border-left: none;
    border-top: 2px solid var(--divider);
    padding-top: 10px;
    overflow-x: auto;         /* 水平滚动条 */
    -webkit-overflow-scrolling: touch;
  }
  /* 移动端：横向滚动，每列 = 班级名(上) + 时间(下) + 标签 */
  .timeline-item {
    flex-shrink: 0;
    width: 100px;            /* 每列固定宽度，避免挤在一起 */
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
  /* el-overlay 是弹窗的视口层，对齐到顶部附近 */
  .notice-dialog.el-overlay {
    display: flex !important;
    align-items: flex-start !important;
    justify-content: center !important;
    padding-top: 5vh !important;
  }
  /* .el-dialog 是实际弹窗 — 强制覆盖 inline style width */
  .notice-dialog .el-dialog {
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
    flex: 1 1 auto !important;
    max-height: calc(90vh - 90px) !important;
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
