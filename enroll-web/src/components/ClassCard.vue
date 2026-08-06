<!--
  ClassCard.vue · 特色班卡片
  设计方向：「录取通知书的剪影 / Engineering Specification Card」
  4px 顶部色条 + 标题下 1px 分割线 + 直角 → 像从招生章程里剪下的一节
  主题色：#337eff（沿用 themeCss.json --navbar-background）
-->
<template>
  <article
    ref="cardRef"
    class="class-card"
    :class="{ 'is-disabled': !timeStatus.canApply, 'is-single': !isMultiRound, 'is-multi': isMultiRound, 'is-visible': isVisible }"
    @click="onClick"
  >
    <!-- 顶部色条（签名元素：从招生章程里剪下来的视觉特征） -->
    <div class="card-accent-bar" />

    <div class="card-body">
      <!-- 内容区：grid 布局，保证所有行 Y 轴固定对齐 -->
      <div class="card-content">
        <!-- Row1: 标题行 -->
        <div class="card-head">
          <h3 class="card-name" :title="cardTitle">{{ cardTitle }}</h3>
          <button
            v-if="classInfo.description"
            type="button"
            class="desc-btn"
            :title="`查看${cardTitle}的班级介绍`"
            @click.stop="showDescription"
          >
            <img :src="descIcon" alt="班级介绍" class="desc-icon" />
            <span class="desc-btn-text">介绍</span>
          </button>
        </div>

        <!-- Row2: 分割线（永远在同一 Y） -->
        <div class="card-rule" />

        <!-- Row4: 报名时间段（flex-grow 填满剩余空间） -->
        <div class="card-periods">
          <div v-for="(r, idx) in visibleRounds" :key="r.round" class="card-period-row">
            <img :src="calendarIcon" alt="" class="period-icon" aria-hidden="true" />
            <span v-if="isMultiRound" class="round-label">第{{ r.round }}轮</span>
            <el-tooltip :content="`报名时间：${r.period}`" placement="top">
              <span class="period-text">{{ r.period }}</span>
            </el-tooltip>
            <span v-if="isMultiRound" class="round-status" :class="`round-status--${roundStatusList[idx].status}`">
              {{ roundStatusList[idx].label }}
            </span>
            <button v-if="isMultiRound && idx === visibleRounds.length - 1" type="button" class="expand-btn" @click.stop="toggleExpand">
              <svg class="expand-arrow" :class="{ 'is-expanded': isExpanded }" width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M2 4L6 8L10 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 报名按钮 -->
      <button
        type="button"
        class="card-btn"
        :class="btnClass"
        :disabled="!timeStatus.canApply || isApplied || isAdmitted"
        @click.stop="onClick"
      >
        {{ buttonText }}
      </button>
    </div>

    <!-- 班级介绍弹窗：响应式 width，PC=500px，移动端=calc(100vw-40px) -->
    <el-dialog
      v-model="dialogVisible"
      class="desc-dialog"
      :width="dialogWidth"
      :show-close="false"
      :lock-scroll="windowWidth > 768"
      append-to-body
      destroy-on-close
    >
      <!-- 自定义头部区域 -->
      <div class="custom-header">
        <div class="header-content">
          <img :src="descIcon" alt="" class="custom-icon" />
          <div class="header-text">
            <div class="header-title" :title="cardTitle">{{ cardTitle }}</div>
          </div>
        </div>
        <button class="custom-close-btn" @click="dialogVisible = false">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <!-- 统一内容容器 -->
      <div class="dialog-inner">
        <span class="desc-tag">班级介绍</span>

        <div class="desc-dialog-body">
          <p class="desc-dialog-text">{{ classInfo.description }}</p>
        </div>

        <div class="desc-dialog-footer">
          <button class="desc-dialog-confirm-btn" @click="dialogVisible = false">我知道了</button>
        </div>
      </div>
    </el-dialog>
  </article>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import calendarIcon from '../assets/images/calendar(1).svg'
