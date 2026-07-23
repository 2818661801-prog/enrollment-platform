<!--
  AppFooter.vue · 底部信息（数据来源：杭州电子科技大学信息工程学院官网 hziee.edu.cn）
  3 列深色底 → 移动端纵向堆叠
-->
<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <!-- 第 1 列：学校信息（桌面端保留；移动端居顶 logo + 地址） -->
      <div class="footer-col footer-col--school">
        <div class="footer-logo">
          <img :src="logoImg" alt="杭州电子科技大学信息工程学院" class="footer-logo-img" />
        </div>
        <!-- 桌面端：保留原文字地址；移动端：隐藏（CSS 处理） -->
        <p class="footer-addr-text">浙江省杭州市临安区青山湖街道杭电路1号</p>
        <p class="footer-addr-text">邮编：311305</p>

        <!-- 移动端独有：地址条 + 邮编 chip -->
        <div class="footer-addr-row">
          <span class="footer-loc">
            <svg viewBox="0 0 24 24" class="footer-loc-icon"><path d="M12 21s-7-5.6-7-11a7 7 0 0 1 14 0c0 5.4-7 11-7 11Z"/><circle cx="12" cy="10" r="2.6"/></svg>
            <span>浙江省杭州市临安区青山湖街道杭电路1号</span>
          </span>
          <span class="footer-zip-chip"><b>邮编</b>311305</span>
        </div>

        <!-- 移动端独有：装饰分割线 -->
        <div class="footer-rule" aria-hidden="true"></div>
      </div>

      <!-- 移动端：联系方式 + 快速链接 放在同一行左右分布 -->
      <div class="footer-row">
      <!-- 第 2 列：联系方式（卡片行，点击复制） -->
      <div class="footer-col footer-col--contact">
        <h4>联系方式</h4>
        <div class="footer-contact-list">
          <div class="footer-crow" data-copy="0571-58619116">
            <span class="footer-crow-ic">
              <svg viewBox="0 0 24 24"><path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z"/></svg>
            </span>
            <span class="footer-crow-text">
              <span class="footer-crow-lab">招生办电话</span>
              <span class="footer-crow-val footer-mono">0571-58619116</span>
            </span>
          </div>

          <div class="footer-crow" data-copy="0571-58619115">
            <span class="footer-crow-ic">
              <svg viewBox="0 0 24 24"><path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z"/></svg>
            </span>
            <span class="footer-crow-text">
              <span class="footer-crow-lab">学院办公室</span>
              <span class="footer-crow-val footer-mono">0571-58619115</span>
            </span>
          </div>

          <div class="footer-crow" data-copy="***REMOVED***">
            <span class="footer-crow-ic">
              <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>
            </span>
            <span class="footer-crow-text">
              <span class="footer-crow-lab">招生办邮箱</span>
              <span class="footer-crow-val">***REMOVED***</span>
            </span>
          </div>

          <div class="footer-crow" data-copy="***REMOVED***">
            <span class="footer-crow-ic">
              <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>
            </span>
            <span class="footer-crow-text">
              <span class="footer-crow-lab">学院邮箱</span>
              <span class="footer-crow-val">***REMOVED***</span>
            </span>
          </div>

          <div class="footer-crow footer-crow--static">
            <span class="footer-crow-ic">
              <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
            </span>
            <span class="footer-crow-text">
              <span class="footer-crow-lab">咨询时间</span>
              <span class="footer-crow-val">每天 8:00 — 20:00</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 第 3 列：快速链接（编号目录） -->
      <div class="footer-col footer-col--links">
        <h4>快速链接</h4>
        <nav class="footer-link-list">
          <a class="footer-lrow" href="https://www.hziee.edu.cn" target="_blank">
            <span class="footer-lrow-no">01</span>
            <span class="footer-lrow-nm">学院官网</span>
            <svg class="footer-lrow-ar" viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </a>
          <a class="footer-lrow" href="https://www.hziee.edu.cn/zs" target="_blank">
            <span class="footer-lrow-no">02</span>
            <span class="footer-lrow-nm">招生网</span>
            <svg class="footer-lrow-ar" viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </a>
          <a class="footer-lrow" href="https://www.hziee.edu.cn/xxgka/" target="_blank">
            <span class="footer-lrow-no">03</span>
            <span class="footer-lrow-nm">信息公开</span>
            <svg class="footer-lrow-ar" viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </a>
          <a class="footer-lrow" href="https://www.hziee.edu.cn/46/list.htm" target="_blank">
            <span class="footer-lrow-no">04</span>
            <span class="footer-lrow-nm">学院介绍</span>
            <svg class="footer-lrow-ar" viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </a>
          <a class="footer-lrow" href="https://www.hziee.edu.cn/tusg/" target="_blank">
            <span class="footer-lrow-no">05</span>
            <span class="footer-lrow-nm">图书馆</span>
            <svg class="footer-lrow-ar" viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </a>
        </nav>
      </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="footer-bottom">
      <p>© 2026 杭州电子科技大学信息工程学院 · 招生办公室 · 保留所有权利</p>
      <p style="margin-top: 4px;">教育部代码：13279（浙江省代码：0095）</p>
    </div>

    <!-- 自定义 Toast（移动端） -->
    <div class="footer-toast" id="footer-toast" aria-live="polite">
      <svg viewBox="0 0 24 24"><path d="M20 6 9 17l-5-5"/></svg>
      <span class="footer-toast-txt">已复制</span>
    </div>
  </footer>
