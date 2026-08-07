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

<!-- 样式从 HomePage.vue 剪来（非 scoped，理由同 NoticeDialog） -->
<style>
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
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.8;
  padding: 6px 12px;
  background: #ecf5ff;
  border-radius: 6px;
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
    font-size: 13px;
    padding: 5px 10px;
  }
}

/* 非 scoped 结构样式（Element Plus Teleport 内部节点） */
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
