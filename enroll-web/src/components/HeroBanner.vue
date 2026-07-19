<!--
  HeroBanner.vue · 顶部轮播 Banner
  6 张图轮播（01.jpg ~ 06.jpg）+ 文字覆盖层 + 左右箭头 + 圆点指示器 + 页码 + 进度条
  交互：自动播放（4.5s）+ hover 暂停 + 触屏滑动（>50px）+ 键盘左右键
  无障碍：尊重 prefers-reduced-motion
-->
<template>
  <div
    class="hero-banner"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
    @touchstart.passive="onTouchStart"
    @touchend.passive="onTouchEnd"
    tabindex="0"
    @keydown.left="prev"
    @keydown.right="next"
  >
    <!-- 轮播容器：6 张横排 600% 宽，通过 transform 切换 -->
    <div class="slides" :style="{ transform: `translateX(-${current * (100 / slideCount)}%)` }">
      <div
        v-for="(s, i) in slides"
        :key="i"
        class="slide"
        :aria-hidden="i !== current"
      >
        <img :src="s.img" :alt="s.title" class="slide-img" />
      </div>
    </div>

    <!-- 文字内容覆盖层 -->
    <div class="banner-content">
      <div class="welcome-tag">
        <span class="welcome-dot" />
        <span class="welcome-text">{{ slides[current].tag }}</span>
      </div>
      <h2 class="banner-title">{{ slides[current].title }}</h2>
      <p v-if="slides[current].subtitle" class="banner-subtitle">{{ slides[current].subtitle }}</p>
      <button type="button" class="banner-cta" @click="onCtaClick">
        <img :src="docIcon" alt="" class="cta-icon" />
        <span>查看报名须知</span>
      </button>
    </div>

    <!-- 左右箭头 -->
    <button type="button" class="nav-btn nav-prev" aria-label="上一张" @click="prev">
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="white" stroke-width="2.5">
        <polyline points="15 18 9 12 15 6" />
      </svg>
    </button>
    <button type="button" class="nav-btn nav-next" aria-label="下一张" @click="next">
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="white" stroke-width="2.5">
        <polyline points="9 18 15 12 9 6" />
      </svg>
    </button>

    <!-- 圆点指示器 -->
    <div class="indicators" role="tablist">
      <button
        v-for="(s, i) in slides"
        :key="i"
        type="button"
        class="dot"
        :class="{ 'is-active': i === current }"
        :aria-label="`第${i + 1}张`"
        :aria-current="i === current"
        @click="goTo(i)"
      />
    </div>

    <!-- 页码计数器 -->
    <div class="slide-counter">
      <span>{{ String(current + 1).padStart(2, '0') }}</span>
      <span class="counter-sep"> / </span>
      <span>{{ String(slideCount).padStart(2, '0') }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import docIcon from '../assets/images/checklist.svg'

const props = defineProps({
  serverYear: { type: Number, default: () => new Date().getFullYear() },
  classesCount: { type: Number, default: 0 },   // 班级总数（动态副标题用）
  classes:      { type: Array,  default: () => [] },  // 班级列表（动态生成类别、年份）
})

const emit = defineEmits(['cta-click'])

/**
 * 动态副标题生成（根据班级数量自适应）
 * @param {number} n 班级数量
 */
function dynamicSubtitle(n) {
  if (n === 0) return '暂无开放班级，敬请期待'
  if (n === 1) return '1 个特色班级供你选择'
  return `${n} 个特色班级，多元成长路径等你探索`
}

/**
 * 从 classes 自动生成"特色方向概览"
 * @param {Array} classes 班级列表
 * @returns {string} 例：'成电联合培养、ACCA、CFA 等多元方向'
 */
function buildClassesOverview(classes) {
  if (!classes?.length) return '多元成长路径等你探索'
  const allTags = new Set()
  classes.forEach(c => {
    const cats = Array.isArray(c.categories) ? c.categories : Array.isArray(c.categoryNames) ? c.categoryNames : []
    cats.forEach(tag => allTags.add(tag))
  })
  const tags = Array.from(allTags).slice(0, 4)  // 最多取 4 个
  return tags.length ? `${tags.join('、')} 等多元方向` : '多元成长路径等你探索'
}

/**
 * 6 张轮播图配置
 * - tag / title：固定文案（运营文案）
 * - subtitle：第 1/2/4 张动态生成（数据驱动）
 * - 后期可改为接口动态加载（sys_config.banner_texts）
 */
const slides = computed(() => [
  {
    img: new URL('../assets/images/02.jpg', import.meta.url).href,
    tag: '欢迎来到杭电信工特色班',
    title: props.serverYear + ' 特色班报名通道已开启',
    subtitle: dynamicSubtitle(props.classesCount),  // 变数：班级数量
  },
  {
    img: new URL('../assets/images/01.jpg', import.meta.url).href,
    tag: '点亮你的未来',
    title: '选择特色班 成就更好的自己',
    subtitle: buildClassesOverview(props.classes),  // 变数：班级类别
  },
  {
    img: new URL('../assets/images/03.jpg', import.meta.url).href,
    tag: '春暖花开 等你来',
    title: '在最美的地方 做最棒的自己',
    subtitle: '镜湖之畔，信工之窗，让梦想启航',
  },
  {
    img: new URL('../assets/images/04.jpg', import.meta.url).href,
    tag: '传承与创新并重',
    title: '历史与现代交汇',
    subtitle: '承学府底蕴，开时代新篇',
  },
  {
    img: new URL('../assets/images/05.jpg', import.meta.url).href,
    tag: '静水流深 见贤思齐',
    title: '镜湖映照 梦想起航',
    subtitle: '名师领航，同学砥砺，共赴卓越',
  },
  {
    img: new URL('../assets/images/06.jpg', import.meta.url).href,
    tag: '门为你开',
    title: '筑梦杭电信工 不负韶华',
    subtitle: '扫码填报 / 在线查询 / 录取查询一站直达',
  },
])

const slideCount = computed(() => slides.value.length)
const current = ref(0)            // 当前 slide 索引
const isPaused = ref(false)       // hover 暂停标记
const AUTO_INTERVAL = 5000        // 自动播放间隔（ms）
const SWIPE_THRESHOLD = 50        // 触屏滑动阈值（px）

let autoTimer = null              // 切换定时器
let startX = 0                    // 触屏起点 X

/** 计算当前 slide 在容器中的偏移百分比（6 张 → 每张 100/6 %） */
const translatePct = computed(() => current.value * (100 / slideCount.value))

function next() {
  current.value = (current.value + 1) % slideCount.value
  resetProgress()
}

function prev() {
  current.value = (current.value - 1 + slideCount.value) % slideCount.value
  resetProgress()
}

function goTo(i) {
  current.value = i
  resetProgress()
}

/** 重置进度条到 0 */
function resetProgress() {
  progress.value = 0
}

/** 启动自动播放 */
function startAuto() {
  stopAuto()
  autoTimer = setInterval(next, AUTO_INTERVAL)
}

function stopAuto() {
  if (autoTimer) clearInterval(autoTimer)
  autoTimer = null
}

function onMouseEnter() {
  isPaused.value = true
  stopAuto()
}
function onMouseLeave() {
  isPaused.value = false
  startAuto()
}

function onTouchStart(e) {
  startX = e.touches[0].clientX
}

function onTouchEnd(e) {
  const dx = e.changedTouches[0].clientX - startX
  if (Math.abs(dx) >= SWIPE_THRESHOLD) {
    if (dx < 0) next()
    else prev()
  }
}

function onCtaClick() {
  emit('cta-click')
}

onMounted(() => {
  startAuto()
  // 检查用户系统是否启用了减少动画偏好
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduceMotion) {
    stopAuto()
  }
})

