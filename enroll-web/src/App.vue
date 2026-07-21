<!--
  App.vue · 根组件
  Header 已改为 Vue 组件（AppHeader.vue）
-->
<template>
  <div>
    <AppHeader />
    <router-view />
  </div>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
</script>

<style>
/* 全局复位已在 index.html 中处理 */
/* App.vue 只负责挂载 header，布局全部由各页面自己处理 */
</style>
