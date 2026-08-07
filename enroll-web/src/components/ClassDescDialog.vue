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

<!-- 样式从 ClassCard.vue 搬来（非 scoped：Teleport 到 body 后 Element Plus 内部节点无 data-v） -->
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

/* ==================== 班级介绍弹窗 · 内部内容 ==================== */
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