onUnmounted(() => {
  stopAuto()
})
</script>

<style scoped>
/* ==================== 容器 ==================== */
.hero-banner {
  position: relative;
  width: 100%;
  aspect-ratio: 32 / 9;           /* 桌面端 32:9 超扁型 */
  overflow: hidden;
  border-radius: 14px;
  background: #1e293b;            /* 占位背景：图加载前显示深色 */
  box-shadow:
    0 4px 20px rgba(59, 123, 248, 0.15),
    0 1px 3px rgba(0, 0, 0, 0.06);
  outline: none;
  touch-action: pan-y;            /* 允许垂直滚动，水平交给 JS */
}
.hero-banner:focus-visible {
  box-shadow:
    0 4px 20px rgba(59, 123, 248, 0.15),
    0 1px 3px rgba(0, 0, 0, 0.06),
    0 0 0 3px rgba(59, 123, 248, 0.3);
}

/* ==================== 轮播框架 ==================== */
.slides {
  display: flex;
  width: 600%;                    /* 6 张横排 */
  height: 100%;
  transition: transform 0.7s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  will-change: transform;
}
.slide {
  width: calc(100% / 6);          /* 每张占 1/6 */
  height: 100%;
  flex-shrink: 0;
  position: relative;
}
.slide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ==================== 渐变遮罩 ==================== */
.slide::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    rgba(30, 41, 82, 0.70) 0%,
    rgba(30, 58, 138, 0.40) 45%,
    rgba(30, 41, 82, 0) 72%
  );
  pointer-events: none;
}

