<!--
  FilterBar.vue · 筛选栏（自定义下拉 + 重置 + 登录/我的报名按钮组）
  - 左侧：班级名称筛选（el-select + 自定义样式覆盖）
  - 中间：重置按钮
  - 右侧：登录 + 我的报名按钮组（margin-left: auto）
  - 移动端：右侧按钮组 width:100% 靠右
-->
<template>
  <div class="filter-bar">
    <!-- 左侧：班级名称筛选下拉 -->
    <el-select
      v-model="modelValue"
      class="filter-select"
      placeholder="按班级名称..."
      filterable
      clearable
      size="default"
      @change="emitChange"
    >
      <el-option
        v-for="c in classes"
        :key="c.id"
        :label="c.name"
        :value="c.id"
      />
    </el-select>

    <!-- 重置按钮 -->
    <button type="button" class="reset-btn" @click="onReset">
      重置
    </button>

    <!-- 右侧：登录 + 我的报名按钮组 -->
    <div class="user-actions">
      <!-- 登录（未登录时显示） -->
      <button
        v-if="!isLoggedIn"
        type="button"
        class="user-btn login-btn"
        aria-label="学生登录"
        @click="goLogin"
      >
        <img :src="lockIcon" alt="" class="user-icon" />
        <span>登录</span>
      </button>

      <!-- 我的报名 -->
      <button
        type="button"
        class="user-btn apps-btn"
        aria-label="我的报名"
        @click="goMyApps"
      >
        <img :src="folderIcon" alt="" class="user-icon" />
        <span>我的报名</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import lockIcon from '../assets/images/lock.svg'
import folderIcon from '../assets/images/folder.svg'

const props = defineProps({
  modelValue: { type: [Number, String, null], default: null }, // 当前选中班级 id
  classes:    { type: Array, default: () => [] },             // 班级列表（用于下拉项）
  isLoggedIn: { type: Boolean, default: false },              // 是否已登录
})

const emit = defineEmits(['update:modelValue', 'reset'])

const router = useRouter()

// 用 computed 代理 v-model，父组件可用 v-model 双向绑定
const modelValue = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

function emitChange(v) {
  emit('update:modelValue', v)
}

function onReset() {
  emit('update:modelValue', null)
  emit('reset')
}

function goLogin() {
  router.push('/student-login')
}

function goMyApps() {
  router.push('/my-applications')
}
</script>

<style scoped>
/* ==================== 筛选栏布局 ==================== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

/* ==================== 自定义下拉框 ==================== */
.filter-select {
  flex: 1;
  min-width: 180px;
}
/* 用 :deep() 穿透 Element Plus 组件作用域，覆盖默认样式 */
.filter-select :deep(.el-select__wrapper) {
  height: 38px;
  padding: 0 34px 0 14px;          /* 右侧留 34px 给自定义箭头 */
  background: #ffffff;
  border: 1px solid #e8ecf2;
  border-radius: 8px;
  box-shadow: none !important;     /* 去掉 el-select 默认的悬浮阴影 */
  font-size: 14px;
  color: #1a1a2e;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.filter-select :deep(.el-select__wrapper.is-focused) {
  border-color: #3b7bf8;
  box-shadow: 0 0 0 3px rgba(59, 123, 248, 0.1) !important;
}
.filter-select :deep(.el-select__placeholder) {
  color: #8890a4;
}
/* 自定义下拉箭头（覆盖 Element Plus 默认的 svg）*/
.filter-select :deep(.el-select__suffix) {
  width: 34px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.filter-select :deep(.el-select__suffix .el-icon) {
  display: none;                   /* 隐藏默认箭头 */
}
.filter-select :deep(.el-select__suffix)::before {
  content: '';
  display: block;
  width: 12px;
  height: 12px;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%238890a4' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'/></svg>");
  background-repeat: no-repeat;
  background-position: right center;
  background-size: 12px 12px;
}

/* ==================== 重置按钮 ==================== */
.reset-btn {
  height: 38px;
  padding: 0 18px;
  background: #ffffff;
  border: 1px solid #e8ecf2;
  border-radius: 8px;
  color: #555b70;
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.reset-btn:hover {
  border-color: #3b7bf8;
  color: #3b7bf8;
}

/* ==================== 右侧按钮组 ==================== */
.user-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

/* 通用按钮 */
.user-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  background: #ffffff;
  border: 1px solid #e8ecf2;
  border-radius: 8px;
  color: #555b70;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.user-btn:hover {
  border-color: #3b7bf8;
  color: #3b7bf8;
}
.user-icon {
  width: 16px;
  height: 16px;
  display: block;
}
/* 登录按钮：蓝字 */
.login-btn {
  color: #3b7bf8;
}

/* ==================== 响应式 ≤768px ==================== */
@media (max-width: 768px) {
  .filter-bar {
    gap: 8px;
  }
  .user-actions {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
  }
}
</style>