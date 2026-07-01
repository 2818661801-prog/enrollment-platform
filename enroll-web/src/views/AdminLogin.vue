<!--
  AdminLogin.vue · 管理员登录页
  手机号 + 6位验证码 → JWT → 存 localStorage → 跳转 /admin
-->
<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="title">管理后台登录</h2>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="onSubmit">
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="手机号"
            maxlength="11"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="code">
          <el-input
            v-model="form.code"
            placeholder="验证码"
            maxlength="6"
            prefix-icon="Lock"
            style="flex:1"
            clearable
          >
            <template #append>
              <el-button
                @click="onSendCode"
                :disabled="cooldown > 0 || sending"
                style="min-width:80px"
              >
                {{ cooldown > 0 ? `${cooldown}s` : '发送' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width:100%"
            :loading="loading"
            :disabled="loading"
            @click="onSubmit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="tip">验证码发至服务器控制台，请查看控制台复制</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendLoginCode, verifyLoginCode } from '../utils/api.js'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const sending = ref(false)
const cooldown = ref(0)

const form = reactive({ phone: '', code: '' })

const rules = {
  phone: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请输入手机号'))
      : /^1\d{10}$/.test(v) ? cb() : cb(new Error('手机号格式不对')), trigger: 'blur' },
  ],
  code: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请输入验证码'))
      : /^\d{6}$/.test(v) ? cb() : cb(new Error('验证码6位数字')), trigger: 'blur' },
  ],
}

let cooldownTimer = null

function onSendCode() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  sending.value = true
  sendLoginCode(form.phone)
    .then(() => {
      ElMessage.success('验证码已发送（请查看服务器控制台）')
      cooldown.value = 60
      cooldownTimer = setInterval(() => {
        cooldown.value--
        if (cooldown.value <= 0) clearInterval(cooldownTimer)
      }, 1000)
    })
    .catch(() => ElMessage.error('发送失败'))
    .finally(() => { sending.value = false })
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    const res = await verifyLoginCode(form.phone, form.code)
    if (res.code !== 200) {
      ElMessage.error(res.message || '验证失败')
      return
    }
    localStorage.setItem('admin_token', res.data.token)
    localStorage.setItem('admin_phone', res.data.phone)
    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  width: 380px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.title {
  text-align: center;
  margin-bottom: 28px;
  font-size: 22px;
  color: #333;
}
.tip {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: -8px;
}
</style>
