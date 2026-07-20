<!--
  ClassCard.vue · 特色班卡片
  设计方向：「录取通知书的剪影 / Engineering Specification Card」
  4px 顶部色条 + 标题下 1px 分割线 + 直角 → 像从招生章程里剪下的一节
  主题色：#337eff（沿用 themeCss.json --navbar-background）
-->
<template>
  <article
    class="class-card"
    :class="{ 'is-disabled': !timeStatus.canApply }"
    @click="onClick"
  >
    <!-- 顶部色条（签名元素：从招生章程里剪下来的视觉特征） -->
    <div class="card-accent-bar" />

    <div class="card-body">
      <!-- 标题行：班级名称 + 介绍按钮 + 类别标签（右上角） -->
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
        </button>
        <div v-if="categoryNames.length" class="card-tags">
          <span v-for="n in categoryNames" :key="n" class="card-tag">{{ n }}</span>
        </div>
      </div>

      <!-- 标题下分割线 -->
      <div class="card-rule" />

      <!-- 报名时间段（支持多轮） -->
      <div class="card-periods">
        <div v-for="(r, idx) in rounds" :key="r.round" class="card-period-row">
          <img :src="calendarIcon" alt="" class="period-icon" aria-hidden="true" />
          <span class="round-label">第{{ r.round }}轮</span>
          <el-tooltip :content="`报名时间：${r.period}`" placement="top">
            <span class="period-text">{{ r.period }}</span>
          </el-tooltip>
          <!-- 多轮时右侧显示状态标签 -->
          <span v-if="isMultiRound" class="round-status" :class="`round-status--${roundStatusList[idx].status}`">
            {{ roundStatusList[idx].label }}
          </span>
        </div>
      </div>

      <!-- 三态按钮：已录取 / 灰色已录取 / 未开始 / 已截止 / 立即报名 -->
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

// 从 classRounds 数组解析轮次信息（替代旧的 periods JSON 解析）
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
  border-radius: 4px;         /* 主人要求：圆角 */
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}
.class-card:hover {
  border-color: #337eff;       /* hover：边框变主题蓝 */
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
  background: #337eff;         /* 沿用 themeCss.json 主色 */
}
/* 顶部色条保持主题蓝不变 */

/* ==================== 卡片正文 ==================== */
.card-body {
  padding: 20px 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ==================== 标题行 ==================== */
.card-head {
  position: relative;           /* 让 desc-btn 绝对定位右上角 */
  min-height: 48px;
}
.card-name {
  padding-right: 36px;         /* 给右上角按钮留空位 */
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--ink, #0f172a);
  margin: 0;
  /* 最多两行省略 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 类别标签：右上角小标签，浅蓝底，横向排列 */
.card-tags {
  display: flex;
  flex-direction: row;
  gap: 4px;
  flex-shrink: 1;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-top: 10px;
}
.card-tag {
  font-size: 11px;
  color: #1d4ed8;              /* 深一档蓝，与色条呼应 */
  background: #eaf2ff;         /* 主题蓝的 12% 浅版 */
  padding: 2px 8px;
  border-radius: 3px;          /* 圆角（与卡片整体调性一致） */
}

/* 介绍按钮：绝对定位右上角，和类别标签完全分离 */
.desc-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 32px;
  height: 32px;
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
  pointer-events: auto; /* 穿透 is-disabled 禁用 */
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.15s;
}
.desc-btn:hover {
  background: #eaf2ff;
}
.desc-icon {
  width: 20px;
  height: 20px;
  display: block;
}

/* ==================== 班级介绍弹窗 ==================== */
.desc-dialog :deep(.el-dialog__header) { display: none; }
.desc-dialog :deep(.el-dialog__body)   { padding: 0; }
.desc-dialog :deep(.el-dialog__footer) { display: none; }
.desc-dialog :deep(.el-dialog) { border-radius: 12px; overflow: hidden; }

/* 顶部色条 */
.desc-dialog-bar {
  height: 4px;
  background: linear-gradient(90deg, #337eff, #5b9bff);
}

/* 头部 */
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

/* 副标题 */
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

/* 内容区 */
.desc-dialog-body {
  margin: 14px 16px 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e8ecf2;
  position: relative;
}
.desc-dialog-text {
  font-size: 14px;
  line-height: 1.75;
  color: #334155;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 底部按钮 */
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

/* 移动端适配 */
@media (max-width: 768px) {
  .desc-dialog :deep(.el-dialog) { max-width: 92vw !important; margin: 0 auto !important; }
  .desc-dialog-head { padding: 20px 16px 0; }
  .desc-dialog-subtitle { margin: 6px 16px 0; }
  .desc-dialog-body { margin: 10px 12px 16px; padding: 14px; }
  .desc-dialog-footer { padding: 0 16px 20px; }
}

/* 标题下分割线（签名元素：印刷感） */
.card-rule {
  height: 1px;
  background: var(--rule, #e2e8f0);
}

/* ==================== 时间段（支持多轮） ==================== */
.card-periods {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  pointer-events: auto; /* 恢复点击，让 disabled 卡片里的 el-tooltip 也能触发 */
}

/* 轮次状态标签：报名中=绿，已截止/未开始=灰 */
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

/* ==================== 三态按钮 ==================== */
.card-btn {
  width: 100%;
  height: 38px;
  border: 1px solid transparent;
  border-radius: 4px;          /* 圆角（与卡片一致） */
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
  font-family: inherit;
}
/* open：主题蓝实心（强对比，吸引点击，与品牌色一致） */
.card-btn--open {
  background: #337eff;         /* themeCss.json --navbar-background */
  color: #fff;
}
.card-btn--open:hover {
  background: #2563eb;         /* hover：稍深一档 */
}
/* not_started：浅灰底，提示性文案，加粗醒目 */
.card-btn--not_started {
  background: #e2e8f0;
  color: #64748b;
  font-weight: 700;
  cursor: not-allowed;
}
/* closed：浅灰底，加粗醒目 */
.card-btn--closed {
  background: #f1f5f9;
  color: #b91c1c;
  font-weight: 700;
  cursor: not-allowed;
  border: 1px solid #fecaca;
  opacity: 0.85;
}
/* 已报名：绿色（与开放状态接近，但不等于可点击） */
.card-btn--applied {
  background: #16a34a;
  color: #fff;
  cursor: not-allowed;
}
/* 已登录 + 有记录（未录取）：灰色 */
.card-btn--gray {
  background: #94a3b8;
  color: #fff;
  cursor: not-allowed;
}
/* 已录取：黄色（醒目但不等于可点击） */
.card-btn--admitted {
  background: #f59e0b;
  color: #fff;
  cursor: not-allowed;
}

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .card-body {
    padding: 16px 16px 14px;
    gap: 12px;
  }
  .card-name {
    font-size: 16px;
  }
  .card-head {
    display: flex;
    min-height: 44px;
    flex-direction: column;      /* 标签放名称下方，不挤一排 */
    align-items: flex-start;
  }
  .card-tags {
    justify-content: flex-start; /* 标签左对齐，不右对齐 */
    margin-top: 10px;
  }
  /* 移动端介绍按钮：绝对定位在右上角，不影响标题行布局 */
  .desc-btn {
    position: absolute;
    top: -4px;
    right: -4px;
  }
  .card-btn {
    height: 36px;
    font-size: 13px;
  }
}
</style>