import descIcon from '../assets/images/description.svg'
import { getClassTimeStatus, formatTime } from '../utils/data.js'

const props = defineProps({
  classInfo: { type: Object, required: true }, // 班级数据对象
  isApplied: { type: Boolean, default: false }, // 学生是否已报该班（status=1或4）
  isAdmitted: { type: Boolean, default: false }, // 是否已录取（status=3）
  isLoggedIn: { type: Boolean, default: false }, // 学生是否已登录
})

const emit = defineEmits(['select'])
const dialogVisible = ref(false)
const isExpanded = ref(false)
const isVisible = ref(false)   // 移动端入场动画：是否已进入视口
const cardRef = ref(null)      // 卡片 DOM 引用

// 筛选切换导致卡片数据变化时，重置展开状态
watch(() => props.classInfo._uid, () => {
  isExpanded.value = false
})

// ===== 响应式弹窗宽度：PC 500px，移动端 calc(100vw-40px) =====
// 为什么用 JS 而不用 CSS @media？Element Plus 把 width 写成 inline style，
// CSS 选择器在 Teleport + scoped 组合下容易失效，直接控 prop 最可靠
const windowWidth = ref(window.innerWidth)
function onResize() { windowWidth.value = window.innerWidth }
onMounted(() => {
  window.addEventListener('resize', onResize)
  // 移动端入场动画：IntersectionObserver 检测卡片进入视口时触发
  if (window.innerWidth <= 768 && cardRef.value) {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          isVisible.value = true
          observer.disconnect()  // 只触发一次，不再监听
        }
      },
      { threshold: 0.1 }  // 卡片露出 10% 即触发
    )
    observer.observe(cardRef.value)
  }
})
onUnmounted(() => window.removeEventListener('resize', onResize))
const dialogWidth = computed(() => {
  return windowWidth.value <= 768 ? 'calc(100vw - 40px)' : '500px'
})
// top 用 Element Plus 默认 15vh，不做自定义

// 是否为多轮班（classRounds 数组里有超过 1 条）
const isMultiRound = computed(() => {
  const rounds = props.classInfo.classRounds
  return Array.isArray(rounds) && rounds.length > 1
})

// 卡片标题：多轮班显示"班级名（第X轮报名）"，单轮班显示班级名
// _round 可能是 undefined（异常数据）→ 降级为 rounds 的第一条
const cardTitle = computed(() => {
  if (!isMultiRound.value) return props.classInfo.name
  const round = props.classInfo._round || (rounds.value[0] && rounds.value[0].round) || 1
  return `${props.classInfo.name}（第${round}轮报名）`
})

// 时间状态：按单轮的 _period 算，不看 classInfo.period
const timeStatus = computed(() => {
  const periodStr = props.classInfo._period || props.classInfo.period
  return getClassTimeStatus({ period: periodStr })
})

// 从 classRounds 数组解析轮次信息
const rounds = computed(() => {
  const arr = props.classInfo.classRounds
  if (!arr || !Array.isArray(arr) || arr.length === 0) {
    return [{ round: 1, period: props.classInfo.period || '' }]
  }
  return arr.map(r => ({
    round: r.roundNum,
    period: `${formatTime(r.periodStart)} - ${formatTime(r.periodEnd)}`
  }))
})

// 每轮的时间状态（用于多轮卡片右侧标签显示）
// 注意：必须基于 visibleRounds 而非 rounds.value，
// 否则 collapsed 状态下 idx=0 取到的是全量数组第 0 个（round1 的状态），而非当前轮次
const roundStatusList = computed(() => {
  return visibleRounds.value.map(r => getClassTimeStatus({ period: r.period }))
})

// 多轮班默认只展示卡片对应的那一轮，展开后展示全部
const visibleRounds = computed(() => {
  if (!isMultiRound.value) return rounds.value
  if (isExpanded.value) return rounds.value  // 展开：显示全部轮次

  // 未展开：显示卡片自己对应的那一轮（用 _round 定位，不受 active 影响）
  const cardRound = props.classInfo._round
  const target = rounds.value.find(r => r.round === cardRound)
  return target ? [target] : rounds.value.slice(0, 1)
})