</template>

<script setup>
// 信息来源于杭州电子科技大学信息工程学院官网 https://www.hziee.edu.cn
import { onMounted, onUnmounted, ref } from 'vue'
import logoImg from '../assets/images/logo.png' // Vite 自动处理图片路径

// 点击复制 + Toast 提示
const toastTimer = ref(null)

function showToast(msg) {
  const toast = document.getElementById('footer-toast')
  if (!toast) return
  const txt = toast.querySelector('.footer-toast-txt')
  if (txt) txt.textContent = msg
  toast.classList.add('show')
  if (toastTimer.value) clearTimeout(toastTimer.value)
  toastTimer.value = setTimeout(() => {
    toast.classList.remove('show')
  }, 1600)
}

function fallbackCopy(text) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  try {
    document.execCommand('copy')
    return true
  } catch (e) {
    return false
  } finally {
    document.body.removeChild(ta)
  }
}

async function onCopyClick(e) {
  const target = e.target.closest('[data-copy]')
  if (!target) return
  const text = target.getAttribute('data-copy')
  if (!text) return

  let ok = false
  if (navigator.clipboard && navigator.clipboard.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      ok = true
    } catch (err) {
      ok = fallbackCopy(text)
    }
  } else {
    ok = fallbackCopy(text)
  }
  showToast(ok ? `已复制：${text}` : '复制失败，请手动选择')
}

onMounted(() => {
  document.addEventListener('click', onCopyClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onCopyClick)
  if (toastTimer.value) clearTimeout(toastTimer.value)
})
</script>

<style scoped>
.app-footer {
  background: #1e293b;
  color: #cbd5e1;
  padding: 10px 32px 0;
  margin-top: 12px;
}

.footer-inner {
  display: flex;
  gap: 60px;                         /* 三列之间间距加大，分散 */
  max-width: 1400px;               /* 放宽，给 logo 列更多空间 */
  margin: 0 auto;
  flex-wrap: wrap;
  justify-content: space-between;  /* 三列均分间距 */
}

/* ============ 桌面端：回归纯文本样式 ============ */
.footer-col--school {
  display: flex;
  flex-direction: column;
}
.footer-addr-text {
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}
.footer-addr-row,
.footer-rule {
  display: none; /* 桌面端隐藏 */
}

/* 联系方式（桌面端：纯文本列表，不带卡片框 + 不带蓝色高亮） */
.footer-contact-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.footer-crow {
  display: block;          /* 桌面端：每个联系方式独立成行，文本左对齐到列宽内 */
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
  transition: none;
  cursor: default;
}
.footer-crow-ic {
  display: none;           /* 桌面端隐藏图标 */
}
.footer-crow-text {
  display: block;          /* 桌面端：标签 + 值上下排列 */
}
.footer-crow-lab {
  display: none;           /* 桌面端隐藏"招生办电话"标签（保持简洁列表风格） */
}
.footer-crow-val {
  font-size: 13px;
  color: #cbd5e1;
  word-break: break-all;
  display: block;
}
.footer-mono {
  font-family: inherit;
  letter-spacing: normal;
}

