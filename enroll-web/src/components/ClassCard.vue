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
      <!-- 标题行：班级名称 + 类别标签（右上角） -->
      <div class="card-head">
        <h3 class="card-name" :title="classInfo.name">{{ classInfo.name }}</h3>
        <div v-if="categoryNames.length" class="card-tags">
          <span v-for="n in categoryNames" :key="n" class="card-tag">{{ n }}</span>
        </div>
      </div>

      <!-- 标题下分割线 -->
      <div class="card-rule" />

      <!-- 报名时间段 -->
      <div class="card-period">
        <img :src="calendarIcon" alt="" class="period-icon" aria-hidden="true" />
        <span class="period-text">{{ classInfo.period }}</span>
      </div>

      <!-- 三态按钮：立即报名 / 未开始 / 已截止 -->
      <button
        type="button"
        class="card-btn"
        :class="`card-btn--${timeStatus.status}`"
        :disabled="!timeStatus.canApply"
        @click.stop="onClick"
      >
        {{ buttonText }}
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import calendarIcon from '../assets/images/calendar(1).svg'
import { getClassTimeStatus } from '../utils/data.js'

const props = defineProps({
  classInfo: { type: Object, required: true }, // 班级数据对象
})

const emit = defineEmits(['select'])

const timeStatus = computed(() => getClassTimeStatus(props.classInfo))

// 类别标签：兼容 categoryNames（数组）和 category（字符串/null）两种来源
const categoryNames = computed(() => {
  if (Array.isArray(props.classInfo.categoryNames) && props.classInfo.categoryNames.length) {
    return props.classInfo.categoryNames
  }
  if (props.classInfo.category) return [props.classInfo.category]
  return []
})

const buttonText = computed(() => {
  switch (timeStatus.value.status) {
    case 'not_started': return '未开始报名'
    case 'closed':      return '已截止'
    default:            return '立即报名'
  }
})

function onClick() {
  if (!timeStatus.value.canApply) {
    // 不可报名时：不跳转，仅给出时间提示
    return
  }
  emit('select', props.classInfo.id)
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
}
.class-card.is-disabled:hover {
  border-color: var(--rule, #e2e8f0);
}

/* ==================== 签名元素：4px 顶部色条 ==================== */
.card-accent-bar {
  height: 4px;
  background: #337eff;         /* 沿用 themeCss.json 主色 */
}
.class-card.is-disabled .card-accent-bar {
  background: #cbd5e1;         /* 不可报名：色条变灰 */
}

/* ==================== 卡片正文 ==================== */
.card-body {
  padding: 20px 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ==================== 标题行 ==================== */
.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;            /* 防止标签少时整体塌陷 */
}
.card-name {
  flex: 1;
  min-width: 0;
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

/* 类别标签：右上角小标签，浅蓝底 */
.card-tags {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  align-items: flex-end;
}
.card-tag {
  font-size: 11px;
  color: #1d4ed8;              /* 深一档蓝，与色条呼应 */
  background: #eaf2ff;         /* 主题蓝的 12% 浅版 */
  padding: 2px 8px;
  border-radius: 3px;          /* 圆角（与卡片整体调性一致） */
  white-space: nowrap;
  line-height: 1.4;
  font-weight: 500;
}

/* 标题下分割线（签名元素：印刷感） */
.card-rule {
  height: 1px;
  background: var(--rule, #e2e8f0);
}

/* ==================== 时间段 ==================== */
.card-period {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ink-soft, #475569);
  line-height: 1.5;
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
/* not_started：浅灰底，提示性文案 */
.card-btn--not_started {
  background: var(--disabled, #f1f5f9);
  color: var(--disabled-text, #94a3b8);
  cursor: not-allowed;
}
/* closed：浅灰底，文案 */
.card-btn--closed {
  background: var(--disabled, #f1f5f9);
  color: var(--disabled-text, #94a3b8);
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
    min-height: 44px;
  }
  .card-btn {
    height: 36px;
    font-size: 13px;
  }
}
</style>