function toggleExpand() {
  isExpanded.value = !isExpanded.value
}

// 已录取（status=3）：黄色
// 已登录 + 有记录（status=1/4）：灰色"已报名"
// 无记录：按时间状态
const buttonText = computed(() => {
  if (props.isAdmitted) return '已录取'
  if (props.isApplied) return '已报名'
  switch (timeStatus.value.status) {
    case 'not_started': return '未开始报名'
    case 'closed':      return '已截止'
    default:            return '立即报名'
  }
})

// 按钮样式：已录取黄，已报名绿，其他按时间状态
const btnClass = computed(() => {
  if (props.isAdmitted) return 'card-btn--admitted'
  if (props.isApplied) return 'card-btn--applied'
  return `card-btn--${timeStatus.value.status}`
})

function onClick() {
  if (!timeStatus.value.canApply || props.isApplied || props.isAdmitted) return
  emit('select', { id: props.classInfo.id, period: props.classInfo._period })
}

function showDescription() {
  dialogVisible.value = true
}
</script>

<!-- 非 scoped：覆盖 el-dialog Teleport 到 body 后的尺寸 -->
<style>
/* 隐藏原生头部/底部（.el-dialog__header 是 .desc-dialog 的子元素，后代选择器正确） */
.desc-dialog .el-dialog__header,
.desc-dialog .el-dialog__footer {
  display: none !important;
}
.desc-dialog .el-dialog__body {
  padding: 0 !important;
}

/* PC 端弹窗：width 由 JS 响应式 prop 控制，CSS 只管外观 */
.el-dialog.desc-dialog {
  border-radius: 16px;
  overflow: hidden;
  padding: 36px !important;
  box-sizing: border-box !important;
}

/* 移动端弹窗 */
@media (max-width: 768px) {
  .el-dialog.desc-dialog {
    border-radius: 14px !important;
    padding: 28px 20px !important;
    box-sizing: border-box !important;
  }
}

/* ==================== 移动端卡片入场动画 ==================== */
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
/* 移动端动画声明（必须在非 scoped 块，否则 keyframes 名与 animation 引用不匹配） */
@media (max-width: 768px) {
  /* 初始状态：隐藏，等待 IntersectionObserver 触发 */
  .class-card {
    opacity: 0;
    transform: translateY(20px);
    transition: border-color 0.18s ease, box-shadow 0.18s ease !important;
  }
  .class-card:hover {
    transform: none !important;
  }
  /* 进入视口后：播放滑入动画 */
  .class-card.is-visible {
    animation: cardSlideIn 0.3s ease-out both;
    animation-delay: var(--anim-delay, 0s);
  }
}
</style>

