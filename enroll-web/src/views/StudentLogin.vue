<!--
  StudentLogin.vue · 学生手机验证码登录页
  流程：输入手机号 → 发送验证码 → 填验证码 → 登录 → JWT 存 localStorage → 跳转 /my-applications
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
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="phoneForm.phone"
            placeholder="请输入手机号"
            maxlength="11"
            @keyup.enter="onSendCode"
          >
            <template #prefix>
              <span class="phone-prefix">+86</span>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <!-- 发送验证码按钮 -->
      <el-button
        type="primary"
        class="send-btn"
        :disabled="countdown > 0"
        :loading="sending"
        @click="onSendCode"
      >
        {{ countdown > 0 ? `${countdown}秒后重发` : '发送验证码' }}
      </el-button>

      <!-- 验证码输入 + 登录 -->
      <div v-if="codeSent" class="login-form">
        <el-form ref="codeFormRef" :model="codeForm" :rules="codeRules" label-position="top">
          <el-form-item label="验证码" prop="code">
            <el-input
              v-model="codeForm.code"
              placeholder="请输入6位验证码"
              maxlength="6"
              @keyup.enter="onLogin"
            />
          </el-form-item>
        </el-form>
        <el-button
          type="primary"
          class="login-btn"
          :loading="logging"
          @click="onLogin"
        >
          登录
        </el-button>
      </div>

      <!-- 返回首页 -->
      <div class="back-home">
        <el-button text @click="goHome">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

// ==================== 状态 ====================
const phoneFormRef = ref(null)
const codeFormRef  = ref(null)
const sending   = ref(false)
const logging   = ref(false)
const countdown = ref(0)
const codeSent  = ref(false)
let timer = null

// ==================== 手机号表单 ====================
const phoneForm = reactive({ phone: '' })
const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        /^1[3-9]\d{9}$/.test(v) ? cb() : cb(new Error('手机号格式不正确')),
      trigger: 'blur',
    },
  ],
}

// ==================== 验证码表单 ====================
const codeForm = reactive({ code: '' })
const codeRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        /^\d{6}$/.test(v) ? cb() : cb(new Error('验证码为6位数字')),
      trigger: 'blur',
    },
  ],
}

// ==================== 发送验证码 ====================
async function onSendCode() {
  try {
    await phoneFormRef.value.validate()
  } catch {
    return
  }

  sending.value = true
  try {
    const res = await fetch('/api/auth/send-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: phoneForm.phone }),
    })
    const data = await res.json()
    if (data.code !== 200) {
      ElMessage.error(data.message || '发送失败')
      return
    }
    ElMessage.success('验证码已发送')
    codeSent.value = true
    // 启动 60s 倒计时
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    sending.value = false
  }
}

// ==================== 登录 ====================
async function onLogin() {
  try {
    await codeFormRef.value.validate()
  } catch {
    return
  }

  logging.value = true
  try {
    const res = await fetch('/api/auth/login/sms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: phoneForm.phone, code: codeForm.code }),
    })
    const data = await res.json()
    if (data.code !== 200) {
      ElMessage.error(data.message || '登录失败')
      return
    }
    const { token, phone } = data.data
    // 存 localStorage
    localStorage.setItem('student_token', token)
    localStorage.setItem('student_phone', phone)
    ElMessage.success('登录成功')
    clearInterval(timer)
    router.push('/my-applications')
  } catch (e) {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    logging.value = false
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
:deep(.el-form-item__label) {
  font-weight: 500;
  padding-bottom: 4px !important;
}
.phone-prefix {
  font-size: 14px;
  color: #999;
}
.send-btn {
  width: 100%;
  margin-bottom: 20px;
}
.login-form {
  margin-top: 4px;
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
