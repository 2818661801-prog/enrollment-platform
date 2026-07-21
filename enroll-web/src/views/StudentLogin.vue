<!--
  StudentLogin.vue · 学生手机验证码登录页
  流程：输入手机号 → 发送验证码 → 填验证码 → 登录 → JWT 存 localStorage → 跳转 /my-applications
-->
<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧蓝色装饰竖条 -->
      <div class="card-accent-top" />

      <div class="card-body">
        <div class="login-header">
          <h2 class="login-title">学生登录</h2>
          <p class="login-sub">报名查询请先登录</p>
        </div>

        <!-- 手机号输入 -->
        <el-input
          v-model="phoneCode.phone.value"
          placeholder="请输入手机号"
          maxlength="11"
          class="phone-input"
          @keyup.enter="phoneCode.onSendCode"
        >
          <template #prefix>
            <span class="phone-prefix">+86</span>
          </template>
        </el-input>

        <!-- 验证码输入 + 发送验证码 并排 -->
        <div class="code-row">
          <el-input
            v-model="phoneCode.code.value"
            placeholder="请输入验证码"
            maxlength="6"
            class="code-input"
            @keyup.enter="onLogin"
          />
          <el-button
            type="primary"
            class="send-btn"
            :disabled="phoneCode.countdown.value > 0"
            :loading="phoneCode.sending.value"
            @click="phoneCode.onSendCode"
          >
            {{ phoneCode.countdown.value > 0 ? `${phoneCode.countdown.value}s` : '获取验证码' }}
          </el-button>
        </div>

        <!-- 登录按钮 -->
        <el-button
          type="primary"
          class="login-btn"
          :loading="phoneCode.logging.value"
          @click="onLogin"
        >
          登录
        </el-button>

        <!-- 底部操作栏 -->
        <div class="bottom-bar">
          <el-button text @click="goHome">去报名首页</el-button>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePhoneCode } from '../composables/usePhoneCode.js'
import AppFooter from '../components/AppFooter.vue'

const router = useRouter()
const phoneCode = usePhoneCode()

// 已登录 → 自动跳转到我的报名，不让访问登录页
onMounted(() => {
  if (localStorage.getItem('student_token')) {
    router.replace('/my-applications')
  }
})

async function onLogin() {
  const ok = await phoneCode.onLogin()
  if (ok) {
    window.dispatchEvent(new Event('login_changed'))
    router.push('/my-applications')
  }
}

function goHome() {
  router.push('/home')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;             /* 水平居中 .login-card */
  background: #f5f6fa;
  padding-top: 56px;               /* fixed header 高度，防止内容被遮挡 */
}
.login-card {
  display: flex;
  flex-direction: column;
  width: 400px;
  margin-top: 15vh;               /* 距顶部约 15% */
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.07);
  overflow: hidden;
}

/* 顶部蓝色装饰条 — 签名元素 */
.card-accent-top {
  height: 4px;
  background: linear-gradient(90deg, #337ffe 0%, #5a9fff 100%);
  width: 100%;
}

.card-body {
  padding: 40px 36px 36px;
  width: 100%;
  box-sizing: border-box;
}

/* 标题区 */
.login-header {
  margin-bottom: 28px;
}
.login-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
}
.login-sub {
  margin: 0;
  font-size: 13px;
  color: #9ba0b0;
}

/* 输入框间距 */
.phone-input {
  margin-bottom: 12px;
}
.phone-prefix {
  font-size: 14px;
  color: #9ba0b0;
}
/* 验证码 + 发送按钮并排 */
.code-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.code-input {
  flex: 1;
  min-width: 0;
}
.send-btn {
  width: 110px;
  flex-shrink: 0;
}
.login-btn {
  width: 100%;
  height: 40px;
  font-size: 15px;
}
.bottom-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 16px;
}
.bottom-bar .el-button {
  color: #9ba0b0;
}

/* AppFooter 贴底 */
:deep(.app-footer) {
  width: 100%;
  margin-top: auto;
  padding-top: 50px;  /* 与上方 card 保持 50px 间距（移动端适配）*/
}

@media (max-width: 768px) {
  .login-page { padding-top: 52px; }
}

@media (max-width: 440px) {
  .login-card {
    width: 94vw;
    border-radius: 8px;
  }
  .card-body {
    padding: 28px 20px 24px;
  }
}
</style>
