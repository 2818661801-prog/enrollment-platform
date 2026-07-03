<!--
  FilterBar.vue · 筛选栏（自定义下拉 + 重置 + 登录/我的报名按钮组）
  - 左侧：班级名称筛选（自定义下拉，点击打开/关闭，点击外部不关闭）
  - 中间：重置按钮
  - 右侧：登录 + 我的报名按钮组（margin-left: auto）
  - 移动端：右侧按钮组 width:100% 靠右
-->
<template>
  <div class="filter-bar">
    <!-- 左侧：班级名称筛选下拉（自定义，点击 toggle，点击外部不关闭） -->
    <div class="custom-select" ref="selectRef">
      <!-- 触发器 -->
      <button
        type="button"
        class="select-trigger"
        :class="{ 'is-opened': isOpen }"
        @click="toggleDrop"
      >
        <span class="select-label">
          {{ selectedLabel || '按班级名称...' }}
        </span>
        <span class="select-arrow" :class="{ 'is-up': isOpen }"></span>
      </button>

      <!-- 下拉面板 -->
      <div v-show="isOpen" class="select-dropdown">
        <div
          v-for="c in classes"
          :key="c.id"
          class="select-option"
          :class="{ 'is-selected': modelValue === c.id }"
          @click="pick(c)"
        >
          {{ getClassLabel(c) }}
        </div>
      </div>
    </div>

    <!-- 重置按钮 -->
    <button type="button" class="reset-btn" @click="onReset">
      重置
    </button>

    <!-- 第二轮筛选 -->
    <label class="round2-filter">
      <input
        type="checkbox"
        :checked="props.showOnlyRound2"
        @change="emit('update:showOnlyRound2', $event.target.checked)"
      />
      <span>只看第二轮报名</span>
    </label>

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

      <!-- 退出登录（已登录时显示） -->
      <button
        v-if="isLoggedIn"
        type="button"
        class="user-btn logout-btn"
        aria-label="退出登录"
        @click="onLogout"
      >
        <img :src="lockIcon" alt="" class="user-icon" />
        <span>退出登录</span>
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

    <!-- 移动端第二行：checkbox + 登录 + 我的报名 均分 -->
    <div class="mobile-actions-row">
      <label class="round2-filter">
        <input
          type="checkbox"
          :checked="props.showOnlyRound2"
          @change="emit('update:showOnlyRound2', $event.target.checked)"
        />
        <span>第二轮</span>
      </label>
      <button v-if="!isLoggedIn" type="button" class="user-btn login-btn" @click="goLogin">
        <img :src="lockIcon" alt="" class="user-icon" />
        <span>登录</span>
      </button>
      <button v-if="isLoggedIn" type="button" class="user-btn logout-btn" @click="onLogout">
        <img :src="lockIcon" alt="" class="user-icon" />
        <span>退出</span>
      </button>
      <button type="button" class="user-btn apps-btn" @click="goMyApps">
        <img :src="folderIcon" alt="" class="user-icon" />
        <span>我的报名</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import lockIcon from '../assets/images/lock.svg'
import folderIcon from '../assets/images/folder.svg'

const props = defineProps({
  modelValue: { type: [Number, String, null], default: null },
  classes:    { type: Array, default: () => [] },
  isLoggedIn: { type: Boolean, default: false },
  showOnlyRound2: { type: Boolean, default: false }, // 只看第二轮
})

const emit = defineEmits(['update:modelValue', 'reset', 'logout', 'update:showOnlyRound2'])

const router = useRouter()
const selectRef = ref(null)
const isOpen = ref(false)

const selectedLabel = computed(() => {
  if (!props.modelValue) return null
  const found = props.classes.find(c => c.id === props.modelValue)
  return found ? found.name : null
})

function toggleDrop() {
  isOpen.value = !isOpen.value
}

function pick(c) {
  emit('update:modelValue', c.id)
  isOpen.value = false
}

function onReset() {
  emit('update:modelValue', null)
  emit('update:showOnlyRound2', false)
  emit('reset')
}

/**
 * 生成班级选项显示名称：多轮班加"（第X轮报名）"后缀
 */
