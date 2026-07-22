<!--
  AppHeader.vue · 顶部导航栏
  左：Logo + 标题（点击回首页）
  右：登录/退出 + 我的报名按钮（移动端自适应）
-->
<template>
  <header class="app-header">
    <div class="header-inner">
      <!-- 左：Logo 区（点击回首页） -->
      <router-link to="/home" class="logo-area">
        <img :src="logoImg" alt="杭电信工学院" class="logo-image" />
      </router-link>

      <!-- 右：登录/退出 + 我的报名按钮 -->
      <div class="header-actions">
        <!-- 未登录：显示登录按钮 -->
        <button
          v-if="!isLoggedIn"
          type="button"
          class="action-btn login-btn"
          aria-label="学生登录"
          @click="goLogin"
        >
          <img :src="lockIcon" alt="" class="action-icon" />
          <span class="action-label">登录</span>
        </button>

        <!-- 已登录：显示退出登录按钮 -->
        <button
          v-if="isLoggedIn"
          type="button"
          class="action-btn logout-btn"
          aria-label="退出登录"
          @click="onLogout"
        >
          <img :src="lockIcon" alt="" class="action-icon" />
          <span class="action-label">退出登录</span>
        </button>

        <!-- 始终显示：我的报名 -->
        <button
          type="button"
          class="action-btn apps-btn"
          aria-label="我的报名"
          @click="goMyApps"
        >
          <img :src="folderIcon" alt="" class="action-icon" />
          <span class="action-label">我的报名</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import logoImg from '../assets/images/logo.png'
import lockIcon from '../assets/images/lock.svg'
import folderIcon from '../assets/images/folder.svg'

const router = useRouter()
const isLoggedIn = ref(false)

// 每次组件挂载时从 localStorage 读取最新登录态
// （解决在 MyApplications.vue 登录后返回首页状态不同步的问题）
function refreshLoginState() {
  isLoggedIn.value = !!localStorage.getItem('student_token')
}

onMounted(() => {
  refreshLoginState()
  // 监听其他页面修改 token，保证跨页面状态同步
  window.addEventListener('storage', refreshLoginState)
  window.addEventListener('login_changed', refreshLoginState)
})

onUnmounted(() => {
  window.removeEventListener('storage', refreshLoginState)
  window.removeEventListener('login_changed', refreshLoginState)
})

function goLogin() {
  router.push('/student-login')
}

function goMyApps() {
  router.push('/my-applications')
}

function onLogout() {
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    alignCenter: true,
    roundButton: true,
  }).then(() => {
    localStorage.removeItem('student_token')
    localStorage.removeItem('student_phone')
    isLoggedIn.value = false
    // 通知其他页面登录态变化（HomePage 监听 application_changed，会触发 loadData 重刷数据）
    window.dispatchEvent(new Event('login_changed'))
    window.dispatchEvent(new Event('application_changed'))
  }).catch(() => {})
}
</script>

<style scoped>
.app-header {
  background: #337eff;
  color: #fff;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 56px;
  z-index: 999;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transform: translateZ(0);
  -webkit-transform: translateZ(0);
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;  /* 左贴左，右贴右 */
  height: 56px;
  padding: 0 12px;                  /* 左右各 12px 边距（对称）*/
  width: 100%;
  box-sizing: border-box;
}

/* ==================== Logo ==================== */
.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #fff;
  flex-shrink: 0;
}
.logo-image {
  height: 32px;
  width: auto;
  display: block;
  border-radius: 3px;
}

/* ==================== 右侧按钮组 ==================== */
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 通用按钮样式 */
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.18);  /* 半透明白底 + 蓝底协调 */
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
  white-space: nowrap;
}
.action-btn:hover {
  background: rgba(255, 255, 255, 0.28);
  border-color: rgba(255, 255, 255, 0.5);
}
.action-btn:active {
  background: rgba(255, 255, 255, 0.22);
}

.action-icon {
  width: 16px;
  height: 16px;
  display: block;
  flex-shrink: 0;
}
.action-label {
  line-height: 1;
}

/* 退出登录按钮：白色文字，hover 时变红 */
.logout-btn {
  color: #fff;
}
.logout-btn:hover {
  background: rgba(245, 108, 108, 0.25);
  border-color: rgba(245, 108, 108, 0.5);
  color: #fff;
}

/* ==================== 响应式：移动端 ≤768px ==================== */
@media (max-width: 768px) {
  .app-header {
    height: 52px;
  }
  .header-inner {
    height: 52px;
    padding: 0 8px;               /* 紧凑边距 */
  }
  .logo-image {
    height: 26px;                 /* 稍小一点 */
  }
  /* 移动端：按钮文字正常显示，紧凑间距 */
  .header-actions {
    gap: 4px;                     /* 按钮间距收窄 */
  }
  .action-btn {
    height: 32px;
    padding: 0 8px;               /* 按钮更紧凑 */
    background: rgba(255, 255, 255, 0.18);
    border-color: rgba(255, 255, 255, 0.35);
  }
  .action-icon {
    width: 14px;
    height: 14px;                 /* 图标同步缩小 */
  }
  .action-label {
    font-size: 12px;              /* 文字稍小 */
  }
  .login-btn,
  .logout-btn {
    padding: 0 6px;              /* 登录按钮更窄 */
  }
}

/* ==================== 响应式：超小屏 ≤375px ==================== */
@media (max-width: 375px) {
  .action-btn {
    padding: 0 8px;
  }
  .action-icon {
    width: 16px;
    height: 16px;
  }
}
</style>
