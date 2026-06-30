<!--
  ClassCard.vue · 特色班卡片
  展示班级图标、名称、报名时间
-->
<template>
  <el-card class="class-card" shadow="hover" @click="$emit('select', classInfo.id)">
    <!-- 班级图标 -->
    <div class="card-icon">
      <svg viewBox="0 0 40 40" width="40" height="40">
        <rect x="4" y="8" width="32" height="24" rx="3" :fill="iconBg" stroke="none" />
        <line x1="12" y1="16" x2="28" y2="16" stroke="#fff" stroke-width="2" stroke-linecap="round" />
        <line x1="12" y1="21" x2="24" y2="21" stroke="#fff" stroke-width="2" stroke-linecap="round" />
        <line x1="12" y1="26" x2="20" y2="26" stroke="#fff" stroke-width="2" stroke-linecap="round" />
      </svg>
    </div>
    <!-- 班级名称 -->
    <div class="card-name">{{ classInfo.name }}</div>
    <!-- 报名时间 -->
    <div class="card-period">{{ classInfo.period }}</div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  classInfo: { type: Object, required: true }, // 班级数据对象
})
defineEmits(['select'])

// 根据分类选图标背景色：理工类→蓝，经管类→青
const iconBg = computed(() =>
  props.classInfo.category === '理工类' ? '#337eff' : '#06b6d4'
)
</script>

<style scoped>
.class-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: var(--radius-md);
}
.class-card:hover {
  transform: translateY(-4px); /* hover 浮起效果 */
}
.card-icon {
  margin-bottom: 12px;
}
.card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  margin-bottom: 6px;
  /* 最多两行，超出省略 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-period {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 10px;
}
</style>
