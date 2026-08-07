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
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth.js'
import logoImg from '../assets/images/logo.png'
import lockIcon from '../assets/images/lock.svg'
import folderIcon from '../assets/images/folder.svg'

const router = useRouter()
const auth = useAuthStore()
// 响应式登录态：login/logout 后自动更新（不再手动读 localStorage + 监听事件）
const { isLoggedIn } = storeToRefs(auth)

function goLogin() {
  // 当前已在登录页 → 提示用户（避免重复点击感觉没反应）
  if (router.currentRoute.value.path === '/student-login') {
    ElMessage({
      message: '您已进入登录页面',
      type: 'warning',
      customClass: 'app-header-message--yellow',  // 自定义黄底样式
    })
    return
  }
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
    // 统一走 Pinia：清 store + localStorage + 派发 login_changed/application_changed
    // （HomePage 监听 application_changed 会重刷已报名标记）
    auth.logout()
    ElMessage.success('已退出登录')
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
    background: transparent;
    border: none;                 /* 去掉边框 */
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

/* ==================== ElMessage 自定义黄底 ==================== */
/* 全局覆盖 ElMessage.warning 的背景色为黄色 */
.app-header-message--yellow.el-message.el-message--warning {
  background-color: #facc15;       /* Tailwind yellow-400 */
  border-color: #facc15;
  color: #1f2937;                  /* 深灰字，跟黄色对比清晰 */
}
.app-header-message--yellow.el-message.el-message--warning .el-message__content {
  color: #1f2937;
}
</style>