/* ==================== 文字内容 ==================== */
.banner-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 60%;
  height: 100%;
  padding: 28px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
  z-index: 5;
  pointer-events: none;           /* 让 hover 穿透到容器，触发暂停 */
}
.banner-content > * { pointer-events: auto; }

/* 欢迎标签 */
.welcome-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.13);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  margin-bottom: 10px;
}
.welcome-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.6);
  animation: pulse 2s ease-in-out infinite;
}
.welcome-text {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.03em;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.5; transform: scale(1.3); }
}

/* 主标题 */
.banner-title {
  font-size: clamp(20px, 2.8vw, 30px);
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.01em;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.35);
  margin: 0 0 6px;
}

/* 副标题 */
.banner-subtitle {
  font-size: clamp(13px, 1.2vw, 15px);
  font-weight: 300;
  line-height: 1.5;
  opacity: 0.88;
  max-width: 420px;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.25);
  margin: 0 0 16px;
}

/* CTA 按钮 */
.banner-cta {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  background: rgba(255, 255, 255, 0.92);
  color: #1e40af;
  font-size: 13px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
  font-family: inherit;
  transition: transform 0.2s, background 0.2s, box-shadow 0.2s;
}
.banner-cta:hover {
  background: #fff;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}
.banner-cta:active { transform: scale(0.98); }
.banner-cta:focus-visible {
  outline: 2px solid #3b7bf8;
  outline-offset: 2px;
}
.cta-icon { width: 16px; height: 16px; }

/* ==================== 左右箭头 ==================== */
.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1.5px solid rgba(255, 255, 255, 0.55);
  cursor: pointer;
  z-index: 6;
  opacity: 0;
  transition: opacity 0.25s, background 0.2s, border-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.hero-banner:hover .nav-btn { opacity: 1; }
.nav-btn:hover {
  background: rgba(59, 123, 248, 0.8);
  border-color: rgba(255, 255, 255, 0.8);
}
.nav-btn:focus-visible { opacity: 1; }
.nav-prev { left: 12px; }
.nav-next { right: 12px; }

/* ==================== 圆点指示器 ==================== */
.indicators {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 7px;
  z-index: 6;
  padding: 4px 10px;
  border-radius: 100px;
  background: rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.38);
  border: none;
  padding: 0;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.dot:hover {
  transform: scale(1.2);
  background: rgba(255, 255, 255, 0.7);
}
.dot.is-active {
  width: 22px;
  border-radius: 4px;
  background: #fff;
}

/* ==================== 页码计数器 ==================== */
.slide-counter {
  position: absolute;
  bottom: 12px;
  right: 18px;
  z-index: 6;
  padding: 3px 10px;
  border-radius: 100px;
  background: rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.05em;
}
.counter-sep { opacity: 0.5; margin: 0 2px; }

/* ==================== 响应式 ≤768px ==================== */
@media (max-width: 768px) {
  .hero-banner {
    aspect-ratio: 16 / 9;         /* 移动端切 16:9 */
    border-radius: 10px;
  }
  .banner-content {
    width: 88%;
    padding: 20px 22px;
  }
  .banner-title { font-size: 20px; }
  .banner-subtitle { font-size: 13px; max-width: 100%; }
  .nav-btn {
    opacity: 1;                   /* 移动端常显 */
    width: 30px;
    height: 30px;
  }
  .slide-counter { display: none; }
  .dot { width: 6px; height: 6px; }
  .dot.is-active { width: 16px; }
}

/* ==================== 减少动画 ==================== */
@media (prefers-reduced-motion: reduce) {
  .slides { transition: none; }
  .welcome-dot { animation: none; }
  .banner-cta { transition: none; }
  .dot { transition: none; }
}
</style>