/* 快速链接（桌面端：纯文本列表，不带编号 + 不带 hover 高亮） */
.footer-link-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.footer-lrow {
  display: block;
  padding: 0;
  text-decoration: none;
  color: #94a3b8;
  border-bottom: none;
  transition: none;
}
.footer-lrow:hover {
  color: #fff;
  padding-left: 0;
}
.footer-lrow-no,
.footer-lrow-ar {
  display: none;           /* 桌面端隐藏编号和箭头 */
}
.footer-lrow-nm {
  font-size: 13px;
}

/* 自定义 Toast（桌面端隐藏） */
.footer-toast {
  display: none;
}

/* 桌面端：.footer-row 让子元素穿透，直接参与 .footer-inner 的 flex 3 列布局 */
.footer-row {
  display: contents;
}
.footer-row .footer-col {
  /* 桌面端：每列占 1/3 等宽 */
  flex: 1 1 0;
  min-width: 260px;
}

.footer-col {
  flex: 1 1 0;                     /* 三列等宽 */
  min-width: 260px;                /* logo 加大后列宽也要增加 */
  text-align: center;              /* 所有列内容居中 */
}

.footer-col h4 {
  color: #fff;
  font-size: 15px;
  margin: 0 auto 10px;             /* 居中 + 下方间距 */
  padding-bottom: 6px;
  border-bottom: 1px solid #475569; /* 略亮的分隔线 */
  display: inline-block;           /* 边框只跟文字宽度 */
}

.footer-col p {
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}

.footer-logo {
  display: flex;
  align-items: center;
  justify-content: center;         /* logo 居中 */
  gap: 10px;
  margin-bottom: 6px;
}

.footer-logo-img {
  border-radius: 4px;
  display: block;
  max-width: 450px;
  height: auto;
  margin: 0 auto 10px;             /* logo 居中 */
}

.footer-link {
  color: #94a3b8;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #fff;
}

.footer-bottom {
  text-align: center;
  padding: 10px 0;
  margin-top: 12px;
  border-top: 1px solid #334155;
  font-size: 12px;
  color: #64748b;
}

