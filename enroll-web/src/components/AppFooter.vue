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
import logoImg from '../assets/images/logo.png' // Vite 自动处理图片路径
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

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .app-footer {
    padding: 10px 12px 0;          /* 进一步压缩 */
    margin-top: 16px;
  }

  /* 桌面端 footer-inner 是横向 3 列（flex row）；移动端改纵向 */
  .footer-inner {
    flex-direction: column;
    gap: 8px;                       /* 列间距再缩 */
    text-align: center;
    justify-content: center;
  }

  /* 桌面端：直接子 .footer-col 是 3 个；移动端：第 1 个是学校信息，第 2/3 在 .footer-row 里 */
  /* 这里 .footer-row 是移动端独有的，桌面端没有这个容器 */
  .footer-row {
    display: flex;                   /* 移动端：横排左右分布 */
    justify-content: space-between;
    align-items: flex-start;
    padding: 0 16px;                /* 左右各 16px 留白 */
    box-sizing: border-box;
    gap: 8px;
  }
  .footer-row .footer-col {
    flex: 0 0 auto;
    min-width: unset;
    text-align: center;             /* 内容居中 */
  }

  .footer-col {
    min-width: unset;
  }

  .footer-col h4 {
    font-size: 12px;
    margin: 0 auto 3px;
    padding-bottom: 2px;
  }

  .footer-col p {
    font-size: 10px;
    line-height: 1.35;              /* 紧凑行高 */
    margin: 0;                       /* 重置浏览器默认 p margin，避免行距过大 */
  }

  .footer-logo {
    margin-bottom: 0;
  }

  .footer-logo-img {
    max-width: 250px;
    margin-bottom: 2px;
  }

  .footer-bottom {
    padding: 8px 0;
    margin-top: 10px;
    font-size: 9px;
  }
  .footer-bottom p { line-height: 1.4; margin: 0; }
}
</style>
