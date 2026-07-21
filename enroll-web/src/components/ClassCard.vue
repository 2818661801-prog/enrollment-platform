<!--
  ClassCard.vue · 特色班卡片
  设计方向：「录取通知书的剪影 / Engineering Specification Card」
  4px 顶部色条 + 标题下 1px 分割线 + 直角 → 像从招生章程里剪下的一节
  主题色：#337eff（沿用 themeCss.json --navbar-background）
-->
<template>
  <article
    class="class-card"
    :class="{ 'is-disabled': !timeStatus.canApply, 'is-single': !isMultiRound, 'is-multi': isMultiRound }"
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

        <!-- Row2: 类别标签（始终占位，无标签时 empty） -->
        <div class="card-tags"><span v-for="n in categoryNames" :key="n" class="card-tag">{{ n }}</span></div>

        <!-- Row3: 分割线（永远在同一 Y） -->
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

    <!-- 班级介绍弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      width="90%"
      max-width="460px"
      destroy-on-close
      :append-to-body="true"
      :show-close="false"
      class="desc-dialog"
    >
      <!-- 顶部蓝色色条 -->
      <div class="desc-dialog-bar" />
      <!-- 头部 -->
      <div class="desc-dialog-head">
        <img :src="descIcon" alt="" class="desc-dialog-icon" />
        <span class="desc-dialog-title">{{ cardTitle }}</span>
      </div>
      <div class="desc-dialog-subtitle">班级介绍</div>
      <!-- 内容 -->
      <div class="desc-dialog-body">
        <p class="desc-dialog-text">{{ classInfo.description }}</p>
      </div>
      <!-- 底部按钮 -->
      <div class="desc-dialog-footer">
        <el-button class="desc-dialog-close-btn" @click="dialogVisible = false">我知道了</el-button>
      </div>
    </el-dialog>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
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

// 是否为多轮班（classRounds 数组里有超过 1 条）
const isMultiRound = computed(() => {
  const rounds = props.classInfo.classRounds
  return Array.isArray(rounds) && rounds.length > 1
})

// 卡片标题：多轮班显示"班级名（第X轮报名）"，单轮班显示班级名
const cardTitle = computed(() => {
  const round = props.classInfo._round
  if (round && isMultiRound.value) {
    return `${props.classInfo.name}（第${round}轮报名）`
  }
  return props.classInfo.name
})

// 时间状态：按单轮的 _period 算，不看 classInfo.period
const timeStatus = computed(() => {
  const periodStr = props.classInfo._period || props.classInfo.period
  return getClassTimeStatus({ period: periodStr })
})

// 类别标签：优先读 categories 数组（中间表来源），兼容 categoryNames
const categoryNames = computed(() => {
  if (Array.isArray(props.classInfo.categories) && props.classInfo.categories.length) {
    return props.classInfo.categories
  }
  if (Array.isArray(props.classInfo.categoryNames) && props.classInfo.categoryNames.length) {
    return props.classInfo.categoryNames
  }
  if (props.classInfo.category) return [props.classInfo.category]
  return []
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
const roundStatusList = computed(() => {
  return rounds.value.map(r => getClassTimeStatus({ period: r.period }))
})

// 多轮班默认只展示第1轮，展开后展示全部
const visibleRounds = computed(() => {
  if (!isMultiRound.value) return rounds.value
  return isExpanded.value ? rounds.value : rounds.value.slice(0, 1)
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
  pointer-events: none;
}
.class-card.is-disabled:hover {
  border-color: var(--rule, #e2e8f0);
  box-shadow: none;
  transform: none;
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
  gap: 20px;                     /* 调大 10px */
}

/* 内容区：flex 纵向布局，各行固定高度，保证所有卡片对齐 */
.card-content {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 标题行：固定高度，溢出隐藏 */
.card-head {
  flex-shrink: 0;
  position: relative;              /* 让 desc-btn 绝对定位 */
  display: flex;
  align-items: flex-start;
  gap: 6px;
  overflow: hidden;
  height: 48px;                    /* 2行标题约48px，避免被标签遮挡 */
}

/* 类别标签：固定高度，溢出隐藏 */
.card-tags {
  flex-shrink: 0;
  height: 28px;
  overflow: hidden;
  display: flex;
  flex-direction: row;
  gap: 4px;
  flex-wrap: wrap;
  align-content: flex-start;
  justify-content: flex-start;    /* PC 端左对齐 */
  margin-top: 8px;
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
  padding-top: 8px;                 /* 与分割线保持 7px 间距 */
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

.card-tag {
  font-size: 11px;
  color: #1d4ed8;
  background: #eaf2ff;
  padding: 2px 8px;
  border-radius: 3px;
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

/* ==================== 班级介绍弹窗 ==================== */
.desc-dialog :deep(.el-dialog__header) { display: none; }
.desc-dialog :deep(.el-dialog__body)   { padding: 0; }
.desc-dialog :deep(.el-dialog__footer) { display: none; }
.desc-dialog :deep(.el-dialog) { border-radius: 12px; overflow: hidden; }
.desc-dialog-bar {
  height: 4px;
  background: linear-gradient(90deg, #337eff, #5b9bff);
}
.desc-dialog-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 24px 0;
}
.desc-dialog-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}
.desc-dialog-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}
.desc-dialog-subtitle {
  display: inline-block;
  margin: 6px 24px 0;
  padding: 3px 12px;
  font-size: 13px;
  color: #337eff;
  font-weight: 600;
  background: #eaf2ff;
  border-radius: 4px;
  letter-spacing: 0.04em;
}
.desc-dialog-body {
  margin: 14px 16px 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e8ecf2;
}
.desc-dialog-text {
  font-size: 14px;
  line-height: 1.75;
  color: #334155;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
.desc-dialog-footer {
  padding: 0 24px 24px;
}
.desc-dialog-close-btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  background: #337eff;
  color: #fff;
  border: none;
  transition: background 0.2s;
}
.desc-dialog-close-btn:hover {
  background: #2563eb;
  color: #fff;
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
  .card-tags {
    justify-content: flex-start;
    margin-bottom: 6px;
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
@media (max-width: 768px) {
  .desc-dialog :deep(.el-dialog) { max-width: 92vw !important; margin: 0 auto !important; }
  .desc-dialog-head { padding: 20px 16px 0; }
  .desc-dialog-subtitle { margin: 6px 16px 0; }
  .desc-dialog-body { margin: 10px 12px 16px; padding: 14px; }
  .desc-dialog-footer { padding: 0 16px 20px; }
}
</style>
