<!--
  AppFooter.vue · 底部信息（数据来源：杭州电子科技大学信息工程学院官网 hziee.edu.cn）
  3 列深色底 → 移动端纵向堆叠
-->
<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <!-- 第 1 列：学校信息 -->
      <div class="footer-col">
        <div class="footer-logo">
          <!-- 杭电信工学院校徽 -->
          <img :src="logoImg" alt="校徽" width="468" height="80" class="footer-logo-img" />
        </div>
        <p>浙江省杭州市临安区青山湖街道杭电路1号</p>
        <p>邮编：311305</p>
      </div>

      <!-- 第 2 列：联系方式 -->
      <div class="footer-col">
        <h4>联系方式</h4>
        <p>招生办电话：0571-58619116</p>
        <p>学院办公室：0571-58619115</p>
        <p>招生办邮箱：***REMOVED***</p>
        <p>学院邮箱：***REMOVED***</p>
        <p>咨询时间：每天 8:00 — 20:00</p>
      </div>

      <!-- 第 3 列：快速链接 -->
      <div class="footer-col footer-col--center">
        <h4>快速链接</h4>
        <p><a href="https://www.hziee.edu.cn" target="_blank" class="footer-link">学院官网</a></p>
        <p><a href="https://www.hziee.edu.cn/zs" target="_blank" class="footer-link">招生网</a></p>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="footer-bottom">
      <p>© 2026 杭州电子科技大学信息工程学院 · 招生办公室 · 保留所有权利</p>
      <p style="margin-top: 4px;">教育部代码：13279（浙江省代码：0095）</p>
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
  padding: 32px 32px 0;
  margin-top: 48px;
}

.footer-inner {
  display: flex;
  gap: 0;
  max-width: 1400px;               /* 放宽，给 logo 列更多空间 */
  margin: 0 auto;
  flex-wrap: wrap;
  justify-content: space-between;  /* 三列均分间距 */
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
  padding: 18px 0;
  margin-top: 24px;
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

  .footer-inner {
    flex-direction: column;
    gap: 8px;                       /* 列间距再缩 */
    text-align: center;
    justify-content: center;
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
  .footer-bottom p { line-height: 1.4; }
}
</style>
