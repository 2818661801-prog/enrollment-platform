<!--
  App.vue · 根组件
  Header 已写在 index.html 中（纯 HTML，不走 Vue），这里只挂路由
-->
<template>
  <router-view />
</template>

<script setup>
// Header 不再是 Vue 组件，直接写在 index.html 里
</script>

<style>
/* 全局复位已在 index.html 中处理 */
</style>