/* 桌面端：.footer-row 让子元素穿透到 .footer-inner，参与 3 列布局 */
.footer-row {
  display: contents;
}
.footer-row .footer-col {
  flex: 1 1 0;
  min-width: 260px;
}

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .app-footer {
    padding: 16px 12px 0;
    margin-top: 16px;
  }

  .footer-inner {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }

  /* 移动端：联系方式 + 快速链接 同一行左右分布 */
  .footer-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    padding: 0 12px;
    box-sizing: border-box;
  }
  .footer-row .footer-col {
    flex: 1 1 0;
    min-width: 0;
    text-align: left;          /* 两列内左对齐 */
  }

  /* 学校信息列：移动端居顶 logo + 地址条 */
  .footer-col--school {
    align-items: center;
  }
  .footer-logo {
    margin-bottom: 8px;
  }
  .footer-logo-img {
    max-width: 180px;        /* logo 调大 140→180 */
    height: auto;
    border-radius: 6px;
    margin-bottom: 0;
  }
  .footer-addr-text {
    display: none;            /* 桌面端文字地址在移动端隐藏 */
  }
  .footer-addr-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 6px 10px;
    font-size: 11px;          /* 地址字号缩小 12→11 */
    color: #cbd5e1;
    margin-top: 4px;
  }
  .footer-loc {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  .footer-loc-icon {
    width: 13px;
    height: 13px;
    stroke: #337eff;
    fill: none;
    stroke-width: 1.8;
    flex: 0 0 auto;
  }
  .footer-zip-chip {
    font-family: 'SF Mono', monospace;
    font-size: 11px;
    color: #93c5fd;
    border: 1px solid rgba(51, 126, 255, 0.35);
    border-radius: 999px;
    padding: 3px 10px;
    background: rgba(51, 126, 255, 0.08);
    letter-spacing: 1px;
  }
  .footer-zip-chip b {
    font-weight: 500;
    color: #94a3b8;
    font-family: 'Noto Sans SC', system-ui, sans-serif;
    font-size: 10px;
    letter-spacing: 0.5px;
    margin-right: 4px;
  }

  /* 装饰分割线（不带菱形） */
  .footer-rule {
    display: block;
    position: relative;
    height: 1px;
    margin: 10px 8px 2px;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.12) 12%, rgba(255, 255, 255, 0.12) 88%, transparent);
  }
  .footer-rule::after {
    display: none;          /* 不显示菱形点 */
  }

  /* 联系方式 + 快速链接 列 */
  .footer-col--contact,
  .footer-col--links {
    min-width: unset;
    text-align: left;
  }
  .footer-col {
    min-width: unset;
  }
  .footer-col h4 {
    font-size: 12px;
    margin: 0 0 10px;
    padding-bottom: 4px;
    border-bottom: 1px solid rgba(51, 126, 255, 0.25);
    display: inline-block;
    color: #fff;
  }
  .footer-col p {
    font-size: 10px;
    line-height: 1.35;
    margin: 0;
  }

  /* 联系方式（移动端极简版：纯文本行，无卡片框，跟快速链接对齐） */
  .footer-contact-list {
    gap: 2px;                   /* 行间距 2px */
  }
  .footer-crow {
    display: flex;             /* 覆盖桌面端的 display: block */
    align-items: baseline;
    gap: 4px;
    padding: 2px 0;            /* 几乎无 padding，跟快速链接 row 等高 */
    border: none;
    border-radius: 0;
    background: transparent;
    transition: none;
    cursor: pointer;
  }
  .footer-crow-ic {
    display: none;             /* 移动端隐藏图标（节省高度） */
  }
  .footer-crow-text {
    display: flex;
    flex-direction: row;       /* 标签 + 值横排（一行一项） */
    align-items: baseline;
    gap: 6px;
    min-width: 0;
    flex: 1;
    flex-wrap: nowrap;         /* 强制单行，不让邮箱换行成两行 */
    overflow: hidden;          /* 超长截断 */
  }
  .footer-crow-lab {
    display: inline;
    font-size: 11px;
    color: #94a3b8;
    flex: 0 0 auto;
    margin-bottom: 0;
    white-space: nowrap;       /* 标签强制不换行 */
  }
  .footer-crow-val {
    font-size: 10px;           /* 邮箱字号再缩小，让单行能容纳 */
    color: #cbd5e1;
    white-space: nowrap;       /* 值强制单行 */
    overflow: hidden;
    text-overflow: ellipsis;   /* 超长省略号 */
    flex: 1;
    min-width: 0;
  }

  /* 编号目录（移动端紧凑版） */
  .footer-lrow {
    display: flex;               /* 覆盖桌面端的 display: block */
    align-items: center;
    gap: 8px;
    padding: 3px 4px;            /* 上下 padding 减半 */
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    text-decoration: none;
    color: #94a3b8;
    transition: color 0.25s, padding-left 0.25s;
  }
  .footer-lrow:last-child {
    border-bottom: none;
  }
  .footer-lrow-no {
    display: block;              /* 覆盖桌面端的 display: none */
    font-size: 11px;             /* 编号字号缩小 */
    font-style: italic;
    color: rgba(51, 126, 255, 0.6);
    font-family: 'SF Mono', monospace;
    flex: 0 0 auto;
  }
  .footer-lrow-nm {
    font-size: 11px;             /* 链接字号缩小 */
    flex: 1;
  }
  .footer-lrow-ar {
    display: block;              /* 覆盖桌面端的 display: none */
    width: 12px;
    height: 12px;
    stroke: #94a3b8;
    fill: none;
    stroke-width: 1.8;
    flex: 0 0 auto;
  }

  /* 底部版权 */
  .footer-bottom {
    padding: 8px 0 6px;
    margin-top: 4px;          /* 再压缩 8→4 */
    font-size: 10px;
    border-top: 1px solid rgba(255, 255, 255, 0.08);   /* 顶部加细分隔线，视觉上紧贴上方内容 */
  }
  .footer-bottom p {
    line-height: 1.4;
    margin: 0;
  }

  /* 自定义 Toast（移动端启用） */
  .footer-toast {
    display: flex;
    position: fixed;
    left: 50%;
    bottom: 24px;
    transform: translate(-50%, 16px);
    background: #1c2c47;
    border: 1px solid rgba(51, 126, 255, 0.4);
    color: #eaf0fa;
    padding: 9px 16px;
    border-radius: 8px;
    font-size: 12px;
    box-shadow: 0 12px 32px -8px rgba(0, 0, 0, 0.6);
    align-items: center;
    gap: 8px;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.3s, transform 0.3s;
    z-index: 1000;
  }
  .footer-toast.show {
    opacity: 1;
    transform: translate(-50%, 0);
  }
  .footer-toast svg {
    width: 14px;
    height: 14px;
    stroke: #4ade80;
    fill: none;
    stroke-width: 2;
  }
}
</style>
