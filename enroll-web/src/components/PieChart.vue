<!--
  PieChart.vue · 纯 SVG 环形图
  显示各班级报名人数分布，viewBox="0 0 260 260"
  不依赖任何第三方图表库
-->
<template>
  <div class="chart-container">
    <h4 class="chart-title">班级报名分布</h4>
    <svg viewBox="0 0 260 260" class="pie-svg">
      <!-- ===== 扇形切片 ===== -->
      <path v-for="(slice, i) in slices" :key="'s'+i"
        :d="slice.path"
        :fill="slice.color"
        stroke="#fff" stroke-width="2">
        <title>{{ slice.name }}: {{ slice.count }} 人 ({{ slice.percent }}%)</title>
      </path>

      <!-- ===== 中心白色圆（形成环形） ===== -->
      <circle cx="130" cy="130" r="55" fill="#fff" />

      <!-- ===== 中心文字：总数 ===== -->
      <text x="130" y="124" text-anchor="middle" font-size="24" font-weight="700" fill="#1e293b">
        {{ total }}
      </text>
      <text x="130" y="146" text-anchor="middle" font-size="11" fill="#94a3b8">
        累计报名
      </text>
    </svg>

    <!-- ===== 下方图例 ===== -->
    <div class="legend">
      <div v-for="(slice, i) in slices" :key="'l'+i" class="legend-item">
        <span class="legend-dot" :style="{ background: slice.color }"></span>
        <span class="legend-label">{{ slice.shortName }}</span>
        <span class="legend-val">{{ slice.count }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { classes, generateMockApplications, classColors } from '../utils/data.js'

// ===== 从 mock 数据统计各班人数 =====
const mockApps = generateMockApplications()

const slices = computed(() => {
  // 按 classId 统计
  const countMap = {}
  mockApps.forEach(a => {
    countMap[a.classId] = (countMap[a.classId] || 0) + 1
  })

  const total = mockApps.length
  const data = classes.map((c, i) => ({
    name: c.name,
    shortName: c.name.length > 12 ? c.name.slice(0, 12) + '...' : c.name,
    count: countMap[c.id] || 0,
    color: classColors[i],
    percent: total > 0 ? Math.round(((countMap[c.id] || 0) / total) * 100) : 0,
  })).filter(d => d.count > 0) // 过滤无数据的班

  // 计算每个扇形的 SVG arc path
  const cx = 130, cy = 130, r = 100
  let startAngle = -Math.PI / 2 // 从 12 点方向开始

  return data.map(d => {
    const sliceAngle = (d.count / total) * 2 * Math.PI
    const endAngle = startAngle + sliceAngle

    // 起止点坐标
    const x1 = cx + r * Math.cos(startAngle)
    const y1 = cy + r * Math.sin(startAngle)
    const x2 = cx + r * Math.cos(endAngle)
    const y2 = cy + r * Math.sin(endAngle)

    // 大弧标志：弧 > 180° 时 largeArcFlag=1
    const largeArcFlag = sliceAngle > Math.PI ? 1 : 0

    // SVG arc path: M cx,cy L x1,y1 A r,r 0 largeArcFlag,1 x2,y2 Z
    const path = `M ${cx} ${cy} L ${x1.toFixed(1)} ${y1.toFixed(1)} A ${r} ${r} 0 ${largeArcFlag} 1 ${x2.toFixed(1)} ${y2.toFixed(1)} Z`

    const result = { ...d, path }
    startAngle = endAngle
    return result
  })
})

const total = computed(() => mockApps.length)
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
.pie-svg {
  width: 100%;
  height: auto;
}
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 12px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}
.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.legend-label {
  color: var(--text-secondary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.legend-val {
  color: var(--text-primary);
  font-weight: 600;
}
</style>