<style scoped>
/* ==================== 卡片容器 ==================== */
.class-card {
  position: relative;
  background: var(--paper, #fff);
  border: 1px solid var(--rule, #e2e8f0);
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}
.class-card:hover {
  border-color: #337eff;
  box-shadow: 0 4px 12px rgba(51, 126, 255, 0.12);
}
.class-card.is-disabled {
  cursor: not-allowed;
  /* 不设 pointer-events: none，子元素（介绍按钮、展开按钮）仍可点击 */
}
.class-card.is-disabled:hover {
  border-color: var(--rule, #e2e8f0);
  box-shadow: none;
  transform: none;
}
/* 只禁用报名按钮，而非整个卡片 */
.class-card.is-disabled .card-btn {
  pointer-events: none;
}

/* ==================== 签名元素：4px 顶部色条 ==================== */
.card-accent-bar {
  height: 4px;
  background: #337eff;
}

/* ==================== 卡片正文 ==================== */
.card-body {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;                     /* 紧凑间距，删类别后收紧 */
}

/* 内容区：flex 纵向布局，各行固定高度，保证所有卡片对齐 */
.card-content {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 标题行：高度自适应，溢出隐藏 */
.card-head {
  flex-shrink: 0;
  position: relative;              /* 让 desc-btn 绝对定位 */
  display: flex;
  align-items: flex-start;
  gap: 4px;
  overflow: hidden;
  min-height: 40px;                /* 至少一行标题高度 */
  padding-bottom: 2px;             /* 紧凑 */
}

/* 分割线：固定高度 */
.card-rule {
  flex-shrink: 0;
  height: 1px;
}

/* 时间区：flex-grow 填满剩余空间 */
.card-periods {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 6px;                 /* 紧凑间距 */
}

.card-name {
  flex: 1;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--ink, #0f172a);
  margin: 0;
  overflow: visible;
  padding-right: 56px;           /* 给右上角 absolute 按钮留空间 */
}

/* 介绍按钮：绝对定位右上角 */
.desc-btn {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px 8px;
  background: rgba(51, 126, 255, 0.08);
  border: 1px solid rgba(51, 126, 255, 0.2);
  border-radius: 4px;
  cursor: pointer;
  pointer-events: auto;
  transition: background 0.15s;
  white-space: nowrap;
}
.desc-btn:hover {
  background: rgba(51, 126, 255, 0.15);
}
.desc-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}
.desc-btn-text {
  font-size: 12px;
  color: #337eff;
  font-weight: 600;
}

/* 标题下分割线（签名元素） */
.card-rule {
  grid-row: 3;
  height: 1px;
  background: var(--rule, #e2e8f0);
}

/* ==================== 时间段（支持多轮） ==================== */
.card-periods {
  grid-row: 4;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.card-period-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ink-soft, #475569);
  line-height: 1.4;
}
.round-label {
  font-size: 12px;
  font-weight: 600;
  color: #337eff;
  background: #eaf2ff;
  padding: 3px 10px;
  border-radius: 10px;
  flex-shrink: 0;
  letter-spacing: 0.3px;
}
.period-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  opacity: 0.7;
}
.period-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: auto;
}

/* 轮次状态标签 */
.round-status {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
  flex-shrink: 0;
  letter-spacing: 0.3px;
}
.round-status--open {
  color: #16a34a;
  background: #f0fdf4;
}
.round-status--closed {
  color: #94a3b8;
  background: #f1f5f9;
}
.round-status--not_started {
  color: #94a3b8;
  background: #f1f5f9;
}

/* ==================== 展开其他轮次按钮 ==================== */
.expand-btn {
  display: inline-flex;
  align-items: center;
  background: none;
  border: none;
  color: #337ffe;
  cursor: pointer;
  padding: 2px 4px;
  transition: color 0.15s;
  pointer-events: auto;          /* 即使卡片禁用也能点击 */
}
.expand-btn:hover {
  color: #2563eb;
}
.expand-arrow {
  transition: transform 0.2s;
}
.expand-arrow.is-expanded {
  transform: rotate(180deg);
}

/* ==================== 三态按钮 ==================== */
.card-btn {
  width: 100%;
  height: 34px;
  margin-top: auto;               /* 永远贴卡片底部 */
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
  font-family: inherit;
}
/* open */
.card-btn--open {
  background: #337eff;
  color: #fff;
}
.card-btn--open:hover {
  background: #2563eb;
}
/* not_started */
.card-btn--not_started {
  background: #e2e8f0;
  color: #64748b;
  font-weight: 700;
  cursor: not-allowed;
}
/* closed */
.card-btn--closed {
  background: #f1f5f9;
  color: #b91c1c;
  font-weight: 700;
  cursor: not-allowed;
  border: 1px solid #fecaca;
  opacity: 0.85;
}
/* 已报名 */
.card-btn--applied {
  background: #16a34a;
  color: #fff;
  cursor: not-allowed;
}
/* 已登录+有记录（未录取） */
.card-btn--gray {
  background: #94a3b8;
  color: #fff;
  cursor: not-allowed;
}
/* 已录取 */
.card-btn--admitted {
  background: #f59e0b;
  color: #fff;
  cursor: not-allowed;
}

/* ==================== 班级介绍弹窗 · 内部内容组件 ==================== */
.custom-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.custom-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.header-text {
  flex: 1;
  min-width: 0;
}

.header-title {
  margin: 0 0 4px;
  font-size: clamp(15px, 4.2vw, 18px);
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
  word-break: keep-all;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.custom-close-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #ef4444;
  color: #fff;
  border: 2px solid rgba(255, 255, 255, 0.9);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.15s ease;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.4);
}

.custom-close-btn:hover {
  background: #dc2626;
  transform: scale(1.08);
}

.dialog-inner {
  width: 100%;
}

.desc-tag {
  display: block;
  width: fit-content;
  margin-bottom: 12px;
  padding: 4px 12px;
  font-size: 13px;
  color: #337eff;
  font-weight: 600;
  background: #eaf2ff;
  border-radius: 6px;
  letter-spacing: 0.04em;
}

.desc-dialog-body {
  margin: 0 0 20px;
  padding: 16px;
  background: #f1f5f9;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  max-height: 35vh;
  overflow-y: auto;
  width: 100%;
  box-sizing: border-box;
}

.desc-dialog-text {
  font-size: 15px;
  line-height: 1.9;
  color: #1e293b;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: justify;
  letter-spacing: 0.02em;
}

.desc-dialog-footer {
  width: 100%;
}

.desc-dialog-confirm-btn {
  width: 100%;
  height: 42px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  background: #337eff;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: background 0.2s;
}

.desc-dialog-confirm-btn:hover {
  background: #2563eb;
}

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .card-body {
    padding: 15px 17px;
    gap: 8px;
  }
  .card-content {
    display: flex;               /* 移动端恢复 flex，自然流 */
    flex-direction: column;
    gap: 0;
  }
  .card-head {
    position: relative;
    display: flex;
    align-items: flex-start;
    gap: 6px;
    margin-top: 9px;
  }
  .card-name {
    font-size: 15px;
    padding-right: 52px;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    display: -webkit-box;
    overflow: hidden;
  }
  .desc-btn {
    position: absolute;
    top: 0;
    right: 0;
    padding: 4px 6px;
  }
  .card-rule {
    display: block;
  }
  .card-periods {
    gap: 4px;
  }
  .card-btn {
    height: 36px;
    font-size: 13px;
  }
}
/* ==================== 移动端弹窗内容适配 ==================== */
@media (max-width: 768px) {
  .custom-header {
    gap: 10px;
    margin-bottom: 14px;
  }

  .header-content {
    gap: 8px;
  }

  .custom-icon {
    width: 24px;
    height: 24px;
  }

  .header-title {
    font-size: clamp(14px, 4vw, 15px) !important;
    line-height: 1.4 !important;
  }

  .custom-close-btn {
    width: 36px !important;
    height: 36px !important;
    border-width: 2px !important;
    box-shadow: 0 3px 10px rgba(239, 68, 68, 0.45) !important;
  }

  .custom-close-btn svg {
    width: 18px !important;
    height: 18px !important;
  }

  .desc-dialog-body {
    margin: 0 0 14px;
    padding: 12px !important;
    background: #f1f5f9 !important;
    border: 1px solid #cbd5e1 !important;
    max-height: 35vh;
    border-radius: 8px;
  }

  .desc-dialog-text {
    font-size: 14px !important;
    line-height: 1.85 !important;
    text-align: justify !important;
  }

  .desc-tag {
    font-size: 12px;
    padding: 3px 10px;
    margin-bottom: 10px;
  }

  .desc-dialog-confirm-btn {
    height: 46px !important;
    font-size: 15px;
    border-radius: 10px !important;
  }
}
</style>
