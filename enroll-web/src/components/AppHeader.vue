<!--
  AppHeader.vue · 顶部导航栏（问卷式 · 移动端适配）
  极简：仅 Logo + 标题，点击回到首页
  问卷系统不需要复杂导航，学生扫码进来只管填表
-->
<template>
  <header class="app-header">
    <div class="header-inner">
      <!-- Logo 区（点击回首页） -->
      <router-link to="/home" class="logo-area">
        <!-- 校徽 logo（复用 AppFooter 同款 /assets/images/logo.png） -->
        <img :src="logoImg" alt="杭电信工学院" class="logo-image" />
        <span class="logo-text">杭电特色班报名</span>
      </router-link>
    </div>
  </header>
</template>

<script setup>
// 导入校徽 logo（Vite 自动处理路径 + 构建时 hash 防缓存）
import logoImg from '../assets/images/logo.png'
</script>

<style scoped>
.app-header {
  background: #337eff;
  color: #fff;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;                /* 三方向都钉死，跨越整个视口 */
  width: 100%;
  height: 56px;            /* 显式高度，防止父容器影响 */
  z-index: 9999;           /* 最高层级，避开任何 dialog/mask 干扰 */
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transform: translateZ(0); /* 触发 GPU 层，让 fixed 永远生效 */
  -webkit-transform: translateZ(0);
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: flex-start;  /* logo 左对齐 */
  height: 56px;
  padding: 0 0 0 20px;          /* 距左 20px */
  /* 移除 max-width/margin:0 auto，让 logo 真正贴屏幕左边 */
}
.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #fff;
  flex-shrink: 0;
}
/* 校徽 logo：固定高度，宽度自适应（原图 468×80 比例） */
.logo-image {
  height: 32px;
  width: auto;
  display: block;
  border-radius: 3px;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .header-inner {
    height: 52px;
    padding: 0 16px;
  }
  .logo-image {
    height: 28px;  /* 移动端略小 */
  }
  .logo-text {
    font-size: 14px;
  }
}
</style>