function getClassLabel(cls) {
  try {
    const arr = JSON.parse(cls.periods || '[]')
    if (arr.length > 1) {
      return `${cls.name}（第1轮报名） / 第2轮报名`
    }
  } catch {}
  return cls.name
}

function goLogin() {
  router.push('/student-login')
}

function goMyApps() {
  router.push('/my-applications')
}

function onLogout() {
  localStorage.removeItem('student_token')
  localStorage.removeItem('student_phone')
  emit('logout')
  // 提示由 HomePage 的 onLogout 统一处理，避免重复提示
}

// 点击外部不关闭（mousedown 拦截，不拦截 click）
// 这样下拉框可以保持打开，用户可以正常操作
function onDocMousedown(e) {
  if (!isOpen.value) return
  if (selectRef.value && !selectRef.value.contains(e.target)) {
    // 点击外部，但不下拉——本次 mousedown 不关闭
    // 我们用 preventDefault 阻止 el-select 获得焦点导致关闭
    e.preventDefault()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', onDocMousedown)
})
onUnmounted(() => {
  document.removeEventListener('mousedown', onDocMousedown)
})
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
.custom-select {
  position: relative;
  flex: 1;
  min-width: 180px;
}

/* 触发器按钮 */
.select-trigger {
  width: 100%;
  height: 38px;
  padding: 0 34px 0 14px;
  background: #ffffff;
  border: 1px solid #e8ecf2;
  border-radius: 8px;
  box-shadow: none;
  font-size: 14px;
  color: #1a1a2e;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: center;
}
.select-trigger.is-opened,
.select-trigger:hover {
  border-color: #3b7bf8;
  box-shadow: 0 0 0 3px rgba(59, 123, 248, 0.1);
}
.select-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #8890a4;
}
.select-label:not(:empty) {
  color: #1a1a2e;
}

/* 自定义箭头 */
.select-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%238890a4' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'/></svg>");
  background-repeat: no-repeat;
  background-position: center;
  background-size: 12px 12px;
  transition: transform 0.2s;
}
.select-arrow.is-up {
  transform: translateY(-50%) rotate(180deg);
}

/* 下拉面板 */
.select-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e8ecf2;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  z-index: 1000;
  max-height: 280px;
  overflow-y: auto;
}

/* 下拉选项 */
.select-option {
  padding: 10px 14px;
  font-size: 14px;
  color: #1a1a2e;
  cursor: pointer;
  transition: background-color 0.15s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.select-option:hover {
  background-color: #f0f4ff;
}
.select-option.is-selected {
  color: #3b7bf8;
  background-color: #e8f0ff;
  font-weight: 500;
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

/* ==================== 第二轮筛选 checkbox ==================== */
.round2-filter {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #555b70;
  cursor: pointer;
  user-select: none;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #e8ecf2;
  border-radius: 8px;
  transition: border-color 0.2s, color 0.2s;
}
.round2-filter:hover {
  border-color: #3b7bf8;
  color: #3b7bf8;
}
.round2-filter input[type="checkbox"] {
  width: 15px;
  height: 15px;
  cursor: pointer;
  accent-color: #3b7bf8;
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
/* 退出登录按钮：红字 */
.logout-btn {
  color: #f56c6c;
}
.logout-btn:hover {
  border-color: #f56c6c;
  color: #f56c6c;
}

/* ==================== 响应式 ≤768px ==================== */
.mobile-actions-row { display: none; }
@media (max-width: 768px) {
  .filter-bar {
    gap: 8px;
  }
  /* PC端第二轮 checkbox 移动端隐藏 */
  .round2-filter {
    display: none;
  }
  /* user-actions（PC端按钮组）移动端隐藏 */
  .user-actions {
    display: none;
  }
  /* 移动端第二行：均分三个元素 */
  .mobile-actions-row {
    display: flex;
    gap: 6px;
    width: 100%;
  }
  .mobile-actions-row .round2-filter {
    display: flex;
    flex: 1;
    height: 34px;
    justify-content: center;
    align-items: center;
    font-size: 12px;
    margin: 0;
  }
  .mobile-actions-row .user-btn {
    flex: 1;
    justify-content: center;
  }
}
</style>
