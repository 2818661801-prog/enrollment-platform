<!--
  StudentLogin.vue · 学生手机验证码登录页（V2.0 · 复用 usePhoneCode）
  流程：输入手机号 → 发送验证码 → 填验证码 → 登录 → JWT 存 localStorage → 跳转 /my-applications
  关键：登录逻辑全部抽到 usePhoneCode composable，本文件只负责 UI
-->
<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 顶部 Logo 区 -->
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

      <!-- 发送验证码按钮 -->
      <el-button
        type="primary"
        class="send-btn"
        :disabled="phoneCode.countdown.value > 0"
        :loading="phoneCode.sending.value"
        @click="phoneCode.onSendCode"
      >
        {{ phoneCode.countdown.value > 0 ? `${phoneCode.countdown.value}秒后重发` : '发送验证码' }}
      </el-button>

      <!-- 验证码输入 + 登录（发码后才显示） -->
      <template v-if="phoneCode.codeSent.value">
        <el-input
          v-model="phoneCode.code.value"
          placeholder="请输入6位验证码"
          maxlength="6"
          class="code-input"
          @keyup.enter="onLogin"
        />

        <el-button
          type="primary"
          class="login-btn"
          :loading="phoneCode.logging.value"
          @click="onLogin"
        >
          登录
        </el-button>
      </template>

      <!-- 返回首页 -->
      <div class="back-home">
        <el-button text @click="goHome">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { usePhoneCode } from '../composables/usePhoneCode.js'

const router = useRouter()
const phoneCode = usePhoneCode()  // 共享的"发码+验证+登录"逻辑

/**
 * 登录成功后跳转
 */
async function onLogin() {
  const ok = await phoneCode.onLogin()
  if (ok) {
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
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #337ffe 0%, #6c8df5 100%);
}
.login-card {
  width: 360px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 32px 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 28px;
}
.login-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  color: #333;
}
.login-sub {
  margin: 0;
  font-size: 13px;
  color: #999;
}
.phone-input {
  margin-bottom: 12px;
}
.phone-prefix {
  font-size: 14px;
  color: #999;
}
.send-btn {
  width: 100%;
  margin-bottom: 20px;
}
.code-input {
  margin-bottom: 12px;
}
.login-btn {
  width: 100%;
  margin-top: 8px;
  height: 40px;
  font-size: 15px;
}
.back-home {
  margin-top: 20px;
  text-align: center;
}
.back-home .el-button {
  color: #999;
}
@media (max-width: 400px) {
  .login-card {
    width: 94vw;
    padding: 28px 20px 22px;
  }
}
</style>