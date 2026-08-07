<!--
  NoticeDialog.vue · 报名须知弹窗
  从 HomePage 抽出：标题 + 报名条件 + 特色班简介表格 + 注意事项 + 同意按钮
  自带移动端/PC 自适应（useDialogAdapt），HomePage 不再需要两个重复 watch
-->
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="title"
    width="720px"
    :close-on-click-modal="true"
    :destroy-on-close="false"
    :lock-scroll="windowWidth > 768"
    class="notice-dialog"
  >
    <template #header>
      <span class="notice-dialog-title">{{ title }}</span>
      <button class="notice-dialog-close" @click="emit('update:modelValue', false)" type="button" aria-label="关闭">
        <el-icon :size="18"><Close /></el-icon>
      </button>
    </template>

    <!-- 一、报名条件（从 sys_config 动态加载） -->
    <section class="nd-section">
      <h3>一、报名条件</h3>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
        <template #title>以下条件需 <strong>全部满足</strong> 方可报名</template>
      </el-alert>
      <ul v-if="conditions.length">
        <li v-for="(c, i) in conditions" :key="i" v-html="c" />
      </ul>
      <ul v-else>
        <li>加载报名条件失败，请刷新页面</li>
      </ul>
    </section>

    <!-- 二、班级介绍（每个班 × 每轮 = 一行，多轮班多轮时间都能展示） -->
    <section class="nd-section">
      <h3>二、特色班简介</h3>
      <div class="nd-table-wrap">
        <el-table :data="tableData" border size="small">
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

    <!-- 三、注意事项 -->
    <section class="nd-section">
      <h3>三、注意事项</h3>
      <ul v-if="notices.length">
        <li v-for="(n, i) in notices" :key="i" v-html="n" />
      </ul>
      <ul v-else>
        <li>暂无注意事项</li>
      </ul>
    </section>

    <template #footer>
      <div class="notice-footer">
        <el-button type="primary" size="large" @click="emit('agree')">我已知晓并同意</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { Close } from '@element-plus/icons-vue'
import { useWindowWidth } from '../composables/useWindowWidth.js'
import { useDialogAdapt } from '../composables/useDialogAdapt.js'

// 声明 props：modelValue=开关，title=标题，conditions=条件数组，notices=须知数组，tableData=班级表格数据
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '杭州电子科技大学 2026 级特色班报名须知' },
  conditions: { type: Array, default: () => [] },
  notices: { type: Array, default: () => [] },
  tableData: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'agree'])

// lock-scroll 需要响应式窗口宽度
const windowWidth = useWindowWidth()
// 弹窗尺寸自适应（原来 HomePage 里 40 行重复 watch，现在一行）
useDialogAdapt('notice-dialog', () => props.modelValue, { maxWidthPc: 720 })
</script>

<!-- 样式从 HomePage.vue 剪来（非 scoped：Teleport 到 body 后 Element Plus 内部节点无 data-v，
     scoped 选择器不生效，必须用全局选择器 + .notice-dialog 前缀限定） -->
<style>
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
@media (max-width: 768px) {
  .nd-section h3 {
    font-size: 14px;
  }
  .nd-section ul {
    font-size: 12px;
    line-height: 1.8;
    padding-left: 14px;
  }
}

/* 通用样式优化 */
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
</style>
