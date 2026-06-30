<!--
  TrendChart.vue · 纯 SVG 折线图
  显示 30 天报名趋势，viewBox="0 0 700 280"
  不依赖任何第三方图表库
-->
<template>
  <div class="chart-container">
    <h4 class="chart-title">近30天报名趋势</h4>
    <svg viewBox="0 0 700 280" class="trend-svg">
      <!-- ===== 水平网格线（5 条） ===== -->
      <line v-for="y in gridLines" :key="'g'+y.val"
        :x1="margin.left" :y1="y.svg" :x2="chartW + margin.left" :y2="y.svg"
        stroke="#e2e8f0" stroke-width="1" stroke-dasharray="4,4" />

      <!-- ===== Y 轴刻度标签 ===== -->
      <text v-for="y in gridLines" :key="'t'+y.val"
        :x="margin.left - 8" :y="y.svg + 4"
        text-anchor="end" font-size="11" fill="#94a3b8">{{ y.val }}</text>

      <!-- ===== X 轴日期标签（每隔 5 天标一个） ===== -->
      <text v-for="(pt, i) in xLabels" :key="'xl'+i"
        :x="pt.x" :y="chartH + margin.top + 22"
        text-anchor="middle" font-size="10" fill="#94a3b8">{{ pt.label }}</text>

      <!-- ===== 数据折线 ===== -->
      <polyline
        :points="linePoints"
        fill="none" stroke="#337eff" stroke-width="2.5"
        stroke-linejoin="round" stroke-linecap="round" />

      <!-- ===== 渐变填充区域 ===== -->
      <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#337eff" stop-opacity="0.15" />
        <stop offset="100%" stop-color="#337eff" stop-opacity="0" />
      </linearGradient>
      <polygon :points="areaPoints" fill="url(#areaGrad)" />

      <!-- ===== 数据点圆点 ===== -->
      <circle v-for="(pt, i) in dataPoints" :key="'c'+i"
        :cx="pt.x" :cy="pt.y" r="3.5"
        fill="#337eff" stroke="#fff" stroke-width="1.5">
        <title>{{ trendData[i].date }} — {{ trendData[i].count }} 人</title>
      </circle>

      <!-- ===== 右侧图例 ===== -->
      <rect :x="chartW + margin.left - 110" y="8" width="100" height="24" rx="4" fill="#fff" stroke="#e2e8f0" stroke-width="1" />
      <circle :cx="chartW + margin.left - 98" cy="20" r="4" fill="#337eff" />
      <text :x="chartW + margin.left - 88" y="24" font-size="11" fill="#64748b">报名人数</text>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { trendData } from '../utils/data.js'

// ===== 布局常量 =====
const margin = { left: 50, right: 20, top: 20, bottom: 30 }
const chartW = 700 - margin.left - margin.right  // 630
const chartH = 280 - margin.top - margin.bottom  // 230

// ===== Y 轴网格（0 ~ 100，5 档） =====
const gridLines = [0, 25, 50, 75, 100].map(val => ({
  val,
  svg: margin.top + chartH - (val / 100) * chartH, // 映射到 SVG Y 坐标
}))

// ===== X 轴标签（每 5 天标一个日期） =====
const xLabels = computed(() =>
  trendData
    .map((d, i) => ({ label: d.date, x: margin.left + (i / 29) * chartW }))
    .filter((_, i) => i % 5 === 0 || i === 29) // 第 0,5,10,15,20,25,29 天
)

// ===== 数据点 SVG 坐标 =====
const dataPoints = computed(() =>
  trendData.map((d, i) => ({
    x: margin.left + (i / 29) * chartW,
    y: margin.top + chartH - (d.count / 100) * chartH,
  }))
)

// ===== 折线 points 属性字符串 =====
const linePoints = computed(() =>
  dataPoints.value.map(p => `${p.x},${p.y}`).join(' ')
)

// ===== 面积填充 polygon points =====
const areaPoints = computed(() => {
  const pts = dataPoints.value
  const top = pts.map(p => `${p.x},${p.y}`).join(' ')
  const bottomLeft = `${margin.left},${margin.top + chartH}`
  const bottomRight = `${margin.left + chartW},${margin.top + chartH}`
  return `${pts[0].x},${margin.top + chartH} ${top} ${bottomRight} ${bottomLeft}`
})
</script>

<style scoped>
.chart-container {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 16px;
}
.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.trend-svg {
  width: 100%;
  height: auto;
}
</style>
